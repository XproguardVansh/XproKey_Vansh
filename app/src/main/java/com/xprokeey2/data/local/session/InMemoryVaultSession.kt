package com.xprokeey2.data.local.session

import com.xprokeey2.domain.security.VaultSession
import javax.inject.Inject
import javax.inject.Singleton

/** Never written to disk, the equivalent of the web app's sessionStorage `vault_key`. */
@Singleton
class InMemoryVaultSession @Inject constructor() : VaultSession {

    @Volatile
    override var vaultKey: String? = null
        private set

    override fun unlock(vaultKey: String) {
        this.vaultKey = vaultKey
    }

    override fun lock() {
        vaultKey = null
    }
}
