"""
Tests for core.account_deletion.delete_user_account (the shared service the
web "Delete My Account" view and the API's DELETE /api/v1/auth/me/ endpoint
both call) plus the web confirmation view itself.

API-side auth/CSRF/ownership tests for the same DELETE endpoint live in
core/api/tests/test_account_deletion.py — kept separate since that suite
needs APITestCase/force_authenticate, while the web view here goes through
the real onboarding_required + session-login stack.
"""
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.contrib.contenttypes.models import ContentType
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase
from django.urls import reverse

from core.account_deletion import AccountDeletionError, delete_user_account
from core.models import (
    AdminCategoryPermission, AdminRequest, Business, Category, Comment, Favorite, Intent, Like, ListingStatus,
    Location, LoginHistory, Notification, Profile, PushSubscription, Report, Review, UserRole,
)
from core.supabase_auth import SupabaseAuthError

User = get_user_model()


def _make_user(username, supabase_uid=None):
    user = User.objects.create_user(username=username, email=f'{username}@example.com', password='pass-12345!')
    profile = Profile.objects.create(
        user=user, role=UserRole.USER, full_name=username.title(), profile_completed=True,
        intent=Intent.EXPLORE, supabase_uid=supabase_uid,
    )
    return user, profile


class AccountDeletionServiceTests(TestCase):
    """Exercises the dependency map: what's deleted, what's detached-not-deleted, what's left alone."""

    def setUp(self):
        self.user, self.profile = _make_user('deleteme')
        self.other_user, self.other_profile = _make_user('bystander')

        self.category = Category.objects.create(key='biz-adt', label='Business', listing_model='business', is_active=True)
        self.city = Location.objects.create(kind=Location.Kind.CITY, name='Kuppam', slug='kuppam-adt', country_code='IN')
        self.listing = Business.objects.create(
            name='Deleteme Shop', category='retail', address='123 Main St', phone_number='9998887777',
            owner=self.user, city=self.city, listing_category=self.category, status=ListingStatus.APPROVED, is_active=True,
        )
        self.ct = ContentType.objects.get_for_model(Business)

    def test_deletes_profile_and_user(self):
        user_id = self.user.id
        delete_user_account(self.user)
        self.assertFalse(User.objects.filter(id=user_id).exists())
        self.assertFalse(Profile.objects.filter(user_id=user_id).exists())

    def test_deletes_favorites_likes_push_subscriptions_notifications_login_history(self):
        Favorite.objects.create(content_type=self.ct, object_id=self.listing.pk, user=self.user)
        Like.objects.create(content_type=self.ct, object_id=self.listing.pk, user=self.user)
        PushSubscription.objects.create(user=self.user, endpoint='https://push.example/1', p256dh='k', auth='a')
        Notification.objects.create(recipient=self.user, type='new_comment', message='hi')
        LoginHistory.objects.create(user=self.user, event_type='login')

        delete_user_account(self.user)

        self.assertEqual(Favorite.objects.count(), 0)
        self.assertEqual(Like.objects.count(), 0)
        self.assertEqual(PushSubscription.objects.count(), 0)
        self.assertEqual(Notification.objects.count(), 0)
        self.assertEqual(LoginHistory.objects.count(), 0)

    def test_deletes_admin_request_and_delegated_permissions(self):
        AdminRequest.objects.create(user=self.user)
        AdminCategoryPermission.objects.create(admin=self.user, category=self.category)

        delete_user_account(self.user)

        self.assertEqual(AdminRequest.objects.count(), 0)
        self.assertEqual(AdminCategoryPermission.objects.count(), 0)

    def test_comments_and_replies_are_detached_not_deleted(self):
        comment = Comment.objects.create(content_type=self.ct, object_id=self.listing.pk, user=self.user, body='Nice place')
        reply = Comment.objects.create(
            content_type=self.ct, object_id=self.listing.pk, user=self.other_user, parent=comment, body='Agreed!',
        )

        delete_user_account(self.user)

        comment.refresh_from_db()
        reply.refresh_from_db()
        self.assertIsNone(comment.user_id)
        self.assertEqual(comment.body, 'Nice place')
        # The other user's reply must survive untouched — this is exactly
        # the cascade-collateral-damage bug the SET_NULL migration prevents.
        self.assertEqual(reply.user_id, self.other_user.id)
        self.assertEqual(reply.parent_id, comment.id)

    def test_review_is_detached_not_deleted_and_rating_survives(self):
        review = Review.objects.create(content_type=self.ct, object_id=self.listing.pk, user=self.user, rating=5, body='Loved it')

        delete_user_account(self.user)

        review.refresh_from_db()
        self.assertIsNone(review.user_id)
        self.assertEqual(review.rating, 5)

    def test_report_is_detached_not_deleted(self):
        report = Report.objects.create(content_type=self.ct, object_id=self.listing.pk, user=self.user, reason='spam')

        delete_user_account(self.user)

        report.refresh_from_db()
        self.assertIsNone(report.user_id)
        self.assertEqual(report.reason, 'spam')

    def test_owned_listing_survives_ownerless_and_public(self):
        delete_user_account(self.user)

        self.listing.refresh_from_db()
        self.assertIsNone(self.listing.owner_id)
        self.assertEqual(self.listing.status, ListingStatus.APPROVED)
        self.assertTrue(self.listing.is_active)

    def test_profile_photo_file_is_deleted_from_storage(self):
        self.profile.profile_photo = SimpleUploadedFile('avatar.jpg', b'fake-image-bytes', content_type='image/jpeg')
        self.profile.save()
        storage = self.profile.profile_photo.storage
        stored_name = self.profile.profile_photo.name
        self.assertTrue(storage.exists(stored_name))

        delete_user_account(self.user)

        self.assertFalse(storage.exists(stored_name))

    @patch('core.account_deletion.delete_supabase_user')
    def test_deletes_supabase_identity_when_linked(self, mock_delete_supabase):
        self.profile.supabase_uid = 'uid-123'
        self.profile.save()

        delete_user_account(self.user)

        mock_delete_supabase.assert_called_once_with('uid-123')

    @patch('core.account_deletion.delete_supabase_user')
    def test_skips_supabase_call_when_no_supabase_uid(self, mock_delete_supabase):
        delete_user_account(self.user)
        mock_delete_supabase.assert_not_called()

    @patch('core.account_deletion.delete_supabase_user')
    def test_local_deletion_survives_supabase_failure(self, mock_delete_supabase):
        mock_delete_supabase.side_effect = SupabaseAuthError('boom')
        self.profile.supabase_uid = 'uid-999'
        self.profile.save()
        user_id = self.user.id

        delete_user_account(self.user)  # must not raise/roll back the local deletion

        self.assertFalse(User.objects.filter(id=user_id).exists())

    def test_repeated_call_on_same_instance_raises_cleanly(self):
        delete_user_account(self.user)
        with self.assertRaises(AccountDeletionError):
            delete_user_account(self.user)

    def test_does_not_touch_another_users_data(self):
        Favorite.objects.create(content_type=self.ct, object_id=self.listing.pk, user=self.other_user)

        delete_user_account(self.user)

        self.assertTrue(User.objects.filter(id=self.other_user.id).exists())
        self.assertEqual(Favorite.objects.filter(user=self.other_user).count(), 1)


