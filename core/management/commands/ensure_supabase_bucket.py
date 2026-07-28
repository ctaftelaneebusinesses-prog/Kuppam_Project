from django.conf import settings
from django.core.management.base import BaseCommand, CommandError
from supabase import create_client


class Command(BaseCommand):
    help = (
        'Creates the public Supabase Storage bucket used by core.storage.SupabaseMediaStorage '
        '(SUPABASE_STORAGE_BUCKET, default "media") if it does not already exist. Safe to re-run.'
    )

    def handle(self, *args, **options):
        if not settings.SUPABASE_URL or not settings.SUPABASE_SERVICE_ROLE_KEY:
            raise CommandError('SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY must be set in .env first.')

        bucket_name = settings.SUPABASE_STORAGE_BUCKET
        client = create_client(settings.SUPABASE_URL, settings.SUPABASE_SERVICE_ROLE_KEY)

        existing = {bucket.id for bucket in client.storage.list_buckets()}
        if bucket_name in existing:
            self.stdout.write(self.style.SUCCESS(f'Bucket "{bucket_name}" already exists.'))
            return

        client.storage.create_bucket(bucket_name, options={'public': True})
        self.stdout.write(self.style.SUCCESS(f'Created public bucket "{bucket_name}".'))
