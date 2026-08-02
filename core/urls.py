from django.urls import path
from . import views

app_name = 'core'

urlpatterns = [
    path('', views.home, name='home'),
    path('search/', views.search, name='search'),
    path('history/', views.history, name='history'),
    path('about/', views.about, name='about'),
    path('contact/', views.contact, name='contact'),
    path('newsletter/subscribe/', views.newsletter_subscribe, name='newsletter_subscribe'),
    path('privacy-policy/', views.privacy_policy, name='privacy_policy'),
    path('terms-of-service/', views.terms_of_service, name='terms_of_service'),

    # Staff-only login for the existing Django admin backend (unchanged).
    path('login/', views.admin_login, name='admin_login'),
    path('logout/', views.admin_logout, name='admin_logout'),

    # Main sign-in page (Sign In tab: password + Google; Register tab: self-signup)
    # for everyone, including the Super Admin.
    path('signin/', views.google_login, name='google_login'),
    path('signin/password/', views.password_login, name='password_login'),
    path('register/', views.register, name='register'),
    path('auth/callback/', views.auth_callback_page, name='auth_callback'),
    path('auth/callback/complete/', views.auth_callback_api, name='auth_callback_api'),

    # Onboarding: profile completion -> "what brings you here?" -> admin request
    path('welcome/profile/', views.complete_profile, name='complete_profile'),
    path('welcome/intent/', views.choose_intent, name='choose_intent'),
    path('welcome/upload-request/', views.admin_request_new, name='admin_request_new'),
    path('welcome/upload-request/status/', views.admin_request_pending, name='admin_request_pending'),

    # Businesses
    path('businesses/', views.business_list, name='business_list'),
    path('businesses/<slug:slug>/', views.business_detail, name='business_detail'),

    # Restaurants / Hospitals & Healthcare / Education / Transport
    # (all Business records, filtered by category — see DIRECTORY_CATEGORIES).
    # Education is one category page (School + College & University as
    # filterable sub-tags within it), not two separate top-level pages.
    # Shopping-type businesses (retail, grocery, clothing) live on the
    # general businesses/ page as sub-category tags, same as automobile,
    # hardware, salon, stationery, jewellery, etc.
    path('restaurants/', views.directory_list, {'category': 'restaurants'}, name='restaurant_list'),
    path('hospitals/', views.directory_list, {'category': 'hospitals'}, name='hospital_list'),
    path('education/', views.directory_list, {'category': 'education'}, name='education_list'),
    path('transport/', views.directory_list, {'category': 'transport'}, name='transport_list'),

    # Properties
    path('properties/', views.property_list, name='property_list'),
    path('properties/<slug:slug>/', views.property_detail, name='property_detail'),

    # Jobs
    path('jobs/', views.job_list, name='job_list'),
    path('jobs/<slug:slug>/', views.job_detail, name='job_detail'),

    # Events
    path('events/', views.event_list, name='event_list'),
    path('events/<slug:slug>/', views.event_detail, name='event_detail'),

    # News
    path('news/', views.news_list, name='news_list'),
    path('news/<slug:slug>/', views.news_detail, name='news_detail'),

    # Upcoming Projects
    path('projects/', views.project_list, name='project_list'),
    path('projects/<slug:slug>/', views.project_detail, name='project_detail'),

    # Community features (generic across all 5 listing types, keyed by model_key/pk)
    path('l/<str:model_key>/<int:pk>/like/', views.toggle_like, name='toggle_like'),
    path('l/<str:model_key>/<int:pk>/favorite/', views.toggle_favorite, name='toggle_favorite'),
    path('l/<str:model_key>/<int:pk>/comment/', views.add_comment, name='add_comment'),
    path('l/<str:model_key>/<int:pk>/review/', views.add_review, name='add_review'),
    path('l/<str:model_key>/<int:pk>/report/', views.report_listing, name='report_listing'),
    path('l/<str:model_key>/<int:pk>/share/', views.record_share, name='record_share'),
    path('favorites/', views.my_favorites, name='my_favorites'),

    # Notifications
    path('notifications/', views.notifications_list, name='notifications_list'),
    path('notifications/<int:pk>/read/', views.notification_mark_read, name='notification_mark_read'),
    path('notifications/read-all/', views.notifications_mark_all_read, name='notifications_mark_all_read'),
    path('notifications/<int:pk>/open/', views.notification_open, name='notification_open'),
    path('notifications/unread-count/', views.notifications_unread_count, name='notifications_unread_count'),

    # Content Provider (Admin) — "My Listings" + submission wizard
    path('dashboard/my-listings/', views.my_listings, name='my_listings'),
    path('dashboard/my-listings/new/<str:category_key>/', views.listing_submit, name='listing_submit'),
    path('dashboard/my-listings/<str:model_key>/<int:pk>/edit/', views.listing_edit, name='listing_edit'),
    path('dashboard/my-listings/<str:model_key>/<int:pk>/delete/', views.listing_delete, name='listing_delete'),
    path('dashboard/my-listings/<str:model_key>/<int:pk>/media/add/', views.my_listing_media_add, name='my_listing_media_add'),
    path('dashboard/my-listings/media/<str:media_type>/<int:pk>/delete/', views.my_listing_media_delete, name='my_listing_media_delete'),
    path('dashboard/my-listings/media/image/<int:pk>/set-cover/', views.my_listing_media_set_cover, name='my_listing_media_set_cover'),

    # Dashboard (role-dispatched: Super Admin vs Admin/Content Provider)
    path('dashboard/', views.dashboard, name='dashboard'),
    path('dashboard/profile/', views.dashboard_profile, name='dashboard_profile'),
    path('dashboard/users/', views.dashboard_users, name='dashboard_users'),
    path('dashboard/users/<int:user_id>/', views.dashboard_user_detail, name='dashboard_user_detail'),
    path('dashboard/users/<int:user_id>/block/', views.dashboard_user_toggle_block, name='dashboard_user_toggle_block'),
    path('dashboard/users/<int:user_id>/suspend/', views.dashboard_user_toggle_suspend, name='dashboard_user_toggle_suspend'),
    path('dashboard/users/bulk-delete/', views.dashboard_users_bulk_delete, name='dashboard_users_bulk_delete'),
    path('dashboard/users/export/excel/', views.dashboard_users_export_excel, name='dashboard_users_export_excel'),
    path('dashboard/users/export/pdf/', views.dashboard_users_export_pdf, name='dashboard_users_export_pdf'),
    path('dashboard/admin-requests/', views.dashboard_admin_requests, name='dashboard_admin_requests'),
    path('dashboard/admin-requests/<int:pk>/', views.dashboard_admin_request_detail, name='dashboard_admin_request_detail'),
    path('dashboard/categories/', views.dashboard_categories, name='dashboard_categories'),
    path('dashboard/site-theme/', views.dashboard_site_settings, name='dashboard_site_settings'),
    path('dashboard/messages/', views.dashboard_messages, name='dashboard_messages'),
    path('dashboard/messages/<int:pk>/toggle-read/', views.dashboard_message_toggle_read, name='dashboard_message_toggle_read'),
    path('dashboard/listings/', views.dashboard_pending_listings, name='dashboard_pending_listings'),
    path('dashboard/listings/<str:model_key>/<int:pk>/review/', views.dashboard_listing_review, name='dashboard_listing_review'),

    # Posts (Super Admin — unified cross-model listing management)
    path('dashboard/posts/', views.dashboard_posts, name='dashboard_posts'),
    path('dashboard/posts/new/', views.dashboard_post_create_picker, name='dashboard_post_create_picker'),
    path('dashboard/posts/new/<str:category_key>/', views.dashboard_post_create, name='dashboard_post_create'),
    path('dashboard/posts/bulk-action/', views.dashboard_posts_bulk_action, name='dashboard_posts_bulk_action'),
    path('dashboard/posts/export/excel/', views.dashboard_posts_export_excel, name='dashboard_posts_export_excel'),
    path('dashboard/posts/export/pdf/', views.dashboard_posts_export_pdf, name='dashboard_posts_export_pdf'),
    path('dashboard/posts/images/<int:image_pk>/delete/', views.dashboard_post_delete_image, name='dashboard_post_delete_image'),
    path('dashboard/posts/images/<int:image_pk>/set-cover/', views.dashboard_post_set_cover_image, name='dashboard_post_set_cover_image'),
    path('dashboard/posts/videos/<int:video_pk>/delete/', views.dashboard_post_delete_video, name='dashboard_post_delete_video'),
    path('dashboard/posts/<str:model_key>/<int:pk>/', views.dashboard_post_detail, name='dashboard_post_detail'),
    path('dashboard/posts/<str:model_key>/<int:pk>/toggle-active/', views.dashboard_post_toggle_active, name='dashboard_post_toggle_active'),
    path('dashboard/posts/<str:model_key>/<int:pk>/toggle-featured/', views.dashboard_post_toggle_featured, name='dashboard_post_toggle_featured'),
    path('dashboard/posts/<str:model_key>/<int:pk>/images/add/', views.dashboard_post_add_images, name='dashboard_post_add_images'),

    # Excel Upload Center (staff only)
    path('uploads/', views.upload_hub, name='upload_hub'),
    path('uploads/<str:model_key>/', views.upload_view, name='upload_view'),
    path('uploads/<str:model_key>/sample/', views.download_sample, name='download_sample'),
]
