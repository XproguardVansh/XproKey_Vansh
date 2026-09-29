package com.xprokeey2.presentation.about

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

/** One question; [answer] null is the refund request answer, which has links. */
private data class FaqItem(@param:StringRes val question: Int, @param:StringRes val answer: Int?)

private data class FaqCategory(@param:StringRes val title: Int, val items: List<FaqItem>)

/** The web FAQ page's sections and questions, in its order. */
private val FaqCategories = listOf(
    FaqCategory(
        R.string.faq_cat_security,
        listOf(
            FaqItem(R.string.faq_q_employees, R.string.faq_a_employees),
            FaqItem(R.string.faq_q_breach, R.string.faq_a_breach),
        ),
    ),
    FaqCategory(
        R.string.faq_cat_recovery,
        listOf(
            FaqItem(R.string.faq_q_forgot, R.string.faq_a_forgot),
            FaqItem(R.string.faq_q_recovery_key, R.string.faq_a_recovery_key),
            FaqItem(R.string.faq_q_recovery_works, R.string.faq_a_recovery_works),
        ),
    ),
    FaqCategory(
        R.string.faq_cat_privacy,
        listOf(
            FaqItem(R.string.faq_q_stored, R.string.faq_a_stored),
            FaqItem(R.string.faq_q_encrypted, R.string.faq_a_encrypted),
        ),
    ),
    FaqCategory(
        R.string.faq_cat_refunds,
        listOf(
            FaqItem(R.string.faq_q_refund_policy, R.string.faq_a_refund_policy),
            FaqItem(R.string.faq_q_refund_request, null),
            FaqItem(R.string.faq_q_refund_time, R.string.faq_a_refund_time),
            FaqItem(R.string.faq_q_business_refund, R.string.faq_a_business_refund),
        ),
    ),
)

@Composable
fun FaqScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onOpenSupport: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    FaqScreen(
        user = user,
        snackbarHostState = snackbarHostState,
        onSectionClick = onSectionClick,
        onOpenSupport = onOpenSupport,
        onEmail = {
            // The web's mailto:contact@xprokey.com link.
            val address = context.getString(R.string.faq_contact_email)
            try {
                context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri()))
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.faq_email_unavailable)) }
            }
        },
    )
}

@Composable
fun FaqScreen(
    user: UserBadge?,
    snackbarHostState: SnackbarHostState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onOpenSupport: () -> Unit,
    onEmail: () -> Unit,
) {
    val colors = XpTheme.colors
    // Like the web accordions: at most one answer open per section (section index → question index).
    var openItems by rememberSaveable { mutableStateOf(mapOf<Int, Int>()) }

    WorkspaceScaffold(
        user = user,
        currentSection = WorkspaceSection.FAQ,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FaqBadge()
            Spacer(Modifier.height(16.dp))
            val titleStart = stringResource(R.string.faq_title_start)
            val titleHighlight = stringResource(R.string.faq_title_highlight)
            Text(
                text = buildAnnotatedString {
                    append(titleStart)
                    withStyle(SpanStyle(color = colors.primary)) { append(titleHighlight) }
                },
                style = XpTheme.typography.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.faq_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            FaqCategories.forEachIndexed { categoryIndex, category ->
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(category.title).uppercase(),
                        style = XpTheme.typography.fieldLabel.copy(fontSize = 11.sp, letterSpacing = 1.5.sp),
                        color = colors.textLabel,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    )
                    category.items.forEachIndexed { itemIndex, item ->
                        val isOpen = openItems[categoryIndex] == itemIndex
                        FaqAccordionItem(
                            question = stringResource(item.question),
                            isOpen = isOpen,
                            onToggle = {
                                openItems = if (isOpen) openItems - categoryIndex else openItems + (categoryIndex to itemIndex)
                            },
                        ) {
                            if (item.answer != null) {
                                AnswerText(stringResource(item.answer))
                            } else {
                                RefundRequestAnswer(onOpenSupport = onOpenSupport, onEmail = onEmail)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            StillCurious(onEmail = onEmail)
        }
    }
}

/** "● ANSWERED BY THE TEAM" */
@Composable
private fun FaqBadge() {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f))
            .border(1.dp, colors.primary.copy(alpha = 0.18f), shape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(colors.primary, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.faq_badge).uppercase(),
            style = XpTheme.typography.caption.copy(fontSize = 10.sp, letterSpacing = 1.5.sp),
            color = colors.primary,
        )
    }
}

