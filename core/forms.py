from django import forms
from django.contrib.auth import get_user_model
from django.contrib.auth.forms import AuthenticationForm
from django.contrib.auth.password_validation import validate_password

from .models import (
    Business, Category, Event, Job, Location, News, PlatformSettings, Profile, Project, Property,
)

User = get_user_model()


class AdminLoginForm(AuthenticationForm):
    """
    Staff-only login form for the OneTownCity admin area.
    """
    username = forms.CharField(
        widget=forms.TextInput(attrs={
            'class': 'form-control',
            'placeholder': 'Username',
            'autofocus': True,
        })
    )
    password = forms.CharField(
        widget=forms.PasswordInput(attrs={
            'class': 'form-control',
            'placeholder': 'Password',
        })
    )

    def confirm_login_allowed(self, user):
        super().confirm_login_allowed(user)
        if not user.is_staff:
            raise forms.ValidationError(
                'This login is for administrators only.',
                code='not_staff',
            )


class PasswordLoginForm(AuthenticationForm):
    """
    General-purpose username/password sign-in for any account — Explorer,
    Content Provider, or Super Admin. No is_staff restriction (unlike
    AdminLoginForm, which exclusively guards the Django /admin/ backend).
    """
    username = forms.CharField(
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Username', 'autofocus': True, 'autocomplete': 'username',
        })
    )
    password = forms.CharField(
        widget=forms.PasswordInput(attrs={
            'class': 'form-control', 'placeholder': 'Password', 'id': 'id_login_password',
            'autocomplete': 'current-password',
        })
    )


class RegisterForm(forms.Form):
    """
    Self-service registration: collects the same profile details the
    Google flow asks for on complete_profile, but upfront — so a
    username/password account is fully onboarded (profile_completed=True)
    the moment it's created.
    """
    full_name = forms.CharField(
        max_length=150,
        widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Full Name', 'autocomplete': 'name'}),
    )
    email = forms.EmailField(
        widget=forms.EmailInput(attrs={'class': 'form-control', 'placeholder': 'Email', 'autocomplete': 'email'})
    )
    phone_number = forms.CharField(
        max_length=10,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Mobile Number', 'maxlength': '10', 'inputmode': 'numeric',
            'autocomplete': 'tel',
        }),
    )
    password = forms.CharField(
        widget=forms.PasswordInput(attrs={
            'class': 'form-control', 'placeholder': 'Password', 'autocomplete': 'new-password',
        })
    )
    confirm_password = forms.CharField(
        widget=forms.PasswordInput(attrs={
            'class': 'form-control', 'placeholder': 'Confirm Password', 'autocomplete': 'new-password',
            'data-match': 'id_password',
        })
    )
    profile_photo = forms.ImageField(
        required=False, widget=forms.ClearableFileInput(attrs={'class': 'form-control'})
    )
    address = forms.CharField(
        required=False,
        widget=forms.Textarea(attrs={
            'class': 'form-control', 'rows': 2, 'placeholder': 'Address', 'autocomplete': 'street-address',
        }),
    )
    city = forms.CharField(
        required=False, max_length=100,
        widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'City', 'autocomplete': 'address-level2'}),
    )
    state = forms.CharField(
        required=False, max_length=100,
        widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'State', 'autocomplete': 'address-level1'}),
    )
    pincode = forms.CharField(
        required=False, max_length=10,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Pincode', 'inputmode': 'numeric', 'autocomplete': 'postal-code',
        }),
    )

    def clean_email(self):
        email = self.cleaned_data['email']
        if User.objects.filter(email__iexact=email).exists():
            raise forms.ValidationError('An account with this email already exists. Try signing in instead.')
        return email

    def clean_phone_number(self):
        phone = self.cleaned_data['phone_number'].strip()
        if not phone.isdigit() or len(phone) != 10:
            raise forms.ValidationError('Enter a valid 10-digit phone number.')
        return phone

    def clean_password(self):
        password = self.cleaned_data['password']
        validate_password(password)
        return password

    def clean(self):
        cleaned = super().clean()
        password = cleaned.get('password')
        confirm_password = cleaned.get('confirm_password')
        if password and confirm_password and password != confirm_password:
            self.add_error('confirm_password', 'Passwords do not match.')
        return cleaned


