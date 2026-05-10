package com.peoplemanager.adapters.encryption

import com.peoplemanager.application.ports.EncryptionPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption adapter for sensitive content.
 *
 * Ciphertext format (Base64-encoded):
 *   [1 byte version][12 bytes IV][N bytes ciphertext+tag]
 *
 * The version byte allows future algorithm changes without breaking existing data.
 */
@Component
class AesGcmEncryptionAdapter(
    @Value("\${app.encryption.key:}") private val encryptionKeyBase64: String
) : EncryptionPort {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val VERSION: Byte = 1
    }

    private val secretKey: SecretKeySpec? by lazy {
        if (encryptionKeyBase64.isBlank()) {
            null
        } else {
            val keyBytes = Base64.getDecoder().decode(encryptionKeyBase64)
            require(keyBytes.size == 32) {
                "ENCRYPTION_KEY must be exactly 32 bytes (256 bits) when Base64-decoded. Got ${keyBytes.size} bytes."
            }
            SecretKeySpec(keyBytes, KEY_ALGORITHM)
        }
    }

    private val secureRandom = SecureRandom()

    override fun isEnabled(): Boolean = encryptionKeyBase64.isNotBlank()

    override fun encrypt(plaintext: String?): String? {
        if (plaintext == null) return null
        val key = secretKey ?: return plaintext // If no key configured, return plaintext

        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Format: [version][iv][ciphertext+tag]
        val output = ByteArray(1 + GCM_IV_LENGTH + ciphertext.size)
        output[0] = VERSION
        System.arraycopy(iv, 0, output, 1, GCM_IV_LENGTH)
        System.arraycopy(ciphertext, 0, output, 1 + GCM_IV_LENGTH, ciphertext.size)

        return Base64.getEncoder().encodeToString(output)
    }

    override fun decrypt(ciphertext: String?): String? {
        if (ciphertext == null) return null
        val key = secretKey ?: return ciphertext // If no key configured, return as-is

        val decoded = try {
            Base64.getDecoder().decode(ciphertext)
        } catch (e: IllegalArgumentException) {
            // Not Base64 — likely unencrypted legacy data
            return ciphertext
        }

        // Minimum size: 1 (version) + 12 (IV) + 16 (GCM tag) = 29 bytes
        if (decoded.size < 29) {
            // Too short to be encrypted data — return as-is (legacy unencrypted)
            return ciphertext
        }

        val version = decoded[0]
        if (version != VERSION) {
            // Unknown version — return as-is (could be unencrypted legacy data)
            return ciphertext
        }

        val iv = decoded.copyOfRange(1, 1 + GCM_IV_LENGTH)
        val encryptedData = decoded.copyOfRange(1 + GCM_IV_LENGTH, decoded.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        val plaintext = cipher.doFinal(encryptedData)
        return String(plaintext, Charsets.UTF_8)
    }
}
