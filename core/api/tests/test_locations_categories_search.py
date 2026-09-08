from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import ListingStatus

from . import factories as f


class CityLocationTests(APITestCase):
    def setUp(self):
        self.kuppam = f.make_city('Kuppam')
        self.chittoor = f.make_city('Chittoor')

    def test_search_matches_by_name(self):
        response = self.client.get(reverse('api:cities'), {'q': 'Kup'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        names = {row['name'] for row in response.data}
        self.assertIn('Kuppam', names)
        self.assertNotIn('Chittoor', names)

    def test_search_empty_query_includes_active_cities(self):
        # The seed migration (core.0022_seed_indian_locations) pre-populates
        # real cities, so this can't assert an exact total — only that ours
        # are in there too.
        response = self.client.get(reverse('api:cities'))
        names = {row['name'] for row in response.data}
        self.assertIn('Kuppam', names)
        self.assertIn('Chittoor', names)

    def test_detail_by_id(self):
        response = self.client.get(reverse('api:city_detail', args=[self.kuppam.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['slug'], self.kuppam.slug)

    def test_detail_invalid_id_is_404(self):
        response = self.client.get(reverse('api:city_detail', args=[999999]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch('core.api.views.reverse_geocode')
    def test_reverse_geocode_success(self, mock_reverse_geocode):
        mock_reverse_geocode.return_value = {'cityId': self.kuppam.pk, 'city': 'Kuppam'}
        response = self.client.post(reverse('api:reverse_geocode'), {'latitude': 13.16, 'longitude': 78.23}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['city'], 'Kuppam')

    def test_reverse_geocode_missing_fields_is_validation_error(self):
        response = self.client.post(reverse('api:reverse_geocode'), {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)


class CategoryTests(APITestCase):
    def test_list_only_active(self):
        active = f.make_category('business', key='active-cat')
        inactive = f.make_category('business', key='inactive-cat')
        inactive.is_active = False
        inactive.save()
        response = self.client.get(reverse('api:categories'))
        keys = {row['key'] for row in response.data}
        self.assertIn(active.key, keys)
        self.assertNotIn(inactive.key, keys)

    def test_filter_by_listing_model(self):
        biz_cat = f.make_category('business', key='biz-cat-2')
        job_cat = f.make_category('job', key='job-cat-2')
        response = self.client.get(reverse('api:categories'), {'listing_model': 'job'})
        keys = {row['key'] for row in response.data}
        self.assertIn(job_cat.key, keys)
        self.assertNotIn(biz_cat.key, keys)


class SearchTests(APITestCase):
    def setUp(self):
        self.city = f.make_city('Kuppam')
        self.owner, _ = f.make_user()
        self.match = f.make_business(owner=self.owner, city=self.city, name='Kuppam Bakery', status=ListingStatus.APPROVED)
        self.pending_match = f.make_business(owner=self.owner, city=self.city, name='Kuppam Pending Bakery', status=ListingStatus.PENDING)
        self.no_match = f.make_business(owner=self.owner, city=self.city, name='Totally Unrelated', status=ListingStatus.APPROVED)

    def test_empty_query_returns_no_results(self):
        response = self.client.get(reverse('api:search'))
        self.assertEqual(response.data['total_results'], 0)

    def test_query_matches_only_public_listings(self):
        response = self.client.get(reverse('api:search'), {'q': 'Bakery'})
        business_section = next((s for s in response.data['results'] if s['model_key'] == 'business'), None)
        self.assertIsNotNone(business_section)
        names = {item['name'] for item in business_section['items']}
        self.assertIn('Kuppam Bakery', names)
        self.assertNotIn('Kuppam Pending Bakery', names)

    def test_query_with_no_matches_returns_empty_results(self):
        response = self.client.get(reverse('api:search'), {'q': 'zzzznomatchzzzz'})
        self.assertEqual(response.data['total_results'], 0)
        self.assertEqual(response.data['results'], [])
