package com.xprokeey2.presentation.legal

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Static legal page (Terms of Service / Privacy Policy) rendered by [LegalDocumentScreen]. */
data class LegalDocument(
    @param:DrawableRes val badgeIcon: Int,
    @param:StringRes val badge: Int,
    @param:StringRes val titlePrefix: Int,
    @param:StringRes val titleHighlight: Int,
    @param:StringRes val subtitle: Int,
    val sections: List<LegalSection>,
)

data class LegalSection(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    val blocks: List<LegalBlock>,
    @param:StringRes val subtitle: Int? = null,
    /** The emphasised card at the top of the Privacy Policy. */
    val highlighted: Boolean = false,
)

sealed interface LegalBlock {
    data class Paragraph(@param:StringRes val text: Int) : LegalBlock

    /** Paragraph with a bold middle part. */
    data class RichParagraph(
        @param:StringRes val prefix: Int,
        @param:StringRes val bold: Int,
        @param:StringRes val suffix: Int,
    ) : LegalBlock

    data class Bullets(val items: List<Int>) : LegalBlock
    data class SubHeading(@param:StringRes val text: Int) : LegalBlock

    /** Tinted callout, e.g. "Important: ...". */
    data class Note(@param:StringRes val label: Int, @param:StringRes val text: Int) : LegalBlock

    /** Bordered box with an uppercase heading and bullets (SERVICE / ACCOUNT). */
    data class LabeledList(@param:StringRes val heading: Int, val items: List<Int>) : LegalBlock

    data object ContactEmail : LegalBlock
}
