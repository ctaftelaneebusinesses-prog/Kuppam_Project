(function () {
    'use strict';

    var selector = document.getElementById('hkLocationSelector');
    var modal = document.getElementById('hkLocationModal');
    if (!selector || !modal) return;

    var searchInput = document.getElementById('hkLocationSearch');
    var results = document.getElementById('hkLocationResults');
    var status = document.getElementById('hkLocationStatus');
    var currentButton = document.getElementById('hkUseCurrentLocation');
    var searchTimer;

    function csrfToken() {
        var match = document.cookie.match(/(?:^|; )csrftoken=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : '';
    }

    function showStatus(message, error) {
        status.textContent = message || '';
        status.classList.toggle('text-danger', !!error);
    }

    function render(items) {
        results.innerHTML = '';
        if (!items.length) {
            results.innerHTML = '<li class="list-group-item text-muted">No supported cities found.</li>';
            return;
        }
        items.forEach(function (item) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
            var text = document.createElement('span');
            var city = document.createElement('strong');
            var state = document.createElement('small');
            city.textContent = item.city;
            state.className = 'd-block text-muted';
            state.textContent = item.state || '';
            text.appendChild(city);
            text.appendChild(state);
            button.appendChild(text);
            var arrow = document.createElement('i');
            arrow.className = 'bi bi-arrow-right';
            button.appendChild(arrow);
            button.addEventListener('click', function () { select(item.cityId, 'manual_selection'); });
            results.appendChild(button);
        });
    }

    function search() {
        var query = searchInput.value.trim();
        fetch('/api/locations/search/?q=' + encodeURIComponent(query), { credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) throw new Error('City search unavailable');
                return response.json();
            })
            .then(render)
            .catch(function () { showStatus('City search is unavailable. Please try again.', true); });
    }

    function select(cityId, source) {
        showStatus('Updating your city...');
        fetch('/api/locations/select/', {
            method: 'POST', credentials: 'same-origin',
            headers: {'Content-Type': 'application/json', 'X-CSRFToken': csrfToken()},
            body: JSON.stringify({cityId: cityId, source: source})
        }).then(function (response) {
            if (!response.ok) throw new Error('Selection failed');
            return response.json();
        }).then(function (location) {
            try { localStorage.setItem('onetowncity_location', JSON.stringify(location)); } catch (e) {}
            window.location.href = '/c/' + encodeURIComponent(location.slug) + '/';
        }).catch(function () { showStatus('That city could not be selected. Please try again.', true); });
    }

    function useCurrentLocation() {
        if (!navigator.geolocation) {
            showStatus('Location is unavailable in this browser. Search for a city instead.', true);
            return;
        }
        currentButton.disabled = true;
        showStatus('Finding your city...');
        navigator.geolocation.getCurrentPosition(function (position) {
            fetch('/api/locations/reverse-geocode/', {
                method: 'POST', credentials: 'same-origin',
                headers: {'Content-Type': 'application/json', 'X-CSRFToken': csrfToken()},
                body: JSON.stringify({latitude: position.coords.latitude, longitude: position.coords.longitude})
            }).then(function (response) {
                return response.json().then(function (data) {
                    if (!response.ok) throw new Error(data.error || 'Location unavailable');
                    return data;
                });
            }).then(function (data) { select(data.cityId, 'geolocation'); })
                .catch(function (error) { showStatus(error.message || 'We could not resolve your city.', true); })
                .finally(function () { currentButton.disabled = false; });
        }, function (error) {
            currentButton.disabled = false;
            showStatus(error.code === 1 ? 'Location permission was denied. Search for a city instead.' : 'We could not access your location. Search for a city instead.', true);
        }, {timeout: 8000, maximumAge: 600000});
    }

    modal.addEventListener('shown.bs.modal', function () {
        searchInput.focus();
        search();
    });
    searchInput.addEventListener('input', function () {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(search, 250);
    });
    currentButton.addEventListener('click', useCurrentLocation);

    var saved;
    try { saved = JSON.parse(localStorage.getItem('onetowncity_location')); } catch (e) {}
    if (!window.HK_CURRENT_LOCATION && saved && saved.cityId) select(saved.cityId, 'saved_preference');
    else if (!window.HK_CURRENT_LOCATION && navigator.geolocation && (function () {
        try {
            if (localStorage.getItem('onetowncity_location_attempted')) return false;
            localStorage.setItem('onetowncity_location_attempted', '1');
        } catch (e) {}
        return true;
    }())) {
        navigator.geolocation.getCurrentPosition(function (position) {
            fetch('/api/locations/reverse-geocode/', {
                method: 'POST', credentials: 'same-origin',
                headers: {'Content-Type': 'application/json', 'X-CSRFToken': csrfToken()},
                body: JSON.stringify({latitude: position.coords.latitude, longitude: position.coords.longitude})
            }).then(function (response) { return response.ok ? response.json() : null; })
                .then(function (data) { if (data && data.cityId) select(data.cityId, 'geolocation'); })
                .catch(function () {});
        }, function () {}, {timeout: 8000, maximumAge: 600000});
    }
}());
