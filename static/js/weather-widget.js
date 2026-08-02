(function () {
    'use strict';

    var widget = document.getElementById('hkWeatherWidget');
    if (!widget) return;

    var emojiEl = document.getElementById('hkWeatherEmoji');
    var tempEl = document.getElementById('hkWeatherTemp');
    var conditionEl = document.getElementById('hkWeatherCondition');
    var windEl = document.getElementById('hkWeatherWind');

    // Kuppam, Chittoor district, Andhra Pradesh — used when geolocation is
    // unavailable or denied.
    var FALLBACK_COORDS = { latitude: 12.75, longitude: 78.23 };

    // WMO weather codes as returned by Open-Meteo's `weather_code` field.
    var WEATHER_CODES = {
        0: ['Clear sky', '☀️'],
        1: ['Mainly clear', '🌤️'],
        2: ['Partly cloudy', '⛅'],
        3: ['Overcast', '☁️'],
        45: ['Fog', '🌫️'],
        48: ['Fog', '🌫️'],
        51: ['Light drizzle', '🌦️'],
        53: ['Drizzle', '🌦️'],
        55: ['Dense drizzle', '🌦️'],
        56: ['Freezing drizzle', '🌧️'],
        57: ['Freezing drizzle', '🌧️'],
        61: ['Slight rain', '🌧️'],
        63: ['Rain', '🌧️'],
        65: ['Heavy rain', '🌧️'],
        66: ['Freezing rain', '🌧️'],
        67: ['Freezing rain', '🌧️'],
        71: ['Slight snow', '❄️'],
        73: ['Snow', '❄️'],
        75: ['Heavy snow', '❄️'],
        77: ['Snow grains', '❄️'],
        80: ['Rain showers', '🌦️'],
        81: ['Rain showers', '🌦️'],
        82: ['Violent rain showers', '⛈️'],
        85: ['Snow showers', '🌨️'],
        86: ['Snow showers', '🌨️'],
        95: ['Thunderstorm', '⛈️'],
        96: ['Thunderstorm with hail', '⛈️'],
        99: ['Thunderstorm with hail', '⛈️']
    };

    function describeWeatherCode(code) {
        return WEATHER_CODES[code] || ['Unknown', '🌡️'];
    }

    function renderWeather(data) {
        var current = data && data.current;
        if (!current) {
            renderError();
            return;
        }
        var described = describeWeatherCode(current.weather_code);
        var condition = described[0];
        var emoji = described[1];

        emojiEl.textContent = emoji;
        tempEl.textContent = Math.round(current.temperature_2m) + '°C';
        conditionEl.textContent = condition;
        windEl.textContent = Math.round(current.wind_speed_10m) + ' km/h';
    }

    function renderError() {
        emojiEl.textContent = '🌡️';
        tempEl.textContent = '--°C';
        conditionEl.textContent = 'Weather unavailable';
        windEl.textContent = '-- km/h';
    }

    function fetchWeather(coords) {
        var url = 'https://api.open-meteo.com/v1/forecast'
            + '?latitude=' + encodeURIComponent(coords.latitude)
            + '&longitude=' + encodeURIComponent(coords.longitude)
            + '&current=temperature_2m,wind_speed_10m,weather_code'
            + '&timezone=auto';

        fetch(url)
            .then(function (response) {
                if (!response.ok) throw new Error('Weather request failed');
                return response.json();
            })
            .then(renderWeather)
            .catch(renderError);
    }

    function resolveCoordsAndFetch() {
        if (!('geolocation' in navigator)) {
            fetchWeather(FALLBACK_COORDS);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            function (position) {
                fetchWeather({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude
                });
            },
            function () {
                fetchWeather(FALLBACK_COORDS);
            },
            { timeout: 8000, maximumAge: 600000 }
        );
    }

    resolveCoordsAndFetch();
})();
