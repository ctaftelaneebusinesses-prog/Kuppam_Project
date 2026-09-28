"""Shared category-listing layout (listing_page_base.html): every listing page
gets the photo hero + search panel, real per-type tile counts that link to
working filters, a results heading, filter-preserving pagination and the
photo-first cards."""
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse

from core.models import Business, Job, ListingStatus, LostFound, Project, Scholarship
from core.templatetags.hk_extras import choice_tint, tint_index

PUBLIC = {'status': ListingStatus.APPROVED, 'is_active': True}


def _job(title, job_type='regular'):
    return Job.objects.create(
        job_title=title, company='Kuppam Motors', job_type=job_type, location='Main Road',
        contact_number='9876543210', **PUBLIC,
    )


def _tiles(response):
    return {tile['value']: tile for tile in response.context['type_tiles']}


class ListingLayoutTests(TestCase):
    def setUp(self):
        cache.clear()

    def test_every_listing_page_uses_the_shared_layout(self):
        for name in ['business_list', 'restaurant_list', 'transport_list', 'property_list', 'job_list',
                     'event_list', 'news_list', 'project_list', 'scholarship_list', 'lost_found_list']:
            with self.subTest(page=name):
                response = self.client.get(reverse(f'core:{name}'))
                self.assertEqual(response.status_code, 200)
                self.assertTemplateUsed(response, 'listing_page_base.html')
                self.assertContains(response, 'class="hk-lp-search"')
                self.assertContains(response, 'class="hk-lp-results-head"')

    def test_every_hero_draws_its_title_eyebrow_and_count(self):
        # Including the photos with a title printed in them (restaurants,
        # hospitals, education, events, news, projects), which get a heavier
        # scrim instead of losing the copy.
        Business.objects.create(name='Annapurna Mess', category='restaurant', address='Main Road',
                                phone_number='9876543210', **PUBLIC)
        for name in ['restaurant_list', 'hospital_list', 'education_list', 'event_list', 'news_list', 'project_list']:
            with self.subTest(page=name):
                response = self.client.get(reverse(f'core:{name}'))
                self.assertContains(response, 'hk-lp-hero--baked')
                self.assertContains(response, '<h1 id="hk-lp-hero-title" class="hk-lp-hero-title">', html=False)
                self.assertContains(response, 'class="hk-lp-eyebrow"')
        self.assertContains(self.client.get(reverse('core:restaurant_list')), '1 place listed')
        # Projects' printed title fills the photo's top half, so the banner
        # is anchored to the bottom of the photo instead.
        self.assertContains(self.client.get(reverse('core:project_list')), 'background-position:center bottom')

    def test_empty_page_does_not_claim_featured_listings(self):
        response = self.client.get(reverse('core:lost_found_list'))
        self.assertContains(response, 'No reports found')
        self.assertNotContains(response, 'featured listings first')


class TypeTileTests(TestCase):
    def setUp(self):
        cache.clear()

    def test_job_tiles_count_and_filter(self):
        _job('Site Supervisor')
        hourly = _job('Weekend Helper', 'hourly')
        response = self.client.get(reverse('core:job_list'))
        tiles = _tiles(response)
        self.assertEqual(tiles['regular']['count'], 1)
        self.assertEqual(tiles['hourly']['count'], 1)
        self.assertEqual(tiles['hourly']['url'], '?type=hourly')
        self.assertContains(response, '2 open jobs')

        filtered = self.client.get(reverse('core:job_list'), {'type': 'hourly'})
        self.assertContains(filtered, hourly.job_title)
        self.assertNotContains(filtered, 'Site Supervisor')
        self.assertTrue(_tiles(filtered)['hourly']['is_active'])
        # The active tile links back to the unfiltered page.
        self.assertEqual(_tiles(filtered)['hourly']['url'], reverse('core:job_list'))

    def test_tile_links_keep_the_search_query(self):
        _job('Site Supervisor')
        response = self.client.get(reverse('core:job_list'), {'q': 'site', 'page': '1'})
        self.assertEqual(_tiles(response)['hourly']['url'], '?q=site&type=hourly')

    def test_project_status_tiles_and_filter(self):
        Project.objects.create(title='Bypass Road', location='NH', project_status='ongoing', **PUBLIC)
        Project.objects.create(title='Town Hall', location='Centre', project_status='completed', **PUBLIC)
        tiles = _tiles(self.client.get(reverse('core:project_list')))
        self.assertEqual((tiles['planned']['count'], tiles['ongoing']['count'], tiles['completed']['count']), (0, 1, 1))

        filtered = self.client.get(reverse('core:project_list'), {'status': 'ongoing'})
        self.assertContains(filtered, 'Bypass Road')
        self.assertNotContains(filtered, 'Town Hall')

    def test_unknown_status_is_ignored(self):
        Project.objects.create(title='Bypass Road', location='NH', project_status='ongoing', **PUBLIC)
        response = self.client.get(reverse('core:project_list'), {'status': 'bogus'})
        self.assertContains(response, 'Bypass Road')
        self.assertFalse(response.context['filters_active'])

    def test_lost_found_tiles(self):
        LostFound.objects.create(report_type='lost', title='Black wallet', location='Bus stand', **PUBLIC)
        tiles = _tiles(self.client.get(reverse('core:lost_found_list')))
        self.assertEqual(tiles['lost']['count'], 1)
        self.assertEqual(tiles['found']['count'], 0)

    def test_general_business_page_only_tiles_categories_with_listings(self):
        # More than TYPE_TILE_LIMIT categories, so empty ones are left out.
        Business.objects.create(name='Sri Salon', category='salon', address='Main Road', phone_number='9876543210', **PUBLIC)
        tiles = _tiles(self.client.get(reverse('core:business_list')))
        self.assertEqual(list(tiles), ['salon'])

    def test_multi_category_directory_has_tiles_single_category_has_none(self):
        Business.objects.create(name='Hot Bakery', category='bakery', address='Main Road', phone_number='9876543210', **PUBLIC)
        self.assertEqual(set(_tiles(self.client.get(reverse('core:restaurant_list')))), {'restaurant', 'bakery'})
        self.assertEqual(self.client.get(reverse('core:transport_list')).context['type_tiles'], [])

    def test_counts_refresh_after_a_new_listing(self):
        self.client.get(reverse('core:scholarship_list'))
        Scholarship.objects.create(title='Merit Award', scholarship_type='merit', provider='State Board', **PUBLIC)
        tiles = _tiles(self.client.get(reverse('core:scholarship_list')))
        self.assertEqual(tiles['merit']['count'], 1)


class PaginationTests(TestCase):
    def test_page_links_keep_the_active_filter(self):
        cache.clear()
        for i in range(13):
            _job(f'Hourly Role {i}', 'hourly')
        response = self.client.get(reverse('core:job_list'), {'type': 'hourly'})
        self.assertContains(response, 'href="?type=hourly&amp;page=2"', html=False)


class CardTintTests(TestCase):
    def test_card_pill_and_tile_share_a_color(self):
        job = Job(job_type='hourly')
        self.assertEqual(choice_tint(job, 'job_type'), tint_index(Job, 'job_type', 'hourly'))
        self.assertEqual(tint_index(Job, 'job_type', 'hourly'), 1)
        self.assertEqual(tint_index(Job, 'job_type', 'not-a-type'), 0)
