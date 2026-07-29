from django.core.management.base import BaseCommand

from core.classification import classify_education_bucket, classify_hospital_bucket
from core.models import Business

# Legacy/ambiguous buckets that pre-date the Schools vs Colleges & Universities
# split, or that may contain rows uploaded through the wrong template.
BUCKETS = {
    'education': classify_education_bucket,
    'hospital': classify_hospital_bucket,
}


class Command(BaseCommand):
    help = (
        "Re-runs category classification for Business rows stuck in legacy/ambiguous "
        "buckets ('education', 'hospital') so each listing lands on its correct "
        "category (school, college, hospital, pharmacy). Prints a report; pass "
        "--apply to actually save the changes."
    )

    def add_arguments(self, parser):
        parser.add_argument(
            '--apply', action='store_true',
            help='Save the reclassified categories. Without this flag, only a preview is printed.'
        )

    def handle(self, *args, **options):
        apply_changes = options['apply']
        total_changed = 0

        for bucket, classify in BUCKETS.items():
            businesses = Business.objects.filter(category=bucket).order_by('name')
            if not businesses.exists():
                continue

            self.stdout.write(self.style.MIGRATE_HEADING(f"\n=== {bucket} ({businesses.count()} records) ==="))
            for business in businesses:
                new_category = classify(business.name)
                if new_category == business.category:
                    continue
                total_changed += 1
                line = f'{business.pk:>5}  {business.category:10} -> {new_category:10}  {business.name}'
                if apply_changes:
                    business.category = new_category
                    business.save(update_fields=['category'])
                    self.stdout.write(self.style.SUCCESS(line))
                else:
                    self.stdout.write(line)

        if not total_changed:
            self.stdout.write(self.style.SUCCESS('\nNothing to reclassify.'))
        elif apply_changes:
            self.stdout.write(self.style.SUCCESS(f'\n{total_changed} record(s) reclassified.'))
        else:
            self.stdout.write(self.style.WARNING(f'\n{total_changed} record(s) would change. Re-run with --apply to save.'))
