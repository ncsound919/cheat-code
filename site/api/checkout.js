'use strict';

/**
 * POST /api/checkout
 *
 * Creates a Stripe Checkout Session for PropsLab Premium and returns its URL. Used by the
 * pricing button on the site (a page that can read JSON) and by anything else that would
 * rather show Stripe in a new tab than replace the current page.
 */

const { createSession, originOf } = require('../lib/checkout');

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Use POST.' });
    return;
  }
  const base = originOf(req);
  if (!base) {
    res.status(500).json({ error: 'The deployment has no host header.' });
    return;
  }
  const result = await createSession(base);
  if (!result.ok) {
    const message = (result.json && result.json.error && result.json.error.message) || 'Stripe refused the request.';
    res.status(502).json({ error: message });
    return;
  }
  res.status(200).json({ url: result.json.url, id: result.json.id });
};
