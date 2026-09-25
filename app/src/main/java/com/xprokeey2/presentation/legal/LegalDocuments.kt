package com.xprokeey2.presentation.legal

import com.xprokeey2.R
import com.xprokeey2.presentation.legal.LegalBlock.Bullets
import com.xprokeey2.presentation.legal.LegalBlock.ContactEmail
import com.xprokeey2.presentation.legal.LegalBlock.LabeledList
import com.xprokeey2.presentation.legal.LegalBlock.Note
import com.xprokeey2.presentation.legal.LegalBlock.Paragraph
import com.xprokeey2.presentation.legal.LegalBlock.RichParagraph
import com.xprokeey2.presentation.legal.LegalBlock.SubHeading

/** Content transcribed from the web app's Terms of Service page. */
val TermsOfService = LegalDocument(
    badgeIcon = R.drawable.ic_file_text,
    badge = R.string.terms_badge,
    titlePrefix = R.string.terms_title_prefix,
    titleHighlight = R.string.terms_title_highlight,
    subtitle = R.string.terms_subtitle,
    sections = listOf(
        LegalSection(
            icon = R.drawable.ic_shield,
            title = R.string.terms_acceptance_title,
            blocks = listOf(Paragraph(R.string.terms_acceptance_body)),
        ),
        LegalSection(
            icon = R.drawable.ic_database,
            title = R.string.terms_service_title,
            blocks = listOf(
                Paragraph(R.string.terms_service_body),
                Bullets(
                    listOf(
                        R.string.terms_service_item_1,
                        R.string.terms_service_item_2,
                        R.string.terms_service_item_3,
                        R.string.terms_service_item_4,
                        R.string.terms_service_item_5,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_user,
            title = R.string.terms_responsibilities_title,
            blocks = listOf(
                Bullets(
                    listOf(
                        R.string.terms_responsibilities_item_1,
                        R.string.terms_responsibilities_item_2,
                        R.string.terms_responsibilities_item_3,
                        R.string.terms_responsibilities_item_4,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_credit_card,
            title = R.string.terms_billing_title,
            blocks = listOf(
                Bullets(
                    listOf(
                        R.string.terms_billing_item_1,
                        R.string.terms_billing_item_2,
                        R.string.terms_billing_item_3,
                        R.string.terms_billing_item_4,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_building,
            title = R.string.terms_business_title,
            blocks = listOf(
                Bullets(
                    listOf(
                        R.string.terms_business_item_1,
                        R.string.terms_business_item_2,
                        R.string.terms_business_item_3,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_lock,
            title = R.string.terms_security_title,
            blocks = listOf(
                Paragraph(R.string.terms_security_body_1),
                Paragraph(R.string.terms_security_body_2),
                Paragraph(R.string.terms_security_body_3),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_alert_circle,
            title = R.string.terms_prohibited_title,
            blocks = listOf(
                Bullets(
                    listOf(
                        R.string.terms_prohibited_item_1,
                        R.string.terms_prohibited_item_2,
                        R.string.terms_prohibited_item_3,
                        R.string.terms_prohibited_item_4,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_scale,
            title = R.string.terms_liability_title,
            blocks = listOf(
                Paragraph(R.string.terms_liability_body_1),
                Paragraph(R.string.terms_liability_body_2),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_file_text,
            title = R.string.terms_termination_title,
            blocks = listOf(Paragraph(R.string.terms_termination_body)),
        ),
        LegalSection(
            icon = R.drawable.ic_scale,
            title = R.string.terms_law_title,
            blocks = listOf(Paragraph(R.string.terms_law_body)),
        ),
        LegalSection(
            icon = R.drawable.ic_mail,
            title = R.string.terms_contact_title,
            blocks = listOf(ContactEmail),
        ),
    ),
)

/** Content transcribed from the web app's Privacy Policy page. */
val PrivacyPolicy = LegalDocument(
    badgeIcon = R.drawable.ic_shield,
    badge = R.string.privacy_badge,
    titlePrefix = R.string.privacy_title_prefix,
    titleHighlight = R.string.privacy_title_highlight,
    subtitle = R.string.privacy_subtitle,
    sections = listOf(
        LegalSection(
            icon = R.drawable.ic_lock,
            title = R.string.privacy_zk_title,
//            highlighted = true,
            blocks = listOf(
                RichParagraph(
                    prefix = R.string.privacy_zk_body_prefix,
                    bold = R.string.privacy_zk_body_bold,
                    suffix = R.string.privacy_zk_body_suffix,
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_database,
            title = R.string.privacy_collect_title,
            subtitle = R.string.privacy_collect_subtitle,
            blocks = listOf(
                SubHeading(R.string.privacy_collect_account_heading),
                Bullets(
                    listOf(
                        R.string.privacy_collect_account_item_1,
                        R.string.privacy_collect_account_item_2,
                        R.string.privacy_collect_account_item_3,
                    )
                ),
                SubHeading(R.string.privacy_collect_vault_heading),
                Bullets(
                    listOf(
                        R.string.privacy_collect_vault_item_1,
                        R.string.privacy_collect_vault_item_2,
                        R.string.privacy_collect_vault_item_3,
                    )
                ),
                Note(label = R.string.privacy_collect_note_label, text = R.string.privacy_collect_note),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_eye,
            title = R.string.privacy_use_title,
            subtitle = R.string.privacy_use_subtitle,
            blocks = listOf(
                LabeledList(
                    heading = R.string.privacy_use_service_heading,
                    items = listOf(
                        R.string.privacy_use_service_item_1,
                        R.string.privacy_use_service_item_2,
                        R.string.privacy_use_service_item_3,
                        R.string.privacy_use_service_item_4,
                    ),
                ),
                LabeledList(
                    heading = R.string.privacy_use_account_heading,
                    items = listOf(
                        R.string.privacy_use_account_item_1,
                        R.string.privacy_use_account_item_2,
                        R.string.privacy_use_account_item_3,
                        R.string.privacy_use_account_item_4,
                    ),
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_shield,
            title = R.string.privacy_security_title,
            subtitle = R.string.privacy_security_subtitle,
            blocks = listOf(
                Bullets(
                    listOf(
                        R.string.privacy_security_item_1,
                        R.string.privacy_security_item_2,
                        R.string.privacy_security_item_3,
                        R.string.privacy_security_item_4,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_users,
            title = R.string.privacy_sharing_title,
            subtitle = R.string.privacy_sharing_subtitle,
            blocks = listOf(Paragraph(R.string.privacy_sharing_body)),
        ),
        LegalSection(
            icon = R.drawable.ic_globe,
            title = R.string.privacy_rights_title,
            subtitle = R.string.privacy_rights_subtitle,
            blocks = listOf(
                Bullets(
                    listOf(
                        R.string.privacy_rights_item_1,
                        R.string.privacy_rights_item_2,
                        R.string.privacy_rights_item_3,
                    )
                ),
            ),
        ),
        LegalSection(
            icon = R.drawable.ic_server,
            title = R.string.privacy_retention_title,
            blocks = listOf(Paragraph(R.string.privacy_retention_body)),
        ),
        LegalSection(
            icon = R.drawable.ic_mail,
            title = R.string.privacy_contact_title,
            blocks = listOf(ContactEmail),
        ),
    ),
)
