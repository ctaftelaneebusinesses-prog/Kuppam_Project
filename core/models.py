from django.conf import settings
from django.contrib.contenttypes.fields import GenericForeignKey
from django.contrib.contenttypes.models import ContentType
from django.db import models
from django.urls import reverse
from django.utils import timezone
from django.utils.text import slugify


def unique_slug_for(model_cls, text, instance_pk=None):
    """
    Builds a unique slug for `text` within `model_cls`, appending -2, -3, ...
    on collision. Used by every listing model's save() to auto-fill slug.
    """
    base = slugify(text)[:200] or 'listing'
    slug = base
    n = 1
    qs = model_cls.objects.all()
    while qs.filter(slug=slug).exclude(pk=instance_pk).exists():
        n += 1
        slug = f'{base}-{n}'
    return slug


# ---------------------------------------------------------------------------
# Roles & profile
# ---------------------------------------------------------------------------

class UserRole(models.TextChoices):
    SUPER_ADMIN = 'super_admin', 'Super Admin'
    ADMIN = 'admin', 'Admin (Content Provider)'
    USER = 'user', 'User'


class Intent(models.TextChoices):
    UPLOAD = 'upload', 'I want to Upload My Information'
    EXPLORE = 'explore', 'I want to Explore OneTownCity'


class Profile(models.Model):
    """
    Extends Django's auth.User (kept as the identity backbone for both
    Google/Supabase-authenticated visitors and existing Django staff
    accounts) with OneTownCity-specific role and profile data.
    """
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='profile')
    supabase_uid = models.CharField(max_length=100, unique=True, null=True, blank=True)

    role = models.CharField(max_length=20, choices=UserRole.choices, default=UserRole.USER)

    full_name = models.CharField(max_length=150, blank=True)
    phone_number = models.CharField(max_length=15, blank=True)
    profile_photo = models.ImageField(upload_to='profiles/', blank=True, null=True)
    profile_photo_url = models.URLField(
        max_length=500, blank=True,
        help_text='Google account photo URL, used until a custom photo is uploaded'
    )
    address = models.TextField(blank=True)
    city = models.CharField(max_length=100, blank=True)
    state = models.CharField(max_length=100, blank=True)
    pincode = models.CharField(max_length=10, blank=True)

    intent = models.CharField(max_length=20, choices=Intent.choices, blank=True)
    profile_completed = models.BooleanField(default=False)

    is_blocked = models.BooleanField(default=False, help_text='Blocked users cannot sign in')
    is_suspended = models.BooleanField(default=False, help_text='Suspended admins keep their account but lose listing permissions')

    last_active_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return self.full_name or self.user.get_username()

    @property
    def display_photo(self):
        if self.profile_photo:
            return self.profile_photo.url
        if self.profile_photo_url:
            return self.profile_photo_url
        initial = (self.full_name or self.user.get_username() or 'U')[:1].upper()
        return f'https://placehold.co/200x200?text={initial}'

    @property
    def is_super_admin(self):
        return self.role == UserRole.SUPER_ADMIN

    @property
    def is_admin(self):
        return self.role == UserRole.ADMIN

    def can_manage_category(self, category):
        """Whether this user may create/edit/delete listings in `category`."""
        if self.is_super_admin:
            return True
        if not self.is_admin or self.is_suspended:
            return False
        return AdminCategoryPermission.objects.filter(admin_id=self.user_id, category=category).exists()

    def managed_category_ids(self):
        if self.is_super_admin:
            return list(Category.objects.filter(is_active=True).values_list('id', flat=True))
        return list(AdminCategoryPermission.objects.filter(admin_id=self.user_id).values_list('category_id', flat=True))


# ---------------------------------------------------------------------------
# Categories (the "What do you want to manage?" checkbox list)
# ---------------------------------------------------------------------------

