from django.core.cache import cache
from django.db.models import Avg, Count
from django.db.models.signals import post_delete, post_save
from django.dispatch import receiver

from .context_processors import CATEGORY_TREE_CACHE_KEY, SITE_THEME_CACHE_KEY
from .models import Category, Comment, Favorite, Like, Notification, Review, Share, SiteSettings


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
    if created:
        obj = _target(instance)
        owner_id = getattr(obj, 'owner_id', None)
        if owner_id and owner_id != instance.user_id:
            Notification.objects.create(
                recipient_id=owner_id,
                type='new_comment',
                message=f'{instance.user.get_username()} commented on your listing "{obj}"',
                url=obj.get_absolute_url() if hasattr(obj, 'get_absolute_url') else '',
            )


@receiver(post_save, sender=Category)
@receiver(post_delete, sender=Category)
def on_category_changed(sender, instance, **kwargs):
    cache.delete(CATEGORY_TREE_CACHE_KEY)


@receiver(post_save, sender=SiteSettings)
def on_site_settings_changed(sender, instance, **kwargs):
    cache.delete(SITE_THEME_CACHE_KEY)


@receiver(post_save, sender=Review)
@receiver(post_delete, sender=Review)
def on_review_changed(sender, instance, created=False, **kwargs):
    _recount(instance)
    if created:
        obj = _target(instance)
        owner_id = getattr(obj, 'owner_id', None)
        if owner_id and owner_id != instance.user_id:
            Notification.objects.create(
                recipient_id=owner_id,
                type='new_review',
                message=f'{instance.user.get_username()} left a {instance.rating}-star review on "{obj}"',
                url=obj.get_absolute_url() if hasattr(obj, 'get_absolute_url') else '',
            )
