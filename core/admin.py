from django.contrib import admin
from django.utils.html import format_html
from .models import (
    AdminCategoryPermission, AdminRequest, Business, Category, Comment, ContactMessage, Event, Favorite,
    Job, Like, LoginHistory, News, NewsletterSubscriber, Notification, PostImage, PostVideo, Profile,
    Project, Property, Report, Review, Share, SiteSettings, TranslationCache, Location,
)


@admin.register(Location)
class LocationAdmin(admin.ModelAdmin):
    list_display = ('name', 'kind', 'parent', 'country_code', 'latitude', 'longitude', 'is_active')
    list_filter = ('kind', 'is_active', 'country_code')
    search_fields = ('name', 'aliases')
    prepopulated_fields = {'slug': ('name',)}


class ImagePreviewMixin:
    """
    Adds a read-only thumbnail preview (uploaded photo, Image URL, or
    placeholder) to the admin change form and list view.
    """
    readonly_fields = ('image_preview',)

    @admin.display(description='Preview')
    def image_preview(self, obj):
        if not obj.pk:
            return '—'
        return format_html(
            '<img src="{}" style="height:70px;width:110px;object-fit:cover;'
            'border-radius:8px;border:1px solid #e5e7f5;" />',
            obj.display_image,
        )


@admin.register(Business)
class BusinessAdmin(ImagePreviewMixin, admin.ModelAdmin):
    list_display = ('image_preview', 'name', 'category', 'phone_number', 'owner', 'status', 'is_featured', 'is_active', 'created_at')
    list_filter = ('category', 'status', 'is_featured', 'is_active')
    search_fields = ('name', 'address', 'phone_number')
    list_editable = ('is_featured', 'is_active')
    ordering = ('-is_featured', 'name')

    fieldsets = (
        ('Basic Information', {
            'fields': ('name', 'category', 'description')
        }),
        ('Contact Details', {
            'fields': ('city', 'address', 'phone_number')
        }),
        ('Media', {
            'fields': ('image_preview', 'image', 'image_url'),
            'description': 'Upload a photo directly, or paste an external Image URL. '
                            'An uploaded photo always takes priority when both are set.',
        }),
        ('Visibility', {
            'fields': ('is_featured', 'is_active')
        }),
    )


@admin.register(Property)
class PropertyAdmin(ImagePreviewMixin, admin.ModelAdmin):
    list_display = ('image_preview', 'title', 'property_type', 'price', 'location', 'owner', 'status', 'is_featured', 'is_active', 'created_at')
    list_filter = ('property_type', 'status', 'is_featured', 'is_active')
    search_fields = ('title', 'location', 'contact_number')
    list_editable = ('is_featured', 'is_active')
    ordering = ('-is_featured', '-created_at')

    fieldsets = (
        ('Basic Information', {
            'fields': ('title', 'property_type', 'description')
        }),
        ('Pricing & Location', {
            'fields': ('city', 'price', 'location')
        }),
        ('Contact Details', {
            'fields': ('contact_number',)
        }),
        ('Media', {
            'fields': ('image_preview', 'image', 'image_url'),
            'description': 'Upload a photo directly, or paste an external Image URL. '
                            'An uploaded photo always takes priority when both are set.',
        }),
        ('Visibility', {
            'fields': ('is_featured', 'is_active')
        }),
    )


@admin.register(Job)
class JobAdmin(ImagePreviewMixin, admin.ModelAdmin):
    list_display = ('image_preview', 'job_title', 'company', 'location', 'salary', 'owner', 'status', 'is_featured', 'is_active', 'created_at')
    list_filter = ('status', 'is_featured', 'is_active')
    search_fields = ('job_title', 'company', 'location', 'contact_number')
    list_editable = ('is_featured', 'is_active')
    ordering = ('-is_featured', '-created_at')

    fieldsets = (
        ('Basic Information', {
            'fields': ('job_title', 'company', 'description')
        }),
        ('Compensation & Location', {
            'fields': ('city', 'salary', 'location')
        }),
        ('Contact Details', {
            'fields': ('contact_number',)
        }),
        ('Media', {
            'fields': ('image_preview', 'image', 'image_url'),
            'description': 'Upload a photo directly, or paste an external Image URL. '
                            'An uploaded photo always takes priority when both are set.',
        }),
        ('Visibility', {
            'fields': ('is_featured', 'is_active')
        }),
    )