class Category(models.Model):
    LISTING_MODEL_CHOICES = [
        ('business', 'Business'),
        ('property', 'Property'),
        ('job', 'Job'),
        ('event', 'Event'),
        ('news', 'News'),
        ('project', 'Project'),
    ]

    key = models.SlugField(max_length=50, unique=True)
    label = models.CharField(max_length=100)
    listing_model = models.CharField(
        max_length=20, choices=LISTING_MODEL_CHOICES,
        help_text='Which listing table this category publishes into'
    )
    business_subcategory = models.CharField(
        max_length=20, blank=True,
        help_text="Matching Business.category value, only used when listing_model = 'business'"
    )
    icon = models.CharField(max_length=50, blank=True, default='bi-tag')
    is_active = models.BooleanField(default=True)
    order = models.PositiveSmallIntegerField(default=0)

    class Meta:
        ordering = ['order', 'label']
        verbose_name_plural = 'Categories'

    def __str__(self):
        return self.label


# ---------------------------------------------------------------------------
# Admin (Content Provider) requests & granted permissions
# ---------------------------------------------------------------------------

class AdminRequestStatus(models.TextChoices):
    PENDING = 'pending', 'Pending'
    APPROVED = 'approved', 'Approved'
    REJECTED = 'rejected', 'Rejected'
    CHANGES_REQUESTED = 'changes_requested', 'Changes Requested'


class AdminRequest(models.Model):
    """A user's application to become an Admin/Content Provider for one or more categories."""
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='admin_requests')
    categories = models.ManyToManyField(Category, related_name='admin_requests')
    status = models.CharField(max_length=20, choices=AdminRequestStatus.choices, default=AdminRequestStatus.PENDING)
    review_note = models.TextField(blank=True, help_text='Rejection reason or requested changes, shown to the user')
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True, related_name='admin_requests_reviewed'
    )
    reviewed_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f'Admin request by {self.user} ({self.get_status_display()})'


class AdminCategoryPermission(models.Model):
    """The actual granted permission, created when an AdminRequest is approved."""
    admin = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='category_permissions')
    category = models.ForeignKey(Category, on_delete=models.CASCADE, related_name='admin_permissions')
    granted_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, related_name='+')
    granted_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('admin', 'category')

    def __str__(self):
        return f'{self.admin} -> {self.category}'


# ---------------------------------------------------------------------------
# Listings
# ---------------------------------------------------------------------------

class ListingStatus(models.TextChoices):
    PENDING = 'pending', 'Pending Approval'
    APPROVED = 'approved', 'Approved'
    REJECTED = 'rejected', 'Rejected'
    CHANGES_REQUESTED = 'changes_requested', 'Changes Requested'


class ListingMixin(models.Model):
    """
    Shared approval-workflow, ownership, and community-stat fields for every
    public listing type (Business, Property, Job, Event, News). Existing
    rows default to status=approved/owner=None so nothing already on the
    site is affected.
    """
    listing_category = models.ForeignKey(
        Category, on_delete=models.PROTECT, null=True, blank=True, related_name='%(class)s_listings'
    )
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True, related_name='%(class)s_listings'
    )
    status = models.CharField(max_length=20, choices=ListingStatus.choices, default=ListingStatus.APPROVED)
    rejection_reason = models.TextField(blank=True)
    slug = models.SlugField(max_length=220, unique=True, blank=True, null=True)
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True, related_name='%(class)s_reviewed'
    )
    reviewed_at = models.DateTimeField(null=True, blank=True)

    view_count = models.PositiveIntegerField(default=0)
    like_count = models.PositiveIntegerField(default=0)
    comment_count = models.PositiveIntegerField(default=0)
    review_count = models.PositiveIntegerField(default=0)
    avg_rating = models.DecimalField(max_digits=3, decimal_places=2, default=0)
    share_count = models.PositiveIntegerField(default=0)

    class Meta:
        abstract = True

    @property
    def is_public(self):
        return self.status == ListingStatus.APPROVED and self.is_active