class AccountDeleteWebViewTests(TestCase):
    """The 'Delete My Account' web page — auth gate, GET-never-deletes, and the typed confirmation."""

    def setUp(self):
        self.user, self.profile = _make_user('webdeleteme')
        self.url = reverse('core:account_delete_confirm')

    def test_unauthenticated_get_redirects_to_login(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 302)

    def test_unauthenticated_post_redirects_to_login_and_does_not_delete(self):
        response = self.client.post(self.url, {'confirm': 'DELETE'})
        self.assertEqual(response.status_code, 302)
        self.assertTrue(User.objects.filter(id=self.user.id).exists())

    def test_get_while_authenticated_shows_confirmation_and_never_deletes(self):
        self.client.force_login(self.user)
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(User.objects.filter(id=self.user.id).exists())

    def test_post_wrong_confirmation_text_does_not_delete(self):
        self.client.force_login(self.user)
        response = self.client.post(self.url, {'confirm': 'please delete my account'})
        self.assertRedirects(response, self.url)
        self.assertTrue(User.objects.filter(id=self.user.id).exists())

    def test_post_correct_confirmation_deletes_and_logs_out(self):
        self.client.force_login(self.user)
        user_id = self.user.id

        response = self.client.post(self.url, {'confirm': 'DELETE'})

        self.assertEqual(response.status_code, 302)
        self.assertFalse(User.objects.filter(id=user_id).exists())
        # Session must be invalidated — a follow-up request to any
        # onboarding_required page should bounce to login, not succeed.
        response2 = self.client.get(reverse('core:dashboard_profile'))
        self.assertEqual(response2.status_code, 302)

    def test_post_confirmation_is_case_insensitive(self):
        self.client.force_login(self.user)
        response = self.client.post(self.url, {'confirm': 'delete'})
        self.assertEqual(response.status_code, 302)
        self.assertFalse(User.objects.filter(id=self.user.id).exists())

    def test_repeated_post_after_deletion_does_not_error(self):
        """A second, already-logged-out submission (e.g. a resubmitted form)
        must not 500 — it should simply be treated as unauthenticated."""
        self.client.force_login(self.user)
        self.client.post(self.url, {'confirm': 'DELETE'})

        response = self.client.post(self.url, {'confirm': 'DELETE'})

        self.assertEqual(response.status_code, 302)  # bounced to login, not a crash
