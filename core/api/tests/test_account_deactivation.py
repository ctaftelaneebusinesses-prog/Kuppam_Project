"""
API tests for POST /api/v1/auth/me/deactivate/ (core.account_deactivation)
and for an app token reactivating a deactivated account. The web page and the
service itself are covered in core/test_account_deactivation.py.
"""
from types import SimpleNamespace
from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import MobileDevice, Profile

from . import factories as f


class AccountDeactivationAPITests(APITestCase):
    def test_unauthenticated_deactivate_is_401(self):
        response = self.client.post(reverse('api:deactivate_me'))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_deactivates_own_account_without_deleting_anything(self):
        user, _ = f.make_user()
        other, _ = f.make_user()
        MobileDevice.objects.create(user=user, token='device-token-1')
        self.client.force_authenticate(user)

        response = self.client.post(reverse('api:deactivate_me'))

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        profile = Profile.objects.get(user=user)
        self.assertIsNotNone(profile.deactivated_at)
        self.assertFalse(MobileDevice.objects.filter(user=user).exists())
        self.assertIsNone(Profile.objects.get(user=other).deactivated_at)

    def test_delete_on_me_is_no_longer_allowed(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_405_METHOD_NOT_ALLOWED)
        self.assertTrue(Profile.objects.filter(user=user).exists())

    @patch('core.api.authentication.fetch_supabase_user')
    def test_signing_in_with_app_token_reactivates(self, mock_fetch):
        user, profile = f.make_user()
        profile.supabase_uid = 'uid-back'
        profile.save()
        self.client.force_authenticate(user)
        self.client.post(reverse('api:deactivate_me'))
        self.client.force_authenticate(None)
        mock_fetch.return_value = SimpleNamespace(id='uid-back', email=user.email, user_metadata={})

        response = self.client.get(reverse('api:me'), HTTP_AUTHORIZATION='Bearer good-token')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNone(Profile.objects.get(user=user).deactivated_at)
