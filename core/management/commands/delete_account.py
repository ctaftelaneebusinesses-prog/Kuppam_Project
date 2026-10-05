from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError

from core.account_deletion import AccountDeletionError, delete_user_account


class Command(BaseCommand):
    help = (
        'Permanently deletes a user account and its personal data, for a deletion '
        'request emailed to us (see the Privacy Policy). Users can only deactivate '
        'their own account themselves. Cannot be undone.'
    )

    def add_arguments(self, parser):
        parser.add_argument('email', type=str, help='Email address of the account to delete')
        parser.add_argument('--yes', action='store_true', help='Confirm the permanent deletion')

    def handle(self, *args, **options):
        email = options['email']
        users = list(get_user_model().objects.filter(email__iexact=email))
        if not users:
            raise CommandError(f'No user found with email "{email}".')
        if len(users) > 1:
            raise CommandError(
                f'{len(users)} accounts use "{email}" ({", ".join(u.get_username() for u in users)}); '
                'resolve which one was requested before deleting.'
            )
        if not options['yes']:
            raise CommandError(f'This permanently deletes {users[0].get_username()} ({email}). Re-run with --yes to confirm.')

        try:
            delete_user_account(users[0])
        except AccountDeletionError as exc:
            raise CommandError(str(exc))
        self.stdout.write(self.style.SUCCESS(f'Deleted the account for {email}.'))
