"""
core.api — the shared read/write contract for both the web app's own AJAX
calls and a future native Android client. Function-based DRF views (matching
the rest of this project — see core/views.py), built almost entirely out of
existing pieces: the same querysets (_public_qs/_detail_qs), the same
ownership predicate (_can_manage_post), the same listing-submission state
machine (apply_new_listing_submission/apply_listing_edit_state), the same
Django forms for validation, and the same rate limiter — nothing here is a
second implementation of business rules that already live in core/views.py.

Every endpoint validates its own input and re-derives permissions from
`request.user` server-side; no user id, owner id, listing id, city id, or
role ever comes from the client as something to be trusted outright.
"""
from django.contrib.auth import logout as django_logout
from django.contrib.contenttypes.models import ContentType
from django.contrib.contenttypes.prefetch import GenericPrefetch
from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import DatabaseError
from django.db.models import Prefetch
from django.urls import reverse
from django.utils import timezone

from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.exceptions import NotAuthenticated, NotFound, PermissionDenied, ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from ..account_deletion import AccountDeletionError, delete_user_account
from ..decorators import rate_limit
from ..forms import LISTING_SUBMIT_FORMS, CommentForm, ReportForm, ReviewForm
from ..location_service import reverse_geocode, search_cities
from ..models import Category, Comment, Favorite, Like, Location, Notification, PostImage, PostVideo, Report, Review
from ..push import notify
from ..views import (
    GALLERY_IMAGE_MAX_BYTES, GALLERY_IMAGE_TYPES, GALLERY_VIDEO_MAX_BYTES, GALLERY_VIDEO_TYPES, LISTING_MODELS,
    MAX_LISTINGS_PER_MODEL_SCAN, _SEARCH_FILTERS, _bump_views, _can_manage_post, _get_owned_media, _public_qs,
    _validate_gallery_files, apply_listing_edit_state, apply_new_listing_submission,
)
from .pagination import StandardResultsSetPagination
from .permissions import IsActiveAccount, get_profile
from .serializers import (
    LISTING_SERIALIZERS, CategorySerializer, CommentSerializer, LocationSerializer, MeSerializer,
    NotificationSerializer, PostImageSerializer, PostVideoSerializer, ReviewSerializer,
)

AUTH_REQUIRED = [IsAuthenticated, IsActiveAccount]

# Every listing serializer nests `owner` (PublicOwnerSerializer) and `city`
# (LocationSerializer, whose .state/.district properties walk city.parent /
# city.parent.parent — see core/models.py's Location) — without prefetching
# the city's parent chain too, each serialized row costs 1-2 extra queries
# just for LocationSerializer. Confirmed via CaptureQueriesContext: a
# 20-result business listing page issued 40 extra core_location queries
# before this was added (one per .state/.district access).
_LISTING_SELECT_RELATED = ('owner__profile', 'listing_category', 'city', 'city__parent', 'city__parent__parent')


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------

@api_view(['GET', 'DELETE'])
@permission_classes(AUTH_REQUIRED)
def me(request):
    """
    GET: the authenticated caller's own profile — works for either a session
    (web) or a Supabase bearer token (native).

    DELETE: permanently deletes the caller's own account and personal data
    (core.account_deletion.delete_user_account — the same function the
    web's "Delete My Account" page calls, so both clients delete an account
    identically). Ownership can't be bypassed since there's no id in the
    URL — it always acts on request.user, never a client-supplied id.
    Requires {"confirm": "DELETE"} in the request body so a client bug that
    fires an empty DELETE can't silently wipe an account; this mirrors the
    "type DELETE to confirm" step the web flow requires (these accounts have
    no usable password to re-prompt for — see core.supabase_auth).
    """
    if request.method == 'GET':
        return Response(MeSerializer(get_profile(request.user)).data)

    if request.data.get('confirm') != 'DELETE':
        raise ValidationError({'confirm': 'Send {"confirm": "DELETE"} to permanently delete your account.'})

    try:
        delete_user_account(request.user)
    except AccountDeletionError as exc:
        raise NotFound(str(exc))

    django_logout(request)
    return Response(status=status.HTTP_204_NO_CONTENT)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def logout_view(request):
    django_logout(request)
    return Response(status=status.HTTP_204_NO_CONTENT)


