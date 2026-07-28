from pathlib import Path

from django.conf import settings
from django.core.management.base import BaseCommand, CommandError
from django.core.files.base import ContentFile

from core.storage import SupabaseMediaStorage


class Command(BaseCommand):
    help = (
        'One-off safety net for the FileSystemStorage -> Supabase Storage cutover. '
        'Walks MEDIA_ROOT and uploads every file to the Supabase bucket under the same '
        'relative path, so ImageField values saved before the cutover keep resolving. '
        'Skips files that already exist in the bucket. Safe to re-run.'
    )

    def handle(self, *args, **options):
        media_root = Path(settings.MEDIA_ROOT)
        if not media_root.exists():
            self.stdout.write('No local media/ directory found — nothing to migrate.')
            return

        if not settings.SUPABASE_URL or not settings.SUPABASE_SERVICE_ROLE_KEY:
            raise CommandError('SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY must be set in .env first.')

        storage = SupabaseMediaStorage()
        uploaded, skipped = 0, 0

        for path in media_root.rglob('*'):
            if not path.is_file():
                continue
            relative_name = path.relative_to(media_root).as_posix()
            if storage.exists(relative_name):
                skipped += 1
                continue
            with open(path, 'rb') as fh:
                storage.save(relative_name, ContentFile(fh.read(), name=relative_name))
            uploaded += 1
            self.stdout.write(f'Uploaded {relative_name}')

        self.stdout.write(self.style.SUCCESS(f'Done. Uploaded {uploaded}, skipped {skipped} (already present).'))
