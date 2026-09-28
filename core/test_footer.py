"""Site footer (templates/base.html): town skyline, business call-to-action,
DB-driven category links with icons, and the legal/contact rows."""
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse

from core.models import Category


class FooterTests(TestCase):
    def setUp(self):
        cache.clear()

    def test_business_cta_links_to_the_listing_request_flow(self):
        response = self.client.get(reverse('core:about'))
        self.assertContains(response, 'Put your business on the map')
        self.assertContains(response, f'href="{reverse("core:admin_request_new")}" class="hk-footer-cta-primary"', html=False)

    def test_categories_render_with_their_own_icons(self):
        category = Category.objects.filter(parent=None, is_active=True).exclude(icon='').first()
        self.assertIsNotNone(category, 'migrations seed the top-level categories')
        response = self.client.get(reverse('core:about'))
        self.assertContains(
            response,
            f'<li><a href="{category.list_url}"><span class="hk-footer-cat-icon"><i class="bi {category.icon}"></i></span>',
            html=False,
        )

    def test_skyline_is_decorative_and_legal_links_present(self):
        response = self.client.get(reverse('core:about'))
        self.assertContains(response, 'class="hk-footer-skyline"')
        self.assertContains(response, 'aria-hidden="true" focusable="false"')
        self.assertContains(response, reverse('core:privacy_policy'))
        self.assertContains(response, reverse('core:terms_of_service'))
        self.assertContains(response, 'action="{}"'.format(reverse('core:newsletter_subscribe')))
