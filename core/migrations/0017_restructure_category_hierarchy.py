# Restructures the flat, never-nested Category rows (Business/Shop/Restaurant/
# School/College/Hospital/Transport Service/Other, each a separate top-level
# row with zero children) into the same broad-category-plus-subcategory shape
# the homepage's hardcoded CATEGORIES list already used. This lets the
# homepage grid become DB-driven from these top-level rows, and gives the
# "Manage Categories" admin table real subcategories to expand instead of
# permanently showing "No subcategories yet."
#
# Existing top-level rows are renamed/reparented in place (by pk) rather than
# deleted and recreated, so nothing referencing them (permission grants,
# onboarding checkboxes) breaks.
from django.db import migrations


HOMEPAGE_COPY = {
    'real-estate': (
        'Real Estate', 'images/services/real-estate.jpg',
        'Find houses, apartments, plots, villas, rental properties, and commercial spaces available in your location.',
    ),
    'businesses': (
        'Businesses', 'images/services/business.jpg',
        'Explore car garages, clothing and textile shops, stationery shops, supermarkets, salons, and other local businesses across the city.',
    ),
    'jobs': (
        'Jobs', 'images/services/jobs.jpg',
        'Browse job openings from local shops, offices, and companies hiring across the city.',
    ),
    'events': (
        'Events', 'images/services/events.jpg',
        'Stay updated with upcoming festivals, exhibitions, public events, and local programs.',
    ),
    'restaurants': (
        'Restaurants', 'images/services/restaurants.jpg',
        'Discover the best restaurants, cafés, bakeries, and food outlets in your location.',
    ),
    'hospitals': (
        'Hospitals & Healthcare', 'images/services/hospitals.jpg',
        'Find hospitals, clinics, diagnostic centers, pharmacies, and emergency healthcare services.',
    ),
    'education': (
        'Education', 'images/services/education.jpg',
        'Explore schools, colleges, universities, and other educational institutions in your location.',
    ),
    'transport': (
        'Transport', 'images/services/transport.jpg',
        'Find buses, taxis, auto services, logistics, and transportation facilities.',
    ),
    'news': (
        'OneTownCity News', 'images/services/news.jpg',
        'Catch up on local announcements, civic updates, and news from across the city.',
    ),
    'projects': (
        'Upcoming Projects', 'images/services/upcoming-projects.jpg',
        'Track planned and ongoing civic and infrastructure projects shaping your city.',
    ),
}


