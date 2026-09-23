"""
Tests for /sitemap.xml (core/sitemaps.py). Guards against the exact gap this
file fixes: 7 public category/directory pages (repair-services,
places-to-visit, tuition-centers, student-services, marketplace,
scholarships, lost-found) plus the Scholarship/LostFound models' own detail
pages were previously absent from the sitemap entirely.

Runs entirely against the isolated SQLite test database (see
hello_kuppam/settings.py's 'test' in sys.argv override) — never touches the
real Supabase-backed database.
"""
import xml.etree.ElementTree as ET

from django.test import TestCase
from django.urls import reverse

from core.api.tests import factories as f
from core.models import ListingStatus


class SitemapTests(TestCase):
    def sitemap_xml(self):
        response = self.client.get(reverse('sitemap'))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/xml')
        return response.content

    def locs(self, xml_bytes):
        # Namespace-agnostic: strip the sitemap.org namespace so plain 'loc'
        # tag lookups work regardless of the exact xmlns Django emits.
        root = ET.fromstring(xml_bytes)
        return [el.text for el in root.iter() if el.tag.endswith('}loc') or el.tag == 'loc']

    def test_valid_xml(self):
        """The response is well-formed XML — this is the one thing that
        breaks the ENTIRE sitemap (and search engine trust in it) if any
        single URL's reverse() ever raises or renders malformed output."""
        xml_bytes = self.sitemap_xml()
        try:
            ET.fromstring(xml_bytes)
        except ET.ParseError as exc:
            self.fail(f'/sitemap.xml is not valid XML: {exc}')

    def test_previously_missing_category_pages_are_now_included(self):
        locs = self.locs(self.sitemap_xml())
        for path in [
            '/repair-services/', '/places-to-visit/', '/tuition-centers/',
            '/student-services/', '/marketplace/', '/scholarships/', '/lost-found/',
        ]:
            self.assertTrue(
                any(loc.endswith(path) for loc in locs),
                f'{path} is missing from the sitemap',
            )

    def test_privacy_and_terms_are_included(self):
        locs = self.locs(self.sitemap_xml())
        for path in ['/privacy-policy/', '/terms-of-service/']:
            self.assertTrue(any(loc.endswith(path) for loc in locs), f'{path} is missing from the sitemap')

    def test_city_home_is_not_duplicated_with_home(self):
        """core:city_home ('/c/<slug>/') must not appear alongside '/' —
        indexing both would be duplicate content once a city is active."""
        f.make_city('Kuppam', slug='kuppam')
        locs = self.locs(self.sitemap_xml())
        self.assertFalse(any('/c/' in loc for loc in locs), 'a /c/<slug>/ URL leaked into the sitemap')

    def test_no_duplicate_locs(self):
        locs = self.locs(self.sitemap_xml())
        self.assertEqual(len(locs), len(set(locs)), 'sitemap contains duplicate <loc> entries')

    def test_approved_scholarship_and_lostfound_detail_pages_are_included(self):
        city = f.make_city('Kuppam')
        scholarship = f.make_scholarship(city=city, status=ListingStatus.APPROVED, is_active=True)
        lostfound = f.make_lostfound(city=city, status=ListingStatus.APPROVED, is_active=True)
        locs = self.locs(self.sitemap_xml())
        self.assertTrue(any(loc.endswith(f'/scholarships/{scholarship.slug}/') for loc in locs))
        self.assertTrue(any(loc.endswith(f'/lost-found/{lostfound.slug}/') for loc in locs))

    def test_pending_scholarship_is_excluded(self):
        """Only approved + active rows are indexable — mirrors every other
        listing sitemap's contract (_ListingSitemap.items())."""
        city = f.make_city('Kuppam')
        pending = f.make_scholarship(city=city, status=ListingStatus.PENDING, is_active=True)
        locs = self.locs(self.sitemap_xml())
        self.assertFalse(any(loc.endswith(f'/scholarships/{pending.slug}/') for loc in locs))

    def test_inactive_lostfound_is_excluded(self):
        city = f.make_city('Kuppam')
        inactive = f.make_lostfound(city=city, status=ListingStatus.APPROVED, is_active=False)
        locs = self.locs(self.sitemap_xml())
        self.assertFalse(any(loc.endswith(f'/lost-found/{inactive.slug}/') for loc in locs))
