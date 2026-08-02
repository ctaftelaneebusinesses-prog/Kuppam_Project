// OneTownCity — Luxury palette switcher.
//
// Second theming axis on top of the existing light/dark toggle (main.js):
// data-palette="summer-silk|rainy-velvet|nordic-aurora" on <html>, persisted
// in localStorage under 'hkPalette' and applied pre-paint by the inline
// anti-flash script in base.html (same pattern as the theme toggle already
// uses, so a page never flashes the default palette before swapping).
//
// Selecting a seasonal palette also forces data-theme to the mode that
// palette's colors are built for (light for Summer Silk, dark for the other
// two) so any other code branching on data-theme stays correct — but every
// seasonal palette's own tokens.css block is itself independent of
// data-theme, so the user's plain sun/moon toggle stays harmlessly usable
// afterward without breaking the palette's look.
document.addEventListener('DOMContentLoaded', function () {
    var root = document.documentElement;
    var switchers = document.querySelectorAll('[data-palette-switcher]');
    // Scoped to [data-palette] specifically (not just .hk-palette-option) —
    // the language switcher (language-switcher.js) reuses the same option
    // button styling for its own <button name="language" value="..."> rows,
    // which must NOT be picked up here.
    var allOptions = document.querySelectorAll('.hk-palette-option[data-palette]');
    if (!allOptions.length) return;

    // Site-wide enforcement (Super Admin's Site Theme panel) already picked
    // the effective palette pre-paint (see base.html's anti-flash script) —
    // window.HK_SITE_ENFORCE_PALETTE there is already false for the Super
    // Admin's own session, so this only locks out everyone else's switcher.
    // The palette/canvas itself still needs applying below either way —
    // enforcement only means "don't let them change it", not "don't run it".
    var enforced = !!window.HK_SITE_ENFORCE_PALETTE;
    if (enforced) {
        switchers.forEach(function (sw) { sw.style.display = 'none'; });
    }

    var PALETTE_THEME = {
        'default': null,
        'summer-silk': 'light',
        'rainy-velvet': 'dark',
        'nordic-aurora': 'dark',
        // Winter/Spring/Autumn are weather effects, not color palettes —
        // they never force data-theme, so the visitor's own light/dark
        // choice (plain toggle) stays exactly as it was underneath them.
        'winter': null,
        'spring': null,
        'autumn': null,
        'rain': null,
    };
    var PALETTE_CANVAS_MODE = {
        'default': 'none',
        'summer-silk': 'summer',
        'rainy-velvet': 'rain',
        'nordic-aurora': 'aurora',
        'winter': 'winter',
        'spring': 'spring',
        'autumn': 'autumn',
        'rain': 'rain',
    };

    // Dashboard sidebar has its own compact seasonal-effect picker
    // (dashboard/base_dashboard.html, [data-weather-season-option]) — same
    // underlying state as the navbar switcher above, just a second surface
    // for it, so both always read this one applyPalette()/paintActive() pair
    // rather than running independent logic. "none" there maps to "default"
    // here since the navbar has no button literally named "none".
    var seasonOptions = document.querySelectorAll('[data-weather-season-option]');

    function paintActive(palette) {
        allOptions.forEach(function (opt) {
            var active = opt.dataset.palette === palette;
            opt.classList.toggle('is-active', active);
            opt.setAttribute('aria-pressed', String(active));
        });
        seasonOptions.forEach(function (opt) {
            var value = opt.dataset.weatherSeasonOption === 'none' ? 'default' : opt.dataset.weatherSeasonOption;
            var active = value === palette;
            opt.classList.toggle('is-active', active);
            opt.setAttribute('aria-pressed', String(active));
        });
    }

    function applyPalette(palette, persist) {
        if (palette === 'default') {
            root.removeAttribute('data-palette');
        } else {
            root.setAttribute('data-palette', palette);
        }

        var forcedTheme = PALETTE_THEME[palette];
        if (forcedTheme) {
            root.setAttribute('data-theme', forcedTheme);
            var themeIcons = document.querySelectorAll('[id^="hkThemeIcon"]');
            themeIcons.forEach(function (icon) {
                icon.className = forcedTheme === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
            });
        }

        if (persist) {
            try { localStorage.setItem('hkPalette', palette); } catch (e) {}
            if (forcedTheme) {
                try { localStorage.setItem('hkTheme', forcedTheme); } catch (e) {}
            }
        }

        paintActive(palette);
        if (window.HKSeasonalCanvas) {
            window.HKSeasonalCanvas.setMode(PALETTE_CANVAS_MODE[palette] || 'none');
        }
        // The world-scene background storyline (world-scene.js) is
        // suppressed while any seasonal palette is active — re-check live
        // so switching palettes without a page reload hides/reveals it too.
        if (window.HKWorldScene) {
            window.HKWorldScene.refresh();
        }
    }

    // Initial paint: the anti-flash script already set data-palette on
    // <html> pre-paint, so just read it back and sync the UI + canvas.
    var current = root.getAttribute('data-palette') || 'default';
    applyPalette(current, false);

    // Everything past this point only wires up the ability to *change* the
    // palette — skipped under enforcement, since the switcher is hidden and
    // there's nothing for these handlers to do.
    if (enforced) return;

    allOptions.forEach(function (opt) {
        opt.addEventListener('click', function () {
            applyPalette(opt.dataset.palette, true);
            switchers.forEach(function (sw) { sw.classList.remove('is-open'); });
        });
    });

    seasonOptions.forEach(function (opt) {
        opt.addEventListener('click', function () {
            var value = opt.dataset.weatherSeasonOption === 'none' ? 'default' : opt.dataset.weatherSeasonOption;
            applyPalette(value, true);
        });
    });

    // Floating panel open/close (desktop trigger only — the mobile drawer
    // row has no panel to toggle, it's always visible inline).
    switchers.forEach(function (switcher) {
        var trigger = switcher.querySelector('[data-palette-trigger]');
        if (!trigger) return;
        trigger.addEventListener('click', function (e) {
            e.stopPropagation();
            var willOpen = !switcher.classList.contains('is-open');
            switchers.forEach(function (sw) { sw.classList.remove('is-open'); });
            switcher.classList.toggle('is-open', willOpen);
        });
    });

    document.addEventListener('click', function () {
        switchers.forEach(function (sw) { sw.classList.remove('is-open'); });
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            switchers.forEach(function (sw) { sw.classList.remove('is-open'); });
        }
    });
});
