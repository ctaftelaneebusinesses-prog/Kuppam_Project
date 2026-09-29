"""
Public legal/trust pages: refund policy, cookie policy, business details.
They state facts about the operator and the site, so the tests pin the facts
that must never silently drift (operator, official email, grievance officer,
"no charges") and that every page is reachable and linked site-wide.
"""
from django.test import TestCase
from django.urls import reverse

OFFICIAL_EMAIL = 'ctaftelaneebusinesses@gmail.com'
LEGAL_URL_NAMES = ['core:refund_policy', 'core:cookie_policy', 'core:business_details']


class LegalPagesTests(TestCase):
    def test_pages_render_publicly(self):
        for name in LEGAL_URL_NAMES:
            with self.subTest(page=name):
                response = self.client.get(reverse(name))
                self.assertEqual(response.status_code, 200)
                self.assertContains(response, 'Last updated')

    def test_business_details_names_operator_and_grievance_officer(self):
        response = self.client.get(reverse('core:business_details'))
        self.assertContains(response, 'Kamishetty Mallikarjuna')
        self.assertContains(response, 'CraftLanee')
        self.assertContains(response, 'Grievance Officer')
        self.assertContains(response, OFFICIAL_EMAIL)
        self.assertContains(response, 'not yet registered')

    def test_refund_policy_states_service_is_free(self):
        response = self.client.get(reverse('core:refund_policy'))
        self.assertContains(response, 'free to use')
        self.assertContains(response, 'nothing to refund')

    def test_cookie_policy_lists_cookies_actually_set(self):
        response = self.client.get(reverse('core:cookie_policy'))
        for cookie in ('sessionid', 'csrftoken', 'django_language'):
            self.assertContains(response, cookie)

    def test_footer_links_to_every_legal_page(self):
        response = self.client.get(reverse('core:cookie_policy'))
        for name in ['core:privacy_policy', 'core:terms_of_service'] + LEGAL_URL_NAMES:
            with self.subTest(page=name):
                self.assertContains(response, f'href="{reverse(name)}"')

    def test_privacy_policy_names_grievance_officer(self):
        response = self.client.get(reverse('core:privacy_policy'))
        self.assertContains(response, 'Kamishetty Mallikarjuna')

    def test_contact_page_uses_official_email(self):
        response = self.client.get(reverse('core:contact'))
        self.assertContains(response, OFFICIAL_EMAIL)
        self.assertNotContains(response, 'director@craftlanee.com')