@admin.register(Event)
class EventAdmin(ImagePreviewMixin, admin.ModelAdmin):
    list_display = ('image_preview', 'title', 'event_date', 'location', 'owner', 'status', 'is_featured', 'is_active', 'created_at')
    list_filter = ('status', 'is_featured', 'is_active')
    search_fields = ('title', 'location', 'contact_number')
    list_editable = ('is_featured', 'is_active')
    ordering = ('event_date',)

    fieldsets = (
        ('Basic Information', {
            'fields': ('title', 'event_date', 'description')
        }),
        ('Location & Contact', {
            'fields': ('city', 'location', 'contact_number')
        }),
        ('Media', {
            'fields': ('image_preview', 'image', 'image_url'),
            'description': 'Upload a photo directly, or paste an external Image URL. '
                            'An uploaded photo always takes priority when both are set.',
        }),
        ('Visibility', {
            'fields': ('is_featured', 'is_active')
        }),
    )


@admin.register(News)
class NewsAdmin(ImagePreviewMixin, admin.ModelAdmin):
    list_display = ('image_preview', 'title', 'published_date', 'source', 'owner', 'status', 'is_featured', 'is_active', 'created_at')
    list_filter = ('status', 'is_featured', 'is_active')
    search_fields = ('title', 'content', 'source')
    list_editable = ('is_featured', 'is_active')
    ordering = ('-published_date',)

    fieldsets = (
        ('Basic Information', {
            'fields': ('title', 'city', 'published_date', 'source', 'content')
        }),
        ('Media', {
            'fields': ('image_preview', 'image', 'image_url'),
            'description': 'Upload a photo directly, or paste an external Image URL. '
                            'An uploaded photo always takes priority when both are set.',
        }),
        ('Visibility', {
            'fields': ('is_featured', 'is_active')
        }),
    )


@admin.register(Project)
class ProjectAdmin(ImagePreviewMixin, admin.ModelAdmin):
    list_display = ('image_preview', 'title', 'project_status', 'location', 'expected_completion', 'owner', 'status', 'is_featured', 'is_active', 'created_at')
    list_filter = ('project_status', 'status', 'is_featured', 'is_active')
    search_fields = ('title', 'location', 'department', 'description')
    list_editable = ('is_featured', 'is_active')
    ordering = ('-is_featured', '-created_at')

    fieldsets = (
        ('Basic Information', {
            'fields': ('title', 'project_status', 'city', 'location', 'expected_completion', 'department', 'description')
        }),
        ('Media', {
            'fields': ('image_preview', 'image', 'image_url'),
            'description': 'Upload a photo directly, or paste an external Image URL. '
                            'An uploaded photo always takes priority when both are set.',
        }),
        ('Visibility', {
            'fields': ('is_featured', 'is_active')
        }),
    )


@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ('label', 'key', 'parent', 'listing_model', 'business_subcategory', 'is_active', 'order')
    list_filter = ('listing_model', 'is_active')
    list_editable = ('is_active', 'order')
    search_fields = ('label', 'key')


@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'full_name', 'role', 'phone_number', 'city', 'is_blocked', 'is_suspended', 'created_at')
    list_filter = ('role', 'is_blocked', 'is_suspended', 'profile_completed')
    search_fields = ('full_name', 'phone_number', 'user__email', 'user__username')


@admin.register(AdminRequest)
class AdminRequestAdmin(admin.ModelAdmin):
    list_display = ('user', 'status', 'reviewed_by', 'reviewed_at', 'created_at')
    list_filter = ('status',)
    search_fields = ('user__email', 'user__username')
    filter_horizontal = ('categories',)


@admin.register(AdminCategoryPermission)
class AdminCategoryPermissionAdmin(admin.ModelAdmin):
    list_display = ('admin', 'category', 'granted_by', 'granted_at')
    list_filter = ('category',)
    search_fields = ('admin__email', 'admin__username')


