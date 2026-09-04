(function () {
    'use strict';

    var button = document.getElementById('hkNearbyRepairBtn');
    var grid = document.getElementById('hkNearbyRepairGrid');
    var status = document.getElementById('hkNearbyRepairStatus');
    if (!button || !grid || !status) return;

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function mapsUrl(business) {
        if (business.maps_link) return business.maps_link;
        return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(business.address || '');
    }

    function render(results) {
        if (!results.length) {
            grid.innerHTML = '<p class="text-muted mb-0">No repair shops found within ' +
                '25 km of your current location.</p>';
            return;
        }
        grid.innerHTML = results.map(function (business) {
            return '' +
                '<div class="card h-100 hk-card">' +
                '  <div class="hk-card-placeholder"><i class="bi ' + escapeHtml(business.placeholder_icon || 'bi-wrench-adjustable') + '"></i></div>' +
                '  <div class="card-body d-flex flex-column">' +
                '    <span class="badge bg-primary-subtle text-primary mb-2 align-self-start">' +
                        business.distance_km + ' km away</span>' +
                '    <h5 class="card-title">' + escapeHtml(business.name) + '</h5>' +
                '    <p class="card-text text-muted small mb-1"><i class="bi bi-geo-alt text-primary"></i> ' +
                        escapeHtml(business.address) + '</p>' +
                '    <p class="card-text text-muted small mb-3"><i class="bi bi-telephone text-primary"></i> ' +
                        escapeHtml(business.phone) + '</p>' +
                '    <a href="' + escapeHtml(business.url) + '" class="btn btn-outline-primary mt-auto">' +
                        'View Details <i class="bi bi-arrow-right"></i></a>' +
                '    <div class="hk-card-actions">' +
                '      <a href="' + escapeHtml(mapsUrl(business)) + '" target="_blank" rel="noopener" ' +
                        'class="hk-card-icon-btn" title="Open in Google Maps"><i class="bi bi-geo"></i></a>' +
                '    </div>' +
                '  </div>' +
                '</div>';
        }).join('');
    }

    button.addEventListener('click', function () {
        if (!navigator.geolocation) {
            status.textContent = 'Location is unavailable in this browser.';
            return;
        }
        button.disabled = true;
        status.textContent = 'Finding repair shops near you...';
        navigator.geolocation.getCurrentPosition(function (position) {
            var url = '/api/businesses/nearby-repair/?lat=' + position.coords.latitude +
                '&lng=' + position.coords.longitude;
            fetch(url, { credentials: 'same-origin' })
                .then(function (response) {
                    if (!response.ok) throw new Error('Search failed');
                    return response.json();
                })
                .then(function (data) {
                    status.textContent = '';
                    render(data.results || []);
                })
                .catch(function () {
                    status.textContent = 'We could not load nearby repair shops. Please try again.';
                })
                .finally(function () {
                    button.disabled = false;
                });
        }, function (error) {
            button.disabled = false;
            status.textContent = error.code === 1
                ? 'Location permission was denied.'
                : 'We could not access your location.';
        }, { timeout: 8000, maximumAge: 300000 });
    });
}());
