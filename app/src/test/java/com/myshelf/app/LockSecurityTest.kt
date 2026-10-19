package com.myshelf.app

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class LockSecurityTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
    @Test fun pinRequiresExactlyFourAsciiDigits() {
        assertTrue(PinCode.valid("0042"))
        listOf("", "123", "12345", "12a4", "１２３４", " 123").forEach { assertFalse(PinCode.valid(it)) }
    }
    @Test fun verifierChecksPinSaltAndDeviceKey() {
        val salt = byteArrayOf(1, 2, 3)
        val verifier = PinCode.digest("0042", salt, key)
        assertTrue(PinCode.matches("0042", salt, key, verifier))
        assertFalse(PinCode.matches("0043", salt, key, verifier))
        assertFalse(PinCode.matches("0042", byteArrayOf(3, 2, 1), key, verifier))
        assertFalse(PinCode.matches("0042", salt, SecretKeySpec(ByteArray(32), "HmacSHA256"), verifier))
    }
    @Test fun coldStartIsLockedOnlyWhenEnabled() {
        assertTrue(LockSession(true).locked)
        assertFalse(LockSession(false).locked)
    }
    @Test fun instantLockAppliesBeforeReturningToForeground() {
        val session = LockSession(true)
        session.unlock()
        session.background(1000, instant = true)
        assertTrue(session.locked)
        session.foreground(1001)
        assertTrue(session.locked)
    }
    @Test fun delayedLockPreservesShortTripsButLocksAfterOneMinute() {
        val session = LockSession(true)
        session.unlock()
        session.background(1000, instant = false)
        session.foreground(60_999)
        assertFalse(session.locked)
        session.background(70_000, instant = false)
        session.foreground(130_000)
        assertTrue(session.locked)
    }
    @Test fun disablingClearsLockAndIgnoresBackgroundEvents() {
        val session = LockSession(true)
        session.disable()
        session.background(0, instant = true)
        session.foreground(100_000)
        assertFalse(session.locked)
    }
    @Test fun returningWithoutBackgroundDoesNotLockUnlockedSession() {
        val session = LockSession(true)
        session.unlock()
        session.foreground(100_000)
        assertFalse(session.locked)
    }
}
