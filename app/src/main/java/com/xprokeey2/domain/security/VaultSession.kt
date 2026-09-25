package com.xprokeey2.domain.security

/**
 * Holds the unlocked vault key in memory only (the web app keeps it in sessionStorage).
 * It is gone when the process dies or the user logs out.
 */
interface VaultSession {
    val vaultKey: String?
    fun unlock(vaultKey: String)
    fun lock()
}
