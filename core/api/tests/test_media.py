from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from core.models import ListingStatus, PostImage

from . import factories as f


def _image(name='photo.jpg'):
    return SimpleUploadedFile(name, b'not-really-a-jpeg-but-content-type-is-what-is-checked', content_type='image/jpeg')


class ListingMediaTests(APITestCase):
    def setUp(self):
        self.owner, _ = f.make_user()
        self.other, _ = f.make_user()
        self.listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)

    def url(self):
        return reverse('api:listing_media', args=['business', self.listing.pk])

    def test_anonymous_can_view_gallery(self):
        response = self.client.get(self.url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['images'], [])

    def test_unauthenticated_cannot_upload(self):
        response = self.client.post(self.url(), {'images': [_image()]}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_non_owner_cannot_upload(self):
        self.client.force_authenticate(self.other)
        response = self.client.post(self.url(), {'images': [_image()]}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_owner_can_upload_image(self):
        self.client.force_authenticate(self.owner)
        response = self.client.post(self.url(), {'images': [_image()]}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(len(response.data['images']), 1)
        self.assertTrue(PostImage.objects.filter(object_id=self.listing.pk).exists())

    def test_no_files_is_validation_error(self):
        self.client.force_authenticate(self.owner)
        response = self.client.post(self.url(), {}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_wrong_content_type_is_rejected(self):
        self.client.force_authenticate(self.owner)
        bad_file = SimpleUploadedFile('doc.pdf', b'pdf-bytes', content_type='application/pdf')
        response = self.client.post(self.url(), {'images': [bad_file]}, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_unknown_model_key_is_404(self):
        response = self.client.get(reverse('api:listing_media', args=['tuition', 1]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class ListingMediaDeleteTests(APITestCase):
    def setUp(self):
        self.owner, self.owner_profile = f.make_user()
        self.other, _ = f.make_user()
        self.listing = f.make_business(owner=self.owner, status=ListingStatus.APPROVED)
        self.client.force_authenticate(self.owner)
        upload = self.client.post(
            reverse('api:listing_media', args=['business', self.listing.pk]), {'images': [_image()]}, format='multipart'
        )
        self.image_id = upload.data['images'][0]['id']
        self.client.force_authenticate(None)

    def test_unauthenticated_cannot_delete(self):
        response = self.client.delete(reverse('api:media_image_delete', args=[self.image_id]))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_non_owner_cannot_delete(self):
        self.client.force_authenticate(self.other)
        response = self.client.delete(reverse('api:media_image_delete', args=[self.image_id]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_owner_can_delete(self):
        self.client.force_authenticate(self.owner)
        response = self.client.delete(reverse('api:media_image_delete', args=[self.image_id]))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(PostImage.objects.filter(pk=self.image_id).exists())

    def test_invalid_id_is_404(self):
        self.client.force_authenticate(self.owner)
        response = self.client.delete(reverse('api:media_image_delete', args=[999999]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
