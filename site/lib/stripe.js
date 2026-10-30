'use strict';

/** The smallest possible Stripe client: two calls, both server-side only. */

const API = 'https://api.stripe.com/v1';

function key() {
  const value = process.env.STRIPE_SECRET_KEY;
  if (!value) throw new Error('STRIPE_SECRET_KEY is not set');
  return value;
}

async function request(method, path, params) {
  const url = API + path;
  const init = {
    method,
    headers: {
      Authorization: `Bearer ${key()}`,
      'Stripe-Version': '2024-06-20',
    },
  };
  if (method === 'POST') {
    init.headers['Content-Type'] = 'application/x-www-form-urlencoded';
    init.body = new URLSearchParams(params || {}).toString();
  }
  const response = await fetch(url, init);
  const json = await response.json().catch(() => ({}));
  return { ok: response.ok, status: response.status, json };
}

const post = (path, params) => request('POST', path, params);
const get = (path) => request('GET', path);

module.exports = { post, get };
