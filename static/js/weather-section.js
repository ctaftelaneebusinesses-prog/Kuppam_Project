/*
 * Homepage "Weather Now" section (home.html #weather). Asks the browser for
 * the visitor's position, names the place through the site's own
 * /api/locations/reverse-geocode/ (the same lookup the city picker uses), and
 * reads current conditions from Open-Meteo (free, no API key). When location
 * is denied or unavailable it shows a city search backed by Open-Meteo's
 * geocoding API. All copy comes from data-* attributes on the panel so it is
 * translated with the rest of the page; API values are only ever inserted as
 * text, never as HTML.
 *
 * Separate from weather-widget.js, the dashboard's compact chip, which reads
 * the visitor's saved city instead of their live position.
 */
(function () {
    'use strict';

    var panel = document.getElementById('hkWeatherSection');
    if (!panel) return;

    var nowBox = document.getElementById('hkWeatherNow');
    var iconEl = document.getElementById('hkWeatherIcon');
    var tempEl = document.getElementById('hkWeatherNowTemp');
    var conditionEl = document.getElementById('hkWeatherNowCondition');
    var placeEl = document.getElementById('hkWeatherPlace');
    var humidityEl = document.getElementById('hkWeatherHumidity');
    var windEl = document.getElementById('hkWeatherWindSpeed');
    var statusEl = document.getElementById('hkWeatherStatus');
    var searchForm = document.getElementById('hkWeatherSearch');
    var searchInput = document.getElementById('hkWeatherCityInput');
    var resultsEl = document.getElementById('hkWeatherResults');
    var searchToggle = document.getElementById('hkWeatherSearchToggle');

    var FORECAST_URL = 'https://api.open-meteo.com/v1/forecast';
    var GEOCODING_URL = 'https://geocoding-api.open-meteo.com/v1/search';

    // WMO weather codes (Open-Meteo `weather_code`) -> [label key, day icon, night icon].
    var CONDITIONS = {
        0: ['clear', 'bi-sun', 'bi-moon-stars'],
        1: ['mainly-clear', 'bi-cloud-sun', 'bi-cloud-moon'],
        2: ['partly-cloudy', 'bi-cloud-sun', 'bi-cloud-moon'],
        3: ['overcast', 'bi-clouds'],
        45: ['fog', 'bi-cloud-fog2'],
        48: ['fog', 'bi-cloud-fog2'],
        51: ['drizzle', 'bi-cloud-drizzle'],
        53: ['drizzle', 'bi-cloud-drizzle'],
        55: ['drizzle', 'bi-cloud-drizzle'],
        56: ['freezing-drizzle', 'bi-cloud-sleet'],
        57: ['freezing-drizzle', 'bi-cloud-sleet'],
        61: ['rain', 'bi-cloud-rain'],
        63: ['rain', 'bi-cloud-rain'],
        65: ['rain', 'bi-cloud-rain-heavy'],
        66: ['freezing-rain', 'bi-cloud-sleet'],
        67: ['freezing-rain', 'bi-cloud-sleet'],
        71: ['snow', 'bi-cloud-snow'],
        73: ['snow', 'bi-cloud-snow'],
        75: ['snow', 'bi-snow'],
        77: ['snow', 'bi-cloud-snow'],
        80: ['rain-showers', 'bi-cloud-drizzle'],
        81: ['rain-showers', 'bi-cloud-rain'],
        82: ['rain-showers', 'bi-cloud-rain-heavy'],
        85: ['snow-showers', 'bi-cloud-snow'],
        86: ['snow-showers', 'bi-cloud-snow'],
        95: ['thunderstorm', 'bi-cloud-lightning-rain'],
        96: ['thunderstorm', 'bi-cloud-lightning-rain'],
        99: ['thunderstorm', 'bi-cloud-lightning-rain']
    };

    // data-msg-your-location -> msg('your-location'); data-cond-rain -> cond('rain').
    function attr(prefix, key) {
        var camel = (prefix + '-' + key).replace(/-([a-z])/g, function (_, c) { return c.toUpperCase(); });
        return panel.dataset[camel] || '';
    }
    function msg(key) { return attr('msg', key); }

    function showStatus(text) {
        statusEl.textContent = text || '';
        statusEl.hidden = !text;
    }

    function openSearch(focus) {
        searchForm.hidden = false;
        searchToggle.setAttribute('aria-expanded', 'true');
        if (focus) searchInput.focus();
    }

    function csrfToken() {
        var match = document.cookie.match(/(?:^|; )csrftoken=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : '';
    }

    function getJSON(url, options) {
        return fetch(url, options).then(function (response) {
            if (!response.ok) throw new Error('Request failed: ' + response.status);
            return response.json();
        });
    }

    function render(current, placeName) {
        var condition = CONDITIONS[current.weather_code];
        var isNight = current.is_day === 0;
        var icon = condition ? ((isNight && condition[2]) || condition[1]) : 'bi-thermometer-half';

        iconEl.className = 'bi ' + icon;
        tempEl.textContent = Math.round(current.temperature_2m) + '°C';
        conditionEl.textContent = attr('cond', condition ? condition[0] : 'unknown');
        placeEl.textContent = placeName;
        humidityEl.textContent = Math.round(current.relative_humidity_2m) + '%';
        windEl.textContent = Math.round(current.wind_speed_10m) + ' km/h';
        nowBox.hidden = false;
        showStatus('');
    }

    function loadWeather(latitude, longitude, placeName) {
        showStatus(msg('loading'));
        var url = FORECAST_URL +
            '?latitude=' + encodeURIComponent(latitude) +
            '&longitude=' + encodeURIComponent(longitude) +
            '&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day' +
            '&wind_speed_unit=kmh&timezone=auto';
        return getJSON(url).then(function (data) {
            if (!data || !data.current) throw new Error('No current weather');
            render(data.current, placeName);
        }).catch(function () {
            nowBox.hidden = true;
            showStatus(msg('error'));
        });
    }

    // Names the visitor's position with the site's own city lookup; anywhere it
    // doesn't cover (outside the supported Indian cities) is just "Your location".
    function placeNameFor(latitude, longitude) {
        return getJSON('/api/locations/reverse-geocode/', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json', 'X-CSRFToken': csrfToken()},
            body: JSON.stringify({latitude: latitude, longitude: longitude})
        }).then(function (data) {
            return [data.city, data.state].filter(Boolean).join(', ') || msg('your-location');
        }).catch(function () {
            return msg('your-location');
        });
    }

    function useBrowserLocation() {
        if (!navigator.geolocation) {
            showStatus(msg('unavailable'));
            openSearch(false);
            return;
        }
        showStatus(msg('locating'));
        navigator.geolocation.getCurrentPosition(function (position) {
            var lat = position.coords.latitude;
            var lon = position.coords.longitude;
            placeNameFor(lat, lon).then(function (name) { loadWeather(lat, lon, name); });
        }, function (error) {
            showStatus(error && error.code === 1 ? msg('denied') : msg('unavailable'));
            openSearch(false);
        }, {timeout: 10000, maximumAge: 600000});
    }

    function placeLabel(result) {
        var parts = [result.name];
        if (result.admin1 && result.admin1 !== result.name) parts.push(result.admin1);
        if (result.country) parts.push(result.country);
        return parts.join(', ');
    }

    function showResults(results) {
        resultsEl.textContent = '';
        results.forEach(function (result) {
            var item = document.createElement('li');
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'hk-weather-result';
            var icon = document.createElement('i');
            icon.className = 'bi bi-geo-alt';
            icon.setAttribute('aria-hidden', 'true');
            button.appendChild(icon);
            button.appendChild(document.createTextNode(' ' + placeLabel(result)));
            button.addEventListener('click', function () {
                resultsEl.textContent = '';
                searchInput.value = result.name;
                loadWeather(result.latitude, result.longitude, placeLabel(result));
            });
            item.appendChild(button);
            resultsEl.appendChild(item);
        });
    }

    searchForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var query = searchInput.value.trim();
        if (!query) return;
        resultsEl.textContent = '';
        showStatus(msg('searching'));
        var lang = (document.documentElement.lang || 'en').slice(0, 2);
        getJSON(GEOCODING_URL + '?count=5&format=json&language=' + encodeURIComponent(lang) +
                '&name=' + encodeURIComponent(query))
            .then(function (data) {
                var results = (data && data.results) || [];
                if (!results.length) {
                    showStatus(msg('no-results'));
                } else if (results.length === 1) {
                    loadWeather(results[0].latitude, results[0].longitude, placeLabel(results[0]));
                } else {
                    showStatus('');
                    showResults(results);
                }
            })
            .catch(function () { showStatus(msg('error')); });
    });

    searchToggle.addEventListener('click', function () {
        if (searchForm.hidden) {
            openSearch(true);
        } else {
            searchForm.hidden = true;
            searchToggle.setAttribute('aria-expanded', 'false');
        }
    });

    useBrowserLocation();
})();
