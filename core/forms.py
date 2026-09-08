import re

from django import forms
from django.contrib.auth import get_user_model
from django.contrib.auth.forms import AuthenticationForm
from django.contrib.auth.password_validation import validate_password
from django.utils import timezone
from django.utils.html import format_html
from django.utils.safestring import mark_safe

from .models import (
    Business, Category, Event, Job, Location, LostFound, News, PlatformSettings, Profile, Project, Property,
    Scholarship, WORKING_DAYS,
)

User = get_user_model()


#: Matches an actual Google Maps URL (maps.google.*, google.*/maps, goo.gl/maps,
#: maps.app.goo.gl) rather than just any well-formed URL — used both as the
#: HTML `pattern` attribute (frontend rejection) and, compiled below, for the
#: matching backend check in BusinessSubmitForm.clean_maps_link.
GOOGLE_MAPS_URL_PATTERN = (
    r'https?://(www\.)?(google\.[a-zA-Z.]{2,10}/maps|maps\.google\.[a-zA-Z.]{2,10}'
    r'|goo\.gl/maps|maps\.app\.goo\.gl)(/|\?|$).*'
)
GOOGLE_MAPS_URL_RE = re.compile(GOOGLE_MAPS_URL_PATTERN, re.IGNORECASE)


def _clean_10_digit_phone(value, label='Phone number'):
    """Shared exactly-10-digits check reused by every listing form's
    phone/contact field (see BusinessSubmitForm, PropertySubmitForm,
    JobSubmitForm, EventSubmitForm below)."""
    digits = value.strip()
    if not digits.isdigit() or len(digits) != 10:
        raise forms.ValidationError(f'{label} must contain exactly 10 digits.')
    return digits


class LocationFieldsMixin(forms.Form):
    """
    Shared latitude/longitude/maps_link fields — mixed into every listing
    form (Business/Property/Job/Event/Project below) alongside forms.ModelForm
    so `class FooSubmitForm(LocationFieldsMixin, forms.ModelForm)` picks up
    all three without repeating the field declarations 5 times. Pair with
    `clean()` calling _clean_location_fields(self) for the matching
    conditional-requirement logic.
    """
    latitude = forms.DecimalField(required=False, widget=forms.HiddenInput(attrs={'id': 'id_latitude'}))
    longitude = forms.DecimalField(required=False, widget=forms.HiddenInput(attrs={'id': 'id_longitude'}))
    # required=False at the Django level — the real "is this mandatory"
    # decision happens in _clean_location_fields, since it depends on
    # whether latitude/longitude were also submitted (Django validates
    # required=True fields before clean() ever runs, which is too early to
    # know that). The HTML `required` attribute is still set here so the
    # asterisk convention (partials/form_field.html) and native browser
    # validation both still treat it as required by default; main.js's "Use
    # my current location" handler removes that attribute (and hides the
    # field) once a location is actually captured.
    maps_link = forms.URLField(
        required=False,
        widget=forms.URLInput(attrs={
            'class': 'form-control', 'placeholder': 'Google Maps link', 'required': 'required',
            'pattern': GOOGLE_MAPS_URL_PATTERN,
            'title': 'Enter a Google Maps link, e.g. https://maps.google.com/... or https://maps.app.goo.gl/...',
        }),
    )


def _clean_location_fields(form):
    """
    Shared conditional Maps-link requirement, called from clean() on every
    listing form that carries latitude/longitude + maps_link (Business,
    Property, Job, Event, Project — see _location_field_kwargs above).

    If the visitor shared their location (both latitude/longitude were
    captured via the "Use my current location" button), a manual maps_link
    becomes optional and is auto-derived from the coordinates when left
    blank — the user should never have to manually enter a Google Maps link
    once their location is known. Without a shared location, maps_link is
    required and must actually look like a Google Maps URL.
    """
    cleaned = form.cleaned_data
    lat, lng = cleaned.get('latitude'), cleaned.get('longitude')
    maps_link = (cleaned.get('maps_link') or '').strip()

    if lat is not None and lng is not None:
        cleaned['maps_link'] = maps_link or f'https://www.google.com/maps?q={lat},{lng}'
        return

    if not maps_link:
        form.add_error('maps_link', 'Share your location, or enter a Google Maps link.')
        return
    if not GOOGLE_MAPS_URL_RE.match(maps_link):
        form.add_error(
            'maps_link',
            'Enter a valid Google Maps link, e.g. https://maps.google.com/... or https://maps.app.goo.gl/...'
        )
        return
    cleaned['maps_link'] = maps_link


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

    def __init__(self, *args, queryset=None, **kwargs):
        """
        `queryset` lets a caller restrict which categories are offered —
        e.g. request_additional_category() excludes categories the Content
        Provider already holds an AdminCategoryPermission for, so "Add
        Another Category" only ever offers genuinely new ones. Defaults to
        every active category, matching the original first-time-applicant
        behaviour (admin_request_new) unchanged.
        """
        super().__init__(*args, **kwargs)
        if queryset is not None:
            self.fields['categories'].queryset = queryset


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

