// OneTownCity — Toast notification lifecycle.
//
// Each [data-hk-toast] card (see base.html) auto-dismisses after a few
// seconds, or immediately when its close button is clicked. Either path
// plays the hk-toast-out CSS animation (main.css) before the element is
// actually removed from the DOM, so it never just vanishes.
(function () {
    'use strict';

    var AUTO_DISMISS_MS = 3500;
    var EXIT_ANIMATION_MS = 300;

    function dismiss(toast) {
        if (toast.classList.contains('is-leaving')) return;
        toast.classList.add('is-leaving');
        setTimeout(function () {
            toast.remove();
        }, EXIT_ANIMATION_MS);
    }

    document.querySelectorAll('[data-hk-toast]').forEach(function (toast) {
        var timer = setTimeout(function () { dismiss(toast); }, AUTO_DISMISS_MS);

        var closeBtn = toast.querySelector('[data-hk-toast-close]');
        if (closeBtn) {
            closeBtn.addEventListener('click', function () {
                clearTimeout(timer);
                dismiss(toast);
            });
        }
    });
})();
