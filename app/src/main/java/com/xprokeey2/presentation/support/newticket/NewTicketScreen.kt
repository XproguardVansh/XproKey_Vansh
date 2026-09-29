package com.xprokeey2.presentation.support.newticket

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.support.components.formLabel
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun NewTicketScreenRoot(
    onBack: () -> Unit,
    onCreated: (ticketId: String?, message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: NewTicketViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NewTicketEvent.Created -> onCreated(event.ticketId, event.message.asString(context))
            is NewTicketEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is NewTicketEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    NewTicketScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
fun NewTicketScreen(
    state: NewTicketUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (NewTicketAction) -> Unit,
    onBack: () -> Unit,
) {
    val colors = XpTheme.colors
    val focusManager = LocalFocusManager.current
    WorkspaceScaffold(
        user = null,
        currentSection = WorkspaceSection.SUPPORT,
        onSectionClick = {},
        snackbarHostState = snackbarHostState,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.new_ticket_title),
                style = XpTheme.typography.pageTitle.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.new_ticket_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(20.dp))

            WorkspacePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    XpTextField(
                        value = state.name,
                        onValueChange = { onAction(NewTicketAction.NameChanged(it)) },
                        label = stringResource(R.string.ticket_name_label),
                        required = true,
                        placeholder = stringResource(R.string.ticket_name_placeholder),
                        error = state.nameError?.asString(),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    XpTextField(
                        value = state.email,
                        onValueChange = { onAction(NewTicketAction.EmailChanged(it)) },
                        label = stringResource(R.string.ticket_email_label),
                        required = true,
                        placeholder = stringResource(R.string.ticket_email_placeholder),
                        error = state.emailError?.asString(),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryPicker(
                        selected = state.category,
                        enabled = !state.isSubmitting,
                        onSelected = { onAction(NewTicketAction.CategorySelected(it)) },
                    )
                    XpTextField(
                        value = state.subject,
                        onValueChange = { onAction(NewTicketAction.SubjectChanged(it)) },
                        label = stringResource(R.string.ticket_subject_label),
                        required = true,
                        placeholder = stringResource(R.string.ticket_subject_placeholder),
                        error = state.subjectError?.asString(),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    XpTextField(
                        value = state.message,
                        onValueChange = { onAction(NewTicketAction.MessageChanged(it)) },
                        label = stringResource(R.string.ticket_message_label),
                        required = true,
                        placeholder = stringResource(R.string.ticket_message_placeholder),
                        error = state.messageError?.asString(),
                        enabled = !state.isSubmitting,
                        minLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XpSecondaryButton(
                    text = stringResource(R.string.cancel),
                    onClick = onBack,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
                XpPrimaryButton(
                    text = stringResource(R.string.ticket_submit),
                    onClick = {
                        focusManager.clearFocus()
                        onAction(NewTicketAction.Submit)
                    },
                    isLoading = state.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The web form's category menu: Technical Issue, Billing & Payment, … */
@Composable
private fun CategoryPicker(selected: TicketCategory, enabled: Boolean, onSelected: (TicketCategory) -> Unit) {
    val colors = XpTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    Column {
        Text(
            text = stringResource(R.string.label_category).uppercase(),
            style = XpTheme.typography.fieldLabel,
            color = colors.textLabel,
        )
        Spacer(Modifier.height(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(colors.fieldBackground)
                    .border(1.dp, if (expanded) colors.primary else colors.fieldBorder, shape)
                    .clickable(enabled = enabled, role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(selected.formLabel),
                    style = XpTheme.typography.fieldText,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.textLabel,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = colors.surface) {
                TicketCategory.entries.forEach { category ->
                    val isSelected = category == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(category.formLabel),
                                style = XpTheme.typography.fieldText.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = colors.textPrimary,
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            expanded = false
                            onSelected(category)
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun NewTicketScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        NewTicketScreen(
            state = NewTicketUiState(email = "goelv2610@gmail.com"),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
        )
    }
}