class WorkingDaysHoursWidget(forms.Widget):
    """
    Renders Business.working_days_hours' 7-day picker: each day gets an
    enable checkbox + start/end time inputs. POSTed as
    <name>_<day>_enabled/<name>_<day>_open/<name>_<day>_close per day and
    reassembled here into the {'mon': {'open': '09:00', 'close': '18:00'},
    ...} dict shape Business.working_days_hours stores. Renders its own
    label/layout (not the shared floating-label wrapper) — see
    partials/form_field.html's is_working_hours_widget check. The "copy to
    all checked days" button is wired up in static/js/main.js.
    """
    #: Marks this widget for partials/form_field.html to render bare (its
    #: own full-width block) instead of the standard floating-label wrapper.
    is_working_hours_widget = True

    def value_from_datadict(self, data, files, name):
        schedule = {}
        for key, _label in WORKING_DAYS:
            if data.get(f'{name}_{key}_enabled'):
                open_time = (data.get(f'{name}_{key}_open') or '').strip()
                close_time = (data.get(f'{name}_{key}_close') or '').strip()
                if open_time and close_time:
                    schedule[key] = {'open': open_time, 'close': close_time}
        return schedule

    def value_omitted_from_data(self, data, files, name):
        # An empty schedule ("nothing checked") is itself a meaningful
        # submitted value, not "field absent" — never fall back to initial.
        return False

    def render(self, name, value, attrs=None, renderer=None):
        value = value or {}
        rows = []
        for key, label in WORKING_DAYS:
            day = value.get(key) or {}
            rows.append(format_html(
                '<div class="hk-wh-row" data-wh-day="{key}">'
                '<label class="hk-wh-day-label form-check">'
                '<input type="checkbox" class="form-check-input hk-wh-enable" '
                'name="{name}_{key}_enabled" {checked}> {label}'
                '</label>'
                '<input type="time" class="form-control form-control-sm hk-wh-time" '
                'name="{name}_{key}_open" value="{open_val}" aria-label="{label} opening time">'
                '<span class="hk-wh-sep">{to}</span>'
                '<input type="time" class="form-control form-control-sm hk-wh-time" '
                'name="{name}_{key}_close" value="{close_val}" aria-label="{label} closing time">'
                '</div>',
                key=key, name=name, checked='checked' if day else '', label=label,
                open_val=day.get('open', ''), close_val=day.get('close', ''), to='to',
            ))
        return format_html(
            '<div class="hk-working-hours-picker" data-wh-name="{name}">{rows}'
            '<button type="button" class="btn btn-outline-secondary btn-sm mt-2 hk-wh-copy-btn">'
            '<i class="bi bi-copy"></i> Copy Monday’s hours to every checked day</button></div>',
            name=name, rows=mark_safe(''.join(rows)),
        )


class WorkingDaysHoursField(forms.Field):
    widget = WorkingDaysHoursWidget

    def __init__(self, *args, **kwargs):
        kwargs.setdefault('required', False)
        kwargs.setdefault('label', 'Working Days & Hours')
        super().__init__(*args, **kwargs)

    def clean(self, value):
        if not isinstance(value, dict):
            return {}
        valid_keys = dict(WORKING_DAYS)
        cleaned = {}
        for key, hours in value.items():
            if key not in valid_keys:
                continue
            open_time, close_time = hours.get('open'), hours.get('close')
            if open_time and close_time and open_time >= close_time:
                raise forms.ValidationError(f'Closing time must be after opening time for {valid_keys[key]}.')
            cleaned[key] = {'open': open_time, 'close': close_time}
        return cleaned


