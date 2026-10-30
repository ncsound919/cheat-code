/* Shared behaviour for the storefront. No dependencies, no tracking. */
(function () {
  'use strict';

  document.documentElement.classList.add('js');

  var head = document.querySelector('[data-head]');
  var onScroll = function () {
    if (head) head.classList.toggle('is-scrolled', window.scrollY > 10);
  };
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });

  var button = document.querySelector('[data-menu-btn]');
  var menu = document.querySelector('[data-menu]');
  function closeMenu() {
    if (!button || !menu) return;
    button.setAttribute('aria-expanded', 'false');
    menu.classList.remove('is-open');
  }
  if (button && menu) {
    button.addEventListener('click', function () {
      var open = button.getAttribute('aria-expanded') === 'true';
      button.setAttribute('aria-expanded', String(!open));
      menu.classList.toggle('is-open', !open);
    });
    menu.addEventListener('click', function (event) {
      if (event.target.closest('a')) closeMenu();
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') closeMenu();
    });
    document.addEventListener('click', function (event) {
      if (!menu.classList.contains('is-open')) return;
      if (!menu.contains(event.target) && !button.contains(event.target)) closeMenu();
    });
    window.addEventListener('resize', function () {
      if (window.innerWidth > 980) closeMenu();
    });
  }

  var reveals = document.querySelectorAll('[data-reveal]');
  if ('IntersectionObserver' in window && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('in');
        observer.unobserve(entry.target);
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
    reveals.forEach(function (el) { observer.observe(el); });
  }

  var buy = document.getElementById('buy');
  var note = document.getElementById('buy-note');
  if (buy) {
    buy.addEventListener('click', function (event) {
      if (!window.fetch) return;
      event.preventDefault();
      if (buy.getAttribute('aria-busy') === 'true') return;
      buy.setAttribute('aria-busy', 'true');
      buy.style.pointerEvents = 'none';
      buy.style.opacity = '0.65';
      if (note) note.textContent = 'Opening Stripe…';
      fetch('/api/checkout', { method: 'POST', headers: { Accept: 'application/json' } })
        .then(function (response) {
          return response.json().catch(function () { return {}; }).then(function (body) {
            if (!response.ok || !body.url) throw new Error(body.error || 'Checkout is unavailable.');
            return body;
          });
        })
        .then(function (body) { window.location.assign(body.url); })
        .catch(function (error) {
          if (note) note.textContent = error.message || 'Checkout is unavailable. Try again.';
          buy.removeAttribute('aria-busy');
          buy.style.pointerEvents = '';
          buy.style.opacity = '';
        });
    });
  }
}());