class Business(ListingMixin, models.Model):
    """
    A local business or shop listing. Restaurants, hospitals, schools,
    colleges, and transport services are Business rows filtered by
    `category` rather than separate tables.
    """
    CATEGORY_CHOICES = [
        ('retail', 'Retail Shop'),
        ('grocery', 'Grocery Store'),
        ('restaurant', 'Restaurant & Food'),
        ('electronics', 'Electronics'),
        ('clothing', 'Clothing & Fashion'),
        ('pharmacy', 'Pharmacy'),
        ('hardware', 'Hardware & Building Materials'),
        ('bakery', 'Bakery & Sweets'),
        ('salon', 'Salon & Spa'),
        ('automobile', 'Automobile & Repair'),
        ('stationery', 'Stationery & Books'),
        ('jewellery', 'Jewellery'),
        ('hospital', 'Hospital'),
        ('school', 'School'),
        ('college', 'College'),
        ('transport', 'Transport'),
        ('other', 'Other'),
    ]

    name = models.CharField(max_length=200, verbose_name='Business Name')
    category = models.CharField(max_length=20, choices=CATEGORY_CHOICES, default='other')
    address = models.TextField(help_text='Full shop/business address')
    phone_number = models.CharField(max_length=15, help_text='Contact number, e.g. 9876543210')
    image = models.ImageField(
        upload_to='businesses/', blank=True, null=True,
        help_text='Upload a photo (takes priority over Image URL below if both are set)'
    )
    image_url = models.URLField(max_length=500, blank=True, null=True, verbose_name='Image URL')
    description = models.TextField(blank=True, help_text='Optional short description of the business')
    website = models.URLField(max_length=300, blank=True)
    maps_link = models.URLField(max_length=500, blank=True, verbose_name='Google Maps Link')
    is_featured = models.BooleanField(default=False, help_text='Show this business on the homepage')
    is_active = models.BooleanField(default=True, help_text='Uncheck to hide this listing from the site')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-is_featured', 'name']
        verbose_name = 'Business'
        verbose_name_plural = 'Businesses'

    def __str__(self):
        return self.name

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = unique_slug_for(Business, self.name, self.pk)
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('core:business_detail', kwargs={'slug': self.slug})

    @property
    def display_image(self):
        if self.image:
            return self.image.url
        if self.image_url:
            return self.image_url
        return 'https://placehold.co/600x400?text=Hello+Kuppam'

    #: Bootstrap Icons glyph shown in the category-aware placeholder panel
    #: when a business has no uploaded photo and no image URL.
    PLACEHOLDER_ICONS = {
        'retail': 'bi-shop', 'grocery': 'bi-cart3', 'restaurant': 'bi-cup-hot',
        'electronics': 'bi-cpu', 'clothing': 'bi-bag-heart', 'pharmacy': 'bi-capsule',
        'hardware': 'bi-tools', 'bakery': 'bi-cake2', 'salon': 'bi-scissors',
        'automobile': 'bi-car-front', 'stationery': 'bi-pencil', 'jewellery': 'bi-gem',
        'hospital': 'bi-hospital', 'school': 'bi-mortarboard',
        'college': 'bi-mortarboard', 'transport': 'bi-bus-front', 'other': 'bi-shop-window',
    }

    @property
    def has_image(self):
        return bool(self.image or self.image_url)

    @property
    def placeholder_icon(self):
        return self.PLACEHOLDER_ICONS.get(self.category, 'bi-shop-window')


class Property(ListingMixin, models.Model):
    """A real estate listing (sale, rent, plot, commercial, etc.) in Kuppam."""
    PROPERTY_TYPE_CHOICES = [
        ('sale', 'For Sale'),
        ('rent', 'For Rent'),
        ('apartment', 'Apartment / Flat'),
        ('villa', 'Villa / Independent House'),
        ('plot', 'Plot / Land'),
        ('commercial', 'Commercial Space'),
        ('pg', 'PG / Hostel'),
        ('other', 'Other'),
    ]

    title = models.CharField(max_length=200, verbose_name='Property Title')
    property_type = models.CharField(max_length=20, choices=PROPERTY_TYPE_CHOICES, default='sale', verbose_name='Type')
    price = models.DecimalField(max_digits=12, decimal_places=2, verbose_name='Price (₹)')
    location = models.CharField(max_length=200, help_text='Area / locality in Kuppam')
    contact_number = models.CharField(max_length=15, verbose_name='Contact', help_text='Contact number, e.g. 9876543210')
    image = models.ImageField(
        upload_to='properties/', blank=True, null=True,
        help_text='Upload a photo (takes priority over Image URL below if both are set)'
    )
    image_url = models.URLField(max_length=500, blank=True, null=True, verbose_name='Image URL')
    description = models.TextField(blank=True, help_text='Optional details about the property')
    is_featured = models.BooleanField(default=False, help_text='Show this property on the homepage')
    is_active = models.BooleanField(default=True, help_text='Uncheck to hide this listing from the site')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-is_featured', '-created_at']
        verbose_name = 'Property'
        verbose_name_plural = 'Properties'

    def __str__(self):
        return self.title

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = unique_slug_for(Property, self.title, self.pk)
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('core:property_detail', kwargs={'slug': self.slug})

    @property
    def display_image(self):
        if self.image:
            return self.image.url
        if self.image_url:
            return self.image_url
        return 'https://placehold.co/600x400?text=Hello+Kuppam+Property'

    PLACEHOLDER_ICONS = {
        'sale': 'bi-house-check', 'rent': 'bi-house-heart', 'apartment': 'bi-building',
        'villa': 'bi-house-door', 'plot': 'bi-bounding-box', 'commercial': 'bi-shop',
        'pg': 'bi-house-gear', 'other': 'bi-house',
    }

    @property
    def has_image(self):
        return bool(self.image or self.image_url)

    @property
    def placeholder_icon(self):
        return self.PLACEHOLDER_ICONS.get(self.property_type, 'bi-house-door')


