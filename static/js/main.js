// Hello Kuppam - Main JS
document.addEventListener('DOMContentLoaded', function () {
    console.log('Hello Kuppam frontend loaded.');

    // Site-wide light/dark theme toggle (navbar button). The initial theme is
    // already applied pre-paint by the inline anti-flash script in base.html;
    // this just wires up the button and keeps localStorage in sync so the
    // choice persists across every page, including the dashboard.
    (function () {
        const root = document.documentElement;
        const toggle = document.getElementById('hkThemeToggle');
        const icon = document.getElementById('hkThemeIcon');
        if (!toggle || !icon) return;

        function paintIcon(theme) {
            icon.className = theme === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
        }

        paintIcon(root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light');

        toggle.addEventListener('click', function () {
            const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            root.setAttribute('data-theme', next);
            try { localStorage.setItem('hkTheme', next); } catch (e) {}
            paintIcon(next);
        });
    })();

    // Scroll-reveal animations
    if (window.AOS) {
        AOS.init({ duration: 650, easing: 'ease-out-cubic', once: true, offset: 60 });
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

    // Hero slide carousel (fades between themed panels; autoplay, pauses on hover)
    const heroSlides = document.querySelectorAll('.hk-hero-slide');
    if (heroSlides.length > 1) {
        let activeSlide = 0;
        let heroInterval = setInterval(advanceSlide, 5000);
        const heroSection = document.querySelector('.hk-hero-fullscreen');

        function advanceSlide() {
            heroSlides[activeSlide].classList.remove('is-active');
            activeSlide = (activeSlide + 1) % heroSlides.length;
            heroSlides[activeSlide].classList.add('is-active');
        }

        if (heroSection) {
            heroSection.addEventListener('mouseenter', function () { clearInterval(heroInterval); });
            heroSection.addEventListener('mouseleave', function () { heroInterval = setInterval(advanceSlide, 5000); });
        }
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
            const title = btn.dataset.title || 'Hello Kuppam';
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

    // Close mobile navbar after clicking a nav link
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');
    const navbarCollapse = document.getElementById('navbarMain');

    navLinks.forEach(function (link) {
        link.addEventListener('click', function () {
            if (navbarCollapse.classList.contains('show')) {
                const bsCollapse = bootstrap.Collapse.getOrCreateInstance(navbarCollapse);
                bsCollapse.hide();
            }
        });
    });

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
                    submitBtn.innerHTML = submitBtn.classList.contains('hk-card-icon-btn')
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
});