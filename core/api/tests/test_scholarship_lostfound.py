"""
API tests specific to the two Phase 3 listing types (Scholarship, LostFound)
— generic list/detail/model_key dispatch is already covered for every
listing type (this pair included) by test_listings.AllListingTypesSmokeTests
via factories.MAKERS; this file covers behavior specific to these two:
city filtering, ownership/edit/delete permissions, moderation visibility,
and LostFound's "no future date" validation.
"""
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import ListingStatus, LostFound, Scholarship, UserRole

from . import factories as f


class ScholarshipListTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.other_city = f.make_city('Chittoor')
        self.owner, _ = f.make_user(role=UserRole.ADMIN)
        self.approved = f.make_scholarship(owner=self.owner, city=self.city, status=ListingStatus.APPROVED)
        self.pending = f.make_scholarship(owner=self.owner, city=self.city, status=ListingStatus.PENDING)
        self.other_city_scholarship = f.make_scholarship(owner=self.owner, city=self.other_city, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:listing_collection', args=['scholarship'])

    def test_public_list_excludes_pending(self):
        response = self.client.get(self.url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        titles = {item['title'] for item in response.data['results']}
        self.assertIn(self.approved.title, titles)
        self.assertNotIn(self.pending.title, titles)

    def test_city_filter(self):
        response = self.client.get(self.url(), {'city': self.city.slug})
        titles = {item['title'] for item in response.data['results']}
        self.assertIn(self.approved.title, titles)
        self.assertNotIn(self.other_city_scholarship.title, titles)

    def test_detail_includes_is_open(self):
        response = self.client.get(reverse('api:listing_detail', args=['scholarship', self.approved.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('is_open', response.data)
        self.assertTrue(response.data['is_open'])  # no deadline set -> always open

    def test_official_url_is_whatever_was_submitted_never_invented(self):
        scholarship = f.make_scholarship(owner=self.owner, city=self.city)
        self.assertEqual(scholarship.official_url, '')  # factory never sets one — must stay blank, not auto-filled
        response = self.client.get(reverse('api:listing_detail', args=['scholarship', scholarship.pk]))
        self.assertEqual(response.data['official_url'], '')


class ScholarshipPermissionTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, _ = f.make_user(role=UserRole.ADMIN)
        self.other_user, _ = f.make_user(role=UserRole.ADMIN)
        self.super_admin, _ = f.make_user(super_admin=True)
        self.scholarship = f.make_scholarship(owner=self.owner, city=self.city)

    def detail_url(self, obj):
        return reverse('api:listing_detail', args=['scholarship', obj.pk])

    def test_owner_can_edit(self):
        # PATCH re-validates the whole ModelForm (see core.api.views.listing_detail's
        # own comment) — same as the website's edit form — so every required
        # field must be present, not just the ones actually changing.
        self.client.force_authenticate(self.owner)
        response = self.client.patch(self.detail_url(self.scholarship), {
            'title': 'Updated Title', 'scholarship_type': 'merit', 'provider': 'New Provider', 'city': self.city.pk,
        }, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)

    def test_non_owner_cannot_edit(self):
        self.client.force_authenticate(self.other_user)
        response = self.client.patch(self.detail_url(self.scholarship), {'title': 'Hijacked'}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_non_owner_cannot_delete(self):
        self.client.force_authenticate(self.other_user)
        response = self.client.delete(self.detail_url(self.scholarship))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_owner_can_delete(self):
        self.client.force_authenticate(self.owner)
        response = self.client.delete(self.detail_url(self.scholarship))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Scholarship.objects.filter(pk=self.scholarship.pk).exists())

    def test_super_admin_can_delete_any(self):
        self.client.force_authenticate(self.super_admin)
        response = self.client.delete(self.detail_url(self.scholarship))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

    def test_anonymous_cannot_edit(self):
        response = self.client.patch(self.detail_url(self.scholarship), {'title': 'x'}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class LostFoundListTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.other_city = f.make_city('Chittoor')
        self.owner, _ = f.make_user(role=UserRole.ADMIN)
        self.lost_item = f.make_lostfound(owner=self.owner, city=self.city, report_type='lost', status=ListingStatus.APPROVED)
        self.found_item = f.make_lostfound(owner=self.owner, city=self.city, report_type='found', status=ListingStatus.APPROVED)
        self.pending_item = f.make_lostfound(owner=self.owner, city=self.city, status=ListingStatus.PENDING)
        self.other_city_item = f.make_lostfound(owner=self.owner, city=self.other_city, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:listing_collection', args=['lostfound'])

    def test_public_list_excludes_pending(self):
        response = self.client.get(self.url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        titles = {item['title'] for item in response.data['results']}
        self.assertIn(self.lost_item.title, titles)
        self.assertIn(self.found_item.title, titles)
        self.assertNotIn(self.pending_item.title, titles)

    def test_city_filter(self):
        response = self.client.get(self.url(), {'city': self.city.slug})
        titles = {item['title'] for item in response.data['results']}
        self.assertIn(self.lost_item.title, titles)
        self.assertNotIn(self.other_city_item.title, titles)

    def test_contact_number_blank_by_default(self):
        """Reporter never gets a fabricated contact number — blank stays blank end to end."""
        response = self.client.get(reverse('api:listing_detail', args=['lostfound', self.lost_item.pk]))
        self.assertEqual(response.data['contact_number'], '')

    def test_owner_account_phone_never_exposed_via_owner_field(self):
        """The generic owner field only ever carries PublicOwnerSerializer-safe data — never the reporter's account phone number."""
        response = self.client.get(reverse('api:listing_detail', args=['lostfound', self.lost_item.pk]))
        owner_payload = response.data.get('owner') or {}
        self.assertNotIn('phone_number', owner_payload)


class LostFoundValidationTests(APITestCase):
    """Exercises LostFoundSubmitForm through the generic create endpoint."""

    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, self.owner_profile = f.make_user(role=UserRole.ADMIN)
        self.category = f.make_category(listing_model='lostfound', key='lost-found-test')
        f.grant_category(self.owner, self.category)

    def create_url(self):
        return reverse('api:listing_collection', args=['lostfound'])

    def test_future_event_date_is_rejected(self):
        self.client.force_authenticate(self.owner)
        future_date = (timezone.localdate() + timezone.timedelta(days=5)).isoformat()
        response = self.client.post(self.create_url(), {
            'category_key': self.category.key,
            'report_type': 'lost',
            'title': 'Lost umbrella',
            'item_category': 'other',
            'city': self.city.pk,
            'event_date': future_date,
            'location': 'Bus stand',
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_valid_submission_is_created_pending(self):
        self.client.force_authenticate(self.owner)
        response = self.client.post(self.create_url(), {
            'category_key': self.category.key,
            'report_type': 'found',
            'title': 'Found keys',
            'item_category': 'keys',
            'city': self.city.pk,
            'event_date': timezone.localdate().isoformat(),
            'location': 'Market Street',
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        created = LostFound.objects.get(title='Found keys')
        self.assertEqual(created.owner, self.owner)
        # ADMIN role's own submissions still land in the moderation queue, same as every other listing type.
        self.assertEqual(created.status, ListingStatus.PENDING)

    def test_unauthenticated_cannot_submit(self):
        response = self.client.post(self.create_url(), {
            'category_key': self.category.key,
            'report_type': 'lost',
            'title': 'x',
            'event_date': timezone.localdate().isoformat(),
            'location': 'x',
        })
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
