from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import ListingStatus, UserRole

from . import factories as f


class ListingListTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.other_city = f.make_city('Chittoor')
        self.owner, self.owner_profile = f.make_user(role=UserRole.ADMIN)
        self.approved = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED, is_active=True, name='Approved Biz')
        self.pending = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.PENDING, is_active=True, name='Pending Biz')
        self.rejected = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.REJECTED, is_active=True, name='Rejected Biz')
        self.inactive = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED, is_active=False, name='Inactive Biz')
        self.other_city_biz = f.make_business(owner=self.owner, city=self.other_city, status=ListingStatus.APPROVED, name='Other City Biz')

    def url(self):
        return reverse('api:listing_collection', args=['business'])

    def test_public_list_only_shows_approved_and_active(self):
        response = self.client.get(self.url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        names = {item['name'] for item in response.data['results']}
        self.assertIn('Approved Biz', names)
        self.assertNotIn('Pending Biz', names)
        self.assertNotIn('Rejected Biz', names)
        self.assertNotIn('Inactive Biz', names)

    def test_public_list_response_envelope(self):
        response = self.client.get(self.url())
        for key in ('count', 'next', 'previous', 'page_size', 'results'):
            self.assertIn(key, response.data)

    def test_city_filter(self):
        response = self.client.get(self.url(), {'city': self.city.slug})
        names = {item['name'] for item in response.data['results']}
        self.assertNotIn('Other City Biz', names)

    def test_empty_results_for_unmatched_query(self):
        response = self.client.get(self.url(), {'q': 'no-such-listing-xyz'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 0)
        self.assertEqual(response.data['results'], [])

    def test_pagination_page_size(self):
        for _ in range(25):
            f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED)
        response = self.client.get(self.url(), {'page_size': 10})
        self.assertEqual(len(response.data['results']), 10)
        self.assertIsNotNone(response.data['next'])

    def test_mine_requires_authentication(self):
        response = self.client.get(self.url(), {'mine': 'true'})
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_mine_shows_owner_non_public_listings(self):
        self.client.force_authenticate(self.owner)
        response = self.client.get(self.url(), {'mine': 'true'})
        names = {item['name'] for item in response.data['results']}
        self.assertIn('Pending Biz', names)
        self.assertIn('Rejected Biz', names)
        self.assertIn('Inactive Biz', names)

    def test_unknown_model_key_is_404(self):
        response = self.client.get(reverse('api:listing_collection', args=['tuition']))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertIn('error', response.data)

    def test_blocked_user_cannot_list(self):
        blocked_user, _ = f.make_user(blocked=True)
        self.client.force_authenticate(blocked_user)
        response = self.client.get(self.url())
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class ListingDetailVisibilityTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, self.owner_profile = f.make_user(role=UserRole.ADMIN)
        self.other_user, self.other_profile = f.make_user()
        self.super_admin, self.super_profile = f.make_user(super_admin=True)
        self.approved = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED)
        self.pending = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.PENDING)
        self.rejected = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.REJECTED)
        self.inactive = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED, is_active=False)

    def detail_url(self, obj):
        return reverse('api:listing_detail', args=['business', obj.pk])

    def test_anonymous_can_view_approved(self):
        response = self.client.get(self.detail_url(self.approved))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['id'], self.approved.pk)

    def test_anonymous_cannot_view_pending(self):
        response = self.client.get(self.detail_url(self.pending))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_anonymous_cannot_view_rejected(self):
        response = self.client.get(self.detail_url(self.rejected))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_anonymous_cannot_view_inactive(self):
        response = self.client.get(self.detail_url(self.inactive))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_non_owner_cannot_view_pending(self):
        self.client.force_authenticate(self.other_user)
        response = self.client.get(self.detail_url(self.pending))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_owner_can_view_own_pending(self):
        self.client.force_authenticate(self.owner)
        response = self.client.get(self.detail_url(self.pending))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_super_admin_can_view_any_status(self):
        self.client.force_authenticate(self.super_admin)
        for obj in (self.pending, self.rejected, self.inactive):
            response = self.client.get(self.detail_url(obj))
            self.assertEqual(response.status_code, status.HTTP_200_OK, obj)

    def test_invalid_id_is_404(self):
        response = self.client.get(reverse('api:listing_detail', args=['business', 999999]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_non_numeric_id_is_404(self):
        response = self.client.get('/api/v1/listings/business/not-a-number/')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_owner_field_never_exposes_private_data(self):
        response = self.client.get(self.detail_url(self.approved))
        owner_payload = response.data['owner']
        self.assertEqual(set(owner_payload.keys()), {'id', 'full_name', 'display_photo'})

    def test_view_count_increments_once_per_session(self):
        before = self.approved.view_count
        self.client.get(self.detail_url(self.approved))
        self.client.get(self.detail_url(self.approved))
        self.approved.refresh_from_db()
        self.assertEqual(self.approved.view_count, before + 1)


class ListingCreateTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.category = f.make_category('business', key='retail-cat')
        self.admin, self.admin_profile = f.make_user(role=UserRole.ADMIN)
        f.grant_category(self.admin, self.category)
        self.super_admin, _ = f.make_user(super_admin=True)
        self.suspended_admin, _ = f.make_user(role=UserRole.ADMIN, suspended=True)
        f.grant_category(self.suspended_admin, self.category)
        self.plain_user, _ = f.make_user()
        self.unpermitted_admin, _ = f.make_user(role=UserRole.ADMIN)  # no grant on self.category

    def url(self):
        return reverse('api:listing_collection', args=['business'])

    def payload(self, **overrides):
        data = {
            'name': 'New Shop', 'category': 'retail', 'city': self.city.pk, 'address': '1 Market Rd',
            'phone_number': '9876543210', 'category_key': self.category.key,
            # LocationFieldsMixin (core/forms.py) requires either a captured
            # lat/lng or a Google Maps link — this satisfies that shared
            # cross-listing-type validation without a real GPS fix.
            'maps_link': 'https://www.google.com/maps?q=13.1631,78.2288',
        }
        data.update(overrides)
        return data

    def test_unauthenticated_cannot_create(self):
        response = self.client.post(self.url(), self.payload())
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_plain_user_without_category_permission_forbidden(self):
        self.client.force_authenticate(self.plain_user)
        response = self.client.post(self.url(), self.payload())
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_admin_without_grant_on_this_category_forbidden(self):
        self.client.force_authenticate(self.unpermitted_admin)
        response = self.client.post(self.url(), self.payload())
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_suspended_admin_cannot_create(self):
        self.client.force_authenticate(self.suspended_admin)
        response = self.client.post(self.url(), self.payload())
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_admin_submission_is_pending_and_notifies(self):
        self.client.force_authenticate(self.admin)
        response = self.client.post(self.url(), self.payload())
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data['status'], 'pending')
        self.assertEqual(response.data['owner']['id'], self.admin.id)

    def test_super_admin_submission_is_auto_approved(self):
        self.client.force_authenticate(self.super_admin)
        response = self.client.post(self.url(), self.payload())
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['status'], 'approved')

    def test_missing_category_key_is_validation_error(self):
        self.client.force_authenticate(self.admin)
        payload = self.payload()
        del payload['category_key']
        response = self.client.post(self.url(), payload)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('category_key', response.data['error']['details'])

    def test_invalid_form_field_is_validation_error(self):
        self.client.force_authenticate(self.admin)
        response = self.client.post(self.url(), self.payload(name=''))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('name', response.data['error']['details'])

    def test_wrong_listing_model_category_rejected(self):
        job_category = f.make_category('job', key='job-cat')
        self.client.force_authenticate(self.admin)
        response = self.client.post(self.url(), self.payload(category_key=job_category.key))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_client_cannot_set_owner_or_status(self):
        """A client-supplied owner/status must never be trusted — the server always sets them."""
        self.client.force_authenticate(self.admin)
        response = self.client.post(self.url(), self.payload(owner=999999, status='approved'))
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['owner']['id'], self.admin.id)
        self.assertEqual(response.data['status'], 'pending')


class ListingUpdateDeleteTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, self.owner_profile = f.make_user(role=UserRole.ADMIN)
        # Not the owner and not an Admin/Super Admin — no moderation rights at all.
        self.plain_user, self.plain_profile = f.make_user(role=UserRole.USER)
        # Not the owner, and also just a plain Admin (Content Provider) — per
        # core.views._can_moderate_posts, only Super Admin (or the relevant
        # City Admin/Sub Admin via _can_manage_city_post) gets cross-owner
        # moderation rights; a Content Provider is explicitly confined to
        # their own listings ("Content Provider cannot: Modify another
        # Content Provider's content"), same rule the website's Posts
        # dashboard enforces. This is the isolation the test below checks.
        self.moderating_admin, self.moderating_profile = f.make_user(role=UserRole.ADMIN)
        self.super_admin, _ = f.make_user(super_admin=True)
        self.listing = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED, name='Owned Biz')

    def detail_url(self, obj):
        return reverse('api:listing_detail', args=['business', obj.pk])

    def edit_payload(self, **overrides):
        data = {
            'name': 'Updated Name', 'category': 'retail', 'city': self.city.pk,
            'address': '1 Market Rd', 'phone_number': '9876543210',
            'maps_link': 'https://www.google.com/maps?q=13.1631,78.2288',
        }
        data.update(overrides)
        return data

    def test_unauthenticated_cannot_edit(self):
        response = self.client.patch(self.detail_url(self.listing), self.edit_payload())
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_non_owner_non_admin_cannot_edit(self):
        self.client.force_authenticate(self.plain_user)
        response = self.client.patch(self.detail_url(self.listing), self.edit_payload())
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_another_content_provider_cannot_moderate_edit(self):
        self.client.force_authenticate(self.moderating_admin)
        response = self.client.patch(self.detail_url(self.listing), self.edit_payload())
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_owner_edit_resets_to_pending(self):
        self.client.force_authenticate(self.owner)
        response = self.client.patch(self.detail_url(self.listing), self.edit_payload())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['name'], 'Updated Name')
        self.assertEqual(response.data['status'], 'pending')

    def test_super_admin_edit_stays_approved(self):
        self.client.force_authenticate(self.super_admin)
        response = self.client.patch(self.detail_url(self.listing), self.edit_payload())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'approved')

    def test_non_owner_non_admin_cannot_delete(self):
        self.client.force_authenticate(self.plain_user)
        response = self.client.delete(self.detail_url(self.listing))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_owner_can_delete(self):
        self.client.force_authenticate(self.owner)
        response = self.client.delete(self.detail_url(self.listing))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(type(self.listing).objects.filter(pk=self.listing.pk).exists())

    def test_super_admin_can_delete_others_listing(self):
        self.client.force_authenticate(self.super_admin)
        response = self.client.delete(self.detail_url(self.listing))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)


class AllListingTypesSmokeTests(APITestCase):
    """A light generic-dispatch check across every listing type, not just Business."""

    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, _ = f.make_user(role=UserRole.ADMIN)

    def test_public_list_and_detail_for_every_model_key(self):
        for model_key, maker in f.MAKERS.items():
            with self.subTest(model_key=model_key):
                obj = maker(owner=self.owner, city=self.city)
                list_response = self.client.get(reverse('api:listing_collection', args=[model_key]))
                self.assertEqual(list_response.status_code, status.HTTP_200_OK)
                detail_response = self.client.get(reverse('api:listing_detail', args=[model_key, obj.pk]))
                self.assertEqual(detail_response.status_code, status.HTTP_200_OK)
                self.assertEqual(detail_response.data['model_key'], model_key)