class ContactForm(forms.Form):
    name = forms.CharField(
        max_length=100,
        widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Your Name'})
    )
    email = forms.EmailField(
        widget=forms.EmailInput(attrs={'class': 'form-control', 'placeholder': 'Your Email'})
    )
    subject = forms.CharField(
        max_length=150,
        widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Subject'})
    )
    message = forms.CharField(
        widget=forms.Textarea(attrs={'class': 'form-control', 'placeholder': 'Your Message', 'rows': 5})
    )


class ExcelUploadForm(forms.Form):
    """
    Generic Excel upload form reused by every bulk-upload page
    (Businesses, Properties, Jobs, Events, News).
    """
    file = forms.FileField(
        label='Excel File',
        widget=forms.ClearableFileInput(attrs={'class': 'form-control', 'accept': '.xlsx,.xls'}),
        help_text='Upload a .xlsx or .xls file (max 5 MB).',
    )

    MAX_UPLOAD_SIZE = 5 * 1024 * 1024
    VALID_EXTENSIONS = ('.xlsx', '.xls')

    def clean_file(self):
        uploaded = self.cleaned_data['file']

        if not uploaded.name.lower().endswith(self.VALID_EXTENSIONS):
            raise forms.ValidationError('Please upload a valid Excel file (.xlsx or .xls).')

        if uploaded.size > self.MAX_UPLOAD_SIZE:
            raise forms.ValidationError('File is too large. Maximum allowed size is 5 MB.')

        return uploaded


# ---------------------------------------------------------------------------
# Onboarding: profile completion, "what brings you here", admin request
# ---------------------------------------------------------------------------

class ProfileCompletionForm(forms.ModelForm):
    full_name = forms.CharField(
        max_length=150, widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Full Name'})
    )
    phone_number = forms.CharField(
        max_length=10,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Mobile Number', 'maxlength': '10', 'inputmode': 'numeric',
        }),
    )

    class Meta:
        model = Profile
        fields = ['full_name', 'phone_number', 'profile_photo', 'address', 'city', 'state', 'pincode']
        widgets = {
            'profile_photo': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'address': forms.Textarea(attrs={'class': 'form-control', 'rows': 3, 'placeholder': 'Address'}),
            'city': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'City'}),
            'state': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'State'}),
            'pincode': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Pincode'}),
        }

    def clean_phone_number(self):
        phone = self.cleaned_data['phone_number'].strip()
        if not phone.isdigit() or len(phone) != 10:
            raise forms.ValidationError('Enter a valid 10-digit phone number.')
        return phone


class AdminRequestForm(forms.Form):
    categories = forms.ModelMultipleChoiceField(
        queryset=Category.objects.filter(is_active=True),
        widget=forms.CheckboxSelectMultiple,
        label='What do you want to manage?',
    )


class AdminRequestReviewForm(forms.Form):
    ACTION_CHOICES = [('approve', 'Approve'), ('reject', 'Reject'), ('changes_requested', 'Request Changes')]
    action = forms.ChoiceField(choices=ACTION_CHOICES, widget=forms.HiddenInput)
    note = forms.CharField(
        required=False,
        widget=forms.Textarea(attrs={'class': 'form-control', 'rows': 2, 'placeholder': 'Reason / requested changes (shown to the user)'}),
    )


# ---------------------------------------------------------------------------
# Listing submission forms (Content Provider "Add My Listing" wizard)
# ---------------------------------------------------------------------------

