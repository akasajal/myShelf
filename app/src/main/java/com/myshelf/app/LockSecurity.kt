package com.myshelf.app

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKey

object PinCode {
    fun valid(pin: String) = pin.length == 4 && pin.all { it in '0'..'9' }
    fun digest(pin: String, salt: ByteArray, key: SecretKey): ByteArray {
        require(valid(pin))
        return Mac.getInstance("HmacSHA256").run {
            init(key)
            update(salt)
            doFinal(pin.toByteArray(Charsets.US_ASCII))
        }
    }
    fun matches(pin: String, salt: ByteArray, key: SecretKey, expected: ByteArray): Boolean =
        valid(pin) && MessageDigest.isEqual(digest(pin, salt, key), expected)
}

/** Configuration changes do not call background; a fresh process always starts locked. */
class LockSession(private var enabled: Boolean) {
    var locked = enabled
        private set
    private var backgroundAt: Long? = null

    fun enable() { enabled = true; unlock() }
    fun disable() { enabled = false; unlock() }
    fun unlock() { locked = false; backgroundAt = null }
    fun background(now: Long, instant: Boolean) {
        if (!enabled) return
        backgroundAt = now
        if (instant) locked = true
    }
    fun foreground(now: Long) {
        if (enabled && backgroundAt?.let { now - it >= 60_000 } == true) locked = true
        backgroundAt = null
    }
}
