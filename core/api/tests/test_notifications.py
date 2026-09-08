from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import Notification

from . import factories as f


class NotificationTests(APITestCase):
    def setUp(self):
        self.user, _ = f.make_user()
        self.other, _ = f.make_user()
        self.n1 = Notification.objects.create(recipient=self.user, type='listing_approved', message='Approved!', url='/x/')
        self.n2 = Notification.objects.create(recipient=self.user, type='new_comment', message='New comment', url='/y/', is_read=True)
        Notification.objects.create(recipient=self.other, type='listing_approved', message='Not yours', url='/z/')

    def test_unauthenticated_cannot_list(self):
        response = self.client.get(reverse('api:notifications'))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_list_only_own_notifications(self):
        self.client.force_authenticate(self.user)
        response = self.client.get(reverse('api:notifications'))
        self.assertEqual(response.data['count'], 2)

    def test_empty_for_user_with_none(self):
        third, _ = f.make_user()
        self.client.force_authenticate(third)
        response = self.client.get(reverse('api:notifications'))
        self.assertEqual(response.data['count'], 0)
        self.assertEqual(response.data['results'], [])

    def test_unread_count(self):
        self.client.force_authenticate(self.user)
        response = self.client.get(reverse('api:notifications_unread_count'))
        self.assertEqual(response.data['count'], 1)

    def test_mark_read(self):
        self.client.force_authenticate(self.user)
        response = self.client.post(reverse('api:notification_mark_read', args=[self.n1.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.n1.refresh_from_db()
        self.assertTrue(self.n1.is_read)

    def test_cannot_mark_another_users_notification_read(self):
        other_notification = Notification.objects.create(recipient=self.other, type='listing_approved', message='x')
        self.client.force_authenticate(self.user)
        response = self.client.post(reverse('api:notification_mark_read', args=[other_notification.pk]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        other_notification.refresh_from_db()
        self.assertFalse(other_notification.is_read)

    def test_invalid_id_mark_read_is_404(self):
        self.client.force_authenticate(self.user)
        response = self.client.post(reverse('api:notification_mark_read', args=[999999]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_mark_all_read(self):
        self.client.force_authenticate(self.user)
        response = self.client.post(reverse('api:notifications_mark_all_read'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(Notification.objects.filter(recipient=self.user, is_read=False).count(), 0)
