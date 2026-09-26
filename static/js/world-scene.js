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
        el.style.animation = opts.anim + ' ' + (opts.duration || 'var(--world-duration)') + ' ' + (opts.timing || 'linear') + ' infinite';
        if (opts.delay) el.style.animationDelay = opts.delay;
        if (opts.vars) {
            Object.keys(opts.vars).forEach(function (k) { el.style.setProperty(k, opts.vars[k]); });
        }
        if (opts.img) {
            // Full-color raster artwork (e.g. education's scattered
            // illustration pieces) — no currentColor tint, unlike the
            // hand-drawn SVG actors below.
            var img = document.createElement('img');
            img.src = opts.img;
            img.alt = '';
            img.style.width = '100%';
            img.style.height = '100%';
            img.style.objectFit = 'contain';
            el.appendChild(img);
        } else {
            el.style.color = opts.color;
            el.innerHTML = opts.svg;
        }
        layer.appendChild(el);
        return el;
    }

    // ------------------------------------------------------------------
    // Scene configs — 3 flagship narrative casts + 6 lighter ones.
    // ------------------------------------------------------------------
    var SCENES = {
        // Restaurant: chef preps -> waiter carries the plate across the
        // screen -> customer at the table -> plate cleared -> loop restarts.
        // Restaurants: same treatment as Education/Healthcare — 4 full-color
        // pieces cut from static/images/restaurents.png (storefront, plated
        // dish, phone + takeaway bag, map + cloche).
        restaurant: {
            duration: '15s',
            build: function (layer) {
                addActor(layer, { left: 'calc(100vw - 380px)', top: '5%', w: 340, h: 182, anim: 'wa-generic-hover', delay: '-1s', img: '/static/images/restaurant-scene/storefront.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', top: '9%', w: 240, h: 195, anim: 'wa-generic-hover', delay: '-5s', img: '/static/images/restaurant-scene/dish-table.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', bottom: '5%', w: 270, h: 175, anim: 'wa-generic-hover', delay: '-3s', img: '/static/images/restaurant-scene/phone-takeaway.webp', mobileScale: .4 });
                addActor(layer, { left: 'calc(100vw - 290px)', bottom: '4%', w: 260, h: 171, anim: 'wa-generic-hover', delay: '-7s', img: '/static/images/restaurant-scene/map-cloche.webp', mobileScale: .4 });
            },
        },
        // Healthcare: no walking patient/doctor silhouettes — 4 full-color
        // pieces cropped from static/images/Hospital_img.png, large and
        // balanced 2-left/2-right in the page's outer margins (doctor+
        // hospital top-right, heart+stethoscope top-left, clipboard+
        // medicines bottom-left, ambulance+shield bottom-right), same
        // treatment as Education's scene.
        health: {
            duration: '15s',
            build: function (layer) {
                addActor(layer, { left: 'calc(100vw - 360px)', top: '5%', w: 320, h: 178, anim: 'wa-generic-hover', delay: '-1s', img: '/static/images/hospital-scene/doctor-hospital.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', top: '8%', w: 230, h: 169, anim: 'wa-generic-hover', delay: '-5s', img: '/static/images/hospital-scene/heart-stethoscope.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', bottom: '6%', w: 280, h: 114, anim: 'wa-generic-hover', delay: '-3s', img: '/static/images/hospital-scene/clipboard-meds.webp', mobileScale: .4 });
                addActor(layer, { left: 'calc(100vw - 300px)', bottom: '4%', w: 260, h: 121, anim: 'wa-generic-hover', delay: '-7s', img: '/static/images/hospital-scene/ambulance-shield.webp', mobileScale: .4 });
            },
        },
        // Education: no walking silhouette — 4 full-color pieces cropped
        // from static/images/Education_pics.png, large and placed in the
        // page's outer margins (cap+books top-right, open book bottom-left,
        // backpack bottom-center, globe right side) — z-index:-1 (.world-bg)
        // means they always paint behind real content, so generous size/
        // reach into the content area is safe: cards/pagination render on
        // top regardless of any positional overlap.
        education: {
            duration: '15s',
            build: function (layer) {
                addActor(layer, { left: 'calc(100vw - 360px)', top: '6%', w: 320, h: 171, anim: 'wa-generic-hover', delay: '-1s', img: '/static/images/education-scene/cap-books.webp', mobileScale: .4 });
                addActor(layer, { left: '-2vw', bottom: '8%', w: 260, h: 186, anim: 'wa-generic-hover', delay: '-4s', img: '/static/images/education-scene/open-book.webp', mobileScale: .4 });
                addActor(layer, { left: '36vw', bottom: '2%', w: 230, h: 144, anim: 'wa-generic-hover', delay: '-7s', img: '/static/images/education-scene/backpack.webp', mobileScale: .4 });
                addActor(layer, { left: 'calc(100vw - 280px)', bottom: '20%', w: 210, h: 169, anim: 'wa-generic-hover', delay: '-2.5s', img: '/static/images/education-scene/globe.webp', mobileScale: .4 });
            },
        },
        // Jobs: same treatment — 4 pieces cropped from the bottom row of
        // static/images/jobs.png (laptop search, CV, briefcase, growth chart).
        jobs: {
            duration: '15s',
            build: function (layer) {
                addActor(layer, { left: 'calc(100vw - 340px)', top: '6%', w: 300, h: 219, anim: 'wa-generic-hover', delay: '-1s', img: '/static/images/jobs-scene/laptop-search.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', top: '10%', w: 240, h: 169, anim: 'wa-generic-hover', delay: '-5s', img: '/static/images/jobs-scene/cv-books.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', bottom: '6%', w: 250, h: 197, anim: 'wa-generic-hover', delay: '-3s', img: '/static/images/jobs-scene/briefcase.webp', mobileScale: .4 });
                addActor(layer, { left: 'calc(100vw - 270px)', bottom: '5%', w: 240, h: 184, anim: 'wa-generic-hover', delay: '-7s', img: '/static/images/jobs-scene/growth-chart.webp', mobileScale: .4 });
            },
        },
        // Shops / Businesses: 4 pieces cropped from static/images/shops.png.
        business: {
            duration: '15s',
            build: function (layer) {
                addActor(layer, { left: 'calc(100vw - 300px)', top: '5%', w: 250, h: 329, anim: 'wa-generic-hover', delay: '-1s', img: '/static/images/shops-scene/storefront.webp', mobileScale: .4 });
                addActor(layer, { left: '1vw', top: '10%', w: 150, h: 138, anim: 'wa-generic-hover', delay: '-5s', img: '/static/images/shops-scene/location-pin.webp', mobileScale: .4 });
                addActor(layer, { left: '-1vw', bottom: '5%', w: 220, h: 247, anim: 'wa-generic-hover', delay: '-3s', img: '/static/images/shops-scene/cart-bags.webp', mobileScale: .4 });
                addActor(layer, { left: 'calc(100vw - 240px)', bottom: '4%', w: 200, h: 233, anim: 'wa-generic-hover', delay: '-7s', img: '/static/images/shops-scene/phone-bags.webp', mobileScale: .4 });
            },
        },
    };

    // The remaining categories use the same four-corner layout as the
    // hand-placed scenes above — 4 full-color pieces cut from each
    // category's illustration sheet in static/images/ (one quadrant each),
    // two per side in the page's outer margins.
    var CORNERS = [
        { left: 'calc(100vw - 320px)', top: '6%', delay: '-1s' },
        { left: '-1vw', top: '10%', delay: '-5s' },
        { left: '-1vw', bottom: '5%', delay: '-3s' },
        { left: 'calc(100vw - 300px)', bottom: '4%', delay: '-7s' },
    ];
    var ILLUSTRATED_SCENES = {
        transport: ['bus', 'train', 'taxi', 'truck'],
        news: ['newspaper', 'reporter', 'town-updates', 'community-event'],
        village: ['reading-news', 'gathering', 'weather', 'road-work'],
        realestate: ['villa', 'apartments', 'commercial', 'map-search'],
        events: ['wedding', 'concert', 'seminar', 'garden-party'],
        repair: ['plumber', 'electrician', 'carpenter', 'ac-service'],
        tourism: ['beach', 'temple', 'hilltop', 'theme-park'],
        projects: ['apartments', 'construction', 'layout-plan', 'growth'],
        marketplace: ['buy', 'sell', 'exchange', 'marketplace'],
        students: ['counselling', 'scholarships', 'enrollment', 'study-group'],
        tuition: ['one-on-one', 'classroom', 'books-globe', 'coaching-centre'],
    };
    Object.keys(ILLUSTRATED_SCENES).forEach(function (key) {
        SCENES[key] = {
            duration: '15s',
            build: function (layer) {
                ILLUSTRATED_SCENES[key].forEach(function (name, i) {
                    var pos = CORNERS[i];
                    addActor(layer, {
                        left: pos.left, top: pos.top, bottom: pos.bottom, w: 280, h: 220,
                        anim: 'wa-generic-hover', delay: pos.delay, mobileScale: .4,
                        img: '/static/images/' + key + '-scene/' + name + '.webp',
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
