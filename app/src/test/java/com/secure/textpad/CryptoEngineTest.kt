package com.secure.textpad

import com.secure.textpad.security.CryptoEngine
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CryptoEngineTest {
    private fun password() = "correct horse battery staple".toCharArray()
    private fun mustFail(block: () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        assertTrue("Unauthenticated input must fail", failed)
    }
    @Test fun fileRoundTripFreshSaltAndPasswordErasure() {
        val text = "Hello\nবাংলা 🔐".toByteArray()
        val pass = password()
        val a = CryptoEngine.encrypt(text, pass)
        val b = CryptoEngine.encrypt(text, password())
        assertTrue(pass.all { it == '\u0000' })
        assertTrue(CryptoEngine.isEncrypted(a))
        assertEquals(33 + text.size + 16, a.size)
        assertFalse(a.contentEquals(b))
        assertArrayEquals(text, CryptoEngine.decrypt(a, password()))
        mustFail { CryptoEngine.decrypt(a, "wrong".toCharArray()) }
        a[a.lastIndex] = (a.last().toInt() xor 1).toByte()
        mustFail { CryptoEngine.decrypt(a, password()) }
    }
    @Test fun chunkRoundTripRejectsTruncationReorderingAndTrailingData() {
        val text = ByteArray(150000) { (it * 31).toByte() }
        val out = ByteArrayOutputStream()
        CryptoEngine.writeBackup(out, password()) { it.write(text) }
        val encoded = out.toByteArray()
        fun decode(bytes: ByteArray) = CryptoEngine.readBackup(ByteArrayInputStream(bytes), password()) { it.readBytes() }
        assertArrayEquals(text, decode(encoded))
        mustFail { decode(encoded.copyOf(encoded.size - 21)) } // Removes the mandatory final frame.
        mustFail { decode(encoded + byteArrayOf(0)) }
        val swapped = encoded.copyOf()
        val frameSize = 1 + 4 + 65536 + 16
        val first = encoded.copyOfRange(33, 33 + frameSize)
        val second = encoded.copyOfRange(33 + frameSize, 33 + 2 * frameSize)
        second.copyInto(swapped, 33); first.copyInto(swapped, 33 + frameSize)
        mustFail { decode(swapped) }
        val tampered = encoded.copyOf().apply { this[100] = (this[100].toInt() xor 1).toByte() }
        mustFail { decode(tampered) }
    }
    @Test fun emptyBackupStillRequiresAuthenticatedFinalFrame() {
        val out = ByteArrayOutputStream()
        CryptoEngine.writeBackup(out, password()) { }
        val bytes = out.toByteArray()
        assertEquals(54, bytes.size)
        assertEquals(0, CryptoEngine.readBackup(ByteArrayInputStream(bytes), password()) { it.readBytes().size })
        mustFail { CryptoEngine.readBackup(ByteArrayInputStream(bytes.copyOf(33)), password()) { it.readBytes() } }
    }
}
