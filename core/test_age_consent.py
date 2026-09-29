"""
18+ / Terms consent: captured at register and profile completion, asked once
of pre-existing accounts, enforced on every onboarding-gated page and on the
API. Nothing is ever assumed on a user's behalf — no ticked boxes, no consent.
"""
from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import Profile

from core.api.tests import factories as f

User = get_user_model()

REGISTER_DATA = {
    'full_name': 'New Person', 'email': 'new.person@example.com', 'phone_number': '9876543210',
    'password': 'Str0ng-Pass!word', 'confirm_password': 'Str0ng-Pass!word',
}


class RegisterConsentTests(TestCase):
    def setUp(self):
        cache.clear()

    def test_register_without_consent_creates_nothing(self):
        for extra in ({}, {'confirm_adult': 'on'}, {'accept_terms': 'on'}):
            with self.subTest(extra=extra):
                response = self.client.post(reverse('core:register'), {**REGISTER_DATA, **extra})
                self.assertEqual(response.status_code, 200)
                self.assertFalse(User.objects.filter(email='new.person@example.com').exists())

    def test_register_with_consent_records_timestamps(self):
        response = self.client.post(
            reverse('core:register'), {**REGISTER_DATA, 'confirm_adult': 'on', 'accept_terms': 'on'},
        )
        self.assertEqual(response.status_code, 302)
        profile = Profile.objects.get(user__email='new.person@example.com')
        self.assertTrue(profile.consent_confirmed)
        self.assertIsNotNone(profile.adult_confirmed_at)
        self.assertIsNotNone(profile.terms_accepted_at)

    def test_register_page_shows_consent_checkboxes_with_policy_links(self):
        response = self.client.get(reverse('core:google_login'))
        self.assertContains(response, 'name="confirm_adult"')
        self.assertContains(response, 'name="accept_terms"')
        self.assertContains(response, reverse('core:terms_of_service'))


class CompleteProfileConsentTests(TestCase):
    PROFILE_DATA = {'full_name': 'G User', 'phone_number': '9876543210'}

    def setUp(self):
        self.user = User.objects.create_user('guser', 'g@example.com', 'x')
        Profile.objects.create(user=self.user)
        self.client.force_login(self.user)

    def test_requires_both_boxes(self):
        response = self.client.post(reverse('core:complete_profile'), self.PROFILE_DATA)
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'You must be 18 or older')
        profile = Profile.objects.get(user=self.user)
        self.assertFalse(profile.profile_completed)
        self.assertIsNone(profile.adult_confirmed_at)

    def test_with_boxes_completes_profile_and_records_consent(self):
        response = self.client.post(
            reverse('core:complete_profile'), {**self.PROFILE_DATA, 'confirm_adult': 'on', 'accept_terms': 'on'},
        )
        self.assertEqual(response.status_code, 302)
        profile = Profile.objects.get(user=self.user)
        self.assertTrue(profile.profile_completed)
        self.assertTrue(profile.consent_confirmed)


class ExistingAccountConfirmationTests(TestCase):
    """Accounts that completed their profile before consent capture existed."""

    def setUp(self):
        self.user, self.profile = f.make_user(consented=False)
        self.profile.intent = 'explore'
        self.profile.save()
        self.client.force_login(self.user)

    def test_onboarding_gated_page_redirects_to_confirmation(self):
        response = self.client.get(reverse('core:my_favorites'))
        self.assertRedirects(response, reverse('core:confirm_age'), fetch_redirect_response=False)

    def test_confirmation_page_lists_both_boxes(self):
        response = self.client.get(reverse('core:confirm_age'))
        self.assertContains(response, 'name="confirm_adult"')
        self.assertContains(response, 'name="accept_terms"')

    def test_unticked_boxes_do_not_record_consent(self):
        response = self.client.post(reverse('core:confirm_age'), {'confirm_adult': 'on'})
        self.assertEqual(response.status_code, 200)
        self.profile.refresh_from_db()
        self.assertFalse(self.profile.consent_confirmed)
        self.assertIsNone(self.profile.adult_confirmed_at)

    def test_ticking_both_records_consent_and_continues(self):
        response = self.client.post(reverse('core:confirm_age'), {'confirm_adult': 'on', 'accept_terms': 'on'})
        self.assertEqual(response.status_code, 302)
        self.profile.refresh_from_db()
        self.assertTrue(self.profile.consent_confirmed)
        self.assertEqual(self.client.get(reverse('core:my_favorites')).status_code, 200)

    def test_already_confirmed_user_skips_the_page(self):
        self.profile.record_consent()
        response = self.client.get(reverse('core:confirm_age'))
        self.assertEqual(response.status_code, 302)

    def test_record_consent_keeps_earlier_timestamps(self):
        self.profile.record_consent()
        first = self.profile.adult_confirmed_at
        self.profile.record_consent()
        self.assertEqual(self.profile.adult_confirmed_at, first)


class APIConsentTests(APITestCase):
    def setUp(self):
        cache.clear()
        self.user, self.profile = f.make_user(consented=False)
        self.client.force_authenticate(self.user)

    def test_unconfirmed_account_is_refused_with_consent_code(self):
        response = self.client.get(reverse('api:my_favorites'))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(response.data['error']['code'], 'consent_required')

    def test_unconfirmed_account_can_still_read_me_and_see_state(self):
        response = self.client.get(reverse('api:me'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data['consent_confirmed'])

    def test_unconfirmed_account_can_still_delete_itself(self):
        response = self.client.delete(reverse('api:me'), {'confirm': 'DELETE'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

    def test_confirming_requires_both_true(self):
        for payload in ({}, {'confirm_adult': True}, {'accept_terms': True}, {'confirm_adult': False, 'accept_terms': True}):
            with self.subTest(payload=payload):
                response = self.client.post(reverse('api:age_confirmation'), payload, format='json')
                self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.profile.refresh_from_db()
        self.assertFalse(self.profile.consent_confirmed)

    def test_confirming_unlocks_the_api(self):
        response = self.client.post(
            reverse('api:age_confirmation'), {'confirm_adult': True, 'accept_terms': True}, format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(self.client.get(reverse('api:my_favorites')).status_code, status.HTTP_200_OK)

    def test_anonymous_cannot_confirm(self):
        self.client.force_authenticate(None)
        response = self.client.post(reverse('api:age_confirmation'), {'confirm_adult': True, 'accept_terms': True}, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class PolicyAgeTextTests(TestCase):
    def test_policies_say_18_not_13(self):
        for name in ('core:privacy_policy', 'core:terms_of_service'):
            with self.subTest(page=name):
                response = self.client.get(reverse(name))
                self.assertContains(response, '18')
                self.assertNotContains(response, 'under\n                13')
