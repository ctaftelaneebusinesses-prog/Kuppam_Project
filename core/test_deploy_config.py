"""
Deploy config guard. Railway ignores the Heroku-style `release:` line in the Procfile, so migrations only run
automatically if railway.json declares a pre-deploy command. A deploy without it once shipped code that read columns
the production database did not have yet (every signed-in request returned 500).
"""
import json
from pathlib import Path

from django.conf import settings
from django.test import SimpleTestCase


class DeployConfigTests(SimpleTestCase):
    def test_railway_runs_migrations_before_the_new_version_starts(self):
        config = json.loads((Path(settings.BASE_DIR) / 'railway.json').read_text())
        commands = config['deploy']['preDeployCommand']
        self.assertIn('python manage.py migrate --noinput', commands)

    def test_procfile_still_declares_the_web_process(self):
        procfile = (Path(settings.BASE_DIR) / 'Procfile').read_text()
        self.assertTrue(any(line.startswith('web:') for line in procfile.splitlines()))
