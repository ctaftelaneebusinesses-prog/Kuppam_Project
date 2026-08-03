// OneTownCity — Web Push subscribe flow. Registers the service worker
// (sw.js) and, for a signed-in visitor who hasn't decided yet, shows a
// small dismissible banner offering to turn on real desktop/mobile
// notifications (so approvals, comments, etc. arrive even when the site
// isn't open — the in-app bell only shows what already happened by the
// time the page loads). window.HK_USER_AUTHENTICATED / HK_VAPID_PUBLIC_KEY
// are set in base.html's inline bootstrap script.
(function () {
    'use strict';

    if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;
    if (!window.HK_USER_AUTHENTICATED || !window.HK_VAPID_PUBLIC_KEY) return;
    if (typeof Notification === 'undefined') return;

    function getCookie(name) {
        var match = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
        return match ? match.pop() : '';
    }

    function postJSON(url, body) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRFToken': getCookie('csrftoken') },
            body: JSON.stringify(body),
        });
    }

    // Push subscription keys arrive as raw bytes; the VAPID public key has
    // to be handed to pushManager.subscribe() the same way.
    function urlBase64ToUint8Array(base64String) {
        var padding = '='.repeat((4 - (base64String.length % 4)) % 4);
        var base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
        var rawData = window.atob(base64);
        var outputArray = new Uint8Array(rawData.length);
        for (var i = 0; i < rawData.length; i++) outputArray[i] = rawData.charCodeAt(i);
        return outputArray;
    }

    function subscribe(registration) {
        return registration.pushManager
            .subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(window.HK_VAPID_PUBLIC_KEY) })
            .then(function (subscription) {
                return postJSON('/push/subscribe/', subscription.toJSON());
            });
    }

    function hideBanner(banner) {
        banner.classList.remove('is-visible');
        setTimeout(function () { banner.remove(); }, 300);
    }

    function showEnableBanner(registration) {
        if (localStorage.getItem('hkPushDismissed') === '1') return;

        var banner = document.createElement('div');
        banner.className = 'hk-push-banner';
        banner.innerHTML =
            '<span class="hk-push-banner-text"><i class="bi bi-bell"></i> Get notified the moment something needs your attention — even when OneTownCity is closed.</span>' +
            '<span class="hk-push-banner-actions">' +
            '<button type="button" class="btn btn-primary btn-sm" data-hk-push-enable>Enable</button>' +
            '<button type="button" class="btn btn-link btn-sm text-secondary" data-hk-push-dismiss>Not now</button>' +
            '</span>';
        document.body.appendChild(banner);
        requestAnimationFrame(function () { banner.classList.add('is-visible'); });

        banner.querySelector('[data-hk-push-enable]').addEventListener('click', function () {
            Notification.requestPermission().then(function (permission) {
                if (permission === 'granted') {
                    subscribe(registration).catch(function () {});
                }
                hideBanner(banner);
            });
        });
        banner.querySelector('[data-hk-push-dismiss]').addEventListener('click', function () {
            localStorage.setItem('hkPushDismissed', '1');
            hideBanner(banner);
        });
    }

    navigator.serviceWorker.register('/sw.js').then(function (registration) {
        if (Notification.permission === 'granted') {
            registration.pushManager.getSubscription().then(function (existing) {
                if (!existing) subscribe(registration).catch(function () {});
            });
        } else if (Notification.permission === 'default') {
            showEnableBanner(registration);
        }
    }).catch(function (err) {
        console.warn('OneTownCity: service worker registration failed.', err);
    });
})();