class Job(ListingMixin, models.Model):
    """A job opening posted by a local company/employer in Kuppam."""
    company = models.CharField(max_length=200, verbose_name='Company')
    job_title = models.CharField(max_length=200, verbose_name='Job Title')
    location = models.CharField(max_length=200, help_text='Job location in Kuppam')
    salary = models.CharField(
        max_length=100, blank=True,
        help_text='e.g. ₹15,000 - ₹25,000/month, or "Negotiable"'
    )
    contact_number = models.CharField(max_length=15, verbose_name='Contact', help_text='Contact number, e.g. 9876543210')
    description = models.TextField(blank=True, help_text='Optional job description, requirements, etc.')
    image = models.ImageField(
        upload_to='jobs/', blank=True, null=True,
        help_text='Upload a photo (takes priority over Image URL below if both are set)'
    )
    image_url = models.URLField(max_length=500, blank=True, null=True, verbose_name='Image URL')
    is_featured = models.BooleanField(default=False, help_text='Show this job on the homepage')
    is_active = models.BooleanField(default=True, help_text='Uncheck to close/hide this job listing')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-is_featured', '-created_at']
        verbose_name = 'Job'
        verbose_name_plural = 'Jobs'

    def __str__(self):
        return f'{self.job_title} at {self.company}'

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = unique_slug_for(Job, f'{self.job_title} {self.company}', self.pk)
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('core:job_detail', kwargs={'slug': self.slug})

    @property
    def display_image(self):
        if self.image:
            return self.image.url
        if self.image_url:
            return self.image_url
        return 'https://placehold.co/600x400?text=Hello+Kuppam+Job'

    @property
    def has_image(self):
        return bool(self.image or self.image_url)

    placeholder_icon = 'bi-briefcase'


class Event(ListingMixin, models.Model):
    """A local event (festival, meeting, fair, etc.) happening in Kuppam."""
    title = models.CharField(max_length=200, verbose_name='Event Title')
    event_date = models.DateField(verbose_name='Event Date')
    location = models.CharField(max_length=200, help_text='Venue / location in Kuppam')
    description = models.TextField(blank=True, help_text='Optional details about the event')
    contact_number = models.CharField(max_length=15, blank=True, verbose_name='Contact')
    image = models.ImageField(
        upload_to='events/', blank=True, null=True,
        help_text='Upload a photo (takes priority over Image URL below if both are set)'
    )
    image_url = models.URLField(max_length=500, blank=True, null=True, verbose_name='Image URL')
    is_featured = models.BooleanField(default=False, help_text='Show this event on the homepage')
    is_active = models.BooleanField(default=True, help_text='Uncheck to hide this event from the site')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['event_date']
        verbose_name = 'Event'
        verbose_name_plural = 'Events'

    def __str__(self):
        return f'{self.title} ({self.event_date})'

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = unique_slug_for(Event, self.title, self.pk)
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('core:event_detail', kwargs={'slug': self.slug})

    @property
    def display_image(self):
        if self.image:
            return self.image.url
        if self.image_url:
            return self.image_url
        return 'https://placehold.co/600x400?text=Hello+Kuppam+Event'

    @property
    def has_image(self):
        return bool(self.image or self.image_url)

    placeholder_icon = 'bi-calendar-event'

    @property
    def is_upcoming(self):
        return self.event_date >= timezone.localdate()


