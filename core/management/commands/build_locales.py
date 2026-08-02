"""
Builds the .po/.mo translation catalogs for Telugu/Hindi/Tamil without
needing the GNU gettext toolchain (makemessages/compilemessages normally
shell out to xgettext/msgfmt, which aren't installed on this machine).

Scans every template for {% trans %}/{% blocktrans %} strings with a
regex-based extractor (sufficient for our own templates' conventions — see
templates/base.html for the pattern every template follows), machine-
translates each unique string via core.i18n_utils, and writes compiled
catalogs with polib (pure Python, no system binary required).

Usage: python manage.py build_locales
"""
import re
from pathlib import Path

import polib
from django.conf import settings
from django.core.management.base import BaseCommand

from core.i18n_utils import _translate_via_google

TRANS_RE = re.compile(r'{%\s*trans\s+"((?:[^"\\]|\\.)*)"\s*%}|{%\s*trans\s+\'((?:[^\'\\]|\\.)*)\'\s*%}')
# Plain {% blocktrans %}...{% endblocktrans %} and {% blocktrans with ... %}...{% endblocktrans %}
# blocks — NOT ones using {% plural %} (count-based plurals need msgid/msgid_plural
# pairs per language, out of scope for this regex-based pipeline).
BLOCKTRANS_RE = re.compile(
    r'{%\s*blocktrans(?:\s+with\s+[^%]*)?\s*%}(.*?){%\s*endblocktrans\s*%}',
    re.DOTALL,
)
VAR_RE = re.compile(r'{{\s*(\w+)\s*}}')

LANGUAGES = ['te', 'hi', 'ta', 'kn']


def extract_msgids(text):
    msgids = set()
    for m in TRANS_RE.finditer(text):
        raw = m.group(1) if m.group(1) is not None else m.group(2)
        msgids.add(raw.replace('\\"', '"').replace("\\'", "'"))

    for m in BLOCKTRANS_RE.finditer(text):
        block = m.group(1).strip()
        if '{% plural %}' in block or '{%plural%}' in block:
            continue
        msgids.add(block)
    return msgids


class Command(BaseCommand):
    help = 'Builds locale/<lang>/LC_MESSAGES/django.po + .mo for te/hi/ta from {% trans %}/{% blocktrans %} strings.'

    def handle(self, *args, **options):
        template_dirs = [Path(settings.BASE_DIR) / 'templates']
        all_msgids = set()
        for template_dir in template_dirs:
            for path in template_dir.rglob('*.html'):
                text = path.read_text(encoding='utf-8')
                all_msgids |= extract_msgids(text)

        all_msgids = {m for m in all_msgids if m.strip()}
        self.stdout.write(f'Extracted {len(all_msgids)} unique translatable strings.')

        for lang in LANGUAGES:
            self._build_catalog(lang, sorted(all_msgids))

    def _build_catalog(self, lang, msgids):
        locale_dir = Path(settings.BASE_DIR) / 'locale' / lang / 'LC_MESSAGES'
        locale_dir.mkdir(parents=True, exist_ok=True)
        po_path = locale_dir / 'django.po'
        mo_path = locale_dir / 'django.mo'

        # Reuse any already-translated entries so re-running this command
        # after adding a handful of new strings doesn't re-translate (and
        # re-spend network calls on) everything from scratch.
        existing = {}
        if po_path.exists():
            for entry in polib.pofile(str(po_path)):
                existing[entry.msgid] = entry.msgstr

        po = polib.POFile()
        po.metadata = {
            'Project-Id-Version': 'OneTownCity',
            'MIME-Version': '1.0',
            'Content-Type': 'text/plain; charset=UTF-8',
            'Content-Transfer-Encoding': '8bit',
            'Language': lang,
        }

        translated = 0
        for msgid in msgids:
            msgstr = existing.get(msgid)
            if not msgstr:
                # Placeholders like {{ name }} would otherwise get mangled by
                # the translator — swap them for a stable marker, translate,
                # then restore the original {{ var }} token.
                placeholders = VAR_RE.findall(msgid)
                working = msgid
                for i, var in enumerate(dict.fromkeys(placeholders)):
                    working = working.replace('{{ ' + var + ' }}', f'__VAR{i}__').replace('{{' + var + '}}', f'__VAR{i}__')
                translated_text = _translate_via_google(working, lang)
                for i, var in enumerate(dict.fromkeys(placeholders)):
                    translated_text = translated_text.replace(f'__VAR{i}__', '{{ ' + var + ' }}')
                msgstr = translated_text
                translated += 1
            po.append(polib.POEntry(msgid=msgid, msgstr=msgstr))

        po.save(str(po_path))
        po.save_as_mofile(str(mo_path))
        self.stdout.write(self.style.SUCCESS(
            f'{lang}: {len(msgids)} strings ({translated} newly translated) -> {mo_path}'
        ))
