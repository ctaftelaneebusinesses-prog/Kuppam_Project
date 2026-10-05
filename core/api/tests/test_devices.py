"""
API tests for /api/v1/devices/ — FCM token registration for the native app.
Delivery isn't implemented; these cover the registration contract: auth,
validation, idempotency, token reassignment between accounts, and that a
caller can only ever unregister their own tokens.
"""
from django.core.cache import cache
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import MobileDevice

from . import factories as f


class DeviceRegistrationAPITests(APITestCase):
    def setUp(self):
        cache.clear()
        self.url = reverse('api:devices')

    def test_unauthenticated_is_401(self):
        response = self.client.post(self.url, {'token': 'abc'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_blocked_account_is_403(self):
        user, _ = f.make_user(blocked=True)
        self.client.force_authenticate(user)
        response = self.client.post(self.url, {'token': 'abc'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertFalse(MobileDevice.objects.exists())

    def test_register_creates_device_with_metadata(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        response = self.client.post(
            self.url, {'token': 'tok-1', 'app_version': '1.0.0', 'locale': 'te'}, format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        device = MobileDevice.objects.get(token='tok-1')
        self.assertEqual((device.user, device.platform, device.app_version, device.locale),
                         (user, 'android', '1.0.0', 'te'))

    def test_registering_same_token_again_is_idempotent(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        self.client.post(self.url, {'token': 'tok-1', 'app_version': '1.0.0'}, format='json')
        response = self.client.post(self.url, {'token': 'tok-1', 'app_version': '1.1.0'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(MobileDevice.objects.count(), 1)
        self.assertEqual(MobileDevice.objects.get().app_version, '1.1.0')

    def test_token_signed_in_as_another_user_is_reassigned(self):
        first, _ = f.make_user()
        second, _ = f.make_user()
        self.client.force_authenticate(first)
        self.client.post(self.url, {'token': 'shared-phone'}, format='json')
        self.client.force_authenticate(second)
        self.client.post(self.url, {'token': 'shared-phone'}, format='json')
        self.assertEqual(MobileDevice.objects.get(token='shared-phone').user, second)
        self.assertEqual(MobileDevice.objects.count(), 1)

    def test_invalid_input_is_400(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        for payload in (
            {}, {'token': ''}, {'token': '   '}, {'token': 123}, {'token': 'x' * 513},
            {'token': 't', 'platform': 'ios'}, {'token': 't', 'app_version': 'v' * 33},
        ):
            with self.subTest(payload=payload):
                response = self.client.post(self.url, payload, format='json')
                self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(MobileDevice.objects.exists())

    def test_delete_removes_own_token(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        self.client.post(self.url, {'token': 'tok-1'}, format='json')
        response = self.client.delete(self.url, {'token': 'tok-1'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(MobileDevice.objects.exists())

    def test_delete_cannot_remove_another_users_token(self):
        owner, _ = f.make_user()
        other, _ = f.make_user()
        MobileDevice.objects.create(user=owner, token='owners-token')
        self.client.force_authenticate(other)
        response = self.client.delete(self.url, {'token': 'owners-token'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertTrue(MobileDevice.objects.filter(token='owners-token', user=owner).exists())

    def test_account_deactivation_removes_devices(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        self.client.post(self.url, {'token': 'tok-1'}, format='json')
        self.client.post(reverse('api:deactivate_me'))
        self.assertFalse(MobileDevice.objects.exists())

    def test_rate_limited_after_burst(self):
        user, _ = f.make_user()
        self.client.force_authenticate(user)
        codes = [self.client.post(self.url, {'token': f't{i}'}, format='json').status_code for i in range(31)]
        self.assertEqual(codes[-1], status.HTTP_429_TOO_MANY_REQUESTS)