class News(ListingMixin, models.Model):
    """A local news article or announcement for Kuppam."""
    title = models.CharField(max_length=200, verbose_name='News Title')
    content = models.TextField(verbose_name='Content')
    published_date = models.DateField(default=timezone.localdate, verbose_name='Published Date')
    image = models.ImageField(
        upload_to='news/', blank=True, null=True,
        help_text='Upload a photo (takes priority over Image URL below if both are set)'
    )
    image_url = models.URLField(max_length=500, blank=True, null=True, verbose_name='Image URL')
    source = models.CharField(max_length=150, blank=True, help_text='Optional source/author name')
    is_featured = models.BooleanField(default=False, help_text='Show this article on the homepage')
    is_active = models.BooleanField(default=True, help_text='Uncheck to hide this article from the site')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-published_date']
        verbose_name = 'News Article'
        verbose_name_plural = 'News'

    def __str__(self):
        return self.title

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = unique_slug_for(News, self.title, self.pk)
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('core:news_detail', kwargs={'slug': self.slug})

    @property
    def display_image(self):
        if self.image:
            return self.image.url
        if self.image_url:
            return self.image_url
        return 'https://placehold.co/600x400?text=Hello+Kuppam+News'

    @property
    def has_image(self):
        return bool(self.image or self.image_url)

    placeholder_icon = 'bi-newspaper'


class Project(ListingMixin, models.Model):
    """An upcoming or ongoing civic/infrastructure project for Kuppam (roads, schemes, public works)."""
    STATUS_CHOICES = [
        ('planned', 'Planned'),
        ('ongoing', 'Ongoing'),
        ('completed', 'Completed'),
    ]

    title = models.CharField(max_length=200, verbose_name='Project Title')
    project_status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='planned', verbose_name='Status')
    location = models.CharField(max_length=200, help_text='Area / locality in Kuppam')
    expected_completion = models.DateField(null=True, blank=True, help_text='Optional expected completion date')
    department = models.CharField(max_length=150, blank=True, help_text='Optional executing department/agency')
    description = models.TextField(blank=True, help_text='Optional details about the project')
    image = models.ImageField(
        upload_to='projects/', blank=True, null=True,
        help_text='Upload a photo (takes priority over Image URL below if both are set)'
    )
    image_url = models.URLField(max_length=500, blank=True, null=True, verbose_name='Image URL')
    is_featured = models.BooleanField(default=False, help_text='Show this project on the homepage')
    is_active = models.BooleanField(default=True, help_text='Uncheck to hide this project from the site')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-is_featured', '-created_at']
        verbose_name = 'Upcoming Project'
        verbose_name_plural = 'Upcoming Projects'

    def __str__(self):
        return self.title

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = unique_slug_for(Project, self.title, self.pk)
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('core:project_detail', kwargs={'slug': self.slug})

    @property
    def display_image(self):
        if self.image:
            return self.image.url
        if self.image_url:
            return self.image_url
        return 'https://placehold.co/600x400?text=Hello+Kuppam+Project'

    @property
    def has_image(self):
        return bool(self.image or self.image_url)

    PLACEHOLDER_ICONS = {
        'planned': 'bi-cone-striped', 'ongoing': 'bi-cone-striped', 'completed': 'bi-check-circle',
    }

    @property
    def placeholder_icon(self):
        return self.PLACEHOLDER_ICONS.get(self.project_status, 'bi-cone-striped')


LISTING_CONTENT_TYPE_LIMIT = (
    models.Q(app_label='core', model='business')
    | models.Q(app_label='core', model='property')
    | models.Q(app_label='core', model='job')
    | models.Q(app_label='core', model='event')
    | models.Q(app_label='core', model='news')
    | models.Q(app_label='core', model='project')
)


# ---------------------------------------------------------------------------
# Community features (generic across all 5 listing types)
# ---------------------------------------------------------------------------

class Like(models.Model):
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='likes')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('content_type', 'object_id', 'user')
        indexes = [models.Index(fields=['content_type', 'object_id'])]


class Comment(models.Model):
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='comments')
    parent = models.ForeignKey('self', on_delete=models.CASCADE, null=True, blank=True, related_name='replies')
    body = models.TextField()
    is_flagged = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['created_at']
        indexes = [models.Index(fields=['content_type', 'object_id'])]

    def __str__(self):
        return f'{self.user}: {self.body[:40]}'


