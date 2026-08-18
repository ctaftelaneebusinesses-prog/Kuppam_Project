// OneTownCity — Generic floating dropdown-panel switcher.
//
// Originally drove a theme-palette picker; that feature was removed, but
// the navbar's language switcher (partials/language_options.html) reuses
// the exact same [data-palette-switcher]/[data-palette-trigger] markup and
// panel-toggle behavior (base.html), so this file now only wires up
// open/close for that generic floating-panel pattern.
document.addEventListener('DOMContentLoaded', function () {
    var switchers = document.querySelectorAll('[data-palette-switcher]');
    if (!switchers.length) return;

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
