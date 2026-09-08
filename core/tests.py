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
            self.assertIsNone(category.parent_id)

    def test_tuition_centers_directory_page(self):
        response = self.client.get(reverse('core:tuition_center_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Tuition &amp; Coaching Centers')
        self.assertContains(response, self.tuition_business.name)
        self.assertNotContains(response, self.other_business.name)
        self.assertNotContains(response, self.marketplace_business.name)

    def test_student_services_directory_page(self):
        response = self.client.get(reverse('core:student_services_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Student Services')
        self.assertContains(response, self.student_services_business.name)
        self.assertNotContains(response, self.other_business.name)

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
        self.assertEqual(Category.objects.get(key='tuition_center').list_url, reverse('core:tuition_center_list'))
        self.assertEqual(Category.objects.get(key='student_services').list_url, reverse('core:student_services_list'))
        self.assertEqual(Category.objects.get(key='marketplace').list_url, reverse('core:marketplace_list'))

    def test_homepage_links_to_the_dedicated_directories_not_the_general_page(self):
        response = self.client.get(reverse('core:home'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, reverse('core:tuition_center_list'))
        self.assertContains(response, reverse('core:student_services_list'))
        self.assertContains(response, reverse('core:marketplace_list'))

    def test_search_category_redirect_covers_the_three_new_categories(self):
        for key, url_name in [
            ('tuition_center', 'core:tuition_center_list'),
            ('student_services', 'core:student_services_list'),
            ('marketplace', 'core:marketplace_list'),
        ]:
            response = self.client.get(reverse('core:search'), {'category': key})
            self.assertRedirects(response, reverse(url_name))


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
        self.assertIsNone(category.parent_id)
        self.assertTrue(category.is_active)

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
