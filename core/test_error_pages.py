from unittest.mock import patch

from django.test import Client, TestCase, override_settings
from django.urls import reverse


class ErrorPageRenderTests(TestCase):
    @override_settings(DEBUG=False)
    def test_404_page_renders_through_real_middleware_and_context_processors(self):
        client = Client(raise_request_exception=True)
        response = client.get('/this-page-does-not-exist-xyz/', secure=True)
        self.assertEqual(response.status_code, 404)
        self.assertIn(b'OneTownCity', response.content)
        self.assertIn(b'Page Not Found', response.content)

    @override_settings(DEBUG=False)
    @patch('core.views.render')
    def test_500_page_renders_when_a_real_view_raises(self, mock_render):
        mock_render.side_effect = Exception('simulated unhandled error')
        client = Client(raise_request_exception=False)
        response = client.get(reverse('core:home'), secure=True)
        self.assertEqual(response.status_code, 500)
        self.assertIn(b'OneTownCity', response.content)
        self.assertIn(b'Something Went Wrong', response.content)
