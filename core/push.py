"""
Web Push dispatch — the desktop/mobile OS-level notifications layer that
sits alongside (not instead of) the in-app Notification bell. The bell only
shows what happened by the time someone next loads the page; this fires a
real push through the browser's push service (Chrome/FCM, Firefox/autopush,
etc.) so it arrives even if the site isn't open, on every device the user
has granted permission on.

Every place in the codebase that creates a Notification row should go
through `notify()`/`notify_bulk()` here instead of calling
`Notification.objects.create()` directly, so the two channels never drift
out of sync.
"""
import json
import logging

from django.conf import settings
from django.urls import reverse

from .models import Notification, PushSubscription

logger = logging.getLogger(__name__)


def _send_web_push(subscription, title, body, url):
    from pywebpush import WebPushException, webpush

    try:
        webpush(
            subscription_info={
                'endpoint': subscription.endpoint,
                'keys': {'p256dh': subscription.p256dh, 'auth': subscription.auth},
            },
            data=json.dumps({'title': title, 'body': body, 'url': url or '/'}),
            vapid_private_key=settings.VAPID_PRIVATE_KEY,
            vapid_claims={'sub': f'mailto:{settings.VAPID_ADMIN_EMAIL}'},
        )
    except WebPushException as exc:
        status = exc.response.status_code if exc.response is not None else None
        if status in (404, 410):
            # Browser says this subscription is gone for good (uninstalled,
            # permission revoked, endpoint expired) — stop trying it.
            subscription.delete()
        else:
            logger.warning('Web push failed for subscription %s: %s', subscription.pk, exc)


def send_web_push_to_user(user, title, body, url):
    """Pushes to every device `user` has subscribed on. No-op if VAPID isn't configured yet."""
    if not settings.VAPID_PRIVATE_KEY:
        return
    for subscription in PushSubscription.objects.filter(user=user):
        _send_web_push(subscription, title, body, url)


def notify(recipient, type, message, url=''):
    """
    Creates the in-app Notification row for `recipient` and, if they have
    any push subscriptions, fires a real Web Push alongside it. Use this
    (or notify_bulk) instead of `Notification.objects.create(...)` directly.

    The push's click-through target is notification_open (mark-read +
    redirect), not the raw `url`, so clicking a push notification behaves
    exactly like clicking it in the in-app bell tray — one open, one place
    that marks it read.
    """
    notification = Notification.objects.create(recipient=recipient, type=type, message=message, url=url)
    send_web_push_to_user(
        notification.recipient_id, 'OneTownCity', message,
        reverse('core:notification_open', args=[notification.pk]),
    )


def notify_bulk(recipients, type, message, url=''):
    """Same as notify(), for a list/queryset of recipients (e.g. every Super Admin)."""
    recipients = list(recipients)
    notifications = Notification.objects.bulk_create([
        Notification(recipient=user, type=type, message=message, url=url) for user in recipients
    ])
    for notification in notifications:
        send_web_push_to_user(
            notification.recipient_id, 'OneTownCity', message,
            reverse('core:notification_open', args=[notification.pk]),
        )
