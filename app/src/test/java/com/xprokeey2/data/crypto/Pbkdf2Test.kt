package com.xprokeey2.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

/** Expected values from Python's hashlib.pbkdf2_hmac("sha256", ...). */
class Pbkdf2Test {

    private fun derive(password: String, salt: String, iterations: Int, length: Int) =
        Pbkdf2.deriveKey(password.toByteArray(), salt.toByteArray(), iterations, length)
            .joinToString("") { "%02x".format(it) }

    @Test
    fun singleBlock() {
        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            derive("password", "salt", 1, 32),
        )
    }

    @Test
    fun multipleBlocks() {
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc" +
                "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
            derive("passwd", "salt", 1, 64),
        )
    }

    @Test
    fun manyIterations() {
        assertEquals(
            "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56" +
                "a1d425a1225833549adb841b51c9b3176a272bdebba1d078478f62b397f33c8d",
            derive("Password", "NaCl", 80_000, 64),
        )
    }
}
