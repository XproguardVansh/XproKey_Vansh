package com.xprokeey2.domain.model

import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** A saved password ("vault item") as the server lists it: everything except the password. */
data class VaultItem(
    val id: Long,
    val title: String,
    val username: String,
    val url: String,
    /** Plain text, like the web app (only the password is encrypted). */
    val notes: String,
    val category: String,
    val isFavorite: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

/** [VaultItem] as stored: [encryptedPassword] is ciphertext under the vault key. */
data class StoredVaultItem(
    val item: VaultItem,
    val encryptedPassword: String,
)

/** [VaultItem] with its password decrypted; null when it isn't ciphertext under this vault key. */
data class VaultItemDetails(
    val item: VaultItem,
    val password: String?,
)

/** A new password from the Add password form. */
data class VaultItemDraft(
    val title: String,
    val username: String,
    val url: String,
    val password: String,
    val category: String,
    val notes: String,
    val isFavorite: Boolean,
)

/** Edit: only the fields the user changed (null = unchanged, not sent). */
data class VaultItemChanges(
    val title: String? = null,
    val username: String? = null,
    val url: String? = null,
    val password: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = listOf(title, username, url, password, category, notes, isFavorite).all { it == null }
}

/** Body of createvault, with the password already encrypted client-side. */
data class VaultItemPayload(
    val title: String,
    val username: String,
    val url: String,
    val encryptedPassword: String,
    val category: String,
    val notes: String,
    val isFavorite: Boolean,
)

/** Body of updatevault: only changed fields (null = not sent). */
data class VaultItemUpdatePayload(
    val title: String? = null,
    val username: String? = null,
    val url: String? = null,
    val encryptedPassword: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean? = null,
)

/**
 * Web `isPasswordWeak` (tech doc, lib/vault-security.ts): shorter than 5, no digit or no special
 * character.
 */
fun isPasswordWeak(password: String?): Boolean {
    if (password.isNullOrEmpty() || password.length < 5) return true
    // Same ASCII classes as the web regexes /[0-9]/ and /[^A-Za-z0-9]/.
    if (password.none { it in '0'..'9' }) return true
    if (password.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }) return true
    return false
}

/**
 * Dashboard security numbers, computed exactly like the web dashboard's `calcSecurityScore`
 * (app/(app)/dashboard/page.tsx).
 */
data class VaultSecurity(
    val totalItems: Int,
    /** Web `weakItems`: no username, no website URL, or marked weak. */
    val weakItemIds: Set<Long>,
    /** The "Weak items" number: the larger of [weakItemIds] and the missing-fields count. */
    val weakCount: Int,
    /** Usernames used by more than one item. */
    val reusedCount: Int,
    /** 0-100; meaningless when [hasItems] is false. */
    val score: Int,
) {
    val hasItems: Boolean get() = totalItems > 0

    enum class Rating { EXCELLENT, GOOD, NEEDS_REVIEW, NO_ITEMS }

    val rating: Rating
        get() = when {
            !hasItems -> Rating.NO_ITEMS
            score >= 80 -> Rating.EXCELLENT
            score >= 50 -> Rating.GOOD
            else -> Rating.NEEDS_REVIEW
        }

    companion object {
        /** Web `weakItems`: `!username?.trim() || !url?.trim() || weakItemIds.has(id)`. */
        fun weakItemIds(items: List<VaultItem>, markedWeakIds: Set<Long>): Set<Long> = items
            .filter { it.username.isBlank() || it.url.isBlank() || it.id in markedWeakIds }
            .mapTo(mutableSetOf()) { it.id }

        /**
         * Items with no website URL or no username ("Missing field (URL or Username)"). The web
         * reads this number as `missing_fields_count` from GET /dashboard/summary.
         */
        fun missingFieldsCount(items: List<VaultItem>): Int =
            items.count { it.username.isBlank() || it.url.isBlank() }

        /**
         * `calcSecurityScore(items, missingFieldsCount, weakItemIds)`: 100 minus up to 40 for weak
         * items, 30 for reused usernames and 30 for missing fields, each in proportion to the
         * number of items. [markedWeakIds] is the web's `getWeakVaultItemIds()`.
         */
        fun calculate(items: List<VaultItem>, missingFieldsCount: Int, markedWeakIds: Set<Long>): VaultSecurity {
            if (items.isEmpty()) return VaultSecurity(0, emptySet(), 0, 0, 0)

            val weakIds = weakItemIds(items, markedWeakIds)
            val weakCount = max(weakIds.size, missingFieldsCount)
            // `if (it.username)`: empty usernames aren't counted; the rest are compared as is.
            val reusedCount = items
                .filter { it.username.isNotEmpty() }
                .groupingBy { it.username }
                .eachCount()
                .count { it.value > 1 }

            val total = items.size.toDouble()
            val weakPenalty = min(weakCount / total * 40, 40.0)
            val reusedPenalty = min(reusedCount / total * 30, 30.0)
            val missingPenalty = min(missingFieldsCount / total * 30, 30.0)
            // JavaScript Math.round: halves round up.
            val score = max(0, floor(100 - weakPenalty - reusedPenalty - missingPenalty + 0.5).toInt())
            return VaultSecurity(items.size, weakIds, weakCount, reusedCount, score)
        }
    }
}
