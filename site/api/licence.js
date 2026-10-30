'use strict';

/**
 * GET /api/licence?session_id=cs_...
 *
 * Turns a completed Checkout Session into a licence code. The session id acts as the
 * bearer token a buyer already has, so there is no account to create: the welcome page
 * calls this, shows the code, and offers to email or copy it.
 *
 * The subscription status is re-read from Stripe on every call, so a cancelled subscriber
 * stops getting a code from here. The phone still holds the last code it was given, which
 * is why Premium is described as an offline entitlement rather than a live check.
 */

const { get } = require('../lib/stripe');
const { licenceFor, rateLimited } = require('../lib/licence');

const LIVE = new Set(['active', 'trialing']);

function clientIp(req) {
  const forwarded = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
  return forwarded || 'unknown';
}

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'GET' && req.method !== 'POST') {
    res.status(405).json({ error: 'Use GET.' });
    return;
  }
  if (rateLimited(clientIp(req))) {
    res.status(429).json({ error: 'Too many tries. Wait a minute and reload this page.' });
    return;
  }

  const body = req.method === 'POST' ? (req.body || {}) : {};
  const sessionId = String(
    (req.query && req.query.session_id) || body.session_id || '',
  ).trim();

  if (!/^cs_[A-Za-z0-9_]+$/.test(sessionId)) {
    res.status(400).json({ error: 'Pass the session_id from the page you were sent to.' });
    return;
  }

  const result = await get(`/checkout/sessions/${encodeURIComponent(sessionId)}?expand[]=subscription`);
  if (!result.ok) {
    res.status(404).json({ error: 'That checkout could not be found.' });
    return;
  }

  const session = result.json;
  const subscription = session.subscription || {};
  const status = subscription.status || session.payment_status || 'unknown';
  const email = String(
    (session.customer_details && session.customer_details.email) || session.customer_email || '',
  ).trim().toLowerCase();

  if (!email) {
    res.status(409).json({ error: 'Stripe has no email on that checkout yet. Reload in a moment.' });
    return;
  }
  if (!LIVE.has(status)) {
    res.status(402).json({
      error: `That subscription is ${status}. Start a new checkout from the pricing section.`,
      status,
    });
    return;
  }

  res.status(200).json({
    code: licenceFor(email),
    email,
    status,
    trialEnds: subscription.trial_end ? new Date(subscription.trial_end * 1000).toISOString() : null,
    renews: subscription.current_period_end
      ? new Date(subscription.current_period_end * 1000).toISOString()
      : null,
    portal: process.env.STRIPE_BILLING_PORTAL_URL || null,
  });
};
