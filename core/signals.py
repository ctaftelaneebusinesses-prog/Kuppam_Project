from django.core.cache import cache
from django.db.models import Avg, Count
from django.db.models.signals import post_delete, post_save
from django.dispatch import receiver

from .context_processors import CATEGORY_TREE_CACHE_KEY
from .location_service import location_cache_key
from .middleware import MAINTENANCE_CACHE_KEY
from .models import (
    Business, Category, Comment, Event, Favorite, Job, Like, Location, News, PlatformSettings, Project, Property,
    Review, Share,
)
from .push import notify


def _target(instance):
    """Resolves the GenericForeignKey target for a Like/Comment/Review/Share row."""
    model_cls = instance.content_type.model_class()
    return model_cls.objects.filter(pk=instance.object_id).first()


def _recount(instance):
    obj = _target(instance)
    if obj is None:
        return

    ct = instance.content_type
    obj.like_count = Like.objects.filter(content_type=ct, object_id=obj.pk).count()
    obj.comment_count = Comment.objects.filter(content_type=ct, object_id=obj.pk).count()
    obj.share_count = Share.objects.filter(content_type=ct, object_id=obj.pk).count()

    review_stats = Review.objects.filter(content_type=ct, object_id=obj.pk).aggregate(
        avg=Avg('rating'), total=Count('id')
    )
    obj.review_count = review_stats['total'] or 0
    obj.avg_rating = round(review_stats['avg'], 2) if review_stats['avg'] else 0

    obj.save(update_fields=['like_count', 'comment_count', 'share_count', 'review_count', 'avg_rating'])


@receiver(post_save, sender=Like)
@receiver(post_delete, sender=Like)
@receiver(post_save, sender=Share)
@receiver(post_delete, sender=Share)
def on_like_or_share_changed(sender, instance, **kwargs):
    _recount(instance)


@receiver(post_save, sender=Comment)
@receiver(post_delete, sender=Comment)
def on_comment_changed(sender, instance, created=False, **kwargs):
    _recount(instance)
    if created and instance.user_id:
        obj = _target(instance)
        owner_id = getattr(obj, 'owner_id', None)
        if owner_id and owner_id != instance.user_id:
            notify(
                obj.owner, 'new_comment',
                f'{instance.user.get_username()} commented on your listing "{obj}"',
                url=obj.get_absolute_url() if hasattr(obj, 'get_absolute_url') else '',
            )


@receiver(post_save, sender=Category)
@receiver(post_delete, sender=Category)
def on_category_changed(sender, instance, **kwargs):
    cache.delete(CATEGORY_TREE_CACHE_KEY)


@receiver(post_save, sender=Location)
@receiver(post_delete, sender=Location)
def on_location_changed(sender, instance, **kwargs):
    cache.delete(location_cache_key(instance.pk))


@receiver(post_save, sender=PlatformSettings)
def on_platform_settings_changed(sender, instance, **kwargs):
    cache.delete(MAINTENANCE_CACHE_KEY)


#: Bumped on every homepage-listing save/delete so views._home_sections()
#: stops serving its cached rows immediately (the old version's keys just
#: expire on their own TTL) — an approval, edit or deletion shows up on the
#: homepage on the very next view instead of after the cache TTL.
HOME_SECTIONS_VERSION_KEY = 'core:home_sections_version'


def bump_home_sections_cache():
    try:
        cache.incr(HOME_SECTIONS_VERSION_KEY)
    except ValueError:
        cache.set(HOME_SECTIONS_VERSION_KEY, 1, None)


def on_home_listing_changed(sender, instance, **kwargs):
    bump_home_sections_cache()


for _listing_model in (Business, Property, Job, Event, News, Project):
    post_save.connect(on_home_listing_changed, sender=_listing_model, dispatch_uid=f'home_sections_{_listing_model.__name__}_save')
    post_delete.connect(on_home_listing_changed, sender=_listing_model, dispatch_uid=f'home_sections_{_listing_model.__name__}_delete')


@receiver(post_save, sender=Review)
@receiver(post_delete, sender=Review)
def on_review_changed(sender, instance, created=False, **kwargs):
    _recount(instance)
    if created and instance.user_id:
        obj = _target(instance)
        owner_id = getattr(obj, 'owner_id', None)
        if owner_id and owner_id != instance.user_id:
            notify(
                obj.owner, 'new_review',
                f'{instance.user.get_username()} left a {instance.rating}-star review on "{obj}"',
                url=obj.get_absolute_url() if hasattr(obj, 'get_absolute_url') else '',
            )
