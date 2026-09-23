"""
Web-side regression tests for the P1 fixed in core.views._can_manage_post and
listing_edit's ownership check: a suspended Admin/Content Provider could
still edit/delete their own pre-existing listings and manage its gallery,
because is_suspended was enforced for *new* submissions
(Profile.can_manage_category) but not for managing an already-existing one.

API-side equivalents (PATCH/DELETE /api/v1/listings/..., gallery endpoints)
live in core/api/tests/test_listings.py (SuspendedOwnerListingTests) and
core/api/tests/test_media.py (SuspendedOwnerMediaTests) — kept separate since
those need APITestCase/force_authenticate, while this suite goes through the
real onboarding_required + session-login stack, same convention as
core/test_account_deletion.py's AccountDeleteWebViewTests.
"""
from django.contrib.contenttypes.models import ContentType
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase
from django.urls import reverse

from core.api.tests import factories as f
from core.models import ListingStatus, PostImage, UserRole


def _image(name='photo.jpg'):
    return SimpleUploadedFile(name, b'not-really-a-jpeg-but-content-type-is-what-is-checked', content_type='image/jpeg')


class SuspendedOwnerListingEditDeleteTests(TestCase):
    """listing_edit / listing_delete (core/urls.py 'dashboard/my-listings/<model_key>/<pk>/...')."""

    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, self.owner_profile = f.make_user(role=UserRole.ADMIN, suspended=True)
        self.unsuspended_owner, _ = f.make_user(role=UserRole.ADMIN, suspended=False)
        self.suspended_super_admin, _ = f.make_user(super_admin=True, suspended=True)
        self.listing = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED, name='Suspended Owner Biz')
        self.unsuspended_listing = f.make_business(
            owner=self.unsuspended_owner, city=self.city, status=ListingStatus.APPROVED, name='Active Owner Biz'
        )

    def edit_url(self, obj):
        return reverse('core:listing_edit', args=['business', obj.pk])

    def delete_url(self, obj):
        return reverse('core:listing_delete', args=['business', obj.pk])

    def edit_payload(self):
        return {
            'name': 'Updated Name', 'category': 'retail', 'city': self.city.pk,
            'address': '1 Market Rd', 'phone_number': '9876543210',
            'maps_link': 'https://www.google.com/maps?q=13.1631,78.2288',
        }

    def test_suspended_owner_cannot_open_edit_form(self):
        self.client.force_login(self.owner)
        response = self.client.get(self.edit_url(self.listing))
        self.assertRedirects(response, reverse('core:my_listings'))

    def test_suspended_owner_cannot_submit_edit(self):
        self.client.force_login(self.owner)
        self.client.post(self.edit_url(self.listing), self.edit_payload())
        self.listing.refresh_from_db()
        self.assertEqual(self.listing.name, 'Suspended Owner Biz')

    def test_suspended_owner_cannot_delete(self):
        self.client.force_login(self.owner)
        self.client.post(self.delete_url(self.listing))
        self.assertTrue(type(self.listing).objects.filter(pk=self.listing.pk).exists())

    def test_unsuspended_owner_can_still_edit(self):
        self.client.force_login(self.unsuspended_owner)
        self.client.post(self.edit_url(self.unsuspended_listing), self.edit_payload())
        self.unsuspended_listing.refresh_from_db()
        self.assertEqual(self.unsuspended_listing.name, 'Updated Name')

    def test_unsuspended_owner_can_still_delete(self):
        self.client.force_login(self.unsuspended_owner)
        self.client.post(self.delete_url(self.unsuspended_listing))
        self.assertFalse(type(self.unsuspended_listing).objects.filter(pk=self.unsuspended_listing.pk).exists())

    def test_suspended_super_admin_is_still_exempt(self):
        """Super Admin's own is_suspended flag (however it got set) never blocks moderation rights."""
        self.client.force_login(self.suspended_super_admin)
        self.client.post(self.edit_url(self.listing), self.edit_payload())
        self.listing.refresh_from_db()
        self.assertEqual(self.listing.name, 'Updated Name')


class SuspendedOwnerMediaWebTests(TestCase):
    """my_listing_media_add / _delete / _set_cover (core/urls.py 'dashboard/my-listings/media/...')."""

    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, self.owner_profile = f.make_user(role=UserRole.ADMIN, suspended=True)
        self.listing = f.make_business(owner=self.owner, city=self.city, status=ListingStatus.APPROVED)
        ct = ContentType.objects.get_for_model(type(self.listing))
        self.image = PostImage.objects.create(content_type=ct, object_id=self.listing.pk, image=_image(), uploaded_by=self.owner)

    def test_suspended_owner_cannot_add_media(self):
        self.client.force_login(self.owner)
        response = self.client.post(
            reverse('core:my_listing_media_add', args=['business', self.listing.pk]),
            {'images': _image('new.jpg')},
        )
        self.assertRedirects(response, reverse('core:my_listings'))
        self.assertEqual(PostImage.objects.filter(object_id=self.listing.pk).count(), 1)

    def test_suspended_owner_cannot_delete_media(self):
        self.client.force_login(self.owner)
        response = self.client.post(reverse('core:my_listing_media_delete', args=['image', self.image.pk]))
        self.assertRedirects(response, reverse('core:my_listings'))
        self.assertTrue(PostImage.objects.filter(pk=self.image.pk).exists())

    def test_suspended_owner_cannot_set_cover(self):
        self.client.force_login(self.owner)
        response = self.client.post(reverse('core:my_listing_media_set_cover', args=[self.image.pk]))
        self.assertRedirects(response, reverse('core:my_listings'))
        self.listing.refresh_from_db()
        self.assertFalse(self.listing.image_url)
