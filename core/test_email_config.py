"""
Contact-form email tests. No production provider is configured (see
hello_kuppam/settings.py's EMAIL_* block) — Django's test runner forces
EMAIL_BACKEND to locmem regardless of the real setting, so these verify the
plumbing (message queued, ContactMessage saved, failures handled) without
ever making a real network call or needing real credentials.
"""
from unittest.mock import patch

from django.core import mail
from django.test import TestCase
from django.urls import reverse

from core.models import ContactMessage


class ContactFormEmailTests(TestCase):
    def _post(self):
        return self.client.post(reverse('core:contact'), {
            'name': 'Test User',
            'email': 'testuser@example.com',
            'subject': 'Question about listings',
            'message': 'How do I list my shop?',
        })

    def test_valid_submission_saves_message_and_queues_email(self):
        response = self._post()

        self.assertEqual(response.status_code, 302)
        self.assertEqual(ContactMessage.objects.count(), 1)
        self.assertEqual(len(mail.outbox), 1)
        self.assertIn('Question about listings', mail.outbox[0].subject)
        self.assertIn('testuser@example.com', mail.outbox[0].body)

    @patch('core.views.send_mail')
    def test_email_send_failure_does_not_lose_the_submission_or_crash(self, mock_send_mail):
        mock_send_mail.side_effect = Exception('SMTP connection refused')

        response = self._post()

        # The visitor's message is never lost even if the notification
        # email fails — and the request itself must not 500.
        self.assertEqual(response.status_code, 302)
        self.assertEqual(ContactMessage.objects.count(), 1)
        self.assertEqual(ContactMessage.objects.first().subject, 'Question about listings')

    def test_invalid_submission_does_not_send_email_or_save(self):
        response = self.client.post(reverse('core:contact'), {
            'name': '', 'email': 'not-an-email', 'subject': '', 'message': '',
        })

        self.assertEqual(response.status_code, 200)  # re-renders the form with errors
        self.assertEqual(ContactMessage.objects.count(), 0)
        self.assertEqual(len(mail.outbox), 0)
