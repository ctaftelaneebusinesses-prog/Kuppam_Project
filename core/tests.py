"""
Web-view tests for the Phase 1 student-category discovery work (Tuition
Centers / Student Services / Buy-Sell-Exchange): each gets a real Category
row + dedicated directory URL, following the existing Repair Services /
Places to Visit pattern (see core/models.py's Category._BUSINESS_DIRECTORY_
KEYS/_BUSINESS_DIRECTORY_URL_NAMES and core/views.py's DIRECTORY_CATEGORIES).

There was previously no test coverage at all for core/views.py's
server-rendered pages (only core/api/ has tests) — this file is scoped
narrowly to the pages this change actually touched, not a general web-view
test suite.
"""
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse

from core.models import Business, Category, ListingStatus, LostFound, Scholarship


class StudentDirectoryPagesTests(TestCase):
    """Tuition Centers / Student Services / Buy-Sell-Exchange are reachable,
    correctly labeled, and correctly filtered — mirroring Repair Services /
    Places to Visit."""

    @classmethod
    def setUpTestData(cls):
        cls.tuition_business = Business.objects.create(
            name='Bright Minds Tuition Centre', category='tuition_center',
            address='Main Road, Kuppam', phone_number='9876543210',
            status=ListingStatus.APPROVED, is_active=True,
        )
        cls.student_services_business = Business.objects.create(
            name='Kuppam Student Print Shop', category='student_services',
            address='College Road, Kuppam', phone_number='9876543211',
            status=ListingStatus.APPROVED, is_active=True,
        )
        cls.marketplace_business = Business.objects.create(
            name='Kuppam Book Exchange', category='marketplace',
            address='Market Street, Kuppam', phone_number='9876543212',
            status=ListingStatus.APPROVED, is_active=True,
        )
        # A generic/unrelated business, to prove directory pages don't leak
        # every category and the general Businesses page excludes these three.
        cls.other_business = Business.objects.create(
            name='Sri Lakshmi Grocery', category='grocery',
            address='Bazaar Road, Kuppam', phone_number='9876543213',
            status=ListingStatus.APPROVED, is_active=True,
        )

    def test_migration_seeded_the_three_categories(self):
        for key, business_subcategory in [
            ('tuition_center', 'tuition_center'),
            ('student_services', 'student_services'),
            ('marketplace', 'marketplace'),
        ]:
            category = Category.objects.get(key=key)
            self.assertEqual(category.business_subcategory, business_subcategory)
            self.assertEqual(category.listing_model, 'business')
            self.assertTrue(category.is_active)

    def test_tuition_and_scholarships_are_sections_of_student_services(self):
        student_services = Category.objects.get(key='student_services')
        self.assertIsNone(student_services.parent_id)
        for key in ('tuition_center', 'scholarships'):
            self.assertEqual(Category.objects.get(key=key).parent, student_services)
        self.assertIsNone(Category.objects.get(key='marketplace').parent_id)

    def test_student_services_page_covers_tuition_centers(self):
        response = self.client.get(reverse('core:student_services_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.student_services_business.name)
        self.assertContains(response, self.tuition_business.name)
        self.assertNotContains(response, self.other_business.name)
        self.assertNotContains(response, self.marketplace_business.name)
        # Section tiles: Tuition, the renamed generic tile, and Scholarships.
        self.assertContains(response, 'Tuition &amp; Coaching Centers')
        self.assertContains(response, 'Hostels, Printing &amp; More')
        self.assertContains(response, f'href="{reverse("core:scholarship_list")}"')

    def test_student_services_tuition_filter(self):
        response = self.client.get(reverse('core:student_services_list'), {'type': 'tuition_center'})
        self.assertContains(response, self.tuition_business.name)
        self.assertNotContains(response, self.student_services_business.name)

    def test_old_tuition_centers_url_redirects_permanently(self):
        response = self.client.get('/tuition-centers/', {'q': 'maths'})
        self.assertEqual(response.status_code, 301)
        self.assertEqual(response['Location'], reverse('core:student_services_list') + '?q=maths&type=tuition_center')

    def test_marketplace_directory_page(self):
        response = self.client.get(reverse('core:marketplace_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Buy / Sell / Exchange')
        self.assertContains(response, self.marketplace_business.name)
        self.assertNotContains(response, self.other_business.name)

    def test_general_business_list_excludes_the_three_new_directories(self):
        response = self.client.get(reverse('core:business_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.other_business.name)
        self.assertNotContains(response, self.tuition_business.name)
        self.assertNotContains(response, self.student_services_business.name)
        self.assertNotContains(response, self.marketplace_business.name)

    def test_category_list_url_points_at_the_dedicated_directory(self):
        self.assertEqual(
            Category.objects.get(key='tuition_center').list_url,
            reverse('core:student_services_list') + '?type=tuition_center',
        )
        self.assertEqual(Category.objects.get(key='student_services').list_url, reverse('core:student_services_list'))
        self.assertEqual(Category.objects.get(key='marketplace').list_url, reverse('core:marketplace_list'))

    def test_menus_and_homepage_show_one_student_services_category(self):
        response = self.client.get(reverse('core:home'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, reverse('core:student_services_list'))
        self.assertContains(response, reverse('core:marketplace_list'))
        self.assertNotContains(response, '/tuition-centers/')
        self.assertNotContains(response, 'Tuition &amp; Coaching Centers')
        self.assertNotContains(response, f'href="{reverse("core:scholarship_list")}"')

    def test_search_category_redirect_covers_the_three_new_categories(self):
        for key, url_name in [
            ('tuition_center', 'core:student_services_list'),
            ('student_services', 'core:student_services_list'),
            ('marketplace', 'core:marketplace_list'),
        ]:
            response = self.client.get(reverse('core:search'), {'category': key})
            self.assertRedirects(response, reverse(url_name))


class StudentServicesSubmissionTests(TestCase):
    """A Content Provider granted Student Services can post into both of its
    sections (grants cover subcategories); one granted only Tuition can't
    post Scholarships."""

    def setUp(self):
        from django.contrib.auth import get_user_model
        from core.models import AdminCategoryPermission, Intent, Profile, UserRole
        user = get_user_model().objects.create_user(username='provider', email='provider@example.com', password='pass-12345!')
        self.profile = Profile.objects.create(
            user=user, role=UserRole.ADMIN, full_name='Provider', profile_completed=True, intent=Intent.UPLOAD,
        )
        self.profile.record_consent()
        self.grant = lambda key: AdminCategoryPermission.objects.create(admin=user, category=Category.objects.get(key=key))
        self.client.force_login(user)

    def test_student_services_grant_covers_tuition_and_scholarships(self):
        self.grant('student_services')
        for key in ('tuition_center', 'scholarships'):
            self.assertTrue(self.profile.can_manage_category(Category.objects.get(key=key)))
            response = self.client.get(reverse('core:listing_submit', args=[key]))
            self.assertEqual(response.status_code, 200, key)

    def test_tuition_grant_does_not_cover_scholarships(self):
        self.grant('tuition_center')
        self.assertTrue(self.profile.can_manage_category(Category.objects.get(key='tuition_center')))
        self.assertFalse(self.profile.can_manage_category(Category.objects.get(key='scholarships')))


class ScholarshipPagesTests(TestCase):
    """Phase 3: Scholarships & Government Schemes web pages."""

    @classmethod
    def setUpTestData(cls):
        cls.open_scholarship = Scholarship.objects.create(
            title='Merit Scholarship 2026', scholarship_type='merit', provider='State Education Board',
            status=ListingStatus.APPROVED, is_active=True,
        )
        cls.pending_scholarship = Scholarship.objects.create(
            title='Pending Scheme', scholarship_type='government', provider='Draft Provider',
            status=ListingStatus.PENDING, is_active=True,
        )

    def test_migration_seeded_the_category(self):
        category = Category.objects.get(key='scholarships')
        self.assertEqual(category.listing_model, 'scholarship')
        self.assertEqual(category.parent.key, 'student_services')
        self.assertTrue(category.is_active)

    def test_list_page_links_back_to_student_services(self):
        response = self.client.get(reverse('core:scholarship_list'))
        self.assertContains(response, f'href="{reverse("core:student_services_list")}"')
        detail = self.client.get(self.open_scholarship.get_absolute_url())
        self.assertContains(detail, f'href="{reverse("core:student_services_list")}"')

    def test_student_services_count_includes_scholarships(self):
        cache.clear()
        student_services = Category.objects.get(key='student_services')
        self.assertEqual(student_services.public_listing_count(None), 1)  # just the approved scholarship
        self.assertEqual(Category.public_listing_counts([student_services], None)[student_services.pk], 1)

    def test_list_page_shows_approved_hides_pending(self):
        response = self.client.get(reverse('core:scholarship_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.open_scholarship.title)
        self.assertNotContains(response, self.pending_scholarship.title)

    def test_detail_page(self):
        response = self.client.get(self.open_scholarship.get_absolute_url())
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.open_scholarship.title)
        self.assertContains(response, self.open_scholarship.provider)

    def test_pending_detail_page_404s_for_anonymous(self):
        response = self.client.get(self.pending_scholarship.get_absolute_url())
        self.assertEqual(response.status_code, 404)

    def test_no_official_url_means_no_application_button(self):
        self.assertEqual(self.open_scholarship.official_url, '')
        response = self.client.get(self.open_scholarship.get_absolute_url())
        self.assertNotContains(response, 'Official Application Page')

    def test_category_list_url_points_at_the_dedicated_directory(self):
        self.assertEqual(Category.objects.get(key='scholarships').list_url, reverse('core:scholarship_list'))


class LostFoundPagesTests(TestCase):
    """Phase 3: Lost & Found web pages."""

    @classmethod
    def setUpTestData(cls):
        cls.lost_item = LostFound.objects.create(
            report_type='lost', title='Lost black wallet', item_category='bag_wallet', location='Main Road',
            status=ListingStatus.APPROVED, is_active=True,
        )
        cls.pending_item = LostFound.objects.create(
            report_type='found', title='Pending Found Report', item_category='keys', location='Market',
            status=ListingStatus.PENDING, is_active=True,
        )

    def test_migration_seeded_the_category(self):
        category = Category.objects.get(key='lost-found')
        self.assertEqual(category.listing_model, 'lostfound')
        self.assertIsNone(category.parent_id)

    def test_list_page_shows_approved_hides_pending(self):
        response = self.client.get(reverse('core:lost_found_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.lost_item.title)
        self.assertNotContains(response, self.pending_item.title)

    def test_detail_page(self):
        response = self.client.get(self.lost_item.get_absolute_url())
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.lost_item.title)

    def test_no_contact_number_shows_comments_hint_not_a_fake_number(self):
        self.assertEqual(self.lost_item.contact_number, '')
        response = self.client.get(self.lost_item.get_absolute_url())
        self.assertContains(response, "hasn't shared a phone number")

    def test_type_filter(self):
        response = self.client.get(reverse('core:lost_found_list'), {'type': 'lost'})
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.lost_item.title)

    def test_category_list_url_points_at_the_dedicated_directory(self):
        self.assertEqual(Category.objects.get(key='lost-found').list_url, reverse('core:lost_found_list'))


class CategoryPageHeroTests(TestCase):
    """Listing-page banners: text overlay on plain photos, scene hook per page."""

    def test_transport_hero_draws_visible_title_overlay(self):
        # transport.jpg has no baked-in title, so the overlay h1 must render
        # (styled white via .hk-lp-hero-title, not the global dark h1).
        response = self.client.get(reverse('core:transport_list'))
        self.assertContains(response, 'class="hk-lp-hero-title"')
        self.assertContains(response, 'data-category-bg="transport"')

    def test_hospitals_hero_draws_title_over_darkened_text_baked_photo(self):
        # hospitals.jpg has a title printed in it: our overlay still renders
        # (same hero as every page), over a heavier scrim.
        response = self.client.get(reverse('core:hospital_list'))
        self.assertContains(response, 'class="hk-lp-hero-title"')
        self.assertContains(response, 'hk-lp-hero--baked')
        self.assertContains(response, 'data-category-bg="health"')

    def test_jobs_page_opts_into_its_illustration_scene(self):
        response = self.client.get(reverse('core:job_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'data-category-bg="jobs"')
        self.assertContains(response, 'class="hk-lp-hero-title"')

    def test_business_page_opts_into_shops_scene(self):
        response = self.client.get(reverse('core:business_list'))
        self.assertContains(response, 'data-category-bg="business"')

    def test_restaurants_page_opts_into_its_illustration_scene(self):
        response = self.client.get(reverse('core:restaurant_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'data-category-bg="restaurant"')

    def test_repair_and_places_pages_opt_into_their_scenes(self):
        self.assertContains(self.client.get(reverse('core:repair_list')), 'data-category-bg="repair"')
        self.assertContains(self.client.get(reverse('core:places_to_visit_list')), 'data-category-bg="tourism"')

    def test_news_uses_village_scene_only_for_village_happenings_filter(self):
        self.assertContains(self.client.get(reverse('core:news_list')), 'data-category-bg="news"')
        response = self.client.get(reverse('core:news_list'), {'category': 'village-happenings'})
        self.assertContains(response, 'data-category-bg="village"')

    def test_village_happenings_links_to_its_own_filtered_page(self):
        village = Category.objects.get(key='village-happenings')
        news = Category.objects.get(key='news')
        self.assertEqual(village.list_url, reverse('core:news_list') + '?category=village-happenings')
        self.assertEqual(news.list_url, reverse('core:news_list'))

    def test_news_hero_uses_the_selected_categorys_photo(self):
        Category.objects.filter(key='village-happenings').update(image='images/services/village-happenings.jpg')
        cache.clear()  # .update() skips the signal that clears the cached news categories
        village = self.client.get(reverse('core:news_list'), {'category': 'village-happenings'})
        self.assertContains(village, 'images/services/village-happenings.')
        self.assertNotContains(village, 'images/services/news.')
        self.assertContains(village, '<h1 id="hk-lp-hero-title" class="hk-lp-hero-title">What&#x27;s Happening in Your Village</h1>', html=False)
        self.assertContains(self.client.get(reverse('core:news_list')), 'images/services/news.')

    def test_marketplace_has_its_own_photo_not_the_shops_storefront(self):
        self.assertEqual(Category.objects.get(key='marketplace').image, 'images/services/marketplace.jpg')
        response = self.client.get(reverse('core:marketplace_list'))
        self.assertContains(response, 'images/services/marketplace.')
        self.assertNotContains(response, 'images/services/shops.')

    def test_students_marketplace_pages_opt_into_their_scenes(self):
        self.assertContains(self.client.get(reverse('core:student_services_list')), 'data-category-bg="students"')
        self.assertContains(self.client.get(reverse('core:marketplace_list')), 'data-category-bg="marketplace"')
