package com.myshelf.app.domain.security

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

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
        session.background()
        assertTrue(session.locked)
    }
    @Test fun newlyEnabledLockAppliesOnTheNextBackgroundEvent() {
        val session = LockSession(false)
        session.enable()
        assertFalse(session.locked)
        session.background()
        assertTrue(session.locked)
    }
    @Test fun disablingClearsLockAndIgnoresBackgroundEvents() {
        val session = LockSession(true)
        session.disable()
        session.background()
        assertFalse(session.locked)
    }
    @Test fun unlockingLastsUntilTheNextBackgroundEvent() {
        val session = LockSession(true)
        session.unlock()
        assertFalse(session.locked)
        session.background()
        assertTrue(session.locked)
        session.unlock()
        assertFalse(session.locked)
    }
}
