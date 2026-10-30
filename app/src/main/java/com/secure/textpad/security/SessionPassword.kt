package com.secure.textpad.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** RAM-only sealed credential for an explicitly unlocked document; no password is written to disk. */
class SessionPassword private constructor(private val nonce: ByteArray, private val sealed: ByteArray) {
    private var cleared = false
    @Synchronized fun open(): CharArray {
        check(!cleared) { "Unlock the document again to resume auto-save." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, nonce))
        val bytes = cipher.doFinal(sealed)
        return try {
            val decoded = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes))
            try { CharArray(decoded.remaining()).also { decoded.get(it) } }
            finally { if (decoded.hasArray()) decoded.array().fill('\u0000') }
        } finally { bytes.fill(0) }
    }
    @Synchronized fun clear() { nonce.fill(0); sealed.fill(0); cleared = true }
    companion object {
        private const val ALIAS = "txt.ram.session.password.v1"
        @Synchronized private fun wrappingKey(): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            return (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setRandomizedEncryptionRequired(true).build())
            }.generateKey()
        }
        fun seal(password: CharArray): SessionPassword {
            var encoded: ByteBuffer? = null
            try {
                encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password))
                val bytes = ByteArray(encoded.remaining())
                try {
                    encoded.get(bytes)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
                    return SessionPassword(cipher.iv.copyOf(), cipher.doFinal(bytes))
                } finally { bytes.fill(0) }
            } finally {
                password.fill('\u0000')
                if (encoded?.hasArray() == true) encoded.array().fill(0)
            }
        }
    }
}
