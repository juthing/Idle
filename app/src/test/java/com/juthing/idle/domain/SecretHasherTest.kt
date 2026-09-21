package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.core.crypto.SecretHasher
import org.junit.Test

class SecretHasherTest {

    @Test
    fun `hashing is stable across calls`() {
        assertThat(SecretHasher.hash("idle")).isEqualTo(SecretHasher.hash("idle"))
    }

    @Test
    fun `hashing matches the known SHA-256 of the empty string`() {
        assertThat(SecretHasher.hash(""))
            .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }

    @Test
    fun `different payloads hash differently`() {
        assertThat(SecretHasher.hash("idle-a")).isNotEqualTo(SecretHasher.hash("idle-b"))
    }

    @Test
    fun `matching compares a payload against a stored digest`() {
        val stored = SecretHasher.hash("idle-fridge")

        assertThat(SecretHasher.matches("idle-fridge", stored)).isTrue()
        assertThat(SecretHasher.matches("idle-door", stored)).isFalse()
    }

    @Test
    fun `a digest of the wrong length never matches`() {
        assertThat(SecretHasher.matches("idle", "abc")).isFalse()
    }
}
