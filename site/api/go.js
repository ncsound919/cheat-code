'use strict';

/**
 * GET /api/go — the address the app opens for "Get Premium".
 *
 * A redirect, not a payment link: each visit mints a new Checkout Session server-side, so
 * the price, the trial and the success page are never stale. If Stripe is unreachable the
 * visitor gets a plain sentence and a way back rather than a dead end.
 */

const { createSession, originOf } = require('../lib/checkout');

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  const base = originOf(req);
  if (!base) {
    res.status(500).send('This deployment has no host header.');
    return;
  }
  const result = await createSession(base);
  if (!result.ok || !result.json.url) {
    const message = (result.json && result.json.error && result.json.error.message) || 'Stripe did not answer.';
    res.status(502).send(`Checkout is unavailable right now. ${message}`);
    return;
  }
  res.writeHead(303, { Location: result.json.url, 'Cache-Control': 'no-store' });
  res.end();
};