# ---------------------------------------------------------------------------
# City / location
# ---------------------------------------------------------------------------

def _resolve_city(request):
    slug = request.GET.get('city', '').strip()
    if not slug:
        return None
    return Location.objects.filter(slug=slug, kind=Location.Kind.CITY, is_active=True).first()


@api_view(['GET'])
def cities(request):
    try:
        results = search_cities(request.GET.get('q', ''))
    except DatabaseError:
        return Response(
            {'error': {'code': 'service_unavailable', 'message': 'City search is temporarily unavailable.'}},
            status=status.HTTP_503_SERVICE_UNAVAILABLE,
        )
    return Response(LocationSerializer(results, many=True).data)


@api_view(['GET'])
def city_detail(request, pk):
    city = Location.objects.filter(pk=pk, kind=Location.Kind.CITY, is_active=True).first()
    if city is None:
        raise NotFound('City not found.')
    return Response(LocationSerializer(city).data)


@api_view(['POST'])
def reverse_geocode_view(request):
    try:
        result = reverse_geocode(request.data.get('latitude'), request.data.get('longitude'))
    except (TypeError, ValueError, DjangoValidationError, OSError) as exc:
        raise ValidationError(str(exc) or 'We could not resolve that location. Please choose a city manually.')
    return Response(result)


# ---------------------------------------------------------------------------
# Categories
# ---------------------------------------------------------------------------

@api_view(['GET'])
def categories(request):
    qs = Category.objects.filter(is_active=True).select_related('parent')
    listing_model = request.GET.get('listing_model', '').strip()
    if listing_model:
        qs = qs.filter(listing_model=listing_model)
    return Response(CategorySerializer(qs.order_by('order', 'label'), many=True).data)


# ---------------------------------------------------------------------------
# Search
# ---------------------------------------------------------------------------

@api_view(['GET'])
def search_view(request):
    query = request.GET.get('q', '').strip()[:100]
    if not query:
        return Response({'query': '', 'total_results': 0, 'results': []})

    city = _resolve_city(request)
    try:
        limit = min(max(int(request.GET.get('limit', 6)), 1), 20)
    except (TypeError, ValueError):
        limit = 6

    sections = []
    for key, model_cls in LISTING_MODELS.items():
        qs = _public_qs(model_cls)
        if city:
            qs = qs.filter(city=city)
        qs = qs.filter(_SEARCH_FILTERS[key](query))
        count = qs.count()
        if count:
            sections.append({
                'model_key': key,
                'count': count,
                'items': LISTING_SERIALIZERS[key](qs.select_related(*_LISTING_SELECT_RELATED)[:limit], many=True).data,
            })

    return Response({
        'query': query,
        'total_results': sum(s['count'] for s in sections),
        'results': sections,
    })


# ---------------------------------------------------------------------------
# Listings
# ---------------------------------------------------------------------------

_LISTING_ORDERINGS = {'-created_at', 'created_at', '-view_count', '-avg_rating'}


def _get_visible_listing(request, model_cls, pk):
    """
    A listing this requester may view: anyone sees public rows; the owner
    also sees their own regardless of status; a Super Admin sees everything
    — the same visibility rule core.views._detail_qs / listing_edit apply
    on the website, just returning None instead of raising Http404 so
    callers can 404 with a consistent API error envelope.
    """
    profile = get_profile(request.user)
    select = _LISTING_SELECT_RELATED
    if profile and profile.is_super_admin:
        return model_cls.objects.filter(pk=pk).select_related(*select).first()
    obj = _public_qs(model_cls).filter(pk=pk).select_related(*select).first()
    if obj is not None:
        return obj
    if profile is not None:
        return model_cls.objects.filter(pk=pk, owner=request.user).select_related(*select).first()
    return None