class Review(models.Model):
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='reviews')
    rating = models.PositiveSmallIntegerField(choices=[(i, str(i)) for i in range(1, 6)])
    body = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = ('content_type', 'object_id', 'user')
        ordering = ['-created_at']
        indexes = [models.Index(fields=['content_type', 'object_id'])]


class Favorite(models.Model):
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='favorites')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('content_type', 'object_id', 'user')
        indexes = [models.Index(fields=['content_type', 'object_id'])]


class Share(models.Model):
    PLATFORM_CHOICES = [
        ('whatsapp', 'WhatsApp'),
        ('facebook', 'Facebook'),
        ('telegram', 'Telegram'),
        ('twitter', 'X (Twitter)'),
        ('copy_link', 'Copy Link'),
    ]
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True, related_name='shares')
    platform = models.CharField(max_length=20, choices=PLATFORM_CHOICES)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=['content_type', 'object_id'])]


class Report(models.Model):
    STATUS_CHOICES = [('open', 'Open'), ('reviewed', 'Reviewed'), ('dismissed', 'Dismissed')]
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='reports')
    reason = models.CharField(max_length=200)
    details = models.TextField(blank=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='open')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [models.Index(fields=['content_type', 'object_id'])]


class PostImage(models.Model):
    """
    Extra gallery photos for a listing, beyond its single `image`/`image_url`
    cover photo. Managed exclusively from the Super Admin Posts dashboard.
    """
    content_type = models.ForeignKey(ContentType, on_delete=models.CASCADE, limit_choices_to=LISTING_CONTENT_TYPE_LIMIT)
    object_id = models.PositiveIntegerField()
    content_object = GenericForeignKey('content_type', 'object_id')
    image = models.ImageField(upload_to='post_gallery/')
    order = models.PositiveSmallIntegerField(default=0)
    uploaded_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['order', 'created_at']
        indexes = [models.Index(fields=['content_type', 'object_id'])]

    def delete(self, *args, **kwargs):
        self.image.delete(save=False)
        super().delete(*args, **kwargs)


# ---------------------------------------------------------------------------
# Notifications & activity
# ---------------------------------------------------------------------------

class Notification(models.Model):
    TYPE_CHOICES = [
        ('listing_approved', 'Listing Approved'),
        ('listing_rejected', 'Listing Rejected'),
        ('listing_changes_requested', 'Listing Changes Requested'),
        ('admin_request_submitted', 'New Admin Request'),
        ('admin_request_approved', 'Admin Request Approved'),
        ('admin_request_rejected', 'Admin Request Rejected'),
        ('admin_request_changes_requested', 'Admin Request Changes Requested'),
        ('new_comment', 'New Comment'),
        ('new_review', 'New Review'),
    ]
    recipient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='notifications')
    type = models.CharField(max_length=40, choices=TYPE_CHOICES)
    message = models.CharField(max_length=255)
    url = models.CharField(max_length=300, blank=True)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return self.message


class LoginHistory(models.Model):
    EVENT_CHOICES = [('login', 'Login'), ('logout', 'Logout')]
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='login_history')
    event_type = models.CharField(max_length=10, choices=EVENT_CHOICES)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    user_agent = models.CharField(max_length=300, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        verbose_name_plural = 'Login history'

    def __str__(self):
        return f'{self.user} {self.event_type} @ {self.created_at:%Y-%m-%d %H:%M}'


class ContactMessage(models.Model):
    """A submission from the public Contact Us form."""
    name = models.CharField(max_length=100)
    email = models.EmailField()
    subject = models.CharField(max_length=150)
    message = models.TextField()
    is_read = models.BooleanField(default=False, help_text='Mark as read once handled')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.subject} ({self.email})'


class NewsletterSubscriber(models.Model):
    """An email signed up via the footer 'Stay Updated' newsletter form."""
    email = models.EmailField(unique=True)
    is_active = models.BooleanField(default=True, help_text='Uncheck to treat as unsubscribed')
    subscribed_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-subscribed_at']

    def __str__(self):
        return self.email


from . import signals  # noqa: E402,F401  (registers post_save/post_delete counter updates)
