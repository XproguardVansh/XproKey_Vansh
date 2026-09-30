package com.xprokeey2.presentation.settings

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * The browser's en-IN short month names. Spelled out because Android versions differ on September
 * ("Sep" or "Sept"), while the web shows "Sept".
 */
private val EnInShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")

/** The web's `toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })`, e.g. "26 Sept 2027". */
fun Instant.billingDate(zone: ZoneId = ZoneId.systemDefault()): String {
    val date = atZone(zone).toLocalDate()
    return "${date.dayOfMonth} ${EnInShortMonths[date.monthValue - 1]} ${date.year}"
}

/** The web's `plan_type.charAt(0).toUpperCase() + plan_type.slice(1)`: "yearly" → "Yearly". */
fun String.capitalizeFirst(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }

/** CSS `capitalize`, used on the status badges: "active" → "Active". */
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalizeFirst() }
