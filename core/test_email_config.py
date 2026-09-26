"""
Contact-form email tests. No production provider is configured (see
hello_kuppam/settings.py's EMAIL_* block) — Django's test runner forces
EMAIL_BACKEND to locmem regardless of the real setting, so these verify the
plumbing (message queued, ContactMessage saved, failures handled) without
ever making a real network call or needing real credentials.
"""
import importlib
import os
from unittest.mock import patch

import hello_kuppam.settings as settings_module
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


class EmailBackendSelectionTests(TestCase):
    """
    Exercises the ACTUAL backend-selection rule in
    hello_kuppam/settings.py's EMAIL block, not a re-implementation of it —
    EMAIL_BACKEND must default to SMTP once EMAIL_HOST is set, console
    otherwise, and a warning must be logged if DEBUG=False still resolves
    to console (the P1 audit finding: production must not silently run on
    the console backend).

    django.conf.settings is a LazySettings wrapper that copies these
    values from the hello_kuppam.settings MODULE once, the first time
    Django's settings are accessed — reloading the module afterward
    wouldn't change django.conf.settings, and Django's own test runner
    unconditionally forces EMAIL_BACKEND to locmem for every test (see
    ContactFormEmailTests' docstring above), so django.conf.settings can
    never observe this rule during a test run either way. Reloading the
    hello_kuppam.settings module object directly — with a patched
    os.environ, restored via reload again in `finally` — is the only way
    to see what the real module would compute, without touching the live
    django.conf.settings the rest of this test process depends on.
    """

    def _reload_with(self, email_host='', email_backend=None):
        """
        email_backend=None means "leave EMAIL_BACKEND unset" (so settings.py
        applies its own default) — passing '' instead would be wrong here:
        os.getenv('EMAIL_BACKEND', default) only falls back to `default`
        when the key is ABSENT from os.environ, not when it's merely empty.
        """
        overrides = {'DEBUG': 'False', 'SECRET_KEY': 'test-only-secret-not-a-real-one', 'EMAIL_HOST': email_host}
        try:
            with patch.dict(os.environ, overrides, clear=False):
                if email_backend is None:
                    os.environ.pop('EMAIL_BACKEND', None)
                else:
                    os.environ['EMAIL_BACKEND'] = email_backend
                importlib.reload(settings_module)
                return settings_module.EMAIL_BACKEND
        finally:
            # Always restore the module to reflect the real environment,
            # regardless of the outcome above.
            importlib.reload(settings_module)

    def test_defaults_to_smtp_once_email_host_is_set(self):
        self.assertEqual(
            self._reload_with(email_host='smtp.example.com'),
            'django.core.mail.backends.smtp.EmailBackend',
        )

    def test_defaults_to_console_when_email_host_is_blank(self):
        self.assertEqual(
            self._reload_with(),
            'django.core.mail.backends.console.EmailBackend',
        )

    def test_explicit_email_backend_env_var_overrides_the_default(self):
        self.assertEqual(
            self._reload_with(email_host='smtp.example.com', email_backend='some.custom.ApiBackend'),
            'some.custom.ApiBackend',
        )

    def test_warns_when_debug_false_still_resolves_to_console(self):
        with self.assertLogs('hello_kuppam.settings', level='WARNING') as captured:
            self._reload_with()
        self.assertTrue(any('console email backend' in message for message in captured.output))