def restructure(apps, schema_editor):
    Category = apps.get_model('core', 'Category')

    def top(key, homepage_key, order, icon=None, business_subcategory=''):
        label, image, description = HOMEPAGE_COPY[homepage_key]
        cat = Category.objects.get(key=key)
        cat.label = label
        cat.image = image
        cat.description = description
        cat.order = order
        cat.parent = None
        cat.listing_model = cat.listing_model or 'business'
        cat.business_subcategory = business_subcategory
        if icon:
            cat.icon = icon
        cat.save()
        return cat

    def child(parent, key, label, business_subcategory, listing_model, icon, order):
        Category.objects.filter(key=key).delete()  # in case of a stray re-run
        return Category.objects.create(
            key=key, label=label, parent=parent, listing_model=listing_model,
            business_subcategory=business_subcategory, icon=icon, order=order, is_active=True,
        )

    # --- Businesses (catch-all) -------------------------------------------
    businesses = top('business', 'businesses', order=2, icon='bi-shop')
    shop = Category.objects.get(key='shop')
    shop.label, shop.parent, shop.order = 'Retail Shops', businesses, 1
    shop.save()
    other = Category.objects.get(key='other')
    other.label, other.parent, other.order = 'Other Businesses', businesses, 10
    other.save()
    child(businesses, 'grocery', 'Grocery Store', 'grocery', 'business', 'bi-basket', 2)
    child(businesses, 'electronics', 'Electronics', 'electronics', 'business', 'bi-tv', 3)
    child(businesses, 'clothing', 'Clothing & Fashion', 'clothing', 'business', 'bi-bag-heart', 4)
    child(businesses, 'hardware', 'Hardware & Building Materials', 'hardware', 'business', 'bi-tools', 5)
    child(businesses, 'salon', 'Salon & Spa', 'salon', 'business', 'bi-scissors', 6)
    child(businesses, 'automobile', 'Automobile & Repair', 'automobile', 'business', 'bi-car-front', 7)
    child(businesses, 'stationery', 'Stationery & Books', 'stationery', 'business', 'bi-pencil', 8)
    child(businesses, 'jewellery', 'Jewellery', 'jewellery', 'business', 'bi-gem', 9)

    # --- Restaurants ---------------------------------------------------
    restaurants = top('restaurant', 'restaurants', order=5, business_subcategory='restaurant')
    restaurants.key = 'restaurants'
    restaurants.save()
    child(restaurants, 'bakery', 'Bakery & Sweets', 'bakery', 'business', 'bi-cup-straw', 1)

    # --- Hospitals & Healthcare ------------------------------------------
    hospitals = top('hospital', 'hospitals', order=6, business_subcategory='hospital')
    hospitals.key = 'hospitals'
    hospitals.save()
    child(hospitals, 'pharmacy', 'Pharmacy', 'pharmacy', 'business', 'bi-capsule', 1)

    # --- Education (new parent; School/College reparented under it) ------
    label, image, description = HOMEPAGE_COPY['education']
    education = Category.objects.create(
        key='education', label=label, image=image, description=description,
        listing_model='business', icon='bi-mortarboard', order=7, is_active=True,
    )
    school = Category.objects.get(key='school')
    school.label, school.parent, school.order = 'School', education, 1
    school.save()
    college = Category.objects.get(key='college')
    college.label, college.parent, college.order = 'College & University', education, 2
    college.save()

    # --- Transport (single real sub-category value; left without children) ---
    top('transport', 'transport', order=8, business_subcategory='transport')

    # --- Real Estate --------------------------------------------------
    real_estate = top('property', 'real-estate', order=1, icon='bi-house-door')
    child(real_estate, 'property-sale', 'For Sale', 'sale', 'property', 'bi-house-check', 1)
    child(real_estate, 'property-rent', 'For Rent', 'rent', 'property', 'bi-house-heart', 2)
    child(real_estate, 'property-plot', 'Plots & Land', 'plot', 'property', 'bi-map', 3)
    child(real_estate, 'property-commercial', 'Commercial Space', 'commercial', 'property', 'bi-building', 4)

    # --- Jobs / Events / News: no real filterable sub-dimension yet ------
    top('job', 'jobs', order=3, icon='bi-briefcase')
    top('event', 'events', order=4, icon='bi-calendar-event')
    top('news', 'news', order=9, icon='bi-newspaper')

    # --- Upcoming Projects -------------------------------------------
    projects = top('project', 'projects', order=10, icon='bi-cone-striped')
    child(projects, 'project-planned', 'Planned', 'planned', 'project', 'bi-hourglass', 1)
    child(projects, 'project-ongoing', 'Ongoing', 'ongoing', 'project', 'bi-cone-striped', 2)
    child(projects, 'project-completed', 'Completed', 'completed', 'project', 'bi-check-circle', 3)


def unrestructure(apps, schema_editor):
    # Best-effort reverse: flatten everything back to top-level and blank the
    # homepage fields. Doesn't attempt to restore the exact original labels/
    # keys for rows this migration renamed.
    Category = apps.get_model('core', 'Category')
    Category.objects.exclude(key__in=[
        'business', 'shop', 'restaurants', 'school', 'college', 'hospitals',
        'property', 'job', 'event', 'news', 'transport', 'other', 'project',
    ]).delete()
    Category.objects.update(parent=None, image='', description='')


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0016_category_description_category_image_and_more'),
    ]

    operations = [
        migrations.RunPython(restructure, unrestructure),
    ]
