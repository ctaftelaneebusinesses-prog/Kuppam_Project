"""
Self-service account deactivation (core.account_deactivation) on the web:
the Deactivate page, reactivation on the next sign-in, the removed
"Delete My Account" entry points, and the staff-only `delete_account`
command that handles emailed deletion requests. API coverage lives in
core/api/tests/test_account_deactivation.py.
"""
from io import StringIO

from django.contrib.auth import get_user_model
from django.core.management import CommandError, call_command
from django.test import TestCase
from django.urls import reverse

from core.models import Business, Intent, ListingStatus, LoginHistory, Profile, PushSubscription

User = get_user_model()


def _make_user(username, password='pass-12345!'):
    user = User.objects.create_user(username=username, email=f'{username}@example.com', password=password)
    profile = Profile.objects.create(
        user=user, full_name=username.title(), profile_completed=True, intent=Intent.EXPLORE,
    )
    profile.record_consent()
    return user, profile


class DeactivatePageTests(TestCase):
    def setUp(self):
        self.user, self.profile = _make_user('takingabreak')
        self.url = reverse('core:account_deactivate_confirm')

    def test_requires_sign_in(self):
        self.assertEqual(self.client.get(self.url).status_code, 302)
        self.client.post(self.url)
        self.profile.refresh_from_db()
        self.assertIsNone(self.profile.deactivated_at)

    def test_get_only_explains(self):
        self.client.force_login(self.user)
        response = self.client.get(self.url)
        self.assertContains(response, 'Deactivate My Account')
        self.profile.refresh_from_db()
        self.assertIsNone(self.profile.deactivated_at)

    def test_post_deactivates_signs_out_and_keeps_everything(self):
        business = Business.objects.create(
            name='Still Listed Store', category='grocery', address='Main Road', phone_number='9876543210',
            status=ListingStatus.APPROVED, owner=self.user,
        )
        PushSubscription.objects.create(user=self.user, endpoint='https://push.example/1', p256dh='k', auth='a')
        self.client.force_login(self.user)

        response = self.client.post(self.url)

        self.assertRedirects(response, reverse('core:home'))
        self.profile.refresh_from_db()
        self.assertIsNotNone(self.profile.deactivated_at)
        self.assertTrue(User.objects.filter(pk=self.user.pk).exists())
        business.refresh_from_db()
        self.assertEqual(business.owner, self.user)
        self.assertFalse(PushSubscription.objects.filter(user=self.user).exists())
        self.assertTrue(LoginHistory.objects.filter(user=self.user, event_type='deactivate').exists())
        # Signed out: the dashboard now bounces to sign-in.
        self.assertEqual(self.client.get(reverse('core:dashboard_profile')).status_code, 302)

    def test_old_delete_url_redirects_to_deactivate(self):
        response = self.client.get('/dashboard/profile/delete/')
        self.assertRedirects(response, self.url, status_code=301, fetch_redirect_response=False)

    def test_profile_offers_deactivate_and_sidebar_has_no_delete_link(self):
        self.client.force_login(self.user)
        response = self.client.get(reverse('core:dashboard_profile'))
        self.assertContains(response, self.url)
        self.assertNotContains(response, 'Delete My Account')


class ReactivationTests(TestCase):
    def setUp(self):
        self.user, self.profile = _make_user('comingback')
        self.client.force_login(self.user)
        self.client.post(reverse('core:account_deactivate_confirm'))

    def test_password_sign_in_reactivates(self):
        response = self.client.post(
            reverse('core:password_login'), {'username': 'comingback', 'password': 'pass-12345!'}, follow=True,
        )
        self.profile.refresh_from_db()
        self.assertIsNone(self.profile.deactivated_at)
        self.assertContains(response, 'Your account is active again')
        self.assertTrue(LoginHistory.objects.filter(user=self.user, event_type='reactivate').exists())

    def test_session_left_open_elsewhere_is_signed_out(self):
        other_browser = self.client_class()
        other_browser.force_login(self.user)  # force_login fires user_logged_in, which reactivates
        Profile.objects.filter(user=self.user).update(deactivated_at='2026-10-05T00:00:00Z')

        response = other_browser.get(reverse('core:dashboard_profile'))

        self.assertRedirects(response, reverse('core:google_login'), fetch_redirect_response=False)
        self.profile.refresh_from_db()
        self.assertIsNotNone(self.profile.deactivated_at)


class DeleteAccountCommandTests(TestCase):
    def test_requires_yes(self):
        _make_user('asked')
        with self.assertRaises(CommandError):
            call_command('delete_account', 'asked@example.com', stdout=StringIO())
        self.assertTrue(User.objects.filter(username='asked').exists())

    def test_deletes_with_yes(self):
        _make_user('asked')
        call_command('delete_account', 'ASKED@example.com', '--yes', stdout=StringIO())
        self.assertFalse(User.objects.filter(username='asked').exists())

    def test_unknown_email(self):
        with self.assertRaises(CommandError):
            call_command('delete_account', 'nobody@example.com', '--yes', stdout=StringIO())