class BusinessSubmitForm(LocationFieldsMixin, forms.ModelForm):
    # Re-declared (rather than left to ModelForm's Meta.widgets-only
    # inference) so it can be made required/pattern-constrained here even
    # though the underlying model field is blank=True — that blank=True is
    # for legacy rows and Django admin edits, not this public submission form.
    phone_number = forms.CharField(
        max_length=10,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Phone Number', 'maxlength': '10',
            'inputmode': 'numeric', 'pattern': '[0-9]{10}',
            'title': 'Enter exactly 10 digits',
        }),
    )
    working_days_hours = WorkingDaysHoursField(required=False)

    class Meta:
        model = Business
        fields = [
            'name', 'category', 'city', 'address', 'phone_number', 'description', 'website',
            'maps_link', 'image', 'image_url', 'latitude', 'longitude',
        ]
        widgets = {
            'name': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Business Name'}),
            'category': forms.Select(attrs={'class': 'form-select'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'address': forms.Textarea(attrs={'class': 'form-control', 'rows': 3, 'placeholder': 'Full Address'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'website': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'https://... (website or social media page)'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }

    def clean_phone_number(self):
        return _clean_10_digit_phone(self.cleaned_data['phone_number'])

    def clean(self):
        cleaned = super().clean()
        _clean_location_fields(self)
        return cleaned

    def save(self, commit=True):
        # working_days_hours isn't in Meta.fields (it's not a plain model
        # field ModelForm's construct_instance() can auto-copy — see
        # WorkingDaysHoursField above), so it needs to be assigned by hand.
        obj = super().save(commit=False)
        obj.working_days_hours = self.cleaned_data.get('working_days_hours', {})
        if commit:
            obj.save()
        return obj


class PropertySubmitForm(LocationFieldsMixin, forms.ModelForm):
    contact_number = forms.CharField(
        max_length=10,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Contact Number', 'maxlength': '10',
            'inputmode': 'numeric', 'pattern': '[0-9]{10}', 'title': 'Enter exactly 10 digits',
        }),
    )

    class Meta:
        model = Property
        fields = [
            'title', 'property_type', 'price', 'location', 'city', 'contact_number', 'description',
            'maps_link', 'image', 'image_url', 'latitude', 'longitude',
        ]
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Property Title'}),
            'property_type': forms.Select(attrs={'class': 'form-select'}),
            'price': forms.NumberInput(attrs={'class': 'form-control', 'placeholder': 'Price (₹)'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Location'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }

    def clean_contact_number(self):
        return _clean_10_digit_phone(self.cleaned_data['contact_number'], 'Contact number')

    def clean(self):
        cleaned = super().clean()
        _clean_location_fields(self)
        return cleaned


class JobSubmitForm(LocationFieldsMixin, forms.ModelForm):
    contact_number = forms.CharField(
        max_length=10,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Contact Number', 'maxlength': '10',
            'inputmode': 'numeric', 'pattern': '[0-9]{10}', 'title': 'Enter exactly 10 digits',
        }),
    )

    class Meta:
        model = Job
        fields = [
            'job_title', 'company', 'job_type', 'location', 'city', 'salary', 'contact_number', 'description',
            'shift_date', 'shift_start_time', 'shift_end_time', 'maps_link', 'image', 'image_url',
            'latitude', 'longitude',
        ]
        widgets = {
            'job_title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Job Title'}),
            'company': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Company'}),
            'job_type': forms.Select(attrs={'class': 'form-select'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Location'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'salary': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Salary (optional)'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            # Optional on every job (not just Hourly Basis) — e.g. a one-day
            # hiring drive can carry a date/window too.
            'shift_date': forms.DateInput(attrs={'class': 'form-control', 'type': 'date'}),
            'shift_start_time': forms.TimeInput(attrs={'class': 'form-control', 'type': 'time'}),
            'shift_end_time': forms.TimeInput(attrs={'class': 'form-control', 'type': 'time'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }

    def clean_contact_number(self):
        return _clean_10_digit_phone(self.cleaned_data['contact_number'], 'Contact number')

    def clean(self):
        cleaned = super().clean()
        _clean_location_fields(self)
        return cleaned


class EventSubmitForm(LocationFieldsMixin, forms.ModelForm):
    # Stays optional (blank=True on the model) — only format-checked when filled.
    contact_number = forms.CharField(
        max_length=10, required=False,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Contact Number (optional)', 'maxlength': '10',
            'inputmode': 'numeric', 'pattern': '[0-9]{10}', 'title': 'Enter exactly 10 digits',
        }),
    )

    class Meta:
        model = Event
        fields = [
            'title', 'event_date', 'event_time', 'location', 'city', 'contact_number', 'description',
            'maps_link', 'image', 'image_url', 'latitude', 'longitude',
        ]
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Event Title'}),
            'event_date': forms.DateInput(attrs={'class': 'form-control', 'type': 'date'}),
            'event_time': forms.TimeInput(attrs={'class': 'form-control', 'type': 'time'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Venue / Location'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }

    def clean_contact_number(self):
        contact = self.cleaned_data.get('contact_number', '').strip()
        if not contact:
            return contact
        return _clean_10_digit_phone(contact, 'Contact number')

    def clean(self):
        cleaned = super().clean()
        _clean_location_fields(self)
        return cleaned


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


class ScholarshipSubmitForm(forms.ModelForm):
    contact_number = forms.CharField(
        max_length=10, required=False,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Contact Number (optional)', 'maxlength': '10',
            'inputmode': 'numeric', 'pattern': '[0-9]{10}', 'title': 'Enter exactly 10 digits',
        }),
    )

    class Meta:
        model = Scholarship
        fields = [
            'title', 'scholarship_type', 'provider', 'city', 'description', 'eligibility',
            'application_deadline', 'official_url', 'contact_number', 'image', 'image_url',
        ]
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Scholarship / Scheme Title'}),
            'scholarship_type': forms.Select(attrs={'class': 'form-select'}),
            'provider': forms.TextInput(attrs={'class': 'form-control', 'placeholder': 'Provider / Authority'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description (optional)'}),
            'eligibility': forms.Textarea(attrs={'class': 'form-control', 'rows': 3, 'placeholder': 'Eligibility (optional)'}),
            'application_deadline': forms.DateInput(attrs={'class': 'form-control', 'type': 'date'}),
            'official_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Official application/info URL (optional)'}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }

    def clean_contact_number(self):
        contact = self.cleaned_data.get('contact_number', '').strip()
        if not contact:
            return contact
        return _clean_10_digit_phone(contact, 'Contact number')


class LostFoundSubmitForm(forms.ModelForm):
    contact_number = forms.CharField(
        max_length=10, required=False,
        widget=forms.TextInput(attrs={
            'class': 'form-control', 'placeholder': 'Contact Number (optional — or use comments)', 'maxlength': '10',
            'inputmode': 'numeric', 'pattern': '[0-9]{10}', 'title': 'Enter exactly 10 digits',
        }),
    )

    class Meta:
        model = LostFound
        fields = [
            'report_type', 'title', 'item_category', 'city', 'description', 'event_date', 'location',
            'contact_number', 'image', 'image_url',
        ]
        widgets = {
            'report_type': forms.Select(attrs={'class': 'form-select'}),
            'title': forms.TextInput(attrs={'class': 'form-control', 'placeholder': "e.g. 'Black leather wallet'"}),
            'item_category': forms.Select(attrs={'class': 'form-select'}),
            'city': forms.Select(attrs={'class': 'form-select'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4, 'placeholder': 'Description (optional)'}),
            'event_date': forms.DateInput(attrs={'class': 'form-control', 'type': 'date'}),
            'location': forms.TextInput(attrs={'class': 'form-control', 'placeholder': "Where it was lost/found"}),
            'image': forms.ClearableFileInput(attrs={'class': 'form-control'}),
            'image_url': forms.URLInput(attrs={'class': 'form-control', 'placeholder': 'Image URL (optional)'}),
        }

    def clean_contact_number(self):
        contact = self.cleaned_data.get('contact_number', '').strip()
        if not contact:
            return contact
        return _clean_10_digit_phone(contact, 'Contact number')

    def clean_event_date(self):
        event_date = self.cleaned_data.get('event_date')
        if event_date and event_date > timezone.localdate():
            raise forms.ValidationError('The date lost/found cannot be in the future.')
        return event_date


class ProjectSubmitForm(LocationFieldsMixin, forms.ModelForm):
    class Meta:
        model = Project
        fields = [
            'title', 'project_status', 'location', 'city', 'expected_completion', 'department', 'description',
            'maps_link', 'image', 'image_url', 'latitude', 'longitude',
        ]
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

    def clean(self):
        cleaned = super().clean()
        _clean_location_fields(self)
        return cleaned


LISTING_SUBMIT_FORMS = {
    'business': BusinessSubmitForm,
    'property': PropertySubmitForm,
    'job': JobSubmitForm,
    'event': EventSubmitForm,
    'news': NewsSubmitForm,
    'project': ProjectSubmitForm,
    'scholarship': ScholarshipSubmitForm,
    'lostfound': LostFoundSubmitForm,
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

