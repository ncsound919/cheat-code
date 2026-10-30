'use strict';

/**
 * One Checkout Session, created the same way whether a browser asked or the app did.
 *
 * Keeping this in one place is the reason the app can link to a plain URL: the endpoint
 * decides the price, the trial and where Stripe returns to, so a typo in the APK cannot
 * change what a customer is charged.
 */

const { post } = require('./stripe');

const PRICE = process.env.STRIPE_PRICE_ID || 'price_1UW4M2QrfNRBru0zDyjhYH7N';
const TRIAL_DAYS = 7;

function originOf(req) {
  const proto = String(req.headers['x-forwarded-proto'] || 'https').split(',')[0];
  const host = String(req.headers['x-forwarded-host'] || req.headers.host || '').split(',')[0];
  return host ? `${proto}://${host}` : '';
}

async function createSession(base) {
  return post('/checkout/sessions', {
    mode: 'subscription',
    'line_items[0][price]': PRICE,
    'line_items[0][quantity]': '1',
    allow_promotion_codes: 'true',
    'subscription_data[trial_period_days]': String(TRIAL_DAYS),
    'subscription_data[metadata][product]': 'propslab-premium',
    success_url: `${base}/welcome.html?session_id={CHECKOUT_SESSION_ID}`,
    cancel_url: `${base}/#pricing`,
    'metadata[product]': 'propslab-premium',
  });
}

module.exports = { createSession, originOf, PRICE, TRIAL_DAYS };
