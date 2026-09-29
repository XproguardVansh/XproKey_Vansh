package com.xprokeey2.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The web dashboard's `calcSecurityScore` and `isPasswordWeak` (lib/vault-security.ts). */
class VaultSecurityTest {

    private fun item(id: Long, username: String = "user$id", url: String = "https://site$id.com") =
        VaultItem(id, "Item $id", username, url, "", "Personal", false, null, null)

    private fun calculate(items: List<VaultItem>, markedWeakIds: Set<Long> = emptySet()) =
        VaultSecurity.calculate(items, VaultSecurity.missingFieldsCount(items), markedWeakIds)

    @Test
    fun matchesTheWebDashboardExample() {
        // Github + AppLock, AppLock saved with "walldfs": web shows 80% "Excellent", 1 weak item.
        val items = listOf(item(1, "Vanshgoel2610", "https://github.com"), item(2, "Vansh", "https://applock.com"))
        val security = calculate(items, markedWeakIds = setOf(2))

        assertEquals(80, security.score)
        assertEquals(1, security.weakCount)
        assertEquals(setOf(2L), security.weakItemIds)
        assertEquals(VaultSecurity.Rating.EXCELLENT, security.rating)
    }

    @Test
    fun matchesTheWebForTheEightItemList() {
        // The list from the web screenshot: 4 items without a URL, every username used twice,
        // AppLock (2) marked weak.
        val items = listOf(
            item(1, "Vanshgoel2610", "https://github.com"),
            item(2, "Vansh", "https://applock.com"),
            item(3, "vansh123", ""),
            item(4, "krishna123", ""),
            item(5, "krishna123", ""),
            item(6, "vansh123", ""),
            item(7, "Vansh", "https://applock.com"),
            item(8, "Vanshgoel2610", "https://github.com"),
        )
        val security = calculate(items, markedWeakIds = setOf(2))

        assertEquals(setOf(2L, 3L, 4L, 5L, 6L), security.weakItemIds)
        assertEquals(5, security.weakCount)
        assertEquals(4, security.reusedCount)
        // 100 - (5/8 * 40) - (4/8 * 30) - (4/8 * 30) = 100 - 25 - 15 - 15 = 45
        assertEquals(45, security.score)
        assertEquals(VaultSecurity.Rating.NEEDS_REVIEW, security.rating)
    }

    @Test
    fun missingUsernameOrUrlIsWeakWithoutBeingMarked() {
        val items = listOf(item(1), item(2, url = ""), item(3, username = " "), item(4))
        val security = calculate(items)

        assertEquals(2, VaultSecurity.missingFieldsCount(items))
        assertEquals(setOf(2L, 3L), security.weakItemIds)
        // 100 - (2/4 * 40) - 0 - (2/4 * 30) = 65
        assertEquals(65, security.score)
        assertEquals(VaultSecurity.Rating.GOOD, security.rating)
    }

    @Test
    fun weakCountIsTheLargerOfWeakItemsAndMissingFields() {
        val items = listOf(item(1), item(2), item(3), item(4))
        val security = VaultSecurity.calculate(items, missingFieldsCount = 3, markedWeakIds = setOf(1))

        assertEquals(setOf(1L), security.weakItemIds)
        assertEquals(3, security.weakCount)
        // 100 - (3/4 * 40) - 0 - (3/4 * 30) = 47.5 -> 48
        assertEquals(48, security.score)
    }

    @Test
    fun markedIdsOfOtherItemsAreIgnored() {
        val security = calculate(listOf(item(1), item(2)), markedWeakIds = setOf(99))
        assertEquals(0, security.weakCount)
        assertEquals(100, security.score)
    }

    @Test
    fun reusedCountsExactUsernamesSharedByMoreThanOneItem() {
        val items = listOf(
            item(1, "vansh123"), item(2, "vansh123"),
            item(3, "krishna123"), item(4, "krishna123"),
            item(5, "Vansh"), item(6, "vansh"),
        )
        val security = calculate(items)

        assertEquals(2, security.reusedCount)
        // 100 - 0 - (2/6 * 30) - 0 = 90
        assertEquals(90, security.score)
    }

    @Test
    fun roundsHalvesUpLikeJavaScript() {
        // 100 - (1/16 * 40) = 97.5 -> 98
        val items = (1L..16L).map { item(it) }
        assertEquals(98, calculate(items, markedWeakIds = setOf(1)).score)
    }

    @Test
    fun emptyVaultHasNoScore() {
        val security = VaultSecurity.calculate(emptyList(), missingFieldsCount = 2, markedWeakIds = setOf(1))
        assertFalse(security.hasItems)
        assertEquals(0, security.weakCount)
        assertEquals(VaultSecurity.Rating.NO_ITEMS, security.rating)
    }

    @Test
    fun weakPasswordRule() {
        assertTrue(isPasswordWeak("wall"))
        assertTrue(isPasswordWeak("walldfs")) // no digit, no symbol
        assertTrue(isPasswordWeak("v?vqzb-&hklp&")) // no digit
        assertTrue(isPasswordWeak("Password123")) // no symbol
        assertTrue(isPasswordWeak(""))
        assertFalse(isPasswordWeak("cVkd,FO)w[n09vR8"))
        assertFalse(isPasswordWeak("a1!bc"))
    }
}
