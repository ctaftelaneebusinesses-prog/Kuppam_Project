// OneTownCity — Seasonal canvas animation engine.
//
// One responsive overlay <canvas id="hkSeasonalCanvas"> shared by every
// navbar theme option (see tokens.css / main.css for the element's fixed,
// pointer-events:none positioning). Exposes a tiny global,
// window.HKSeasonalCanvas, so palette-switcher.js can call setMode() whenever
// the active choice changes without either script needing to know the
// other's internals.
//
// Modes:
//   'summer' — a soft glowing sun orb eases down into view, casting a
//              radial heat bloom that drifts gently side to side.
//   'rain'   — thin, mist-like rain streaks drift down the viewport with
//              randomized speed/opacity/length for organic realism.
//   'aurora' — slow drifting ribbons of light (Nordic Aurora's signature),
//              built from the same particle pool as the other two modes.
//   'winter' — "Snow" (Christmas): soft drifting snowflakes (gentle
//              sinusoidal sway) plus fast, fine horizontal wind streaks, and
//              a flat-silhouette Santa + penguin duo that eases in at the
//              bottom-left (same spot/entrance as spring's tree — the two
//              never render together, and bottom-right is where the fixed
//              WhatsApp FAB always sits).
//   'spring' — a flat-silhouette tree eases in at the bottom-left (same
//              ease-out entrance as the summer orb) with blossom dots on its
//              canopy, while petals drift diagonally downward, spinning; a
//              handful bloom in from the four screen corners.
//   'autumn' — warm-toned leaves falling with a continuous horizontal-flip
//              scale trick layered under normal in-plane rotation, the
//              standard 2D-canvas approximation of a leaf tumbling in real
//              3D air currents (a flat canvas has no actual depth axis).
//   'none'   — canvas is cleared and the render loop stops entirely; this
//              is the default palette's mode, so nothing runs until the
//              user actually opts into a seasonal look.
(function () {
    var canvas = null;
    var ctx = null;
    var mode = 'none';
    var particles = [];
    var winterFlakes = [];
    var winterWinds = [];
    var springPetals = [];
    var autumnLeaves = [];
    var rafId = null;
    var lastResize = { w: 0, h: 0, dpr: 1 };
    var sunProgress = 0; // 0 -> 1 easing for the summer orb's entrance
    var treeProgress = 0; // 0 -> 1 easing for the spring tree's entrance
    var santaProgress = 0; // 0 -> 1 easing for winter's Santa + penguin entrance

    function isMobile() {
        return window.matchMedia('(max-width: 768px)').matches;
    }

    function ensureCanvas() {
        if (canvas) return;
        canvas = document.createElement('canvas');
        canvas.id = 'hkSeasonalCanvas';
        canvas.setAttribute('aria-hidden', 'true');
        document.body.appendChild(canvas);
        ctx = canvas.getContext('2d');
        resize();
        window.addEventListener('resize', resize);
    }

    function resize() {
        if (!canvas) return;
        var dpr = Math.min(window.devicePixelRatio || 1, 2);
        var w = window.innerWidth;
        var h = window.innerHeight;
        if (w === lastResize.w && h === lastResize.h && dpr === lastResize.dpr) return;
        lastResize = { w: w, h: h, dpr: dpr };
        canvas.width = w * dpr;
        canvas.height = h * dpr;
        canvas.style.width = w + 'px';
        canvas.style.height = h + 'px';
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        seedParticles();
    }

    function accentColors() {
        var styles = getComputedStyle(document.documentElement);
        return {
            a1: styles.getPropertyValue('--hk-canvas-accent-1').trim() || 'rgba(255,255,255,.5)',
            a2: styles.getPropertyValue('--hk-canvas-accent-2').trim() || 'rgba(255,255,255,.3)',
        };
    }

    // Winter/spring/autumn are independent one-off weather picks, not tied to
    // the active luxury palette's own accent — they always read their own
    // fixed --hk-weather-<name>-1/2 tokens (tokens.css) regardless of which
    // palette (or none) is otherwise active.
    function weatherColors(name) {
        var styles = getComputedStyle(document.documentElement);
        return {
            a1: styles.getPropertyValue('--hk-weather-' + name + '-1').trim() || 'rgba(255,255,255,.5)',
            a2: styles.getPropertyValue('--hk-weather-' + name + '-2').trim() || 'rgba(255,255,255,.3)',
        };
    }

    function withAlpha(rgba, alpha) {
        return rgba.replace(/[\d.]+\)$/, alpha + ')');
    }

    function rand(min, max) {
        return min + Math.random() * (max - min);
    }

    function countFor(minCount, maxCount, densityDesktop, densityMobile) {
        var w = window.innerWidth;
        var h = window.innerHeight;
        var density = isMobile() ? densityMobile : densityDesktop;
        return Math.max(minCount, Math.min(maxCount, Math.round(w * h * density)));
    }

    function seedWinter() {
        winterFlakes = [];
        winterWinds = [];
        var w = window.innerWidth, h = window.innerHeight;
        var flakeCount = countFor(16, 90, 1 / 20000, 1 / 40000);
        for (var i = 0; i < flakeCount; i++) {
            winterFlakes.push({
                x: rand(0, w), y: rand(-h, h),
                r: rand(1.5, 3.5), speed: rand(24, 70),
                swayAmp: rand(10, 34), swayFreq: rand(.4, 1.1), phase: rand(0, Math.PI * 2),
                opacity: rand(.18, .4),
            });
        }
        var windCount = countFor(6, 24, 1 / 90000, 1 / 160000);
        for (var j = 0; j < windCount; j++) {
            winterWinds.push({
                x: rand(-200, w), y: rand(0, h),
                len: rand(60, 160), speed: rand(260, 460), opacity: rand(.08, .2),
            });
        }
    }

    function seedSpring() {
        springPetals = [];
        var w = window.innerWidth, h = window.innerHeight;
        var count = countFor(14, 70, 1 / 22000, 1 / 44000);
        // ~30% of petals are "bloom nodes" — scattered at random points
        // across the whole viewport (not just the corners), scaling in from
        // nothing before joining the normal fall/drift like every other petal.
        for (var i = 0; i < count; i++) {
            var isBloomNode = Math.random() < 0.3;
            var p = {
                size: rand(5, 10), speed: rand(18, 46), driftX: rand(-24, 24),
                rotation: rand(0, Math.PI * 2), rotSpeed: rand(-1.2, 1.2),
                opacity: rand(.16, .34), tone: Math.random() < .6 ? 1 : 2,
                isBloomNode: isBloomNode, bloomAge: 0, bloomDur: rand(.6, 1.05),
            };
            if (isBloomNode) {
                p.nodeX = rand(w * .05, w * .95);
                p.nodeY = rand(h * .05, h * .85);
                p.x = p.nodeX; p.y = p.nodeY;
            } else {
                p.x = rand(0, w); p.y = rand(-h, h * .4);
            }
            springPetals.push(p);
        }
    }

    function seedAutumn() {
        autumnLeaves = [];
        var w = window.innerWidth, h = window.innerHeight;
        var count = countFor(14, 64, 1 / 24000, 1 / 48000);
        for (var i = 0; i < count; i++) {
            autumnLeaves.push({
                x: rand(0, w), y: rand(-h, h),
                size: rand(6, 12), speed: rand(20, 50), driftX: rand(-18, 18),
                rotZ: rand(0, Math.PI * 2), rotZSpeed: rand(-1.4, 1.4),
                rotYPhase: rand(0, Math.PI * 2), rotYSpeed: rand(1.2, 2.6),
                opacity: rand(.18, .36), tone: Math.random() < .55 ? 1 : 2,
            });
        }
    }

    function seedParticles() {
        particles = [];
        if (!canvas) return;
        var w = window.innerWidth;
        var h = window.innerHeight;
        var area = w * h;
        // Density scaled to viewport area, halved on mobile to preserve frame
        // rate on weaker GPUs/battery-constrained devices.
        var density = isMobile() ? 1 / 32000 : 1 / 16000;
        var count = Math.max(14, Math.min(160, Math.round(area * density)));

        if (mode === 'rain') {
            for (var i = 0; i < count; i++) {
                particles.push({
                    x: rand(0, w),
                    y: rand(-h, h),
                    len: rand(18, 60),
                    speed: rand(220, 520),
                    opacity: rand(.08, .32),
                    drift: rand(-8, 8),
                });
            }
        } else if (mode === 'aurora') {
            var ribbons = Math.max(3, Math.min(7, Math.round(count / 16)));
            for (var r = 0; r < ribbons; r++) {
                particles.push({
                    baseY: rand(h * .05, h * .55),
                    amp: rand(30, 90),
                    freq: rand(.0008, .002),
                    speed: rand(6, 18),
                    phase: rand(0, Math.PI * 2),
                    width: rand(80, 180),
                    opacity: rand(.05, .14),
                    offset: 0,
                });
            }
        } else if (mode === 'summer') {
            // A handful of soft ambient dust motes drifting near the orb,
            // in addition to the orb itself (drawn separately each frame).
            for (var m = 0; m < Math.round(count / 4); m++) {
                particles.push({
                    x: rand(0, w),
                    y: rand(0, h * .6),
                    r: rand(1, 3),
                    speed: rand(4, 14),
                    opacity: rand(.1, .35),
                    phase: rand(0, Math.PI * 2),
                });
            }
        } else if (mode === 'winter') {
            seedWinter();
        } else if (mode === 'spring') {
            seedSpring();
        } else if (mode === 'autumn') {
            seedAutumn();
        }
    }

    function drawRain(dt, colors) {
        var h = window.innerHeight;
        var w = window.innerWidth;
        ctx.clearRect(0, 0, w, h);
        ctx.lineCap = 'round';
        particles.forEach(function (p) {
            p.y += p.speed * dt;
            p.x += p.drift * dt * 0.2;
            if (p.y > h + p.len) {
                p.y = -p.len;
                p.x = rand(0, w);
            }
            var grad = ctx.createLinearGradient(p.x, p.y, p.x, p.y + p.len);
            grad.addColorStop(0, 'rgba(255,255,255,0)');
            grad.addColorStop(1, colors.a1.replace(/[\d.]+\)$/, (p.opacity) + ')'));
            ctx.strokeStyle = grad;
            ctx.lineWidth = 1.2;
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p.x + p.drift, p.y + p.len);
            ctx.stroke();
        });
    }

    function drawAurora(dt, colors, t) {
        var h = window.innerHeight;
        var w = window.innerWidth;
        ctx.clearRect(0, 0, w, h);
        particles.forEach(function (p) {
            p.offset += p.speed * dt;
            var grad = ctx.createLinearGradient(0, 0, w, 0);
            grad.addColorStop(0, 'rgba(0,0,0,0)');
            grad.addColorStop(.5, colors.a1.replace(/[\d.]+\)$/, p.opacity + ')'));
            grad.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.strokeStyle = grad;
            ctx.lineWidth = p.width;
            ctx.beginPath();
            for (var x = 0; x <= w; x += 24) {
                var y = p.baseY + Math.sin(x * p.freq + p.phase + p.offset * .02) * p.amp;
                if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
            }
            ctx.stroke();
        });
    }

    function drawSummer(dt, colors, t) {
        var h = window.innerHeight;
        var w = window.innerWidth;
        ctx.clearRect(0, 0, w, h);

        sunProgress = Math.min(1, sunProgress + dt * 0.35);
        var eased = 1 - Math.pow(1 - sunProgress, 3);
        var orbX = w * 0.82;
        var orbTargetY = h * 0.22;
        var orbY = -120 + (orbTargetY + 120) * eased + Math.sin(t * 0.3) * 10;
        var orbR = Math.min(w, h) * 0.14;

        var bloom = ctx.createRadialGradient(orbX, orbY, 0, orbX, orbY, orbR * 5);
        bloom.addColorStop(0, colors.a2.replace(/[\d.]+\)$/, (0.22 * eased) + ')'));
        bloom.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = bloom;
        ctx.fillRect(0, 0, w, h);

        var orb = ctx.createRadialGradient(orbX, orbY, 0, orbX, orbY, orbR);
        orb.addColorStop(0, colors.a1.replace(/[\d.]+\)$/, (0.9 * eased) + ')'));
        orb.addColorStop(.7, colors.a1.replace(/[\d.]+\)$/, (0.35 * eased) + ')'));
        orb.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = orb;
        ctx.beginPath();
        ctx.arc(orbX, orbY, orbR * 1.6, 0, Math.PI * 2);
        ctx.fill();

        particles.forEach(function (p) {
            p.phase += p.speed * dt * 0.05;
            var y = (p.y + Math.sin(p.phase) * 20) % h;
            ctx.fillStyle = colors.a1.replace(/[\d.]+\)$/, p.opacity + ')');
            ctx.beginPath();
            ctx.arc(p.x, y < 0 ? y + h : y, p.r, 0, Math.PI * 2);
            ctx.fill();
        });
    }

    // Flat-silhouette Santa, same bottom-left ease-out entrance spot the
    // spring tree uses (winter/spring never render at the same time, so
    // there's no actual clash) — deliberately NOT the bottom-right, which is
    // where the fixed WhatsApp FAB (.hk-whatsapp-fab-wrap) always sits. A
    // simple round coat/beard/hat built from primitives, in keeping with
    // this file's "abstract silhouette, not an illustrated character"
    // convention (a real illustrated Santa needs licensed/generated art
    // this environment can't source).
    function drawSanta(eased, w, h) {
        var figScale = eased * Math.min(1, Math.min(w, h) / 700);
        var baseX = Math.min(150, w * 0.11);
        var baseY = h;

        ctx.save();
        ctx.globalAlpha = eased;
        ctx.translate(baseX, baseY);
        ctx.scale(figScale, figScale);

        ctx.fillStyle = 'rgba(35, 28, 20, .85)';
        ctx.fillRect(-34, -18, 24, 18);
        ctx.fillRect(10, -18, 24, 18);

        ctx.fillStyle = 'rgba(196, 30, 40, .88)';
        ctx.beginPath();
        ctx.ellipse(-4, -95, 46, 68, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(250, 250, 250, .92)';
        ctx.beginPath();
        ctx.ellipse(-4, -30, 46, 12, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(20, 16, 12, .85)';
        ctx.fillRect(-48, -102, 88, 11);
        ctx.fillStyle = 'rgba(210, 180, 60, .9)';
        ctx.fillRect(-9, -103, 14, 13);

        ctx.fillStyle = 'rgba(250, 250, 250, .92)';
        ctx.beginPath();
        ctx.ellipse(-4, -148, 30, 26, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(235, 194, 150, .95)';
        ctx.beginPath();
        ctx.arc(-4, -170, 17, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(196, 30, 40, .9)';
        ctx.beginPath();
        ctx.moveTo(-22, -180);
        ctx.lineTo(14, -180);
        ctx.lineTo(-2, -226);
        ctx.closePath();
        ctx.fill();
        ctx.fillStyle = 'rgba(250, 250, 250, .92)';
        ctx.beginPath();
        ctx.ellipse(-4, -180, 22, 8, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(-2, -226, 8, 0, Math.PI * 2);
        ctx.fill();

        ctx.restore();
    }

    // A small penguin standing beside Santa — same silhouette convention.
    function drawPenguin(eased, w, h) {
        var figScale = eased * Math.min(1, Math.min(w, h) / 700);
        var baseX = Math.min(150, w * 0.11) + 92;
        var baseY = h;

        ctx.save();
        ctx.globalAlpha = eased;
        ctx.translate(baseX, baseY);
        ctx.scale(figScale, figScale);

        ctx.fillStyle = 'rgba(240, 160, 30, .95)';
        ctx.beginPath();
        ctx.moveTo(-16, -2); ctx.lineTo(-2, -2); ctx.lineTo(-9, 8);
        ctx.closePath(); ctx.fill();
        ctx.beginPath();
        ctx.moveTo(2, -2); ctx.lineTo(16, -2); ctx.lineTo(9, 8);
        ctx.closePath(); ctx.fill();

        ctx.fillStyle = 'rgba(20, 22, 28, .9)';
        ctx.beginPath();
        ctx.ellipse(0, -48, 30, 50, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(250, 250, 250, .92)';
        ctx.beginPath();
        ctx.ellipse(2, -42, 17, 34, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(240, 160, 30, .95)';
        ctx.beginPath();
        ctx.moveTo(-7, -84); ctx.lineTo(7, -84); ctx.lineTo(0, -74);
        ctx.closePath(); ctx.fill();

        ctx.fillStyle = 'rgba(255, 255, 255, .9)';
        ctx.beginPath(); ctx.arc(-8, -88, 3.5, 0, Math.PI * 2); ctx.fill();
        ctx.beginPath(); ctx.arc(8, -88, 3.5, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = 'rgba(20, 22, 28, .9)';
        ctx.beginPath(); ctx.arc(-8, -88, 1.6, 0, Math.PI * 2); ctx.fill();
        ctx.beginPath(); ctx.arc(8, -88, 1.6, 0, Math.PI * 2); ctx.fill();

        ctx.restore();
    }

    function drawWinter(dt, colors) {
        var w = window.innerWidth, h = window.innerHeight;
        ctx.clearRect(0, 0, w, h);

        santaProgress = Math.min(1, santaProgress + dt * 0.5);
        var santaEased = 1 - Math.pow(1 - santaProgress, 3);
        drawSanta(santaEased, w, h);
        drawPenguin(santaEased, w, h);

        winterFlakes.forEach(function (p) {
            p.y += p.speed * dt;
            p.phase += p.swayFreq * dt;
            if (p.y > h + 6) { p.y = -6; p.x = rand(0, w); }
            var x = p.x + Math.sin(p.phase) * p.swayAmp;
            ctx.beginPath();
            ctx.fillStyle = withAlpha(colors.a1, p.opacity);
            ctx.arc(x, p.y, p.r, 0, Math.PI * 2);
            ctx.fill();
        });
        winterWinds.forEach(function (p) {
            p.x += p.speed * dt;
            if (p.x > w + p.len) { p.x = -p.len; p.y = rand(0, h); }
            var grad = ctx.createLinearGradient(p.x, p.y, p.x + p.len, p.y);
            grad.addColorStop(0, 'rgba(255,255,255,0)');
            grad.addColorStop(.5, withAlpha(colors.a2, p.opacity));
            grad.addColorStop(1, 'rgba(255,255,255,0)');
            ctx.strokeStyle = grad;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p.x + p.len, p.y);
            ctx.stroke();
        });
    }

    function drawPetal(p, colors) {
        var color = p.tone === 1 ? colors.a1 : colors.a2;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rotation);
        var scale = 1;
        if (p.isBloomNode && p.bloomAge < p.bloomDur) {
            var e = p.bloomAge / p.bloomDur;
            scale = 1 - Math.pow(1 - e, 3); // ease-out cubic bloom-in
        }
        ctx.scale(scale, scale);
        ctx.fillStyle = withAlpha(color, p.opacity);
        ctx.beginPath();
        ctx.ellipse(0, 0, p.size, p.size * .55, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
    }

    // Flat-silhouette tree, same ease-out entrance the summer orb uses —
    // trunk anchored to the bottom-left corner, blossom dots scattered on
    // the canopy in the spring petal tones so it visually matches the
    // falling petals rather than reading as a separate green blob.
    function drawTree(eased, colors, w, h) {
        var baseX = Math.min(160, w * 0.12);
        var baseY = h;
        var trunkH = Math.min(220, h * 0.28) * eased;
        var trunkW = 16;
        var canopyR = Math.min(110, h * 0.14) * eased;

        ctx.save();
        ctx.globalAlpha = eased;

        ctx.fillStyle = 'rgba(90, 62, 40, .55)';
        ctx.fillRect(baseX - trunkW / 2, baseY - trunkH, trunkW, trunkH);

        var canopyY = baseY - trunkH;
        var blobs = [
            { dx: 0, dy: -canopyR * 0.35, r: canopyR },
            { dx: -canopyR * 0.75, dy: -canopyR * 0.05, r: canopyR * 0.7 },
            { dx: canopyR * 0.75, dy: -canopyR * 0.05, r: canopyR * 0.7 },
        ];
        blobs.forEach(function (b) {
            ctx.fillStyle = withAlpha(colors.a2, .5);
            ctx.beginPath();
            ctx.arc(baseX + b.dx, canopyY + b.dy, b.r, 0, Math.PI * 2);
            ctx.fill();
        });

        // Blossom flecks scattered across the canopy silhouette.
        for (var i = 0; i < 14; i++) {
            var ang = (i / 14) * Math.PI * 2;
            var rr = canopyR * (0.4 + (i % 3) * 0.25);
            var bx = baseX + Math.cos(ang) * rr * 0.9;
            var by = canopyY - canopyR * 0.2 + Math.sin(ang) * rr * 0.55;
            ctx.fillStyle = withAlpha(colors.a1, .8);
            ctx.beginPath();
            ctx.arc(bx, by, 3.2, 0, Math.PI * 2);
            ctx.fill();
        }
        ctx.restore();
    }

    function drawSpring(dt, colors) {
        var w = window.innerWidth, h = window.innerHeight;
        ctx.clearRect(0, 0, w, h);

        treeProgress = Math.min(1, treeProgress + dt * 0.5);
        var eased = 1 - Math.pow(1 - treeProgress, 3);
        drawTree(eased, colors, w, h);

        springPetals.forEach(function (p) {
            p.rotation += p.rotSpeed * dt;
            if (p.isBloomNode && p.bloomAge < p.bloomDur) {
                p.bloomAge += dt;
            } else {
                p.y += p.speed * dt;
                p.x += p.driftX * dt;
                if (p.y > h + p.size || p.x < -p.size || p.x > w + p.size) {
                    if (p.isBloomNode) {
                        p.x = p.nodeX;
                        p.y = p.nodeY;
                        p.bloomAge = 0;
                    } else {
                        p.y = -p.size;
                        p.x = rand(0, w);
                    }
                }
            }
            drawPetal(p, colors);
        });
    }

    function drawAutumn(dt, colors) {
        var w = window.innerWidth, h = window.innerHeight;
        ctx.clearRect(0, 0, w, h);
        autumnLeaves.forEach(function (p) {
            p.y += p.speed * dt;
            p.x += p.driftX * dt;
            p.rotZ += p.rotZSpeed * dt;
            p.rotYPhase += p.rotYSpeed * dt;
            if (p.y > h + p.size || p.x < -p.size || p.x > w + p.size) {
                p.y = -p.size;
                p.x = rand(0, w);
            }
            var color = p.tone === 1 ? colors.a1 : colors.a2;
            var flip = Math.cos(p.rotYPhase);
            ctx.save();
            ctx.translate(p.x, p.y);
            ctx.rotate(p.rotZ);
            // Fake-3D tumble: squashing/flipping the horizontal scale as a
            // cosine wave reads as the leaf rotating edge-on and back, the
            // standard flat-canvas approximation of a real rotateY axis.
            ctx.scale(Math.max(.12, Math.abs(flip)), 1);
            ctx.fillStyle = withAlpha(color, p.opacity);
            ctx.beginPath();
            ctx.ellipse(0, 0, p.size, p.size * .6, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
        });
    }

    var lastFrame = null;
    var startTime = null;

    function loop(now) {
        if (!canvas || mode === 'none') { rafId = null; return; }
        if (lastFrame === null) lastFrame = now;
        if (startTime === null) startTime = now;
        var dt = Math.min((now - lastFrame) / 1000, 0.05);
        var t = (now - startTime) / 1000;
        lastFrame = now;

        if (mode === 'rain') drawRain(dt, accentColors());
        else if (mode === 'aurora') drawAurora(dt, accentColors(), t);
        else if (mode === 'summer') drawSummer(dt, accentColors(), t);
        else if (mode === 'winter') drawWinter(dt, weatherColors('winter'));
        else if (mode === 'spring') drawSpring(dt, weatherColors('spring'));
        else if (mode === 'autumn') drawAutumn(dt, weatherColors('autumn'));

        rafId = requestAnimationFrame(loop);
    }

    function start() {
        if (rafId) return;
        lastFrame = null;
        rafId = requestAnimationFrame(loop);
    }

    function stop() {
        if (rafId) {
            cancelAnimationFrame(rafId);
            rafId = null;
        }
        if (ctx && canvas) ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    function setMode(nextMode) {
        mode = nextMode || 'none';
        if (mode === 'none') {
            stop();
            if (canvas) canvas.classList.remove('is-active');
            return;
        }
        ensureCanvas();
        sunProgress = 0;
        treeProgress = 0;
        santaProgress = 0;
        startTime = null;
        seedParticles();
        canvas.classList.add('is-active');
        start();
    }

    // Pause entirely when the tab is hidden — pure savings, no visible cost
    // since nothing is on screen to animate anyway.
    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            stop();
        } else if (mode !== 'none') {
            start();
        }
    });

    // Exposes whether a seasonal look (not the default look) is active —
    // category-scene.js/world-scene.js read this to suppress the illustrated
    // category scene, since any navbar theme option is an explicit
    // super-override.
    window.HKSeasonalCanvas = {
        setMode: setMode,
        isPaletteActive: function () { return mode !== 'none'; },
    };
})();
