package com.peoplemanager.adapters.encryption

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Base64

class AesGcmEncryptionAdapterTest {

    private fun generateValidKey(): String {
        val keyBytes = ByteArray(32) { it.toByte() }
        return Base64.getEncoder().encodeToString(keyBytes)
    }

    @Nested
    inner class WhenEncryptionEnabled {

        private val adapter = AesGcmEncryptionAdapter(generateValidKey())

        @Test
        fun `should report encryption as enabled`() {
            adapter.isEnabled() shouldBe true
        }

        @Test
        fun `should encrypt plaintext to different ciphertext`() {
            val plaintext = "This is sensitive content about a team member"
            val ciphertext = adapter.encrypt(plaintext)

            ciphertext shouldNotBe null
            ciphertext shouldNotBe plaintext
            ciphertext!! shouldNotContain plaintext
        }

        @Test
        fun `should decrypt ciphertext back to original plaintext`() {
            val plaintext = "Confidential: performance concerns discussed"
            val ciphertext = adapter.encrypt(plaintext)
            val decrypted = adapter.decrypt(ciphertext)

            decrypted shouldBe plaintext
        }

        @Test
        fun `should handle null plaintext`() {
            adapter.encrypt(null) shouldBe null
        }

        @Test
        fun `should handle null ciphertext`() {
            adapter.decrypt(null) shouldBe null
        }

        @Test
        fun `should handle empty string`() {
            val ciphertext = adapter.encrypt("")
            val decrypted = adapter.decrypt(ciphertext)
            decrypted shouldBe ""
        }

        @Test
        fun `should handle unicode content`() {
            val plaintext = "Ñoño discussed 日本語 and émojis 🎉🔐"
            val ciphertext = adapter.encrypt(plaintext)
            val decrypted = adapter.decrypt(ciphertext)

            decrypted shouldBe plaintext
        }

        @Test
        fun `should handle long content`() {
            val plaintext = "A".repeat(10_000)
            val ciphertext = adapter.encrypt(plaintext)
            val decrypted = adapter.decrypt(ciphertext)

            decrypted shouldBe plaintext
        }

        @Test
        fun `should produce different ciphertext for same plaintext due to random IV`() {
            val plaintext = "Same content encrypted twice"
            val ciphertext1 = adapter.encrypt(plaintext)
            val ciphertext2 = adapter.encrypt(plaintext)

            ciphertext1 shouldNotBe ciphertext2
            adapter.decrypt(ciphertext1) shouldBe plaintext
            adapter.decrypt(ciphertext2) shouldBe plaintext
        }

        @Test
        fun `should handle multiline markdown content`() {
            val plaintext = """
                # Meeting Notes
                
                ## Discussion Points
                - Performance review feedback
                - Salary adjustment discussion
                - Personal situation update
                
                > This is **sensitive** content with *markdown* formatting.
            """.trimIndent()

            val ciphertext = adapter.encrypt(plaintext)
            val decrypted = adapter.decrypt(ciphertext)

            decrypted shouldBe plaintext
        }

        @Test
        fun `should gracefully handle non-base64 input on decrypt`() {
            val notBase64 = "This is just plain text, not encrypted"
            val result = adapter.decrypt(notBase64)

            // Should return as-is since it's not valid encrypted data
            result shouldBe notBase64
        }

        @Test
        fun `should gracefully handle too-short base64 input on decrypt`() {
            val shortBase64 = Base64.getEncoder().encodeToString(ByteArray(5))
            val result = adapter.decrypt(shortBase64)

            // Should return as-is since it's too short to be encrypted
            result shouldBe shortBase64
        }

        @Test
        fun `should gracefully handle unknown version byte on decrypt`() {
            // Create data with version byte = 99 (unknown)
            val fakeData = ByteArray(30)
            fakeData[0] = 99
            val fakeBase64 = Base64.getEncoder().encodeToString(fakeData)

            val result = adapter.decrypt(fakeBase64)
            result shouldBe fakeBase64
        }
    }

    @Nested
    inner class WhenEncryptionDisabled {

        private val adapter = AesGcmEncryptionAdapter("")

        @Test
        fun `should report encryption as disabled`() {
            adapter.isEnabled() shouldBe false
        }

        @Test
        fun `should return plaintext unchanged when encrypting`() {
            val plaintext = "This should not be encrypted"
            adapter.encrypt(plaintext) shouldBe plaintext
        }

        @Test
        fun `should return ciphertext unchanged when decrypting`() {
            val text = "This is just plain text"
            adapter.decrypt(text) shouldBe text
        }

        @Test
        fun `should handle null when encrypting`() {
            adapter.encrypt(null) shouldBe null
        }

        @Test
        fun `should handle null when decrypting`() {
            adapter.decrypt(null) shouldBe null
        }
    }

    @Nested
    inner class KeyValidation {

        @Test
        fun `should reject key that is not 32 bytes when decoded`() {
            val shortKey = Base64.getEncoder().encodeToString(ByteArray(16))

            val adapter = AesGcmEncryptionAdapter(shortKey)
            shouldThrow<IllegalArgumentException> {
                adapter.encrypt("test")
            }
        }

        @Test
        fun `should accept exactly 32-byte key`() {
            val validKey = Base64.getEncoder().encodeToString(ByteArray(32))
            val adapter = AesGcmEncryptionAdapter(validKey)

            val ciphertext = adapter.encrypt("test")
            adapter.decrypt(ciphertext) shouldBe "test"
        }
    }

    @Nested
    inner class CrossKeyDecryption {

        @Test
        fun `should not decrypt with a different key`() {
            val key1 = Base64.getEncoder().encodeToString(ByteArray(32) { 1 })
            val key2 = Base64.getEncoder().encodeToString(ByteArray(32) { 2 })

            val adapter1 = AesGcmEncryptionAdapter(key1)
            val adapter2 = AesGcmEncryptionAdapter(key2)

            val plaintext = "Secret data"
            val ciphertext = adapter1.encrypt(plaintext)

            // Decrypting with wrong key should throw (GCM tag mismatch)
            // or return the ciphertext as-is if it catches the exception
            try {
                val result = adapter2.decrypt(ciphertext)
                // If it doesn't throw, it should not return the original plaintext
                result shouldNotBe plaintext
            } catch (e: Exception) {
                // Expected: AEADBadTagException or similar
            }
        }
    }
}
