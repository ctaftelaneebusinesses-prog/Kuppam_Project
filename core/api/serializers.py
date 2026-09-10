"""
Read serializers for core.api. Deliberately read-only: writes go through
the same Django ModelForms (core.forms.LISTING_SUBMIT_FORMS,
CommentForm/ReviewForm/ReportForm) the website's own submission wizard
already uses (see core/api/views.py) — one validated implementation of
"what a Business/Comment/Review looks like", not a second copy in a
serializer.
"""
from rest_framework import serializers

from ..models import Category, Comment, Location, Notification, PostImage, PostVideo, Review
from ..views import LISTING_MODELS


class LocationSerializer(serializers.ModelSerializer):
    state = serializers.CharField(read_only=True)
    district = serializers.CharField(read_only=True)

    class Meta:
        model = Location
        fields = ['id', 'name', 'slug', 'kind', 'state', 'district', 'latitude', 'longitude']
        read_only_fields = fields


class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ['id', 'key', 'label', 'parent', 'listing_model', 'business_subcategory', 'icon', 'order']
        read_only_fields = fields


class PublicOwnerSerializer(serializers.Serializer):
    """
    Owner fields safe to hand to any client — never phone_number, address,
    email, role, supabase_uid, is_blocked/is_suspended, or anything else
    from Profile/auth.User a stranger shouldn't see. `instance` is a
    django.contrib.auth.User.
    """
    id = serializers.IntegerField()
    full_name = serializers.SerializerMethodField()
    display_photo = serializers.SerializerMethodField()

    def get_full_name(self, user):
        profile = getattr(user, 'profile', None)
        return (profile.full_name if profile and profile.full_name else None) or user.get_username()

    def get_display_photo(self, user):
        profile = getattr(user, 'profile', None)
        return profile.display_photo if profile else None


#: Shape-compatible stand-in for PublicOwnerSerializer(...).data when a
#: Comment/Review's `user` is NULL — the account was deleted (see
#: core.account_deletion; Comment.user/Review.user are on_delete=SET_NULL
#: precisely so the comment/review itself survives). Keeps the response
#: shape identical for clients instead of a hole where `user` used to be.
_DELETED_USER = {'id': None, 'full_name': 'Deleted User', 'display_photo': None}


# ---------------------------------------------------------------------------
# Listings — one ModelSerializer per listing type, built from a shared field
# list instead of six hand-written near-duplicates. Every field is read-only;
# create/update happens via the matching Django form (see views.py).
# ---------------------------------------------------------------------------

COMMON_LISTING_FIELDS = [
    'id', 'model_key', 'slug', 'status', 'is_public', 'listing_category', 'city', 'owner',
    'view_count', 'like_count', 'comment_count', 'review_count', 'avg_rating', 'share_count',
    'display_image', 'has_image', 'placeholder_icon', 'url', 'created_at', 'updated_at',
]

#: Fields unique to each listing type, beyond the shared ListingMixin ones
#: above — deliberately excludes rejection_reason/reviewed_by/reviewed_at
#: (moderation-internal, not part of the public/owner-facing contract here).
MODEL_SPECIFIC_FIELDS = {
    'business': ['name', 'category', 'address', 'phone_number', 'description', 'website', 'maps_link', 'is_featured', 'is_active'],
    'property': ['title', 'property_type', 'price', 'location', 'contact_number', 'description', 'is_featured', 'is_active'],
    'job': ['job_title', 'company', 'location', 'salary', 'contact_number', 'description', 'is_featured', 'is_active'],
    'event': ['title', 'event_date', 'location', 'contact_number', 'description', 'is_featured', 'is_active', 'is_upcoming'],
    'news': ['title', 'content', 'published_date', 'source', 'is_featured', 'is_active'],
    'project': ['title', 'project_status', 'location', 'expected_completion', 'department', 'description', 'is_featured', 'is_active'],
    'scholarship': [
        'title', 'scholarship_type', 'provider', 'description', 'eligibility', 'application_deadline',
        'official_url', 'contact_number', 'is_featured', 'is_active', 'is_open',
    ],
    'lostfound': [
        'report_type', 'title', 'item_category', 'description', 'event_date', 'location', 'contact_number',
        'is_resolved', 'is_active',
    ],
}


def _get_model_key(model_key):
    def get_model_key(self, obj):
        return model_key
    return get_model_key


def _get_owner(self, obj):
    return PublicOwnerSerializer(obj.owner).data if obj.owner_id else None


def _get_url(self, obj):
    return obj.get_absolute_url()


def _build_listing_serializer(model_key, model_cls):
    fields = COMMON_LISTING_FIELDS + MODEL_SPECIFIC_FIELDS[model_key]
    meta = type('Meta', (), {'model': model_cls, 'fields': fields, 'read_only_fields': fields})
    namespace = {
        'Meta': meta,
        'model_key': serializers.SerializerMethodField(),
        'get_model_key': _get_model_key(model_key),
        'listing_category': CategorySerializer(read_only=True),
        'city': LocationSerializer(read_only=True),
        'owner': serializers.SerializerMethodField(),
        'get_owner': _get_owner,
        'url': serializers.SerializerMethodField(),
        'get_url': _get_url,
    }
    return type(f'{model_cls.__name__}Serializer', (serializers.ModelSerializer,), namespace)


LISTING_SERIALIZERS = {key: _build_listing_serializer(key, model) for key, model in LISTING_MODELS.items()}


class CommentSerializer(serializers.ModelSerializer):
    user = serializers.SerializerMethodField()
    replies = serializers.SerializerMethodField()

    class Meta:
        model = Comment
        fields = ['id', 'user', 'body', 'parent_id', 'created_at', 'replies']
        read_only_fields = fields

    def get_user(self, obj):
        return PublicOwnerSerializer(obj.user).data if obj.user_id else _DELETED_USER

    def get_replies(self, obj):
        # Only populated when the view prefetched `replies` (top-level comment
        # list) — avoids an extra query per comment when it wasn't.
        cache = getattr(obj, '_prefetched_objects_cache', {})
        replies = cache.get('replies')
        if replies is None:
            return []
        return CommentSerializer(replies, many=True).data


class ReviewSerializer(serializers.ModelSerializer):
    user = serializers.SerializerMethodField()

    class Meta:
        model = Review
        fields = ['id', 'user', 'rating', 'body', 'created_at', 'updated_at']
        read_only_fields = fields

    def get_user(self, obj):
        return PublicOwnerSerializer(obj.user).data if obj.user_id else _DELETED_USER


class NotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notification
        fields = ['id', 'type', 'message', 'url', 'is_read', 'created_at']
        read_only_fields = fields


class PostImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = PostImage
        fields = ['id', 'image', 'order', 'created_at']
        read_only_fields = fields


class PostVideoSerializer(serializers.ModelSerializer):
    class Meta:
        model = PostVideo
        fields = ['id', 'video', 'order', 'created_at']
        read_only_fields = fields


class MeSerializer(serializers.Serializer):
    """The authenticated user's own profile — safe to include private fields, since it's only ever their own."""
    id = serializers.IntegerField(source='user.id')
    username = serializers.CharField(source='user.username')
    email = serializers.EmailField(source='user.email')
    role = serializers.CharField()
    full_name = serializers.CharField()
    phone_number = serializers.CharField()
    display_photo = serializers.CharField()
    address = serializers.CharField()
    city = serializers.CharField()
    state = serializers.CharField()
    pincode = serializers.CharField()
    intent = serializers.CharField()
    profile_completed = serializers.BooleanField()
    is_blocked = serializers.BooleanField()
    is_suspended = serializers.BooleanField()
