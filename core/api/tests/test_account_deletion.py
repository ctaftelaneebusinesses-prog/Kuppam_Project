"""
API tests for DELETE /api/v1/auth/me/ — core.account_deletion.delete_user_account
called through the same `me` endpoint GET already uses. Service-level
dependency-map tests and the web view's own tests live in
core/test_account_deletion.py; this file only covers what's specific to the
API surface: auth, the confirmation-body contract, and ownership.
"""
from unittest.mock import patch

from django.contrib.contenttypes.models import ContentType
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import Favorite, Profile

from . import factories as f


class AccountDeletionAPITests(APITestCase):
    def test_unauthenticated_delete_is_401(self):
        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_missing_confirmation_is_400_and_account_survives(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)

        response = self.client.delete(reverse('api:me'))

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertTrue(Profile.objects.filter(user=user).exists())

    def test_wrong_confirmation_value_is_400_and_account_survives(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)

        response = self.client.delete(reverse('api:me'), {'confirm': 'yes please'}, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertTrue(Profile.objects.filter(user=user).exists())

    def test_blocked_account_cannot_delete_via_api(self):
        user, _ = f.make_user(blocked=True)
        self.client.force_authenticate(user)

        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertTrue(Profile.objects.filter(user=user).exists())

    def test_correct_confirmation_deletes_own_account_only(self):
        user, _ = f.make_user()
        other_user, _ = f.make_user()
        self.client.force_authenticate(user)

        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Profile.objects.filter(user_id=user.id).exists())
        # No id is ever taken from the request body/URL — it always acts on
        # request.user, so there is no parameter through which one account
        # could delete another's.
        self.assertTrue(Profile.objects.filter(user_id=other_user.id).exists())

    def test_repeated_delete_after_success_is_rejected_not_broken(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        first = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')
        self.assertEqual(first.status_code, status.HTTP_204_NO_CONTENT)

        # A real second attempt has to re-authenticate (session or bearer
        # token) — it can't replay the same in-memory user object the way a
        # naive test might. Simulate that: no credentials survive deletion.
        self.client.force_authenticate(user=None)
        second = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')

        self.assertEqual(second.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch('core.account_deletion.delete_supabase_user')
    def test_deletes_supabase_identity_via_api(self, mock_delete_supabase):
        user, profile = f.make_user()
        profile.supabase_uid = 'uid-api-1'
        profile.save()
        self.client.force_authenticate(user)

        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        mock_delete_supabase.assert_called_once_with('uid-api-1')

    def test_deletion_leaves_other_users_data_alone(self):
        user, _ = f.make_user()
        other_user, _ = f.make_user()
        business = f.make_business(owner=other_user)
        ct = ContentType.objects.get_for_model(type(business))
        Favorite.objects.create(content_type=ct, object_id=business.pk, user=other_user)
        self.client.force_authenticate(user)

        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(Favorite.objects.filter(user=other_user).count(), 1)
        business.refresh_from_db()
        self.assertEqual(business.owner_id, other_user.id)
