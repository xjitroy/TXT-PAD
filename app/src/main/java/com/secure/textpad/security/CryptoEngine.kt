package com.secure.textpad.security

import java.io.*
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned formats. Caller owns streams; every supplied password is consumed and erased. */
object CryptoEngine {
    const val ITERATIONS = 600_000
    const val CHUNK_SIZE = 65_536
    private val random = SecureRandom()
    private val magic = byteArrayOf(83, 84, 88, 84)
    private val backupMagic = byteArrayOf(84, 80, 66, 75)

    fun isEncrypted(prefix: ByteArray) = prefix.size >= 4 && (0..3).all { prefix[it] == magic[it] }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword(); password.fill('\u0000') }
    }

    private fun header(backup: Boolean): ByteArray =
        (if (backup) backupMagic else magic) + byteArrayOf(1) +
            ByteArray(16).also(random::nextBytes) + ByteArray(12).also(random::nextBytes)

    private fun readHeader(input: InputStream, backup: Boolean): ByteArray {
        val h = ByteArray(33)
        DataInputStream(input).readFully(h)
        require(h.copyOfRange(0, 4).contentEquals(if (backup) backupMagic else magic) && h[4] == 1.toByte()) {
            "Unsupported or damaged ${if (backup) "backup" else "encrypted file"}."
        }
        return h
    }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            updateAAD(aad)
        }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        var key: ByteArray? = null
        return try {
            val h = header(false)
            key = derive(password, h.copyOfRange(5, 21))
            h + cipher(Cipher.ENCRYPT_MODE, key, h.copyOfRange(21, 33), h).doFinal(plain)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    fun decrypt(encoded: ByteArray, password: CharArray): ByteArray {
        var key: ByteArray? = null
        return try {
            require(encoded.size >= 49) { "Truncated encrypted file." }
            val h = readHeader(ByteArrayInputStream(encoded), false)
            key = derive(password, h.copyOfRange(5, 21))
            cipher(Cipher.DECRYPT_MODE, key, h.copyOfRange(21, 33), h).doFinal(encoded, 33, encoded.size - 33)
        } catch (e: GeneralSecurityException) {
            throw IOException("Wrong password or damaged encrypted file.", e)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    private fun nonce(h: ByteArray, counter: Long, final: Boolean): ByteArray {
        require(counter >= 0 && counter < Long.MAX_VALUE) { "Backup is too large." }
        return h.copyOfRange(21, 33).apply {
            if (final) this[0] = (this[0].toInt() xor 0x80).toByte()
            for (i in 0..7) this[4 + i] = (this[4 + i].toInt() xor (counter ushr (56 - i * 8)).toInt()).toByte()
        }
    }

    private fun aad(h: ByteArray, counter: Long, final: Boolean, length: Int): ByteArray =
        h + ByteBuffer.allocate(13).putLong(counter).put(if (final) 1.toByte() else 0.toByte()).putInt(length).array()

    /** Closes the encrypted wrapper even when the ZIP producer fails; caller deletes incomplete output. */
    fun writeBackup(output: OutputStream, password: CharArray, producer: (OutputStream) -> Unit) {
        var key: ByteArray? = null
        try {
            val h = header(true)
            key = derive(password, h.copyOfRange(5, 21))
            output.write(h)
            ChunkOutput(output, h, key).use(producer)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    /** The consumer must finish successfully AND the final authenticated frame must validate. */
    fun <T> readBackup(input: InputStream, password: CharArray, consumer: (InputStream) -> T): T {
        var key: ByteArray? = null
        try {
            val h = readHeader(input, true)
            key = derive(password, h.copyOfRange(5, 21))
            val stream = ChunkInput(input, h, key)
            return try {
                val value = consumer(stream)
                val drain = ByteArray(CHUNK_SIZE)
                try { while (stream.read(drain) != -1) Unit } finally { drain.fill(0) }
                value
            } finally { stream.erase() }
        } catch (e: GeneralSecurityException) {
            throw IOException("Wrong backup password or damaged backup.", e)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    private class ChunkOutput(val target: OutputStream, val h: ByteArray, val key: ByteArray) : OutputStream() {
        private val buffer = ByteArray(CHUNK_SIZE)
        private var used = 0
        private var counter = 0L
        private var closed = false
        override fun write(b: Int) { write(byteArrayOf(b.toByte()), 0, 1) }
        override fun write(b: ByteArray, off: Int, len: Int) {
            check(!closed)
            require(off >= 0 && len >= 0 && off <= b.size - len)
            var pos = off
            var left = len
            while (left > 0) {
                val n = minOf(left, CHUNK_SIZE - used)
                b.copyInto(buffer, used, pos, pos + n)
                used += n; pos += n; left -= n
                if (used == CHUNK_SIZE) emit(false)
            }
        }
        private fun emit(final: Boolean) {
            val encrypted = cipher(Cipher.ENCRYPT_MODE, key, nonce(h, counter, final), aad(h, counter, final, used)).doFinal(buffer, 0, used)
            try {
                val data = DataOutputStream(target)
                data.writeByte(if (final) 1 else 0)
                data.writeInt(used)
                data.write(encrypted)
                counter++
            } finally { encrypted.fill(0); buffer.fill(0); used = 0 }
        }
        override fun flush() { target.flush() }
        override fun close() {
            if (closed) return
            try { if (used > 0) emit(false); emit(true); target.flush() }
            finally { closed = true; buffer.fill(0) }
        }
    }

    private class ChunkInput(val source: InputStream, val h: ByteArray, val key: ByteArray) : InputStream() {
        private var buffer = ByteArray(0)
        private var position = 0
        private var counter = 0L
        private var ended = false
        private fun load(): Boolean {
            buffer.fill(0)
            if (ended) return false
            val data = DataInputStream(source)
            val flag = data.readUnsignedByte() // EOF here is an error: final frame is mandatory.
            val len = data.readInt()
            require(flag in 0..1 && len in 0..CHUNK_SIZE && (if (flag == 1) len == 0 else len > 0)) { "Invalid backup frame." }
            val encrypted = ByteArray(len + 16)
            data.readFully(encrypted)
            buffer = try { cipher(Cipher.DECRYPT_MODE, key, nonce(h, counter, flag == 1), aad(h, counter, flag == 1, len)).doFinal(encrypted) }
            finally { encrypted.fill(0) }
            counter++; position = 0
            if (flag == 1) {
                ended = true
                require(source.read() == -1) { "Unexpected data after backup." }
                return false
            }
            return true
        }
        override fun read(): Int {
            if (position == buffer.size && !load()) return -1
            return buffer[position++].toInt() and 0xff
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            require(off >= 0 && len >= 0 && off <= b.size - len)
            if (len == 0) return 0
            if (position == buffer.size && !load()) return -1
            val n = minOf(len, buffer.size - position)
            buffer.copyInto(b, off, position, position + n); position += n
            return n
        }
        fun erase() { buffer.fill(0) }
        // ZIP's close must not bypass validation or close the caller's stream.
        override fun close() { }
    }
}