@admin.register(Notification)
class NotificationAdmin(admin.ModelAdmin):
    list_display = ('recipient', 'type', 'message', 'is_read', 'created_at')
    list_filter = ('type', 'is_read')
    search_fields = ('recipient__email', 'message')


@admin.register(LoginHistory)
class LoginHistoryAdmin(admin.ModelAdmin):
    list_display = ('user', 'event_type', 'ip_address', 'created_at')
    list_filter = ('event_type',)
    search_fields = ('user__email', 'user__username', 'ip_address')


@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ('user', 'content_type', 'object_id', 'body', 'is_flagged', 'created_at')
    list_filter = ('is_flagged', 'content_type')
    search_fields = ('body', 'user__email')


@admin.register(Review)
class ReviewAdmin(admin.ModelAdmin):
    list_display = ('user', 'content_type', 'object_id', 'rating', 'created_at')
    list_filter = ('rating', 'content_type')
    search_fields = ('body', 'user__email')


@admin.register(Like)
class LikeAdmin(admin.ModelAdmin):
    list_display = ('user', 'content_type', 'object_id', 'created_at')
    list_filter = ('content_type',)


@admin.register(Favorite)
class FavoriteAdmin(admin.ModelAdmin):
    list_display = ('user', 'content_type', 'object_id', 'created_at')
    list_filter = ('content_type',)


@admin.register(Share)
class ShareAdmin(admin.ModelAdmin):
    list_display = ('user', 'content_type', 'object_id', 'platform', 'created_at')
    list_filter = ('platform', 'content_type')


@admin.register(Report)
class ReportAdmin(admin.ModelAdmin):
    list_display = ('user', 'content_type', 'object_id', 'reason', 'status', 'created_at')
    list_filter = ('status', 'content_type')
    search_fields = ('reason', 'details', 'user__email')


@admin.register(PostImage)
class PostImageAdmin(admin.ModelAdmin):
    list_display = ('content_type', 'object_id', 'order', 'uploaded_by', 'created_at')
    list_filter = ('content_type',)


@admin.register(PostVideo)
class PostVideoAdmin(admin.ModelAdmin):
    list_display = ('content_type', 'object_id', 'order', 'uploaded_by', 'created_at')
    list_filter = ('content_type',)


@admin.register(TranslationCache)
class TranslationCacheAdmin(admin.ModelAdmin):
    list_display = ('content_type', 'object_id', 'field_name', 'language', 'created_at')
    list_filter = ('language', 'field_name')
    search_fields = ('source_text', 'translated_text')


@admin.register(ContactMessage)
class ContactMessageAdmin(admin.ModelAdmin):
    list_display = ('subject', 'name', 'email', 'is_read', 'created_at')
    list_filter = ('is_read',)
    search_fields = ('name', 'email', 'subject', 'message')
    list_editable = ('is_read',)
    ordering = ('-created_at',)
    readonly_fields = ('name', 'email', 'subject', 'message', 'created_at')


@admin.register(NewsletterSubscriber)
class NewsletterSubscriberAdmin(admin.ModelAdmin):
    list_display = ('email', 'is_active', 'subscribed_at')
    list_filter = ('is_active',)
    search_fields = ('email',)
    list_editable = ('is_active',)
    ordering = ('-subscribed_at',)


@admin.register(SiteSettings)
class SiteSettingsAdmin(admin.ModelAdmin):
    """
    The primary editing surface is the Super Admin dashboard's Site Theme
    panel (dashboard/site_settings.html) — this registration just keeps the
    single row visible/editable from /admin/ too, for developer convenience.
    """
    list_display = ('default_palette', 'enforce_palette', 'updated_by', 'updated_at')
    readonly_fields = ('updated_at',)

    def has_add_permission(self, request):
        return not SiteSettings.objects.exists()

    def has_delete_permission(self, request, obj=None):
        return False


# --- Admin branding -------------------------------------------------------
admin.site.site_header = 'OneTownCity Admin'
admin.site.site_title = 'OneTownCity Admin'
admin.site.index_title = 'Manage Listings'
