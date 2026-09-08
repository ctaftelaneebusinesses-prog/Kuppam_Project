from types import SimpleNamespace
from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import Profile
from core.supabase_auth import SupabaseAuthError

from . import factories as f


def _fake_supabase_user(uid='uid-123', email='newuser@example.com', full_name='New User'):
    return SimpleNamespace(id=uid, email=email, user_metadata={'full_name': full_name})


class MeEndpointTests(APITestCase):
    def test_unauthenticated_gets_401(self):
        response = self.client.get(reverse('api:me'))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertIn('error', response.data)

    def test_authenticated_session_user_sees_own_profile(self):
        user, profile = f.make_user()
        self.client.force_authenticate(user)
        response = self.client.get(reverse('api:me'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['username'], user.username)
        self.assertEqual(response.data['role'], profile.role)

    def test_blocked_user_session_gets_403(self):
        user, _ = f.make_user(blocked=True)
        self.client.force_authenticate(user)
        response = self.client.get(reverse('api:me'))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_logout_requires_auth(self):
        response = self.client.post(reverse('api:logout'))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_logout_succeeds_when_authenticated(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        response = self.client.post(reverse('api:logout'))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)


class SupabaseTokenAuthenticationTests(APITestCase):
    """Exercises core.api.authentication.SupabaseTokenAuthentication end-to-end via /api/v1/auth/me/."""

    def test_missing_header_is_401(self):
        response = self.client.get(reverse('api:me'))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch('core.api.authentication.fetch_supabase_user')
    def test_invalid_token_is_401(self, mock_fetch):
        mock_fetch.side_effect = SupabaseAuthError('Invalid or expired sign-in session.')
        response = self.client.get(reverse('api:me'), HTTP_AUTHORIZATION='Bearer bad-token')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch('core.api.authentication.fetch_supabase_user')
    def test_valid_token_creates_new_user_and_profile(self, mock_fetch):
        mock_fetch.return_value = _fake_supabase_user(uid='uid-new', email='brandnew@example.com')
        response = self.client.get(reverse('api:me'), HTTP_AUTHORIZATION='Bearer good-token')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['email'], 'brandnew@example.com')
        self.assertTrue(Profile.objects.filter(supabase_uid='uid-new').exists())

    @patch('core.api.authentication.fetch_supabase_user')
    def test_valid_token_reuses_existing_profile_by_supabase_uid(self, mock_fetch):
        user, profile = f.make_user()
        profile.supabase_uid = 'uid-existing'
        profile.save()
        mock_fetch.return_value = _fake_supabase_user(uid='uid-existing', email=user.email)

        response = self.client.get(reverse('api:me'), HTTP_AUTHORIZATION='Bearer good-token')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['id'], user.id)
        self.assertEqual(Profile.objects.filter(supabase_uid='uid-existing').count(), 1)

    @patch('core.api.authentication.fetch_supabase_user')
    def test_blocked_account_via_token_is_401_not_200(self, mock_fetch):
        user, profile = f.make_user(blocked=True)
        profile.supabase_uid = 'uid-blocked'
        profile.save()
        mock_fetch.return_value = _fake_supabase_user(uid='uid-blocked', email=user.email)

        response = self.client.get(reverse('api:me'), HTTP_AUTHORIZATION='Bearer good-token')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch('core.api.authentication.fetch_supabase_user')
    def test_malformed_header_is_ignored_not_authenticated(self, mock_fetch):
        response = self.client.get(reverse('api:me'), HTTP_AUTHORIZATION='Token something')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        mock_fetch.assert_not_called()
