// OneTownCity — compatibility stub.
//
// The dashboard sidebar's seasonal-effect picker ([data-weather-season-option],
// dashboard/base_dashboard.html) and the navbar's theme switcher
// (partials/palette_options.html) now share ONE engine — see
// palette-switcher.js (wiring + state) and seasonal-canvas.js (the actual
// canvas draw loop, modes: summer/rain/aurora/winter/spring/autumn). Both
// scripts are already loaded site-wide via base.html, so this file has
// nothing left to do — it exists only so dashboard/base_dashboard.html's
// existing <script> tag for it doesn't 404.
