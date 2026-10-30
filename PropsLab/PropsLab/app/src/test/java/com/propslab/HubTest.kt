package com.propslab

import com.propslab.billing.Licensing
import com.propslab.billing.TierStore
import com.propslab.hub.ModelHub
import com.propslab.hub.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shop and the unlock, tested without Android.
 *
 * These are the two places where a wrong character costs a customer money or trust: a
 * licence that refuses a valid code, and an updater that offers a downgrade. Both are pure
 * functions, so both are pinned here.
 */
class HubTest {

    @Test
    fun licenceIssuedForAnEmailIsAccepted() {
        val code = TierStore.issue("buyer@example.com")
        assertTrue("expected a PL5 code, got $code", code.startsWith("PL5-"))
        assertTrue(Licensing.verify(code))
    }

    @Test
    fun theIssuerMatchesTheOwnersScript() {
        // tools/issue_licence.py prints this exact line for the same address. If either
        // implementation drifts, a buyer's code stops working, so the contract is pinned.
        assertEquals("PL5-XHG9-V5TJ-N2KQ-4699", TierStore.issue("buyer@example.com"))
    }

    @Test
    fun licenceIsStableAndIgnoresCaseAndPunctuation() {
        val a = TierStore.issue("Buyer@Example.com")
        val b = TierStore.issue("buyer@example.com")
        assertEquals(a, b)
        val sloppy = " " + a.lowercase().replace("-", " ") + " "
        assertTrue(Licensing.verify(sloppy))
    }

    @Test
    fun oneWrongCharacterIsRefused() {
        val code = TierStore.issue("buyer@example.com")
        val body = code.replace("-", "")
        // Flip a payload character to another legal one. The checksum must catch it.
        val at = 4
        val replacement = if (body[at] == 'A') 'B' else 'A'
        val tampered = body.substring(0, at) + replacement + body.substring(at + 1)
        assertFalse(Licensing.verify(tampered))
    }

    @Test
    fun inventedAndShortCodesAreRefused() {
        assertFalse(Licensing.verify(""))
        assertFalse(Licensing.verify("PL5-AAAA-AAAA-AAAA-AAAA"))
        assertFalse(Licensing.verify("hello"))
        assertFalse(Licensing.verify("PL5-ABCD-EFGH-JKLM-NPQR"))
    }

    @Test
    fun differentEmailsProduceDifferentCodes() {
        val a = TierStore.issue("a@example.com")
        val b = TierStore.issue("b@example.com")
        assertFalse(a == b)
    }

    @Test
    fun aNewerTagIsOfferedAndAnOlderOneIsNot() {
        assertTrue(Updater.isNewer("v1.2.0", "1.1.9"))
        assertTrue(Updater.isNewer("1.10.0", "1.9.9"))
        assertFalse(Updater.isNewer("v1.0.0", "1.0.0"))
        assertFalse(Updater.isNewer("v0.9.0", "1.0.0"))
        assertFalse(Updater.isNewer("nightly", "1.0.0"))
    }

    @Test
    fun theChecksumMatchesAPublishedDigest() {
        // A model is only renamed into place once its bytes hash to what Hugging Face
        // published. Pin the implementation to a known digest so a broken stream cannot
        // silently accept a truncated file.
        val file = java.io.File.createTempFile("propslab-hash", ".bin")
        try {
            file.writeText("abc")
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ModelHub.sha256(file),
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun versionStringsWithoutDotsStillCompare() {
        assertTrue(Updater.isNewer("2026.10", "2026.9"))
        assertFalse(Updater.isNewer("2026.09", "2026.9"))
    }
}
