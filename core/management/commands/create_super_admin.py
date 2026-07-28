from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError

from core.models import Profile, UserRole


class Command(BaseCommand):
    help = (
        'Promotes an existing user to Super Admin. The user must sign in '
        'with Google at /signin/ at least once before running this command, '
        'since there is no self-service way to become Super Admin.'
    )

    def add_arguments(self, parser):
        parser.add_argument('email', type=str, help='Email address of the account to promote')

    def handle(self, *args, **options):
        email = options['email']
        User = get_user_model()
        try:
            user = User.objects.get(email__iexact=email)
        except User.DoesNotExist:
            raise CommandError(
                f'No user found with email "{email}". Sign in with Google at /signin/ first, then re-run this command.'
            )

        profile, _ = Profile.objects.get_or_create(user=user)
        profile.role = UserRole.SUPER_ADMIN
        profile.is_blocked = False
        profile.is_suspended = False
        profile.save(update_fields=['role', 'is_blocked', 'is_suspended'])

        user.is_staff = True
        user.is_superuser = True
        user.save(update_fields=['is_staff', 'is_superuser'])

        self.stdout.write(self.style.SUCCESS(f'{email} is now Super Admin.'))
