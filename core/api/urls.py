from django.urls import path

from . import views

app_name = 'api'

urlpatterns = [
    path('auth/me/', views.me, name='me'),
    path('auth/logout/', views.logout_view, name='logout'),

    path('locations/cities/', views.cities, name='cities'),
    path('locations/cities/<int:pk>/', views.city_detail, name='city_detail'),
    path('locations/reverse-geocode/', views.reverse_geocode_view, name='reverse_geocode'),

    path('categories/', views.categories, name='categories'),
    path('search/', views.search_view, name='search'),

    path('listings/<str:model_key>/', views.listing_collection, name='listing_collection'),
    path('listings/<str:model_key>/<int:pk>/', views.listing_detail, name='listing_detail'),
    path('listings/<str:model_key>/<int:pk>/favorite/', views.toggle_favorite_view, name='toggle_favorite'),
    path('listings/<str:model_key>/<int:pk>/like/', views.toggle_like_view, name='toggle_like'),
    path('listings/<str:model_key>/<int:pk>/comments/', views.comments_view, name='comments'),
    path('listings/<str:model_key>/<int:pk>/reviews/', views.reviews_view, name='reviews'),
    path('listings/<str:model_key>/<int:pk>/report/', views.report_view, name='report'),
    path('listings/<str:model_key>/<int:pk>/media/', views.listing_media_view, name='listing_media'),

    path('media/images/<int:pk>/', views.media_image_delete_view, name='media_image_delete'),
    path('media/videos/<int:pk>/', views.media_video_delete_view, name='media_video_delete'),

    path('my/listings/', views.my_listings_view, name='my_listings'),
    path('my/favorites/', views.my_favorites, name='my_favorites'),

    path('notifications/', views.notifications_view, name='notifications'),
    path('notifications/<int:pk>/read/', views.notification_mark_read_view, name='notification_mark_read'),
    path('notifications/read-all/', views.notifications_mark_all_read_view, name='notifications_mark_all_read'),
    path('notifications/unread-count/', views.notifications_unread_count_view, name='notifications_unread_count'),
]
