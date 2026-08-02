// OneTownCity — Celebration effects module.
//
// window.HKCelebrations.{confetti,burst,glow} spawn short-lived particles
// into one dedicated #fxLayer div. Each particle gets randomized inline
// CSS custom properties (--fx-x, --fx-y, animation-delay/-duration) so a
// single shared @keyframes rule (hk-fx-particle, see main.css) produces
// varied per-particle motion — no per-effect keyframe authoring needed.
//
// Any page can trigger these on a real milestone. This project wires them
// to a few concrete ones via a lightweight convention rather than a
// server-aware JS API: a Django message tagged `celebrate-confetti` or
// `celebrate-burst` (see messages.success(..., extra_tags=...) call sites
// in core/views.py) renders with a matching data-hk-celebrate attribute
// (base.html), and the DOMContentLoaded scan below fires the right effect
// for whatever's present on the page — e.g. a listing submitted, a review
// posted, an admin request sent.
(function () {
    var layer = null;

    function isMobile() {
        return window.matchMedia('(max-width: 768px)').matches;
    }

    function rand(min, max) {
        return min + Math.random() * (max - min);
    }

    function pick(arr) {
        return arr[Math.floor(Math.random() * arr.length)];
    }

    var CONFETTI_COLORS = ['#F97316', '#F59E0B', '#0FA36B', '#5B3DE0', '#3EC1F3', '#E0426B'];

    function ensureLayer() {
        if (layer) return layer;
        layer = document.createElement('div');
        layer.id = 'fxLayer';
        layer.setAttribute('aria-hidden', 'true');
        document.body.appendChild(layer);
        return layer;
    }

    function spawnParticle(className, innerHTML, style) {
        var el = document.createElement('span');
        el.className = 'fx-particle ' + className;
        el.style.left = (style.originX != null ? style.originX : 50) + 'vw';
        el.style.top = (style.originY != null ? style.originY : 40) + 'vh';
        el.style.setProperty('--fx-x', style.x + 'px');
        el.style.setProperty('--fx-y', style.y + 'px');
        el.style.animationDelay = (style.delay || 0) + 'ms';
        el.style.animationDuration = (style.duration || 1200) + 'ms';
        if (style.color) el.style.color = style.color;
        if (innerHTML) el.innerHTML = innerHTML;
        ensureLayer().appendChild(el);

        var life = (style.delay || 0) + (style.duration || 1200) + 150;
        setTimeout(function () { el.remove(); }, life);
        return el;
    }

    // General-purpose celebratory shower — order placed, listing approved,
    // application submitted.
    function confetti(opts) {
        opts = opts || {};
        var count = opts.count || (isMobile() ? 16 : 30);
        for (var i = 0; i < count; i++) {
            spawnParticle('fx-confetti', '', {
                originX: opts.x != null ? opts.x : rand(30, 70),
                originY: opts.y != null ? opts.y : 18,
                x: rand(-140, 140),
                y: rand(160, 340),
                delay: rand(0, 300),
                duration: rand(1100, 1900),
                color: opts.color || pick(CONFETTI_COLORS),
            });
        }
    }

    // A tight radial pop from one point — appointment booked, review
    // posted, a single confirming "burst" rather than a screen-wide shower.
    function burst(opts) {
        opts = opts || {};
        var count = opts.count || 12;
        var originX = opts.x != null ? opts.x : 50;
        var originY = opts.y != null ? opts.y : 50;
        for (var i = 0; i < count; i++) {
            var angle = (i / count) * Math.PI * 2 + rand(-.2, .2);
            var dist = rand(50, 130);
            spawnParticle('fx-burst', opts.glyph || '', {
                originX: originX,
                originY: originY,
                x: Math.cos(angle) * dist,
                y: Math.sin(angle) * dist,
                delay: rand(0, 60),
                duration: rand(500, 850),
                color: opts.color || '#0FA36B',
            });
        }
    }

    // A one-shot ring pulse on a specific element — e.g. a "Saved" heart
    // icon, a submit button, a status pill flipping to "Approved".
    function glow(el) {
        if (!el) return;
        el.classList.remove('fx-glow');
        void el.offsetWidth; // restart the animation if it's already mid-run
        el.classList.add('fx-glow');
        setTimeout(function () { el.classList.remove('fx-glow'); }, 1300);
    }

    window.HKCelebrations = { confetti: confetti, burst: burst, glow: glow };

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-hk-celebrate]').forEach(function (el) {
            var effect = el.getAttribute('data-hk-celebrate');
            if (effect === 'confetti') confetti();
            else if (effect === 'burst') burst();
        });
    });
})();
