"""Properties page redesign: hero/search/type tiles render from real data,
type tiles link to working filters, and prices use compact Indian units."""
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse

from core.models import ListingStatus, Property
from core.templatetags.hk_extras import inr_short


def _property(title, property_type, price):
    return Property.objects.create(
        title=title, property_type=property_type, price=price, location='Main Road, Kuppam',
        contact_number='9876543210', status=ListingStatus.APPROVED, is_active=True,
    )


class InrShortFilterTests(TestCase):
    def test_crore_lakh_and_plain_amounts(self):
        self.assertEqual(inr_short(12500000), '1.25 Cr')
        self.assertEqual(inr_short(10000000), '1 Cr')
        self.assertEqual(inr_short(1500000), '15 Lakh')
        self.assertEqual(inr_short(150000), '1.5 Lakh')
        self.assertEqual(inr_short(18000), '18,000')
        self.assertEqual(inr_short('not a number'), '')


class PropertyListPageTests(TestCase):
    def setUp(self):
        cache.clear()
        self.rental = _property('Two BHK Near Bus Stand', 'rent', 18000)
        self.plot = _property('Corner Residential Plot', 'plot', 1500000)

    def test_hero_search_and_real_listing_count(self):
        response = self.client.get(reverse('core:property_list'))
        self.assertContains(response, 'Find Your Perfect')
        self.assertContains(response, 'Search Properties')
        self.assertContains(response, '2 properties listed')

    def test_type_tiles_show_real_counts_and_filter(self):
        response = self.client.get(reverse('core:property_list'))
        tiles = {tile['value']: tile['count'] for tile in response.context['type_tiles']}
        self.assertEqual(tiles['rent'], 1)
        self.assertEqual(tiles['plot'], 1)
        self.assertEqual(tiles['villa'], 0)
        self.assertNotIn('other', tiles)
        self.assertContains(response, '?type=plot')

        filtered = self.client.get(reverse('core:property_list'), {'type': 'plot'})
        self.assertContains(filtered, self.plot.title)
        self.assertNotContains(filtered, self.rental.title)
        self.assertContains(filtered, 'Clear filters')

    def test_cards_show_compact_price_and_monthly_rent(self):
        response = self.client.get(reverse('core:property_list'))
        self.assertContains(response, '&#8377;15 Lakh')
        self.assertContains(response, '&#8377;18,000<span class="hk-lp-card-per"> /month</span>', html=False)

    def test_type_counts_refresh_after_a_new_listing(self):
        self.client.get(reverse('core:property_list'))
        _property('Hilltop Villa', 'villa', 9000000)
        response = self.client.get(reverse('core:property_list'))
        tiles = {tile['value']: tile['count'] for tile in response.context['type_tiles']}
        self.assertEqual(tiles['villa'], 1)
