package com.juthing.idle.core.crypto

import java.security.MessageDigest

/**
 * Hashes the payload of a QR code or NFC tag.
 *
 * Only the digest is ever stored, so someone reading the database learns whether a scan matches
 * but cannot reconstruct the code they would need to present.
 */
object SecretHasher {

    /** SHA-256 of [value], lowercase hexadecimal. */
    fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    /**
     * Compares a freshly scanned [value] against a stored [expectedHash].
     *
     * The digests are compared byte by byte without short-circuiting. Timing is not a realistic
     * threat here, but constant-time comparison costs nothing and removes the question.
     */
    fun matches(value: String, expectedHash: String): Boolean {
        val actual = hash(value)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) {
            diff = diff or (actual[i].code xor expectedHash[i].code)
        }
        return diff == 0
    }
}
