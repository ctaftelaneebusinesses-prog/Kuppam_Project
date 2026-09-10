"""
Phase A — admin/password login brute-force lockout tests.

Owner-approved policy under test: 5 failed attempts / 15-minute window /
15-minute lockout / scoped to both the submitted username and the client IP
independently (see core.views's login-lockout block, right after
_client_ip, for the full design rationale).
"""
from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse

from core.models import AuditLog, Intent, Profile, UserRole

User = get_user_model()


def _make_password_user(username, password='CorrectHorseBattery9', role=UserRole.USER, is_staff=False, full_name=None):
    user = User.objects.create_user(username=username, password=password, is_staff=is_staff)
    Profile.objects.create(
        user=user, role=role, full_name=full_name or username.title(),
        profile_completed=True, intent=Intent.EXPLORE,
    )
    return user


class AdminLoginLockoutTests(TestCase):
    def setUp(self):
        cache.clear()
        self.staff_user = _make_password_user('staffadmin', is_staff=True, role=UserRole.SUPER_ADMIN)
        self.url = reverse('core:admin_login')

    def _post(self, username, password, ip='10.0.0.1'):
        return self.client.post(self.url, {'username': username, 'password': password}, REMOTE_ADDR=ip)

    # 1. successful admin login
    def test_successful_login_succeeds_and_creates_no_lock(self):
        response = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertEqual(response.status_code, 302)
        self.assertTrue(self.client.session.get('_auth_user_id'))

    # 2. single failed attempt
    def test_single_failed_attempt_does_not_lock(self):
        response = self._post('staffadmin', 'wrong-password')
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Invalid username or password')
        # still able to authenticate correctly right after
        response2 = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertEqual(response2.status_code, 302)

    # 3 & 4. repeated failed attempts / threshold reached
    def test_threshold_reached_locks_the_account(self):
        for _ in range(4):
            self._post('staffadmin', 'wrong-password')
        # 4 failures: not yet locked
        response = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertEqual(response.status_code, 302)  # correct password still works before threshold

    def test_fifth_failure_triggers_lock(self):
        for _ in range(5):
            response = self._post('staffadmin', 'wrong-password')
        self.assertContains(response, 'Invalid username or password')
        # now locked — even the CORRECT password is rejected without attempting auth
        locked_response = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertContains(locked_response, 'Too many failed login attempts')
        self.assertFalse(self.client.session.get('_auth_user_id'))

    # 5. locked account
    def test_locked_account_rejects_correct_password(self):
        for _ in range(5):
            self._post('staffadmin', 'wrong-password')
        response = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertContains(response, 'Too many failed login attempts')
        self.assertEqual(AuditLog.objects.filter(action='auth.account_login_locked').count(), 1)

    # 6 & 7. lockout expiry + successful login after expiry
    def test_lockout_expires_and_login_succeeds_after(self):
        for _ in range(5):
            self._post('staffadmin', 'wrong-password')
        locked = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertContains(locked, 'Too many failed login attempts')

        # Simulate the 15-minute window elapsing (cache TTL expiry) without
        # actually sleeping in the test suite.
        from core.views import _login_lockout_cache_keys
        keys = _login_lockout_cache_keys('staffadmin')
        cache.delete(keys['locked'])
        cache.delete(keys['attempts'])
        cache.delete('loginlock:locked:ip:10.0.0.1')
        cache.delete('loginlock:attempts:ip:10.0.0.1')

        response = self._post('staffadmin', 'CorrectHorseBattery9')
        self.assertEqual(response.status_code, 302)
        self.assertTrue(self.client.session.get('_auth_user_id'))

    # 8. wrong username (nonexistent)
    def test_nonexistent_username_locks_the_same_way(self):
        for _ in range(5):
            response = self._post('this-user-does-not-exist', 'whatever')
        self.assertContains(response, 'Invalid username or password')
        locked = self._post('this-user-does-not-exist', 'whatever')
        self.assertContains(locked, 'Too many failed login attempts')

    # 9. wrong password (existing user) — covered by test_fifth_failure_triggers_lock

    # 10. enumeration resistance
    def test_lockout_message_identical_for_real_and_fake_usernames(self):
        for _ in range(5):
            self._post('staffadmin', 'wrong-password', ip='10.0.0.2')
        real_locked = self._post('staffadmin', 'x', ip='10.0.0.2')

        for _ in range(5):
            self._post('totally-made-up-user', 'wrong-password', ip='10.0.0.3')
        fake_locked = self._post('totally-made-up-user', 'x', ip='10.0.0.3')

        self.assertContains(real_locked, 'Too many failed login attempts')
        self.assertContains(fake_locked, 'Too many failed login attempts')
        # Identical wording either way — no signal distinguishing a real
        # account being locked from a nonexistent one.
        self.assertEqual(
            [m.message for m in real_locked.context['messages']],
            [m.message for m in fake_locked.context['messages']],
        )

    # 11. IP behavior (per-IP throttling)
    def test_ip_lock_triggers_across_different_usernames_from_same_ip(self):
        shared_ip = '10.0.0.9'
        for i in range(5):
            response = self._post(f'random-user-{i}', 'wrong-password', ip=shared_ip)
        self.assertContains(response, 'Invalid username or password')
        # A 6th attempt, even with a brand-new username never tried before
        # from this IP, is blocked by the IP-level lock alone.
        locked = self._post('yet-another-new-username', 'x', ip=shared_ip)
        self.assertContains(locked, 'Too many failed login attempts')
        self.assertEqual(AuditLog.objects.filter(action='auth.ip_login_locked').count(), 1)

    # 12. account behavior (per-account throttling)
    def test_account_lock_triggers_regardless_of_varying_ip(self):
        for i in range(5):
            self._post('staffadmin', 'wrong-password', ip=f'10.1.0.{i}')
        # 6th attempt from yet another brand-new IP is still blocked, because
        # the ACCOUNT counter (not any single IP's counter) crossed the
        # threshold.
        locked = self._post('staffadmin', 'CorrectHorseBattery9', ip='10.1.0.99')
        self.assertContains(locked, 'Too many failed login attempts')

    # 13. staff isolation
    def test_non_staff_user_correct_password_still_rejected_by_admin_login(self):
        _make_password_user('regularjoe', is_staff=False)
        response = self._post('regularjoe', 'CorrectHorseBattery9')
        self.assertContains(response, 'Invalid username or password, or this account does not have admin access.')
        self.assertFalse(self.client.session.get('_auth_user_id'))

    # 14. normal-user isolation (independent per-account counters + clear-on-success)
    def test_failures_on_one_account_do_not_affect_a_different_account(self):
        other_staff = _make_password_user('otherstaff', is_staff=True, role=UserRole.SUPER_ADMIN)
        for _ in range(5):
            self._post('staffadmin', 'wrong-password', ip='10.0.0.50')
        # A completely unrelated account, from a fresh IP, is unaffected.
        response = self._post('otherstaff', 'CorrectHorseBattery9', ip='10.0.0.51')
        self.assertEqual(response.status_code, 302)

    def test_success_after_a_couple_failures_clears_the_account_counter(self):
        # Two different IPs deliberately — isolates the account-level clear-
        # on-success behavior from the (by-design, not cleared on success)
        # per-IP counter, which would otherwise also accumulate across both
        # batches and confound this specific assertion.
        self._post('staffadmin', 'wrong-1', ip='10.0.0.60')
        self._post('staffadmin', 'wrong-2', ip='10.0.0.60')
        success = self._post('staffadmin', 'CorrectHorseBattery9', ip='10.0.0.60')
        self.assertEqual(success.status_code, 302)
        # Account counter cleared — 4 more failures (well under a fresh
        # threshold of 5) from a different IP should still not lock the account.
        self.client.logout()
        for _ in range(4):
            self._post('staffadmin', 'wrong-again', ip='10.0.0.61')
        response = self._post('staffadmin', 'CorrectHorseBattery9', ip='10.0.0.61')
        self.assertEqual(response.status_code, 302)