def _require_can_manage(request, obj):
    profile = get_profile(request.user)
    if profile is None or not _can_manage_post(profile, obj):
        raise PermissionDenied('You can only manage your own listings.')
    return profile


def _list_listings(request, model_key, model_cls):
    profile = get_profile(request.user)
    mine = request.GET.get('mine') == 'true'

    if mine:
        if profile is None:
            raise NotAuthenticated('Sign in to view your own listings.')
        qs = model_cls.objects.all() if profile.is_super_admin else model_cls.objects.filter(owner=request.user)
    else:
        qs = _public_qs(model_cls)
        city = _resolve_city(request)
        if city:
            qs = qs.filter(city=city)

    query = request.GET.get('q', '').strip()[:100]
    if query:
        qs = qs.filter(_SEARCH_FILTERS[model_key](query))

    category_param = request.GET.get('category', '').strip()
    if category_param:
        qs = qs.filter(category=category_param) if model_key == 'business' else qs.filter(listing_category__key=category_param)

    if model_key == 'event' and request.GET.get('upcoming') == 'true':
        qs = qs.filter(event_date__gte=timezone.localdate())

    ordering = request.GET.get('ordering', '')
    qs = qs.order_by(ordering) if ordering in _LISTING_ORDERINGS else qs.order_by('-created_at')

    paginator = StandardResultsSetPagination()
    page = paginator.paginate_queryset(qs.select_related(*_LISTING_SELECT_RELATED), request)
    return paginator.get_paginated_response(LISTING_SERIALIZERS[model_key](page, many=True).data)


def _create_listing(request, model_key, model_cls):
    if not request.user.is_authenticated:
        raise NotAuthenticated('Sign in to add a listing.')
    profile = get_profile(request.user)
    if profile.is_blocked or profile.is_suspended:
        raise PermissionDenied('This account cannot add listings.')

    category_key = (request.data.get('category_key') or '').strip()
    if not category_key:
        raise ValidationError({'category_key': ['This field is required (the Category.key to publish into).']})
    category = Category.objects.filter(key=category_key, is_active=True).first()
    if category is None:
        raise ValidationError({'category_key': ['Unknown category.']})
    if category.listing_model != model_key:
        raise ValidationError({'category_key': [f'This category does not accept {model_key} listings.']})
    if not profile.can_manage_category(category):
        raise PermissionDenied("You don't have permission to add listings in this category.")

    form = LISTING_SUBMIT_FORMS[model_key](request.data, request.FILES)
    if not form.is_valid():
        raise ValidationError(form.errors)

    obj = form.save(commit=False)
    obj.owner = request.user
    obj.listing_category = category
    apply_new_listing_submission(obj, profile, category, request.user)

    return Response(LISTING_SERIALIZERS[model_key](obj).data, status=status.HTTP_201_CREATED)


@api_view(['GET', 'POST'])
@permission_classes([IsActiveAccount])
def listing_collection(request, model_key):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    if request.method == 'POST':
        return _create_listing(request, model_key, model_cls)
    return _list_listings(request, model_key, model_cls)


