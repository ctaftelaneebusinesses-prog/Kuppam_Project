"""
The homepage "Weather Now" section (home.html #weather). The weather itself is
fetched client-side by static/js/weather-section.js, so these tests cover what
the server is responsible for: the section, its search fallback, its
translated copy and the script being on the page.
"""
from django.test import TestCase
from django.urls import reverse


class WeatherSectionTests(TestCase):
    def test_homepage_has_weather_section_and_script(self):
        response = self.client.get(reverse('core:home'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'id="hkWeatherSection"')
        self.assertContains(response, 'Weather Now')
        self.assertContains(response, 'js/weather-section.')
        # Every field the section shows has its slot.
        for element_id in ('hkWeatherPlace', 'hkWeatherNowTemp', 'hkWeatherNowCondition',
                           'hkWeatherHumidity', 'hkWeatherWindSpeed'):
            self.assertContains(response, f'id="{element_id}"')

    def test_city_search_fallback_is_rendered_hidden(self):
        response = self.client.get(reverse('core:home'))
        self.assertContains(response, '<form class="hk-weather-search hk-no-loading-state" id="hkWeatherSearch" role="search" hidden>', html=False)
        self.assertContains(response, 'id="hkWeatherCityInput"')

    def test_messages_for_the_script_come_from_the_template(self):
        response = self.client.get(reverse('core:home'))
        self.assertContains(response, 'data-msg-denied="Location access is off. Search for a city to see its weather."')
        self.assertContains(response, 'data-cond-thunderstorm="Thunderstorm"')

    def test_dashboard_weather_widget_is_untouched(self):
        # The section is homepage-only; the dashboard keeps its own compact widget script.
        response = self.client.get(reverse('core:home'))
        self.assertNotContains(response, 'js/weather-widget.')