class BusinessSubmitForm(forms.ModelForm):
    class Meta:
        model = Business
        fields = ['name', 'category', 'city', 'address', 'phone_number', 'description', 'website', 'maps_link', 'image', 'image_url']
        widgets = {
            'name': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Business Name'}),
            'category': forms.Select(attrs={'class': 'form-select'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'address': forms.Textarea(attrs={'class': 'form-control', 'rows': 3, 'placeholder': 'Full Address'}),
            'phone_number': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Phone Number'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'website': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'https://...'}),
            'maps_link': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Google Maps link'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }


class PropertySubmitForm(forms.ModelForm):
    class Meta:
        model = Property
        fields = ['title', 'property_type', 'price', 'location', 'city', 'contact_number', 'description', 'image', 'image_url']
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Property Title'}),
            'property_type': forms.Select(attrs={'class': 'form-select'}),
            'price': forms.NumberInput(attrs={'class': 'form-control', 'placeholder': 'Price (₹)'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Location'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'contact_number': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Contact Number'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }


class JobSubmitForm(forms.ModelForm):
    class Meta:
        model = Job
        fields = ['job_title', 'company', 'location', 'city', 'salary', 'contact_number', 'description', 'image', 'image_url']
        widgets = {
            'job_title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Job Title'}),
            'company': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Company'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Location'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'salary': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Salary (optional)'}),
            'contact_number': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Contact Number'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }


class EventSubmitForm(forms.ModelForm):
    class Meta:
        model = Event
        fields = ['title', 'event_date', 'location', 'city', 'contact_number', 'description', 'image', 'image_url']
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Event Title'}),
            'event_date': forms.DateInput(attrs={'class': 'form-control', 'type': 'date'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Venue / Location'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'contact_number': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Contact Number (optional)'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }


class NewsSubmitForm(forms.ModelForm):
    class Meta:
        model = News
        fields = ['title', 'city', 'content', 'source', 'image', 'image_url']
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'News Title'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'content': forms.Textarea(attrs={'class': 'form-control', 'rows': 6, 'placeholder': 'Content'}),
            'source': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Source (optional)'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }


class ProjectSubmitForm(forms.ModelForm):
    class Meta:
        model = Project
        fields = ['title', 'project_status', 'location', 'city', 'expected_completion', 'department', 'description', 'image', 'image_url']
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Project Title'}),
            'project_status': forms.Select(attrs={'class': 'form-select'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Area / Locality'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'expected_completion': forms.DateInput(attrs={'class': 'form-control', 'type': 'date'}),
            'department': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Executing Department/Agency (optional)'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }


LISTING_SUBMIT_FORMS = {
    'business': BusinessSubmitForm,
    'property': PropertySubmitForm,
    'job': JobSubmitForm,
    'event': EventSubmitForm,
    'news': NewsSubmitForm,
    'project': ProjectSubmitForm,
}


# ---------------------------------------------------------------------------
# Community features
# ---------------------------------------------------------------------------

class CommentForm(forms.Form):
    body = forms.CharField(
        widget=forms.Textarea(attrs={'class': 'form-control', 'rows': 2, 'placeholder': 'Write a comment...'})
    )
    parent_id = forms.IntegerField(required=False, widget=forms.HiddenInput)


class ReviewForm(forms.Form):
    rating = forms.ChoiceField(
        choices=[(i, f'{i} Star{"s" if i != 1 else ""}') for i in range(1, 6)],
        widget=forms.Select(attrs={'class': 'form-select'}),
    )
    body = forms.CharField(
        required=False,
        widget=forms.Textarea(attrs={'class': 'form-control', 'rows': 3, 'placeholder': 'Share your experience (optional)'}),
    )


class ReportForm(forms.Form):
    reason = forms.CharField(
        max_length=200,
        widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Reason for report'}),
    )
    details = forms.CharField(
        required=False,
        widget=forms.Textarea(attrs={'class': 'form-control', 'rows': 3, 'placeholder': 'Additional details (optional)'}),
    )


# ---------------------------------------------------------------------------
# Super Admin: cascading category / subcategory tree
# ---------------------------------------------------------------------------

class CategoryForm(forms.ModelForm):
    class Meta:
        model = Category
        fields = [
            'label', 'parent', 'listing_model', 'business_subcategory', 'icon',
            'image', 'description', 'order', 'is_active',
        ]
        widgets = {
            'label': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'e.g. Restaurants'}),
            'parent': forms.HiddenInput,
            'listing_model': forms.Select(attrs={'class': 'form-select'}),
            'business_subcategory': forms.TextInput(attrs={
                'class': 'form-control',
                'placeholder': "Matching Business category value (optional, e.g. 'restaurant')",
            }),
            'icon': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'bi-tag (Bootstrap Icons class)'}),
            'image': forms.TextInput(attrs={
                'class': 'form-control',
                'placeholder': "images/services/example.jpg (top-level homepage cards only)",
            }),
            'description': forms.TextInput(attrs={
                'class': 'form-control',
                'placeholder': 'Short blurb for the homepage card (top-level categories only)',
            }),
            'order': forms.NumberInput(attrs={'class': 'form-control'}),
            'is_active': forms.CheckboxInput(attrs={'class': 'form-check-input'}),
        }

    def __init__(self, *args, parent=None, **kwargs):
        super().__init__(*args, **kwargs)
        # Subcategories inherit their parent's listing_model, so hide it/mark optional
        # for them; top-level categories must choose one explicitly.
        is_child = parent is not None or (self.instance and self.instance.parent_id)
        self.fields['listing_model'].required = not is_child
        if is_child:
            self.fields['listing_model'].widget = forms.HiddenInput()


# ---------------------------------------------------------------------------
# Super Admin: platform administration (City Admins, platform settings)
# ---------------------------------------------------------------------------

class CityAdminForm(forms.Form):
    """
    Used both to pre-provision a brand new City Admin (by email, before
    they've ever signed in) and to edit an existing one's name/city scope.
    See auth_callback_api in views.py: on first Google sign-in it already
    falls back to matching an existing User by email, so a pre-provisioned
    account attaches automatically the first time that person signs in.
    """
    full_name = forms.CharField(
        max_length=150, widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Full Name'})
    )
    email = forms.EmailField(
        widget=forms.EmailInput(attrs={'class': 'form-control', 'placeholder': 'name@example.com'})
    )
    cities = forms.ModelMultipleChoiceField(
        queryset=Location.objects.filter(kind=Location.Kind.CITY, is_active=True).order_by('name'),
        widget=forms.SelectMultiple(attrs={'class': 'form-select', 'size': 8}),
        required=False,
    )


class PlatformSettingsForm(forms.ModelForm):
    class Meta:
        model = PlatformSettings
        fields = [
            'site_name', 'support_email', 'support_phone',
            'maintenance_mode', 'maintenance_message', 'auto_approve_listings',
        ]
        widgets = {
            'site_name': forms.TextInput(attrs={'class': 'form-control'}),
            'support_email': forms.EmailInput(attrs={'class': 'form-control'}),
            'support_phone': forms.TextInput(attrs={'class': 'form-control'}),
            'maintenance_mode': forms.CheckboxInput(attrs={'class': 'form-check-input'}),
            'maintenance_message': forms.Textarea(attrs={'class': 'form-control', 'rows': 3}),
            'auto_approve_listings': forms.CheckboxInput(attrs={'class': 'form-check-input'}),
        }


# ---------------------------------------------------------------------------
# City Admin: Sub Admins & Content Providers
# ---------------------------------------------------------------------------

class SubAdminForm(forms.Form):
    """
    Pre-provisions/edits a Sub Admin the same way CityAdminForm does for City
    Admins, but scoped to a single city the acting City Admin actually
    manages — the view passes that restricted queryset in via cities_qs so a
    City Admin can never assign a Sub Admin to a city they don't manage.
    """
    full_name = forms.CharField(
        max_length=150, widget=forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Full Name'})
    )
    email = forms.EmailField(
        widget=forms.EmailInput(attrs={'class': 'form-control', 'placeholder': 'name@example.com'})
    )
    city = forms.ModelChoiceField(
        queryset=Location.objects.none(), widget=forms.Select(attrs={'class': 'form-select'}),
    )

    def __init__(self, *args, cities_qs=None, **kwargs):
        super().__init__(*args, **kwargs)
        if cities_qs is not None:
            self.fields['city'].queryset = cities_qs


class ContentProviderForm(SubAdminForm):
    """Same shape as SubAdminForm, plus which categories this Content
    Provider is granted — writes the same AdminCategoryPermission grant an
    Admin Request approval creates, just City-Admin-initiated instead of
    Super-Admin-approved."""
    categories = forms.ModelMultipleChoiceField(
        queryset=Category.objects.filter(is_active=True),
        widget=forms.CheckboxSelectMultiple,
        required=False,
    )

