package com.xprokeey2.presentation.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle

/** [sensitive] values (passwords, card numbers, CVCs) are hidden from Android 13+ clipboard previews. */
fun copyToClipboard(context: Context, label: String, value: String, sensitive: Boolean) {
    val clip = ClipData.newPlainText(label, value)
    if (sensitive) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
}
