package com.xprokeey2.domain.repository

/**
 * Ids of the vault items whose password is weak: the web's `xprokey_weak_vault_ids`
 * (lib/vault-security.ts). Like the web's localStorage it is kept on this device only — an item
 * is marked when its password is saved here, and dropped when the item is deleted.
 */
interface WeakVaultItemRepository {

    suspend fun getWeakItemIds(): Set<Long>

    /** Web `markVaultItemWeakness`: adds [itemId] when [isWeak], removes it otherwise. */
    suspend fun markWeakness(itemId: Long, isWeak: Boolean)

    /** Web `removeVaultItemWeakness`. */
    suspend fun remove(itemId: Long)
}
