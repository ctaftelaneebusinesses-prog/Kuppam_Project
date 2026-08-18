// OneTownCity - Main JS
document.addEventListener('DOMContentLoaded', function () {
    console.log('OneTownCity frontend loaded.');

    // Site-wide light/dark theme toggle. The initial theme is already applied
    // pre-paint by the inline anti-flash script in base.html; this wires up
    // every toggle button on the page (desktop navbar + mobile drawer both
    // have their own) and keeps them all in sync via localStorage, so the
    // choice persists across every page, including the dashboard.
    (function () {
        const root = document.documentElement;
        const toggles = document.querySelectorAll('[id^="hkThemeToggle"]');
        const icons = document.querySelectorAll('[id^="hkThemeIcon"]');
        if (!toggles.length) return;

        function paintIcons(theme) {
            icons.forEach(function (icon) {
                icon.className = theme === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
            });
        }

        paintIcons(root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light');

        toggles.forEach(function (toggle) {
            toggle.addEventListener('click', function () {
                const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                root.setAttribute('data-theme', next);
                try { localStorage.setItem('hkTheme', next); } catch (e) {}
                paintIcons(next);
            });
        });
    })();

    // Scroll-reveal animations. aos.css (loaded separately from aos.js) hides
    // every [data-aos] element at opacity:0 until AOS.init() reveals it on
    // scroll — so if the CDN script fails (flaky network, ad-blocker,
    // firewall) while the stylesheet still loads, all that content (card
    // titles included) stays invisible forever. Fall back to revealing it
    // immediately rather than leaving it permanently hidden.
    if (window.AOS) {
        // Anything already on-screen at load shouldn't play its entrance
        // animation — that just reads as an unwanted jump right after the
        // page paints. Strip data-aos from whatever's already in the
        // initial viewport before AOS.init() scans for it, so it renders
        // in its final position immediately; content further down the
        // page still gets the normal reveal-on-scroll animation.
        var initialViewportHeight = window.innerHeight;
        document.querySelectorAll('[data-aos]').forEach(function (el) {
            if (el.getBoundingClientRect().top < initialViewportHeight) {
                el.removeAttribute('data-aos');
            }
        });
        AOS.init({ duration: 650, easing: 'ease-out-cubic', once: true, offset: 60 });
    } else {
        document.querySelectorAll('[data-aos]').forEach(function (el) {
            el.classList.add('aos-animate');
        });
    }

    // Navbar: transparent glass over the hero, solid once the user scrolls past it.
    // The navbar is always position:fixed (see main.css); body.hk-has-transparent-hero
    // removes the usual body padding-top compensation on the homepage so the hero
    // renders full-bleed from y=0, directly behind the glass navbar.
    const navbar = document.getElementById('hkNavbar');
    if (navbar && navbar.classList.contains('hk-navbar-transparent')) {
        const toggleNavbar = function () {
            if (window.scrollY > 60) {
                navbar.classList.add('hk-navbar-solid');
            } else {
                navbar.classList.remove('hk-navbar-solid');
            }
        };
        toggleNavbar();
        window.addEventListener('scroll', toggleNavbar, { passive: true });
    }

    // Elite hero: ambient glows drift gently toward the cursor (desktop only)
    // — a subtle parallax cue rather than a literal cursor-follow, so it
    // reads as "alive" without being distracting.
    const heroElite = document.querySelector('.hk-hero-elite');
    if (heroElite && window.matchMedia('(pointer: fine)').matches) {
        const glows = heroElite.querySelectorAll('.hk-hero-glow');
        heroElite.addEventListener('mousemove', function (e) {
            const rect = heroElite.getBoundingClientRect();
            const x = (e.clientX - rect.left) / rect.width - 0.5;
            const y = (e.clientY - rect.top) / rect.height - 0.5;
            glows.forEach(function (glow, i) {
                const strength = (i + 1) * 10;
                glow.style.transform = 'translate(' + (x * strength) + 'px, ' + (y * strength) + 'px)';
            });
        });
        heroElite.addEventListener('mouseleave', function () {
            glows.forEach(function (glow) { glow.style.transform = ''; });
        });
    }

    // Animated counters for the homepage stats strip — counts up once visible
    const counters = document.querySelectorAll('.hk-stat-number[data-count]');
    if (counters.length) {
        const animateCounter = function (el) {
            const target = parseInt(el.dataset.count, 10) || 0;
            const duration = 1200;
            const start = performance.now();
            function tick(now) {
                const progress = Math.min((now - start) / duration, 1);
                const eased = 1 - Math.pow(1 - progress, 3);
                el.textContent = Math.round(eased * target).toLocaleString('en-IN');
                if (progress < 1) {
                    requestAnimationFrame(tick);
                } else {
                    el.textContent = target.toLocaleString('en-IN');
                }
            }
            requestAnimationFrame(tick);
        };

        const observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    animateCounter(entry.target);
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.4 });

        counters.forEach(function (el) { observer.observe(el); });
    }

    // Image loading shimmer: cards/detail photos shimmer until the browser has
    // actually decoded them, then fade in — avoids layout pop on slow photos.
    // Broken or stalled images (bad URL, slow/unreachable host) fall back to the
    // same category-icon placeholder used server-side, instead of shimmering
    // forever or showing a browser "broken image" icon.
    document.querySelectorAll('.hk-card-img, .hk-detail-img').forEach(function (img) {
        if (img.complete && img.naturalWidth > 0) {
            return;
        }
        img.classList.add('hk-img-loading');

        let settled = false;

        const finish = function () {
            settled = true;
            img.classList.remove('hk-img-loading');
        };

        const fail = function () {
            if (settled) return;
            settled = true;
            img.classList.remove('hk-img-loading');
            img.style.display = 'none';
            const icon = img.dataset.fallbackIcon;
            if (icon && !img.nextElementSibling?.classList.contains('hk-img-fallback')) {
                const placeholder = document.createElement('div');
                placeholder.className = (img.classList.contains('hk-detail-img') ? 'hk-detail-placeholder' : 'hk-card-placeholder') + ' hk-img-fallback';
                placeholder.innerHTML = '<i class="bi ' + icon + '"></i>';
                img.insertAdjacentElement('afterend', placeholder);
            }
        };

        img.addEventListener('load', finish, { once: true });
        img.addEventListener('error', fail, { once: true });
        setTimeout(function () { if (!settled) fail(); }, 6000);
    });

    // Interactive 5-star rating picker (review form): keeps the real <select>
    // in the DOM (hidden) so the form still posts exactly as before, just
    // drives its value from clickable star icons instead of a dropdown.
    document.querySelectorAll('[data-star-picker]').forEach(function (picker) {
        const select = picker.querySelector('select');
        const stars = picker.querySelectorAll('[data-star-value]');
        if (!select || !stars.length) return;

        function paint(value) {
            stars.forEach(function (star) {
                const filled = parseInt(star.dataset.starValue, 10) <= value;
                star.classList.toggle('bi-star-fill', filled);
                star.classList.toggle('bi-star', !filled);
                star.classList.toggle('is-filled', filled);
            });
        }

        paint(parseInt(select.value, 10) || 0);

        stars.forEach(function (star) {
            star.addEventListener('mouseenter', function () {
                paint(parseInt(star.dataset.starValue, 10));
            });
            star.addEventListener('click', function () {
                select.value = star.dataset.starValue;
                paint(parseInt(star.dataset.starValue, 10));
            });
        });

        picker.addEventListener('mouseleave', function () {
            paint(parseInt(select.value, 10) || 0);
        });
    });

    // Card share buttons (business/property/job/event/news cards): native share
    // sheet on mobile, clipboard-copy fallback on desktop.
    document.querySelectorAll('.hk-card-share-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const url = btn.dataset.url;
            const title = btn.dataset.title || 'OneTownCity';
            if (navigator.share) {
                navigator.share({ title: title, url: url }).catch(function () {});
            } else if (navigator.clipboard) {
                navigator.clipboard.writeText(url).then(function () {
                    const original = btn.innerHTML;
                    btn.innerHTML = '<i class="bi bi-check2"></i>';
                    setTimeout(function () { btn.innerHTML = original; }, 1200);
                });
            }
        });
    });

    // Custom "All Categories" dropdown (hero search bar). Generic over every
    // .hk-select on the page — trigger toggles the menu, clicking a row
    // updates the visible label + the hidden form field, and clicking
    // anywhere outside any open instance closes it.
    //
    // The menu is "portaled" to <body> while open: every .hk-hero has
    // overflow:hidden (it clips the ambient glow blobs), and .hk-select
    // lives inside one, so a same-parent absolutely-positioned menu taller
    // than the remaining hero space gets hard-clipped at the hero's bottom
    // edge. Moving it to <body> as position:fixed, placed from the
    // trigger's actual viewport coordinates, sidesteps that entirely.
    (function () {
        const selects = document.querySelectorAll('.hk-select');
        if (!selects.length) return;

        function positionMenu(select, trigger, menu) {
            const rect = trigger.getBoundingClientRect();
            const margin = 8;
            const maxLeft = window.innerWidth - menu.offsetWidth - margin;
            menu.style.top = (rect.bottom + margin) + 'px';
            menu.style.left = Math.max(margin, Math.min(rect.left, maxLeft)) + 'px';
        }

        function setOpen(select, trigger, menu, open) {
            select.classList.toggle('is-open', open);
            trigger.setAttribute('aria-expanded', String(open));
            if (open) {
                document.body.appendChild(menu);
                menu.classList.add('is-portaled');
                positionMenu(select, trigger, menu);
                // Force layout so the position lands before the opacity/scale
                // transition starts, then flip the reveal class next frame.
                menu.getBoundingClientRect();
                requestAnimationFrame(function () { menu.classList.add('is-open'); });
            } else {
                menu.classList.remove('is-open');
                select.appendChild(menu);
                menu.classList.remove('is-portaled');
                menu.style.top = '';
                menu.style.left = '';
            }
        }

        // Each entry keeps its own trigger/menu pair so closeAll never has to
        // re-query the DOM for a menu that may currently live under <body>
        // instead of under its .hk-select.
        const instances = [];

        function closeAll(except) {
            instances.forEach(function (inst) {
                if (inst.select === except) return;
                if (inst.select.classList.contains('is-open')) {
                    setOpen(inst.select, inst.trigger, inst.menu, false);
                }
            });
        }

        selects.forEach(function (select) {
            const trigger = select.querySelector('.hk-select-trigger');
            const menu = select.querySelector('.hk-select-menu');
            instances.push({ select: select, trigger: trigger, menu: menu });
            const label = select.querySelector('.hk-select-trigger-label');
            const valueInput = select.querySelector('.hk-select-value');
            const options = select.querySelectorAll('.hk-select-option');

            trigger.addEventListener('click', function (e) {
                e.stopPropagation();
                const willOpen = !select.classList.contains('is-open');
                closeAll(select);
                setOpen(select, trigger, menu, willOpen);
            });

            options.forEach(function (option) {
                option.addEventListener('click', function () {
                    options.forEach(function (o) {
                        o.classList.remove('is-active');
                        o.setAttribute('aria-selected', 'false');
                    });
                    option.classList.add('is-active');
                    option.setAttribute('aria-selected', 'true');
                    label.textContent = option.querySelector('span').textContent;
                    if (valueInput) valueInput.value = option.dataset.value || '';
                    setOpen(select, trigger, menu, false);
                });
            });

            select.addEventListener('keydown', function (e) {
                if (e.key === 'Escape') {
                    setOpen(select, trigger, menu, false);
                    trigger.focus();
                }
            });

            menu.addEventListener('click', function (e) { e.stopPropagation(); });
        });

        // A fixed-position menu is placed once, at open time — it won't track
        // the trigger through a scroll or a viewport resize, so close it
        // rather than let it drift out of alignment. Capture phase is needed
        // to see scrolls on any scrollable ancestor, but that also catches
        // the menu's own internal scroll (its option list has overflow-y:
        // auto) — without the guard below, every wheel tick inside the menu
        // would close it before it could scroll at all.
        window.addEventListener('scroll', function (e) {
            if (!document.querySelector('.hk-select.is-open')) return;
            if (e.target !== document && e.target.closest && e.target.closest('.hk-select-menu')) return;
            closeAll(null);
        }, true);
        window.addEventListener('resize', function () {
            if (document.querySelector('.hk-select.is-open')) closeAll(null);
        });

        document.addEventListener('click', function () { closeAll(null); });
    })();

    // Close the mobile nav drawer after clicking a link inside it
    const navDrawerEl = document.getElementById('hkNavDrawer');
    if (navDrawerEl && window.bootstrap) {
        navDrawerEl.querySelectorAll('.nav-link, a.btn').forEach(function (link) {
            link.addEventListener('click', function () {
                const instance = bootstrap.Offcanvas.getInstance(navDrawerEl);
                if (instance) instance.hide();
            });
        });
    }

    // Bootstrap client-side validation, plus a loading state on the button
    // that actually triggered submission (prevents double-submits and gives
    // instant feedback while the server responds).
    const forms = document.querySelectorAll('form');
    forms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                form.classList.add('was-validated');
                return;
            }
            form.classList.add('was-validated');

            if (form.classList.contains('hk-no-loading-state')) {
                return;
            }
            const submitBtn = event.submitter || form.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.disabled) {
                // Deferred to the next tick: disabling (or otherwise mutating) the
                // submitter *synchronously* inside its own 'submit' handler can make
                // the browser drop that button's name=value pair from the submitted
                // form data (disabled controls aren't submitted) — a real race that
                // broke forms with multiple same-name submit buttons, like the
                // "what brings you here" intent picker. Deferring lets the browser
                // finish capturing the submission first.
                setTimeout(function () {
                    submitBtn.dataset.hkOriginalHtml = submitBtn.innerHTML;
                    // Icon-only buttons (funnel/toggle/delete row actions etc.) have
                    // no room for the "Please wait…" label — it just wraps into a
                    // cramped, broken-looking pill. Detect "icon-only" generically
                    // via empty textContent rather than hardcoding every such
                    // button's class name one at a time.
                    submitBtn.innerHTML = submitBtn.textContent.trim() === ''
                        ? '<span class="spinner-border spinner-border-sm"></span>'
                        : '<span class="spinner-border spinner-border-sm me-2"></span>Please wait…';
                    submitBtn.disabled = true;
                }, 0);
            }
        });
    });

    // Share buttons on listing detail pages: log the click server-side
    // (for the Total Shares stat) and handle the "Copy Link" button.
    function getCookie(name) {
        const match = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
        return match ? match.pop() : '';
    }

    document.querySelectorAll('.hk-share-row').forEach(function (row) {
        const endpoint = row.dataset.shareEndpoint;
        const shareUrl = row.dataset.shareUrl;

        row.querySelectorAll('.hk-share-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (endpoint) {
                    fetch(endpoint, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                            'X-CSRFToken': getCookie('csrftoken'),
                        },
                        body: 'platform=' + encodeURIComponent(btn.dataset.platform),
                    }).catch(function () {});
                }
            });
        });

        const copyBtn = row.querySelector('.hk-copy-link-btn');
        if (copyBtn) {
            copyBtn.addEventListener('click', function () {
                navigator.clipboard.writeText(shareUrl).then(function () {
                    const original = copyBtn.innerHTML;
                    copyBtn.innerHTML = '<i class="bi bi-check2"></i> Copied!';
                    setTimeout(function () { copyBtn.innerHTML = original; }, 1500);
                });
            });
        }
    });

    // Floating WhatsApp button: builds the wa.me link from data attributes
    // (see base.html) and auto-previews the chat tooltip once per browser
    // session, so it reads as a live nudge without nagging on every page.
    (function () {
        const wrap = document.getElementById('hkWhatsappFab');
        if (!wrap) return;

        const btn = document.getElementById('hkWhatsappBtn');
        const tooltip = document.getElementById('hkWhatsappTooltip');
        const closeBtn = document.getElementById('hkWhatsappTooltipClose');
        const number = wrap.dataset.waNumber || '';
        const message = wrap.dataset.waMessage || '';
        btn.href = 'https://wa.me/' + number + (message ? '?text=' + encodeURIComponent(message) : '');

        let autoHideTimer = null;

        function showTooltip() {
            tooltip.classList.add('is-visible');
        }
        function hideTooltip() {
            tooltip.classList.remove('is-visible');
            if (autoHideTimer) {
                clearTimeout(autoHideTimer);
                autoHideTimer = null;
            }
        }

        wrap.addEventListener('mouseenter', showTooltip);
        wrap.addEventListener('mouseleave', hideTooltip);
        btn.addEventListener('focus', showTooltip);
        btn.addEventListener('blur', hideTooltip);
        closeBtn.addEventListener('click', function () {
            hideTooltip();
            try { sessionStorage.setItem('hkWaTooltipSeen', '1'); } catch (e) {}
        });

        let seen = false;
        try { seen = sessionStorage.getItem('hkWaTooltipSeen') === '1'; } catch (e) {}
        if (!seen) {
            setTimeout(function () {
                showTooltip();
                autoHideTimer = setTimeout(hideTooltip, 6000);
                try { sessionStorage.setItem('hkWaTooltipSeen', '1'); } catch (e) {}
            }, 2500);
        }
    })();
});