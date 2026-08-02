from django.contrib.sitemaps import Sitemap
from django.urls import reverse

from .models import Business, Event, Job, ListingStatus, News, Project, Property


class StaticViewSitemap(Sitemap):
    """Fixed pages that never change URL — home, about, history, directory categories."""
    priority = 0.6
    changefreq = 'weekly'

    def items(self):
        return [
            'core:home', 'core:about', 'core:history', 'core:contact',
            'core:business_list', 'core:restaurant_list', 'core:hospital_list',
            'core:education_list', 'core:transport_list',
            'core:property_list', 'core:job_list', 'core:event_list',
            'core:news_list', 'core:project_list',
        ]

    def location(self, item):
        return reverse(item)


class _ListingSitemap(Sitemap):
    """Base for one Sitemap per listing model — only public (approved + active) rows are indexable."""
    changefreq = 'weekly'
    priority = 0.8
    model = None

    def items(self):
        return self.model.objects.filter(is_active=True, status=ListingStatus.APPROVED).exclude(slug__isnull=True)

    def lastmod(self, obj):
        return obj.updated_at


class BusinessSitemap(_ListingSitemap):
    model = Business


class PropertySitemap(_ListingSitemap):
    model = Property
    priority = 0.7


class JobSitemap(_ListingSitemap):
    model = Job
    changefreq = 'daily'
    priority = 0.6


class EventSitemap(_ListingSitemap):
    model = Event
    changefreq = 'daily'
    priority = 0.6


class NewsSitemap(_ListingSitemap):
    model = News
    changefreq = 'daily'
    priority = 0.6


class ProjectSitemap(_ListingSitemap):
    model = Project
    priority = 0.6


sitemaps = {
    'static': StaticViewSitemap,
    'businesses': BusinessSitemap,
    'properties': PropertySitemap,
    'jobs': JobSitemap,
    'events': EventSitemap,
    'news': NewsSitemap,
    'projects': ProjectSitemap,
}
