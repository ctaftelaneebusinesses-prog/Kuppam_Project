from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import Comment, Favorite, Like, ListingStatus, Report, Review

from . import factories as f


class FavoriteToggleTests(APITestCase):
    def setUp(self):
        self.owner, _ = f.make_user()
        self.viewer, _ = f.make_user()
        self.suspended, _ = f.make_user(suspended=True)
        self.public_listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)
        self.pending_listing = f.make_business(owner=self.owner, status=ListingStatus.PENDING)

    def url(self, obj):
        return reverse('api:toggle_favorite', args=['business', obj.pk])

    def test_unauthenticated_cannot_favorite(self):
        response = self.client.post(self.url(self.public_listing))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_authenticated_user_can_favorite_and_unfavorite(self):
        self.client.force_authenticate(self.viewer)
        first = self.client.post(self.url(self.public_listing))
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertTrue(first.data['favorited'])
        self.assertTrue(Favorite.objects.filter(user=self.viewer).exists())

        second = self.client.post(self.url(self.public_listing))
        self.assertFalse(second.data['favorited'])
        self.assertFalse(Favorite.objects.filter(user=self.viewer).exists())

    def test_owner_can_favorite_own_listing(self):
        self.client.force_authenticate(self.owner)
        response = self.client.post(self.url(self.public_listing))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_suspended_user_can_still_favorite(self):
        """Suspension blocks listing-management writes, not ordinary community actions."""
        self.client.force_authenticate(self.suspended)
        response = self.client.post(self.url(self.public_listing))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_cannot_favorite_pending_listing_as_non_owner(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.post(self.url(self.pending_listing))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_invalid_listing_id_is_404(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.post(reverse('api:toggle_favorite', args=['business', 999999]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_my_favorites_list_and_pagination(self):
        self.client.force_authenticate(self.viewer)
        self.client.post(self.url(self.public_listing))
        response = self.client.get(reverse('api:my_favorites'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 1)

    def test_my_favorites_empty_for_new_user(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(reverse('api:my_favorites'))
        self.assertEqual(response.data['count'], 0)
        self.assertEqual(response.data['results'], [])


class LikeToggleTests(APITestCase):
    def setUp(self):
        self.owner, _ = f.make_user()
        self.viewer, _ = f.make_user()
        self.listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:toggle_like', args=['business', self.listing.pk])

    def test_unauthenticated_cannot_like(self):
        response = self.client.post(self.url())
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_like_then_unlike_updates_denormalized_count(self):
        self.client.force_authenticate(self.viewer)
        self.client.post(self.url())
        self.listing.refresh_from_db()
        self.assertEqual(self.listing.like_count, 1)
        self.assertTrue(Like.objects.filter(user=self.viewer).exists())

        self.client.post(self.url())
        self.listing.refresh_from_db()
        self.assertEqual(self.listing.like_count, 0)


class CommentTests(APITestCase):
    def setUp(self):
        self.owner, _ = f.make_user()
        self.commenter, _ = f.make_user()
        self.blocked, _ = f.make_user(blocked=True)
        self.listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:comments', args=['business', self.listing.pk])

    def test_anonymous_can_read_comments(self):
        Comment.objects.create(content_type=self._ct(), object_id=self.listing.pk, user=self.owner, body='hi')
        response = self.client.get(self.url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 1)

    def test_empty_comment_list(self):
        response = self.client.get(self.url())
        self.assertEqual(response.data['count'], 0)
        self.assertEqual(response.data['results'], [])

    def test_unauthenticated_cannot_post_comment(self):
        response = self.client.post(self.url(), {'body': 'Nice place'})
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_authenticated_user_can_comment(self):
        self.client.force_authenticate(self.commenter)
        response = self.client.post(self.url(), {'body': 'Nice place'})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['body'], 'Nice place')

    def test_blocked_user_cannot_comment(self):
        self.client.force_authenticate(self.blocked)
        response = self.client.post(self.url(), {'body': 'spam'})
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_empty_body_is_validation_error(self):
        self.client.force_authenticate(self.commenter)
        response = self.client.post(self.url(), {'body': ''})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_reply_nests_under_parent(self):
        self.client.force_authenticate(self.commenter)
        parent = self.client.post(self.url(), {'body': 'top level'}).data
        reply = self.client.post(self.url(), {'body': 'a reply', 'parent_id': parent['id']})
        self.assertEqual(reply.status_code, status.HTTP_201_CREATED)
        listing = self.client.get(self.url())
        self.assertEqual(listing.data['count'], 1)  # only the top-level comment paginates
        self.assertEqual(len(listing.data['results'][0]['replies']), 1)

    def test_pending_listing_comments_404_for_non_owner(self):
        pending = f.make_business(owner=self.owner, status=ListingStatus.PENDING)
        self.client.force_authenticate(self.commenter)
        response = self.client.get(reverse('api:comments', args=['business', pending.pk]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def _ct(self):
        from django.contrib.contenttypes.models import ContentType
        from core.models import Business
        return ContentType.objects.get_for_model(Business)


class ReviewTests(APITestCase):
    def setUp(self):
        self.owner, _ = f.make_user()
        self.reviewer, _ = f.make_user()
        self.listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:reviews', args=['business', self.listing.pk])

    def test_unauthenticated_cannot_review(self):
        response = self.client.post(self.url(), {'rating': 5, 'body': 'Great!'})
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_authenticated_user_can_review(self):
        self.client.force_authenticate(self.reviewer)
        response = self.client.post(self.url(), {'rating': 5, 'body': 'Great!'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['rating'], 5)

    def test_second_review_from_same_user_updates_not_duplicates(self):
        self.client.force_authenticate(self.reviewer)
        self.client.post(self.url(), {'rating': 3, 'body': 'ok'})
        self.client.post(self.url(), {'rating': 5, 'body': 'actually great'})
        self.assertEqual(Review.objects.filter(user=self.reviewer, object_id=self.listing.pk).count(), 1)

    def test_invalid_rating_is_validation_error(self):
        self.client.force_authenticate(self.reviewer)
        response = self.client.post(self.url(), {'rating': 99, 'body': 'x'})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_avg_rating_recomputed_on_review(self):
        self.client.force_authenticate(self.reviewer)
        self.client.post(self.url(), {'rating': 4, 'body': 'good'})
        self.listing.refresh_from_db()
        self.assertEqual(float(self.listing.avg_rating), 4.0)


class ReportTests(APITestCase):
    def setUp(self):
        self.owner, _ = f.make_user()
        self.reporter, _ = f.make_user()
        self.listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:report', args=['business', self.listing.pk])

    def test_unauthenticated_cannot_report(self):
        response = self.client.post(self.url(), {'reason': 'spam'})
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_authenticated_user_can_report(self):
        self.client.force_authenticate(self.reporter)
        response = self.client.post(self.url(), {'reason': 'spam', 'details': 'looks fake'})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(Report.objects.filter(user=self.reporter, object_id=self.listing.pk).exists())

    def test_missing_reason_is_validation_error(self):
        self.client.force_authenticate(self.reporter)
        response = self.client.post(self.url(), {'reason': ''})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
