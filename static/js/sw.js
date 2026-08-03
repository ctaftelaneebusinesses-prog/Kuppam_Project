// OneTownCity service worker — Web Push only (no offline caching). Served
// at the site root (see core.views.service_worker + /sw.js in urls.py, not
// /static/js/sw.js) so its default scope covers the whole site instead of
// just /static/js/.

self.addEventListener('push', function (event) {
    let data = {};
    try {
        data = event.data ? event.data.json() : {};
    } catch (e) {
        data = { title: 'OneTownCity', body: event.data ? event.data.text() : '' };
    }

    const title = data.title || 'OneTownCity';
    const options = {
        body: data.body || '',
        tag: data.url || undefined,
        renotify: true,
        data: { url: data.url || '/' },
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    const url = (event.notification.data && event.notification.data.url) || '/';

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (windows) {
            for (const win of windows) {
                if (win.url === url && 'focus' in win) return win.focus();
            }
            if (clients.openWindow) return clients.openWindow(url);
        })
    );
});
