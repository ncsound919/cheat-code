#!/usr/bin/env python3
"""Issue PropsLab Premium unlock codes for buyers.

The app verifies a code offline, so this script is the issuer: run it with the email a
buyer used at Stripe checkout and send them the line it prints. The algorithm is the same
FNV-1a checksum the Kotlin `Licensing` object implements; keep the two in step.

    python tools/issue_licence.py buyer@example.com

No secret is stored here that is not already readable inside the APK. This is an offline
entitlement, not a signature: it catches typos and invented codes, and a licence server is
the right replacement once there is one.
"""

from __future__ import annotations

import sys

ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
SALT = "propslab-premium-2026-v1"
PREFIX = "PL5"
PAYLOAD = 12
CHECKSUM = 4
MASK = (1 << 64) - 1


def fnv(text: str) -> int:
    h = 0xCBF29CE484222325
    for ch in text:
        h = ((h ^ ord(ch)) * 0x100000001B3) & MASK
    return h


def payload_for(email: str) -> str:
    value = fnv(email.strip().lower())
    out = []
    for _ in range(PAYLOAD):
        out.append(ALPHABET[value & 0x1F])
        value = ((value >> 5) ^ ((value * 0x100000001B3) & MASK)) & MASK
    return "".join(out)


def checksum(body: str) -> str:
    h = fnv(body + SALT)
    value = h
    out = []
    for _ in range(CHECKSUM):
        out.append(ALPHABET[value & 0x1F])
        value = ((value >> 5) | ((h << 3) & MASK)) & MASK
    return "".join(out)


def issue(email: str) -> str:
    body = payload_for(email)
    grouped = "-".join(body[i:i + 4] for i in range(0, PAYLOAD, 4))
    return f"{PREFIX}-{grouped}-{checksum(body)}"


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 1
    for email in argv:
        print(f"{email.strip().lower()}\t{issue(email)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
