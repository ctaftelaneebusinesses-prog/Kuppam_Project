// Shared Chart.js color/re-render helper for dashboard charts. Colors are
// read from the live CSS custom properties (tokens.css) instead of being
// hardcoded, so a chart matches whatever theme/palette is active on paint —
// and a MutationObserver re-runs the render callback whenever data-theme or
// data-palette changes, so an already-open dashboard restyles instantly
// instead of only picking up the new colors on next page load.
(function () {
    function cssVar(name, fallback) {
        var value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
        return value || fallback;
    }

    window.hkChartPalette = function () {
        return {
            primary: cssVar('--bs-primary', '#0F4C81'),
            violet: cssVar('--hk-violet-500', '#5B3DE0'),
            sky: cssVar('--hk-sky-400', '#3EC1F3'),
            gold: cssVar('--hk-gold-400', '#F0A93A'),
            emerald: cssVar('--hk-emerald-500', '#0FA36B'),
            red: cssVar('--hk-red-600', '#DC3545'),
            ink: cssVar('--hk-ink', '#14172A'),
            inkSoft: cssVar('--hk-ink-soft', '#7C8199'),
            gridLine: cssVar('--hk-border-soft', 'rgba(0, 0, 0, .08)'),
        };
    };

    // Runs `renderFn` once immediately, then again on every theme/palette
    // change so live charts on an already-open dashboard restyle instantly.
    window.hkRegisterThemedCharts = function (renderFn) {
        renderFn();
        var observer = new MutationObserver(function (mutations) {
            var relevant = mutations.some(function (m) {
                return m.attributeName === 'data-theme' || m.attributeName === 'data-palette';
            });
            if (relevant) renderFn();
        });
        observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'data-palette'] });
    };
})();
