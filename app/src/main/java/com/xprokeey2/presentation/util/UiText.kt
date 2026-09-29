package com.xprokeey2.presentation.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/** Text that is either a raw server message or a string resource, resolved in the UI. */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    class Resource(@param:StringRes val id: Int, vararg val args: Any) : UiText
    class Plural(@param:PluralsRes val id: Int, val quantity: Int, vararg val args: Any) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args)
        is Plural -> pluralStringResource(id, quantity, *args)
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args)
        is Plural -> context.resources.getQuantityString(id, quantity, *args)
    }
}
