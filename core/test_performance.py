"""
Page-load performance guards. Production talks to a remote Supabase pooler
where every query is a ~125-250ms network round trip, so the number of
queries a public page issues translates directly into seconds of load time.
These tests pin the query budgets won by caching the homepage sections and
the maintenance-mode flag, batching search counts, and check that the caches
never serve stale content after a listing or setting actually changes.
"""
from django.core.cache import cache
from django.db import connection
from django.test import TestCase
from django.test.utils import CaptureQueriesContext
from django.urls import reverse

from core.location_service import cached_active_city
from core.models import Business, Category, ListingStatus, Location, PlatformSettings
from core.templatetags.hk_extras import webp_sibling


def _business(name, category, **extra):
    return Business.objects.create(
        name=name, category=category, address='Main Road, Kuppam', phone_number='9876543210',
        status=ListingStatus.APPROVED, is_active=True, **extra,
    )


class HomepageCachingTests(TestCase):
    def setUp(self):
        cache.clear()
        _business('Sri Lakshmi Grocery', 'grocery')
        _business('Annapurna Restaurant', 'restaurant')

    def test_repeat_homepage_view_issues_no_database_queries(self):
        self.client.get(reverse('core:home'))
        with CaptureQueriesContext(connection) as queries:
            response = self.client.get(reverse('core:home'))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(queries), 0, [q['sql'] for q in queries])

    def test_new_listing_shows_on_homepage_immediately(self):
        self.client.get(reverse('core:home'))
        _business('Brand New Lakeside Park', 'tourism')
        self.assertContains(self.client.get(reverse('core:home')), 'Brand New Lakeside Park')

    def test_deleted_listing_leaves_homepage_immediately(self):
        doomed = _business('Soon Closing Viewpoint', 'tourism')
        self.assertContains(self.client.get(reverse('core:home')), 'Soon Closing Viewpoint')
        doomed.delete()
        self.assertNotContains(self.client.get(reverse('core:home')), 'Soon Closing Viewpoint')


class HomepageColdCacheTests(TestCase):
    """A cold cache (first visitor after the 60-120s TTL, per worker) used to
    cost ~48 sequential queries — ~45 of them the per-card category counts."""

    def setUp(self):
        cache.clear()
        for category in ('grocery', 'restaurant', 'hospital', 'school', 'repair', 'tourism', 'transport'):
            _business(f'Test {category}', category)

    def test_cold_homepage_stays_within_query_budget(self):
        with CaptureQueriesContext(connection) as queries:
            response = self.client.get(reverse('core:home'))
        self.assertEqual(response.status_code, 200)
        # ~26 today (was ~48): 7 section lists, 4 stats, the category tree, and
        # one aggregate per listing model instead of ~3 queries per card.
        self.assertLessEqual(len(queries), 30, [q['sql'][:120] for q in queries])

    def test_batched_category_counts_match_per_category_counts(self):
        categories = list(Category.objects.filter(parent=None, is_active=True))
        self.assertTrue(categories)
        batched = Category.public_listing_counts(categories, None)
        self.assertEqual(batched, {cat.pk: cat._compute_listing_count() for cat in categories})
        self.assertTrue(any(batched.values()))

    def test_batched_counts_scoped_to_city(self):
        city = Location.objects.create(kind=Location.Kind.CITY, name='Perf Town', slug='perf-town', country_code='IN')
        _business('City Scoped Diner', 'restaurant', city=city)
        categories = list(Category.objects.filter(parent=None, is_active=True))
        batched = Category.public_listing_counts(categories, city)
        self.assertEqual(batched, {cat.pk: cat._compute_listing_count(location=city) for cat in categories})
        self.assertEqual(sum(batched.values()), 1)


class ActiveCityCachingTests(TestCase):
    def setUp(self):
        cache.clear()
        self.city = Location.objects.create(kind=Location.Kind.CITY, name='Cache Town', slug='cache-town', country_code='IN')

    def test_city_is_read_from_cache_after_first_lookup(self):
        self.assertEqual(cached_active_city(self.city.pk), self.city)
        with CaptureQueriesContext(connection) as queries:
            self.assertEqual(cached_active_city(self.city.pk), self.city)
        self.assertEqual(len(queries), 0)

    def test_deactivating_a_city_takes_effect_immediately(self):
        cached_active_city(self.city.pk)
        self.city.is_active = False
        self.city.save()
        self.assertIsNone(cached_active_city(self.city.pk))

    def test_unknown_or_garbage_ids_are_none(self):
        self.assertIsNone(cached_active_city(999999))
        self.assertIsNone(cached_active_city('not-a-number'))


class MaintenanceModeCachingTests(TestCase):
    def setUp(self):
        cache.clear()

    def test_settings_row_is_not_requeried_on_every_page_view(self):
        self.client.get(reverse('core:about'))
        with CaptureQueriesContext(connection) as queries:
            self.client.get(reverse('core:about'))
        self.assertFalse(any('platformsettings' in q['sql'] for q in queries))

    def test_turning_maintenance_on_takes_effect_on_the_next_request(self):
        self.assertEqual(self.client.get(reverse('core:about')).status_code, 200)
        settings_obj = PlatformSettings.load()
        settings_obj.maintenance_mode = True
        settings_obj.maintenance_message = 'Back soon'
        settings_obj.save()
        self.assertEqual(self.client.get(reverse('core:about')).status_code, 503)
        settings_obj.maintenance_mode = False
        settings_obj.save()
        self.assertEqual(self.client.get(reverse('core:about')).status_code, 200)


class SearchCountBatchingTests(TestCase):
    def setUp(self):
        cache.clear()
        _business('Zenith Tiffin Centre', 'restaurant')
        _business('Zenith Bakery Corner', 'bakery')
        _business('Zenith Hardware Mart', 'hardware')
        _business('Unrelated Pharmacy', 'pharmacy')

    def test_business_section_counts_are_still_correct(self):
        response = self.client.get(reverse('core:search'), {'q': 'Zenith'})
        counts = {section['label']: section['count'] for section in response.context['results']}
        self.assertEqual(counts, {'Nearby Shops': 1, 'Restaurants & Food': 2})
        self.assertEqual(response.context['total_results'], 3)

    def test_business_directories_share_one_count_query(self):
        with CaptureQueriesContext(connection) as queries:
            self.client.get(reverse('core:search'), {'q': 'Zenith'})
        business_counts = [q for q in queries if 'core_business' in q['sql'] and 'COUNT(' in q['sql']]
        self.assertEqual(len(business_counts), 1, [q['sql'] for q in business_counts])


class WebpImageTests(TestCase):
    def test_webp_sibling_found_for_service_photos(self):
        self.assertEqual(webp_sibling('images/services/news.jpg'), 'images/services/news.webp')

    def test_webp_sibling_empty_when_no_twin_exists(self):
        self.assertEqual(webp_sibling('images/history/intro-landscape.jpg'), '')
        self.assertEqual(webp_sibling(''), '')

    def test_listing_hero_offers_webp_with_jpeg_fallback(self):
        response = self.client.get(reverse('core:job_list'))
        self.assertContains(response, 'image-set(')
        self.assertContains(response, 'images/services/jobs.')
        self.assertContains(response, ".webp') type('image/webp')")