@api_view(['GET', 'PATCH', 'PUT', 'DELETE'])
@permission_classes([IsActiveAccount])
def listing_detail(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')

    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')

    if request.method == 'GET':
        _bump_views(request, model_cls, obj.pk)
        return Response(LISTING_SERIALIZERS[model_key](obj).data)

    if not request.user.is_authenticated:
        raise NotAuthenticated('Sign in to manage this listing.')
    profile = _require_can_manage(request, obj)

    if request.method == 'DELETE':
        owner, title = obj.owner, str(obj)
        obj.delete()
        if owner and owner.id != request.user.id:
            notify(
                owner, 'listing_deleted',
                f'Your listing "{title}" was deleted by {"a Super Admin" if profile.is_super_admin else "an admin"}.',
                url=reverse('core:my_listings'),
            )
        return Response(status=status.HTTP_204_NO_CONTENT)

    # PATCH/PUT: the reused ModelForm re-validates the whole record, same as
    # the website's own edit form (dashboard/listing_submit.html) — not a
    # deep per-field partial update.
    form = LISTING_SUBMIT_FORMS[model_key](request.data, request.FILES, instance=obj)
    if not form.is_valid():
        raise ValidationError(form.errors)
    obj = form.save(commit=False)
    apply_listing_edit_state(obj, profile, model_key)
    return Response(LISTING_SERIALIZERS[model_key](obj).data)


@api_view(['GET'])
@permission_classes(AUTH_REQUIRED)
def my_listings_view(request):
    profile = get_profile(request.user)
    items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.all() if profile.is_super_admin else model_cls.objects.filter(owner=request.user)
        # owner__profile is required too, not just owner — LISTING_SERIALIZERS'
        # owner field reads obj.owner.profile (PublicOwnerSerializer), so
        # without it every single row was a separate query. Bounded per
        # model (MAX_LISTINGS_PER_MODEL_SCAN) for the same reason as
        # core.views.dashboard_pending_listings: this merges 6 models into
        # one Python-sorted list, so an unbounded per-model fetch (all of
        # them, for a Super Admin) grows without limit as the site does.
        qs = (
            qs.select_related(*_LISTING_SELECT_RELATED)
            .order_by('-created_at')[:MAX_LISTINGS_PER_MODEL_SCAN]
        )
        items.extend((obj.created_at, key, obj) for obj in qs)
    items.sort(key=lambda row: row[0], reverse=True)

    paginator = StandardResultsSetPagination()
    page = paginator.paginate_queryset(items, request)
    return paginator.get_paginated_response([LISTING_SERIALIZERS[key](obj).data for _, key, obj in page])


# ---------------------------------------------------------------------------
# Favorites / Likes
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes(AUTH_REQUIRED)
def my_favorites(request):
    # Same GenericPrefetch idiom as the web's my_favorites (core/views.py) —
    # content_object is a GenericForeignKey, so without this every favorited
    # item is its own query, and each item's serialized owner/city/category
    # would be N+1 again on top of that.
    qs = (
        Favorite.objects.filter(user=request.user)
        .select_related('content_type')
        .prefetch_related(GenericPrefetch('content_object', [
            model.objects.select_related(*_LISTING_SELECT_RELATED)
            for model in LISTING_MODELS.values()
        ]))
        .order_by('-created_at')
    )
    paginator = StandardResultsSetPagination()
    page = paginator.paginate_queryset(qs, request)

    model_key_by_cls = {model: key for key, model in LISTING_MODELS.items()}
    items = []
    for favorite in page:
        model_cls = favorite.content_type.model_class()
        model_key = model_key_by_cls.get(model_cls)
        obj = favorite.content_object
        if model_key is None or obj is None:
            continue
        items.append(LISTING_SERIALIZERS[model_key](obj).data)
    return paginator.get_paginated_response(items)


@api_view(['POST'])
@permission_classes(AUTH_REQUIRED)
def toggle_favorite_view(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')
    ct = ContentType.objects.get_for_model(model_cls)
    favorite, created = Favorite.objects.get_or_create(content_type=ct, object_id=obj.pk, user=request.user)
    if not created:
        favorite.delete()
    return Response({'favorited': created})


@api_view(['POST'])
@permission_classes(AUTH_REQUIRED)
def toggle_like_view(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')
    ct = ContentType.objects.get_for_model(model_cls)
    like, created = Like.objects.get_or_create(content_type=ct, object_id=obj.pk, user=request.user)
    if not created:
        like.delete()
    return Response({'liked': created})


# ---------------------------------------------------------------------------
# Comments / Reviews / Reports — same forms, same rate limits as the website
# ---------------------------------------------------------------------------

@rate_limit('add_comment', limit=10, window_seconds=300)
def _create_comment(request, ct, obj):
    profile = get_profile(request.user)
    if profile.is_blocked:
        raise PermissionDenied('This account cannot perform this action.')
    form = CommentForm(request.data)
    if not form.is_valid():
        raise ValidationError(form.errors)
    parent = None
    parent_id = form.cleaned_data.get('parent_id')
    if parent_id:
        parent = Comment.objects.filter(pk=parent_id, content_type=ct, object_id=obj.pk).first()
    comment = Comment.objects.create(
        content_type=ct, object_id=obj.pk, user=request.user, parent=parent, body=form.cleaned_data['body'],
    )
    return Response(CommentSerializer(comment).data, status=status.HTTP_201_CREATED)


@api_view(['GET', 'POST'])
def comments_view(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')
    ct = ContentType.objects.get_for_model(model_cls)

    if request.method == 'POST':
        if not request.user.is_authenticated:
            raise NotAuthenticated('Sign in to comment.')
        return _create_comment(request, ct, obj)

    comments = (
        Comment.objects.filter(content_type=ct, object_id=obj.pk, parent__isnull=True)
        .select_related('user', 'user__profile')
        .prefetch_related(Prefetch('replies', queryset=Comment.objects.select_related('user', 'user__profile')))
        .order_by('created_at')
    )
    paginator = StandardResultsSetPagination()
    page = paginator.paginate_queryset(comments, request)
    return paginator.get_paginated_response(CommentSerializer(page, many=True).data)


@rate_limit('add_review', limit=10, window_seconds=300)
def _create_review(request, ct, obj):
    profile = get_profile(request.user)
    if profile.is_blocked:
        raise PermissionDenied('This account cannot perform this action.')
    form = ReviewForm(request.data)
    if not form.is_valid():
        raise ValidationError(form.errors)
    review, _created = Review.objects.update_or_create(
        content_type=ct, object_id=obj.pk, user=request.user,
        defaults={'rating': form.cleaned_data['rating'], 'body': form.cleaned_data['body']},
    )
    return Response(ReviewSerializer(review).data)


@api_view(['GET', 'POST'])
def reviews_view(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')
    ct = ContentType.objects.get_for_model(model_cls)

    if request.method == 'POST':
        if not request.user.is_authenticated:
            raise NotAuthenticated('Sign in to leave a review.')
        return _create_review(request, ct, obj)

    reviews = Review.objects.filter(content_type=ct, object_id=obj.pk).select_related('user', 'user__profile').order_by('-created_at')
    paginator = StandardResultsSetPagination()
    page = paginator.paginate_queryset(reviews, request)
    return paginator.get_paginated_response(ReviewSerializer(page, many=True).data)


@rate_limit('report_listing', limit=10, window_seconds=300)
def _create_report(request, ct, obj):
    profile = get_profile(request.user)
    if profile.is_blocked:
        raise PermissionDenied('This account cannot perform this action.')
    form = ReportForm(request.data)
    if not form.is_valid():
        raise ValidationError(form.errors)
    Report.objects.create(
        content_type=ct, object_id=obj.pk, user=request.user,
        reason=form.cleaned_data['reason'], details=form.cleaned_data['details'],
    )
    return Response({'reported': True}, status=status.HTTP_201_CREATED)


@api_view(['POST'])
def report_view(request, model_key, pk):
    if not request.user.is_authenticated:
        raise NotAuthenticated('Sign in to report a listing.')
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')
    ct = ContentType.objects.get_for_model(model_cls)
    return _create_report(request, ct, obj)


# ---------------------------------------------------------------------------
# Listing media (gallery photos/videos)
# ---------------------------------------------------------------------------

@api_view(['GET', 'POST'])
def listing_media_view(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise NotFound('Unknown listing type.')
    obj = _get_visible_listing(request, model_cls, pk)
    if obj is None:
        raise NotFound('Listing not found.')
    ct = ContentType.objects.get_for_model(model_cls)

    if request.method == 'POST':
        if not request.user.is_authenticated:
            raise NotAuthenticated("Sign in to manage this listing's gallery.")
        _require_can_manage(request, obj)

        images, image_errors = _validate_gallery_files(request.FILES.getlist('images'), GALLERY_IMAGE_TYPES, GALLERY_IMAGE_MAX_BYTES, 'image')
        videos, video_errors = _validate_gallery_files(request.FILES.getlist('videos'), GALLERY_VIDEO_TYPES, GALLERY_VIDEO_MAX_BYTES, 'video')
        errors = image_errors + video_errors
        if not images and not videos:
            raise ValidationError({'files': errors or ['Choose at least one photo or video to upload.']})

        created_images = []
        if images:
            start = PostImage.objects.filter(content_type=ct, object_id=obj.pk).count()
            created_images = [
                PostImage.objects.create(content_type=ct, object_id=obj.pk, image=f, order=start + i, uploaded_by=request.user)
                for i, f in enumerate(images)
            ]
        created_videos = []
        if videos:
            start = PostVideo.objects.filter(content_type=ct, object_id=obj.pk).count()
            created_videos = [
                PostVideo.objects.create(content_type=ct, object_id=obj.pk, video=f, order=start + i, uploaded_by=request.user)
                for i, f in enumerate(videos)
            ]
        return Response({
            'images': PostImageSerializer(created_images, many=True).data,
            'videos': PostVideoSerializer(created_videos, many=True).data,
            'errors': errors,
        }, status=status.HTTP_201_CREATED)

    images = PostImage.objects.filter(content_type=ct, object_id=obj.pk).order_by('order')
    videos = PostVideo.objects.filter(content_type=ct, object_id=obj.pk).order_by('order')
    return Response({
        'images': PostImageSerializer(images, many=True).data,
        'videos': PostVideoSerializer(videos, many=True).data,
    })


@api_view(['DELETE'])
def media_image_delete_view(request, pk):
    if not request.user.is_authenticated:
        raise NotAuthenticated('Sign in.')
    media, _obj = _get_owned_media(PostImage, pk, get_profile(request.user))
    if media is None:
        raise NotFound('Image not found.')
    media.delete()
    return Response(status=status.HTTP_204_NO_CONTENT)


@api_view(['DELETE'])
def media_video_delete_view(request, pk):
    if not request.user.is_authenticated:
        raise NotAuthenticated('Sign in.')
    media, _obj = _get_owned_media(PostVideo, pk, get_profile(request.user))
    if media is None:
        raise NotFound('Video not found.')
    media.delete()
    return Response(status=status.HTTP_204_NO_CONTENT)


# ---------------------------------------------------------------------------
# Notifications
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes(AUTH_REQUIRED)
def notifications_view(request):
    qs = Notification.objects.filter(recipient=request.user).order_by('-created_at')
    paginator = StandardResultsSetPagination()
    page = paginator.paginate_queryset(qs, request)
    return paginator.get_paginated_response(NotificationSerializer(page, many=True).data)


@api_view(['POST'])
@permission_classes(AUTH_REQUIRED)
def notification_mark_read_view(request, pk):
    updated = Notification.objects.filter(pk=pk, recipient=request.user).update(is_read=True)
    if not updated:
        raise NotFound('Notification not found.')
    return Response({'marked_read': True})


@api_view(['POST'])
@permission_classes(AUTH_REQUIRED)
def notifications_mark_all_read_view(request):
    Notification.objects.filter(recipient=request.user, is_read=False).update(is_read=True)
    return Response({'marked_read': True})


@api_view(['GET'])
@permission_classes(AUTH_REQUIRED)
def notifications_unread_count_view(request):
    return Response({'count': Notification.objects.filter(recipient=request.user, is_read=False).count()})
