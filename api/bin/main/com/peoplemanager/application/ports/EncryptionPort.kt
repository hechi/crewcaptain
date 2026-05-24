package com.peoplemanager.application.ports

/**
 * Port interface for encrypting and decrypting sensitive text content.
 * Implementations must use authenticated encryption (e.g., AES-256-GCM).
 */
interface EncryptionPort {

    /**
     * Encrypts the given plaintext and returns a ciphertext string
     * (Base64-encoded, including IV/metadata).
     * Returns null if plaintext is null.
     */
    fun encrypt(plaintext: String?): String?

    /**
     * Decrypts the given ciphertext string and returns the original plaintext.
     * Returns null if ciphertext is null.
     */
    fun decrypt(ciphertext: String?): String?

    /**
     * Returns true if encryption is enabled (i.e., an encryption key is configured).
     */
    fun isEnabled(): Boolean
}