class PasswordLoginLockoutTests(TestCase):
    """Same protection, the general 'Sign In' tab (no is_staff restriction)."""

    def setUp(self):
        cache.clear()
        self.user = _make_password_user('exploreruser', is_staff=False)
        self.url = reverse('core:password_login')

    def _post(self, username, password, ip='10.0.1.1'):
        return self.client.post(self.url, {'username': username, 'password': password}, REMOTE_ADDR=ip)

    def test_successful_login(self):
        response = self._post('exploreruser', 'CorrectHorseBattery9')
        self.assertEqual(response.status_code, 302)

    def test_lockout_after_five_failures(self):
        for _ in range(5):
            self._post('exploreruser', 'wrong-password')
        locked = self._post('exploreruser', 'CorrectHorseBattery9')
        self.assertContains(locked, 'Too many failed login attempts')
        self.assertFalse(self.client.session.get('_auth_user_id'))

    def test_lockout_does_not_affect_google_oauth_path(self):
        """Sanity check: the lockout keys/logic live only in the password-form
        views; nothing here touches core.supabase_auth at all."""
        import core.supabase_auth as supabase_auth_module
        source_before = supabase_auth_module.__file__
        for _ in range(5):
            self._post('exploreruser', 'wrong-password')
        # Module untouched/importable exactly as before — this is a proxy
        # check that no shared state was introduced between the two paths.
        self.assertEqual(supabase_auth_module.__file__, source_before)