/** A question card: tap to show or hide its answer; the chevron turns and the border tints when open. */
@Composable
private fun FaqAccordionItem(
    question: String,
    isOpen: Boolean,
    onToggle: () -> Unit,
    answer: @Composable () -> Unit,
) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isOpen) colors.primary.copy(alpha = 0.3f) else colors.divider,
        label = "faqBorder",
    )
    val chevronAngle by animateFloatAsState(targetValue = if (isOpen) 180f else 0f, label = "faqChevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, borderColor, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question,
                style = XpTheme.typography.bodyBold.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronAngle),
            )
        }
        AnimatedVisibility(
            visible = isOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                answer()
            }
        }
    }
}

@Composable
private fun AnswerText(text: String) {
    Text(
        text = text,
        style = XpTheme.typography.body.copy(fontSize = 13.5.sp, lineHeight = 21.sp),
        color = XpTheme.colors.textSecondary,
    )
}

/** "How do I request a refund?": the Support Form link opens Support, the address opens email. */
@Composable
private fun RefundRequestAnswer(onOpenSupport: () -> Unit, onEmail: () -> Unit) {
    val colors = XpTheme.colors
    val body = XpTheme.typography.body.copy(fontSize = 13.5.sp, lineHeight = 21.sp)
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
    )
    val formLabel = stringResource(R.string.faq_refund_form_label)
    val formBefore = stringResource(R.string.faq_refund_form_before)
    val formLink = stringResource(R.string.faq_refund_form_link)
    val formAfter = stringResource(R.string.faq_refund_form_after)
    val emailLabel = stringResource(R.string.faq_refund_email_label)
    val emailBefore = stringResource(R.string.faq_refund_email_before)
    val email = stringResource(R.string.faq_contact_email)
    val emailAfter = stringResource(R.string.faq_refund_email_after)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(R.string.faq_refund_intro), style = body, color = colors.textSecondary)
        Text(
            text = buildAnnotatedString {
                withStyle(bold) { append(formLabel) }
                append(" $formBefore ")
                withLink(LinkAnnotation.Clickable(tag = "support", styles = linkStyles) { onOpenSupport() }) { append(formLink) }
                append(" $formAfter")
            },
            style = body,
            color = colors.textSecondary,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(bold) { append(emailLabel) }
                append(" $emailBefore ")
                withLink(LinkAnnotation.Clickable(tag = "email", styles = linkStyles) { onEmail() }) { append(email) }
                append(" $emailAfter")
            },
            style = body,
            color = colors.textSecondary,
        )
        Text(
            text = stringResource(R.string.faq_refund_note),
            style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = colors.textSecondary,
        )
    }
}

/** "Still curious" card with the contact@xprokey.com button. */
@Composable
private fun StillCurious(onEmail: () -> Unit) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = stringResource(R.string.faq_still_curious).uppercase(),
            style = XpTheme.typography.caption.copy(fontSize = 10.sp, letterSpacing = 1.5.sp),
            color = colors.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.faq_talk_title),
            style = XpTheme.typography.sectionTitle.copy(fontSize = 18.sp, lineHeight = 24.sp),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.faq_talk_body),
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
            color = colors.textLabel,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary)
                .clickable(role = Role.Button, onClick = onEmail),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mail),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.faq_contact_email),
                style = XpTheme.typography.button,
                color = colors.onPrimary,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun FaqScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        FaqScreen(
            user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onOpenSupport = {},
            onEmail = {},
        )
    }
}
