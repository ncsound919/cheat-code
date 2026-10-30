'use strict';

/**
 * The licence token, mirrored from the Kotlin `Licensing` object.
 *
 * A code is `PL5` + twelve payload characters + four checksum characters, all from a
 * 32-letter alphabet without I, O, 0 or 1 so it can be read off a screen. The payload is
 * derived from the buyer's email, so the same purchase always yields the same code and a
 * customer can be re-sent it later.
 *
 * This is an offline entitlement token, not a signature: the app verifies it on the phone
 * without a network, which means the algorithm is public. The server's job is to decide
 * *who* gets a code (a real paid subscription), not to make forgery impossible.
 */

const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const SALT = 'propslab-premium-2026-v1';
const PREFIX = 'PL5';
const PAYLOAD = 12;
const CHECKSUM = 4;
const MASK = (1n << 64n) - 1n;
const PRIME = 0x100000001b3n;

function fnv(text) {
  let h = 0xcbf29ce484222325n;
  for (const ch of text) {
    h = ((h ^ BigInt(ch.codePointAt(0))) * PRIME) & MASK;
  }
  return h;
}

function payloadFor(email) {
  let value = fnv(String(email).trim().toLowerCase());
  let out = '';
  for (let i = 0; i < PAYLOAD; i += 1) {
    out += ALPHABET[Number(value & 0x1fn)];
    value = ((value >> 5n) ^ ((value * PRIME) & MASK)) & MASK;
  }
  return out;
}

function checksum(body) {
  const h = fnv(body + SALT);
  let value = h;
  let out = '';
  for (let i = 0; i < CHECKSUM; i += 1) {
    out += ALPHABET[Number(value & 0x1fn)];
    value = ((value >> 5n) | ((h << 3n) & MASK)) & MASK;
  }
  return out;
}

function normalise(raw) {
  return String(raw || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
}

function verifyCode(code) {
  const clean = normalise(code);
  if (clean.length !== PREFIX.length + PAYLOAD + CHECKSUM) return false;
  if (!clean.startsWith(PREFIX)) return false;
  const body = clean.slice(PREFIX.length, PREFIX.length + PAYLOAD);
  for (const ch of body) if (!ALPHABET.includes(ch)) return false;
  return clean.slice(PREFIX.length + PAYLOAD) === checksum(body);
}

function licenceFor(email) {
  const body = payloadFor(email);
  const grouped = body.match(/.{1,4}/g).join('-');
  return `${PREFIX}-${grouped}-${checksum(body)}`;
}

/** Best-effort per-instance rate limit; serverless instances are not shared. */
const hits = new Map();
function rateLimited(ip, limit = 12, windowMs = 60_000) {
  const now = Date.now();
  const entry = hits.get(ip) || { at: now, count: 0 };
  if (now - entry.at > windowMs) {
    entry.at = now;
    entry.count = 0;
  }
  entry.count += 1;
  hits.set(ip, entry);
  if (hits.size > 5_000) hits.clear();
  return entry.count > limit;
}

module.exports = { licenceFor, verifyCode, payloadFor, normalise, rateLimited, ALPHABET };
