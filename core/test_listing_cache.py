"""Speed work on public pages: listing pages and detail pages are served from
cache on repeat views (the database is a remote region away, so every query
costs hundreds of ms), without ever showing stale listings after a change."""
from django.contrib.auth import get_user_model
from django.contrib.contenttypes.models import ContentType
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse

from core.models import Category, Comment, Job, ListingStatus, PostView, Property

PUBLIC = {'status': ListingStatus.APPROVED, 'is_active': True}


def _job(title, **extra):
    return Job.objects.create(
        job_title=title, company='Kuppam Motors', location='Main Road', contact_number='9876543210',
        **PUBLIC, **extra,
    )


class ListingPageCacheTests(TestCase):
    def setUp(self):
        cache.clear()
        self.job = _job('Site Supervisor')

    def test_repeat_listing_page_view_issues_no_queries(self):
        self.client.get(reverse('core:job_list'))
        with self.assertNumQueries(0):
            response = self.client.get(reverse('core:job_list'))
        self.assertContains(response, 'Site Supervisor')

    def test_new_listing_shows_on_the_next_view(self):
        self.client.get(reverse('core:job_list'))
        _job('Weekend Helper')
        self.assertContains(self.client.get(reverse('core:job_list')), 'Weekend Helper')

    def test_removed_listing_leaves_on_the_next_view(self):
        self.client.get(reverse('core:job_list'))
        self.job.is_active = False
        self.job.save()
        self.assertNotContains(self.client.get(reverse('core:job_list')), 'Site Supervisor')

    def test_filters_and_pages_are_cached_separately(self):
        _job('Weekend Helper', job_type='hourly')
        self.client.get(reverse('core:job_list'))
        hourly = self.client.get(reverse('core:job_list'), {'type': 'hourly'})
        self.assertContains(hourly, 'Weekend Helper')
        self.assertNotContains(hourly, 'Site Supervisor')

    def test_page_numbers_survive_the_cache(self):
        for i in range(13):
            _job(f'Role {i}')
        self.client.get(reverse('core:job_list'), {'page': '2'})
        response = self.client.get(reverse('core:job_list'), {'page': '2'})
        self.assertEqual(response.context['page_obj'].number, 2)
        self.assertEqual(response.context['page_obj'].paginator.num_pages, 2)
        self.assertEqual(len(response.context['page_obj'].object_list), 2)


class DetailPageCacheTests(TestCase):
    def setUp(self):
        cache.clear()
        self.job = _job('Site Supervisor')
        self.url = self.job.get_absolute_url()

    def test_repeat_detail_view_issues_no_queries(self):
        self.client.get(self.url)
        with self.assertNumQueries(0):
            self.client.get(self.url)

    def test_anonymous_view_counted_once_without_creating_a_session(self):
        self.client.get(self.url)
        self.client.get(self.url)
        self.job.refresh_from_db()
        self.assertEqual(self.job.view_count, 1)
        self.assertEqual(PostView.objects.filter(object_id=self.job.pk).count(), 1)
        self.assertNotIn('sessionid', self.client.cookies)

    def test_new_comment_shows_despite_the_cache(self):
        self.client.get(self.url)
        user = get_user_model().objects.create_user(username='reader', password='pw-12345-long')
        Comment.objects.create(
            content_type=ContentType.objects.get_for_model(Job), object_id=self.job.pk,
            user=user, body='Is this still open?',
        )
        self.assertContains(self.client.get(self.url), 'Is this still open?')

    def test_unknown_slug_is_404(self):
        self.assertEqual(self.client.get(reverse('core:job_detail', args=['no-such-job'])).status_code, 404)


class ResponsiveImageTests(TestCase):
    def test_listing_hero_is_a_prioritised_img_with_small_variants(self):
        response = self.client.get(reverse('core:business_list'))
        self.assertContains(response, 'fetchpriority="high"')
        self.assertContains(response, 'business-640')
        self.assertContains(response, 'business-1280')

    def test_homepage_category_cards_use_the_640px_variant(self):
        # Category.image is set by Super Admins in production; the seeded
        # test rows have none, so give one its stock photo.
        # Only categories with listings are shown.
        Category.objects.filter(key='property').update(image='images/services/real-estate.jpg')
        Property.objects.create(title='Corner Plot', property_type='plot', price=1500000, location='Main Road',
                                contact_number='9876543210', **PUBLIC)
        cache.clear()
        response = self.client.get(reverse('core:home'))
        self.assertContains(response, 'real-estate-640.')
        self.assertContains(response, ' 640w')
