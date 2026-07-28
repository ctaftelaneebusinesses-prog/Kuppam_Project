"""
Django file-storage backend that persists to Supabase Storage instead of
local disk. Required because most deployment hosts (Render/Railway, etc.)
run on ephemeral filesystems — anything saved to local MEDIA_ROOT is lost
on every redeploy/restart. Every ImageField in the project (listing photos,
profile photos, the post gallery) uses this transparently once it's set as
STORAGES['default']['BACKEND'] in settings.py.

Uploads use the service-role key so they bypass Storage RLS policies —
this backend only ever runs server-side, never in the browser.
"""
import mimetypes

from django.conf import settings
from django.core.files.storage import Storage
from django.utils.deconstruct import deconstructible
from supabase import create_client


@deconstructible
class SupabaseMediaStorage(Storage):
    def __init__(self, bucket_name=None):
        self.bucket_name = bucket_name or settings.SUPABASE_STORAGE_BUCKET
        self._client = None

    @property
    def client(self):
        if self._client is None:
            self._client = create_client(settings.SUPABASE_URL, settings.SUPABASE_SERVICE_ROLE_KEY)
        return self._client

    @property
    def bucket(self):
        return self.client.storage.from_(self.bucket_name)

    def _save(self, name, content):
        name = name.replace('\\', '/')
        content_type, _ = mimetypes.guess_type(name)
        content.seek(0)
        data = content.read()
        self.bucket.upload(
            name, data,
            file_options={'content-type': content_type or 'application/octet-stream', 'upsert': 'true'},
        )
        return name

    def _list_names(self, folder, filename):
        try:
            return {entry['name'] for entry in self.bucket.list(folder or '')}
        except Exception:
            return set()

    def exists(self, name):
        name = name.replace('\\', '/')
        folder, _, filename = name.rpartition('/')
        return filename in self._list_names(folder, filename)

    def url(self, name):
        return self.bucket.get_public_url(name.replace('\\', '/'))

    def delete(self, name):
        try:
            self.bucket.remove([name.replace('\\', '/')])
        except Exception:
            pass

    def size(self, name):
        folder, _, filename = name.replace('\\', '/').rpartition('/')
        for entry in self.bucket.list(folder or ''):
            if entry['name'] == filename:
                return entry.get('metadata', {}).get('size', 0)
        return 0

    def get_available_name(self, name, max_length=None):
        return super().get_available_name(name, max_length)
