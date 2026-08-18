// OneTownCity — World-scene background storyline system.
//
// Supersedes category-scene.js and dynamic-bg.js from earlier in this
// feature's history: a persistent .world-bg container hosts a per-category
// cast of hand-authored inline-SVG actors, each with its own percentage-
// timed CSS @keyframes loop (see main.css) sharing one --world-duration
// custom property so their individual timelines land in sync and hand off
// to each other — one continuous, seamlessly-looping narrative per
// category, not just floating icons.
//
// Architecture:
//   - .world-bg (fixed, z-index:-1) > .world-ambient (day/night wash) +
//     .world-actors (cleared/rebuilt per scene).
//   - changeWorldScene(categoryKey) is the switcher, exposed globally.
//   - This is a traditional multi-page Django site, so changeWorldScene()
//     is called once on load from <body data-category-bg>, the practical
//     equivalent of a live "category click" trigger.
//
// All actors are genuinely inline <svg> (no icon font, no external
// animation library) built from simple flat shapes — circles, rects, paths
// — composed by hand. 3 categories (Restaurant, Healthcare, Transport) get
// a full multi-actor narrative cast; the other 6 share a lighter 2-motion
// vocabulary (wa-generic-approach / wa-generic-hover) with category-
// specific SVG props, to keep 9 scenes' worth of hand-tuned choreography
// realistically scoped in one pass.
(function () {
    // ------------------------------------------------------------------
    // Small hand-authored SVG shape library — simple flat silhouettes,
    // not detailed illustration (a real constraint of this environment,
    // explained in chat) but genuinely bespoke vector shapes, not an icon
    // font glyph.
    // ------------------------------------------------------------------
    var SVG = {
        person: function (accessory) {
            return '<svg viewBox="0 0 40 80">' +
                '<circle cx="20" cy="12" r="10" fill="currentColor"/>' +
                '<rect x="10" y="24" width="20" height="34" rx="6" fill="currentColor"/>' +
                '<rect x="9" y="56" width="8" height="22" rx="3" fill="currentColor"/>' +
                '<rect x="23" y="56" width="8" height="22" rx="3" fill="currentColor"/>' +
                (accessory || '') +
                '</svg>';
        },
        chefHat: '<rect x="11" y="-6" width="18" height="10" rx="5" fill="currentColor" opacity=".6"/>',
        doctorBadge: '<circle cx="20" cy="30" r="3.5" fill="currentColor" opacity=".5"/>',
        plate: '<svg viewBox="0 0 40 20"><ellipse cx="20" cy="12" rx="18" ry="6" fill="currentColor" opacity=".85"/><circle cx="20" cy="9" r="7" fill="currentColor"/></svg>',
        steam: '<svg viewBox="0 0 20 40"><path d="M5 40 C0 30,10 25,5 15 C0 8,8 4,5 0" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round"/><path d="M14 40 C9 30,19 25,14 15 C9 8,17 4,14 0" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round"/></svg>',
        receipt: '<svg viewBox="0 0 30 40"><rect x="2" y="2" width="26" height="36" rx="3" fill="currentColor" opacity=".18" stroke="currentColor" stroke-width="2"/><line x1="7" y1="12" x2="23" y2="12" stroke="currentColor" stroke-width="2"/><line x1="7" y1="20" x2="23" y2="20" stroke="currentColor" stroke-width="2"/><line x1="7" y1="28" x2="17" y2="28" stroke="currentColor" stroke-width="2"/></svg>',
        clipboard: '<svg viewBox="0 0 30 36"><rect x="10" y="0" width="10" height="7" rx="2" fill="currentColor"/><rect x="3" y="4" width="24" height="30" rx="3" fill="currentColor" opacity=".18" stroke="currentColor" stroke-width="2"/><line x1="8" y1="15" x2="22" y2="15" stroke="currentColor" stroke-width="2"/><line x1="8" y1="21" x2="22" y2="21" stroke="currentColor" stroke-width="2"/><line x1="8" y1="27" x2="16" y2="27" stroke="currentColor" stroke-width="2"/></svg>',
        vehicle: function (extra) {
            return '<svg viewBox="0 0 110 55">' +
                '<rect x="2" y="10" width="106" height="32" rx="8" fill="currentColor"/>' +
                '<rect x="12" y="16" width="20" height="16" rx="2" fill="#fff" opacity=".75"/>' +
                '<rect x="38" y="16" width="20" height="16" rx="2" fill="#fff" opacity=".75"/>' +
                (extra || '') +
                '<circle cx="24" cy="46" r="8" fill="currentColor"/>' +
                '<circle cx="82" cy="46" r="8" fill="currentColor"/>' +
                '</svg>';
        },
        ambulanceCross: '<rect x="66" y="18" width="6" height="18" fill="#fff"/><rect x="60" y="24" width="18" height="6" fill="#fff"/>',
        heartbeat: '<svg viewBox="0 0 200 40"><path d="M0 20 L40 20 L50 5 L60 35 L70 20 L110 20 L120 5 L130 35 L140 20 L200 20" stroke="currentColor" stroke-width="3" fill="none" stroke-linecap="round" stroke-dasharray="220" class="wa-heartbeat-path"/></svg>',
        ticket: '<svg viewBox="0 0 36 22"><rect x="1" y="1" width="34" height="20" rx="3" fill="currentColor" opacity=".9"/><line x1="24" y1="1" x2="24" y2="21" stroke="#fff" stroke-width="1.5" stroke-dasharray="3 3"/></svg>',
        luggage: '<svg viewBox="0 0 30 26"><rect x="2" y="8" width="26" height="16" rx="3" fill="currentColor"/><rect x="10" y="2" width="10" height="8" rx="2" fill="none" stroke="currentColor" stroke-width="2"/></svg>',
        bag: '<svg viewBox="0 0 32 30"><path d="M8 10 a8 8 0 0 1 16 0" stroke="currentColor" stroke-width="2.5" fill="none"/><rect x="4" y="10" width="24" height="18" rx="3" fill="currentColor"/></svg>',
        tag: '<svg viewBox="0 0 30 30"><path d="M2 2 L16 2 L28 14 L16 26 L2 12 Z" fill="currentColor"/><circle cx="9" cy="9" r="3" fill="#fff"/></svg>',
        chart: '<svg viewBox="0 0 40 30"><rect x="2" y="14" width="8" height="16" fill="currentColor"/><rect x="16" y="6" width="8" height="24" fill="currentColor"/><rect x="30" y="0" width="8" height="30" fill="currentColor"/></svg>',
        book: '<svg viewBox="0 0 40 26"><path d="M20 4 C14 0 4 0 2 3 L2 23 C4 20 14 20 20 24 C26 20 36 20 38 23 L38 3 C36 0 26 0 20 4 Z" fill="currentColor" opacity=".9"/></svg>',
        cap: '<svg viewBox="0 0 40 24"><path d="M20 0 L40 8 L20 16 L0 8 Z" fill="currentColor"/><rect x="8" y="10" width="4" height="10" fill="currentColor"/></svg>',
        house: '<svg viewBox="0 0 40 34"><path d="M20 2 L38 18 L33 18 L33 32 L7 32 L7 18 L2 18 Z" fill="currentColor"/></svg>',
        key: '<svg viewBox="0 0 36 18"><circle cx="8" cy="9" r="7" fill="none" stroke="currentColor" stroke-width="3"/><rect x="14" y="7" width="20" height="4" fill="currentColor"/><rect x="26" y="11" width="4" height="6" fill="currentColor"/><rect x="32" y="11" width="4" height="6" fill="currentColor"/></svg>',
        cone: '<svg viewBox="0 0 26 30"><path d="M13 0 L22 26 L4 26 Z" fill="currentColor"/><rect x="0" y="26" width="26" height="4" rx="1" fill="currentColor"/></svg>',
        gift: '<svg viewBox="0 0 34 30"><rect x="2" y="12" width="30" height="18" rx="2" fill="currentColor"/><rect x="2" y="6" width="30" height="8" rx="2" fill="currentColor" opacity=".85"/><rect x="15" y="0" width="4" height="30" fill="#fff" opacity=".7"/></svg>',
        sparkle: '<svg viewBox="0 0 30 30"><path d="M15 0 L18 12 L30 15 L18 18 L15 30 L12 18 L0 15 L12 12 Z" fill="currentColor"/></svg>',
        newspaper: '<svg viewBox="0 0 34 26"><rect x="1" y="1" width="32" height="24" rx="2" fill="currentColor" opacity=".18" stroke="currentColor" stroke-width="2"/><rect x="6" y="6" width="10" height="8" fill="currentColor"/><line x1="19" y1="7" x2="28" y2="7" stroke="currentColor" stroke-width="2"/><line x1="19" y1="12" x2="28" y2="12" stroke="currentColor" stroke-width="2"/><line x1="6" y1="18" x2="28" y2="18" stroke="currentColor" stroke-width="2"/></svg>',
        mic: '<svg viewBox="0 0 20 34"><rect x="4" y="0" width="12" height="20" rx="6" fill="currentColor"/><path d="M2 16 a8 8 0 0 0 16 0" stroke="currentColor" stroke-width="2.5" fill="none"/><line x1="10" y1="24" x2="10" y2="32" stroke="currentColor" stroke-width="2.5"/></svg>',
    };

    function isMobile() {
        return window.matchMedia('(max-width: 768px)').matches;
    }

    function addActor(layer, opts) {
        var el = document.createElement('div');
        el.className = 'world-actor';
        el.style.left = opts.left;
        if (opts.bottom != null) el.style.bottom = opts.bottom;
        if (opts.top != null) el.style.top = opts.top;
        var scale = isMobile() ? (opts.mobileScale || .72) : 1;
        el.style.width = (opts.w * scale) + 'px';
        el.style.height = (opts.h * scale) + 'px';
        el.style.color = opts.color;
        el.style.animation = opts.anim + ' ' + (opts.duration || 'var(--world-duration)') + ' ' + (opts.timing || 'linear') + ' infinite';
        if (opts.delay) el.style.animationDelay = opts.delay;
        if (opts.vars) {
            Object.keys(opts.vars).forEach(function (k) { el.style.setProperty(k, opts.vars[k]); });
        }
        el.innerHTML = opts.svg;
        layer.appendChild(el);
        return el;
    }

    // ------------------------------------------------------------------
    // Scene configs — 3 flagship narrative casts + 6 lighter ones.
    // ------------------------------------------------------------------
    var SCENES = {
        // Restaurant: chef preps -> waiter carries the plate across the
        // screen -> customer at the table -> plate cleared -> loop restarts.
        restaurant: {
            duration: '14s',
            build: function (layer, color) {
                addActor(layer, { left: '16vw', bottom: '14%', w: 34, h: 68, anim: 'wa-r-chef', svg: SVG.person(SVG.chefHat), color: color });
                addActor(layer, { left: '20vw', bottom: '10%', w: 34, h: 68, anim: 'wa-r-waiter', svg: SVG.person(), color: color });
                addActor(layer, { left: '72vw', bottom: '14%', w: 40, h: 20, anim: 'wa-r-plate', svg: SVG.plate, color: color });
                addActor(layer, { left: '73vw', bottom: '20%', w: 20, h: 40, anim: 'wa-r-steam', svg: SVG.steam, color: color });
                addActor(layer, { left: '76vw', bottom: '11%', w: 34, h: 68, anim: 'wa-r-customer', svg: SVG.person(), color: color });
                addActor(layer, { left: '80vw', bottom: '15%', w: 24, h: 32, anim: 'wa-r-receipt', svg: SVG.receipt, color: color });
            },
        },
        // Healthcare: patient arrives -> doctor + clipboard checkup ->
        // patient recovers and leaves -> ambulance cameo -> loop restarts.
        health: {
            duration: '16s',
            build: function (layer, color) {
                addActor(layer, { left: '25vw', bottom: '10%', w: 34, h: 68, anim: 'wa-h-patient', svg: SVG.person(), color: color });
                addActor(layer, { left: '28vw', bottom: '10%', w: 34, h: 68, anim: 'wa-h-doctor', svg: SVG.person(SVG.doctorBadge), color: color });
                addActor(layer, { left: '32vw', bottom: '17%', w: 26, h: 30, anim: 'wa-h-clipboard', svg: SVG.clipboard, color: color });
                addActor(layer, { left: '85vw', bottom: '6%', w: 100, h: 50, anim: 'wa-h-ambulance', svg: SVG.vehicle(SVG.ambulanceCross), color: color });
                addActor(layer, { left: '6vw', top: '12%', w: 160, h: 32, anim: 'wa-h-heartbeat', duration: '3s', svg: SVG.heartbeat, color: color });
            },
        },
        // Transport: commuter walks to the stop -> bus arrives -> boards ->
        // bus crosses the screen -> loop restarts.
        transport: {
            duration: '14s',
            build: function (layer, color) {
                addActor(layer, { left: '50vw', bottom: '10%', w: 34, h: 68, anim: 'wa-t-commuter', svg: SVG.person(), color: color });
                addActor(layer, { left: '54vw', bottom: '17%', w: 30, h: 18, anim: 'wa-t-ticket', svg: SVG.ticket, color: color });
                addActor(layer, { left: '46vw', bottom: '8%', w: 26, h: 22, anim: 'wa-t-luggage', svg: SVG.luggage, color: color });
                addActor(layer, { left: '50vw', bottom: '4%', w: 96, h: 48, anim: 'wa-t-bus', svg: SVG.vehicle(), color: color });
            },
        },
    };

    // The remaining 6 categories share the lighter wa-generic-approach /
    // wa-generic-hover vocabulary — one walker plus 2-3 floating props.
    var LIGHT_SCENES = {
        business: { duration: '15s', props: [SVG.bag, SVG.tag, SVG.chart] },
        education: { duration: '15s', props: [SVG.book, SVG.cap] },
        realestate: { duration: '15s', props: [SVG.house, SVG.key] },
        projects: { duration: '15s', props: [SVG.cone, SVG.chart] },
        events: { duration: '15s', props: [SVG.gift, SVG.sparkle] },
        news: { duration: '15s', props: [SVG.newspaper, SVG.mic] },
    };
    Object.keys(LIGHT_SCENES).forEach(function (key) {
        var cfg = LIGHT_SCENES[key];
        SCENES[key] = {
            duration: cfg.duration,
            build: function (layer, color) {
                addActor(layer, {
                    left: '38vw', bottom: '10%', w: 30, h: 60, anim: 'wa-generic-approach', svg: SVG.person(), color: color,
                    vars: { '--actor-start': '-14vw', '--actor-exit': '30vw' },
                });
                var positions = [{ left: '65vw', bottom: '20%' }, { left: '80vw', bottom: '32%' }, { left: '15vw', bottom: '30%' }];
                cfg.props.forEach(function (svg, i) {
                    var pos = positions[i % positions.length];
                    addActor(layer, {
                        left: pos.left, bottom: pos.bottom, w: 34, h: 34, anim: 'wa-generic-hover',
                        delay: '-' + (i * 2.5) + 's', svg: svg, color: color,
                    });
                });
            },
        };
    });

    // ------------------------------------------------------------------
    // Root construction + the state-management switcher.
    // ------------------------------------------------------------------
    var root = null;
    var ambient = null;
    var actorLayer = null;
    var activeCategory = null;

    function ensureRoot() {
        if (root) return root;
        root = document.createElement('div');
        root.className = 'world-bg';
        root.setAttribute('aria-hidden', 'true');

        ambient = document.createElement('div');
        ambient.className = 'world-ambient';
        root.appendChild(ambient);

        actorLayer = document.createElement('div');
        actorLayer.className = 'world-actors';
        root.appendChild(actorLayer);

        document.body.appendChild(root);
        return root;
    }

    function changeWorldScene(categoryKey) {
        ensureRoot();

        var config = SCENES[categoryKey] || null;
        var nextKey = config ? categoryKey : null;

        if (activeCategory === nextKey) return;

        root.classList.remove('is-active');
        actorLayer.innerHTML = '';
        activeCategory = nextKey;
        if (!config) return;

        root.style.setProperty('--world-duration', config.duration);
        var color = 'rgba(var(--hk-cat-' + categoryKey + '-rgb), .34)';
        config.build(actorLayer, color);
        root.classList.add('is-active');
    }

    window.changeWorldScene = changeWorldScene;

    var initialCategory = document.body.getAttribute('data-category-bg');
    if (initialCategory) {
        changeWorldScene(initialCategory);
    }
})();
