package com.xprokeey2.presentation.cards.form

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.presentation.cards.components.CardNumberVisualTransformation
import com.xprokeey2.presentation.cards.components.ExpiryVisualTransformation
import com.xprokeey2.presentation.cards.components.PaymentCardView
import com.xprokeey2.presentation.cards.components.formatExpiryDigits
import com.xprokeey2.presentation.cards.components.labelRes
import com.xprokeey2.presentation.cards.components.previewCardNumber
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun CardFormScreenRoot(
    onBack: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onSaved: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: CardFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CardFormEvent.Saved -> onSaved(event.message.asString(context))
            is CardFormEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is CardFormEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    CardFormScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onSectionClick = onSectionClick,
    )
}

@Composable
fun CardFormScreen(
    state: CardFormUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (CardFormAction) -> Unit,
    onBack: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    val colors = XpTheme.colors
    val focusManager = LocalFocusManager.current
    val save = {
        focusManager.clearFocus()
        onAction(CardFormAction.Save)
    }

    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.CARDS,
        onSectionClick = onSectionClick,
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
                text = stringResource(if (state.isEditing) R.string.edit_card_title else R.string.add_card_title),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )

            when {
                state.isLoadingCard -> LoadingBlock()
                state.loadError != null -> LoadErrorPanel(
                    message = state.loadError.asString(),
                    onRetry = { onAction(CardFormAction.RetryLoad) },
                )
                else -> {
                    Spacer(Modifier.height(20.dp))
                    SectionCaption(stringResource(R.string.live_preview))
                    Spacer(Modifier.height(10.dp))
                    PaymentCardView(
                        label = state.label,
                        brand = state.brand,
                        number = previewCardNumber(state.number),
                        holderName = state.holderName,
                        expiry = formatExpiryDigits(state.expiry),
                    )
                    Spacer(Modifier.height(24.dp))
                    CardFormFields(state = state, onAction = onAction, onSave = save, onCancel = onBack)
                }
            }
        }
    }
}

@Composable
private fun CardFormFields(
    state: CardFormUiState,
    onAction: (CardFormAction) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = XpTheme.colors
    val monoStyle = XpTheme.typography.mono.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.4.sp)

    WorkspacePanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.card_details_section),
            style = XpTheme.typography.sectionTitle.copy(fontSize = 16.sp),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(18.dp))

        if (state.secretsUnreadable) {
            Text(
                text = stringResource(R.string.card_secrets_unreadable),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.warning,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.warning.copy(alpha = 0.10f))
                    .padding(12.dp),
            )
            Spacer(Modifier.height(18.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            XpTextField(
                value = state.label,
                onValueChange = { onAction(CardFormAction.LabelChanged(it)) },
                label = stringResource(R.string.label_card_type),
                labelIcon = R.drawable.ic_credit_card,
                required = true,
                placeholder = stringResource(R.string.placeholder_card_type),
                error = state.labelError?.asString(),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            XpTextField(
                value = state.holderName,
                onValueChange = { onAction(CardFormAction.HolderNameChanged(it)) },
                label = stringResource(R.string.label_cardholder_name),
                labelIcon = R.drawable.ic_user,
                placeholder = stringResource(R.string.placeholder_cardholder_name),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            XpTextField(
                value = state.number,
                onValueChange = { onAction(CardFormAction.NumberChanged(it)) },
                label = stringResource(R.string.label_card_number),
                labelIcon = R.drawable.ic_credit_card,
                required = true,
                placeholder = stringResource(R.string.placeholder_card_number),
                error = state.numberError?.asString(),
                enabled = !state.isSaving,
                textStyle = monoStyle,
                placeholderStyle = monoStyle,
                visualTransformation = CardNumberVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XpTextField(
                    value = state.expiry,
                    onValueChange = { onAction(CardFormAction.ExpiryChanged(it)) },
                    label = stringResource(R.string.label_expiry),
                    labelIcon = R.drawable.ic_calendar,
                    required = true,
                    placeholder = stringResource(R.string.placeholder_expiry),
                    error = state.expiryError?.asString(),
                    enabled = !state.isSaving,
                    textStyle = monoStyle,
                    placeholderStyle = monoStyle,
                    visualTransformation = ExpiryVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                XpTextField(
                    value = state.cvc,
                    onValueChange = { onAction(CardFormAction.CvcChanged(it)) },
                    label = stringResource(R.string.label_cvv),
                    labelIcon = R.drawable.ic_key,
                    required = true,
                    placeholder = "•".repeat(state.brand.cvcLength),
                    error = state.cvcError?.asString(),
                    enabled = !state.isSaving,
                    textStyle = monoStyle,
                    placeholderStyle = monoStyle,
                    visualTransformation = if (state.isCvcVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { onAction(CardFormAction.ToggleCvcVisibility) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(if (state.isCvcVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                contentDescription = stringResource(if (state.isCvcVisible) R.string.cd_hide_cvv else R.string.cd_show_cvv),
                                tint = colors.textPlaceholder,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            CategoryPicker(
                selected = state.category,
                enabled = !state.isSaving,
                onSelected = { onAction(CardFormAction.CategorySelected(it)) },
            )
            XpTextField(
                value = state.bankName,
                onValueChange = { onAction(CardFormAction.BankNameChanged(it)) },
                label = stringResource(R.string.label_bank_name),
                labelIcon = R.drawable.ic_building,
                placeholder = stringResource(R.string.placeholder_bank_name),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            XpTextField(
                value = state.notes,
                onValueChange = { onAction(CardFormAction.NotesChanged(it)) },
                label = stringResource(R.string.label_notes),
                labelIcon = R.drawable.ic_file_text,
                placeholder = stringResource(R.string.placeholder_notes),
                enabled = !state.isSaving,
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            XpSecondaryButton(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
            XpPrimaryButton(
                text = stringResource(R.string.save_card),
                onClick = onSave,
                isLoading = state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Credit card ▾" chip that opens the category menu (web: Card category dropdown). */
@Composable
private fun CategoryPicker(
    selected: CardCategory,
    enabled: Boolean,
    onSelected: (CardCategory) -> Unit,
) {
    val colors = XpTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_credit_card),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.label_card_category).uppercase(),
                style = XpTheme.typography.fieldLabel,
                color = colors.textLabel,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(shape)
                    .background(colors.fieldBackground)
                    .border(1.dp, if (expanded) colors.primary else colors.fieldBorder, shape)
                    .clickable(enabled = enabled, role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(selected.labelRes),
                    style = XpTheme.typography.fieldText,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.textLabel,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.surface,
            ) {
                CardCategory.entries.forEach { category ->
                    val isSelected = category == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(category.labelRes),
                                style = XpTheme.typography.fieldText.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                color = if (isSelected) colors.primary else colors.textPrimary,
                            )
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

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text.uppercase(),
        style = XpTheme.typography.fieldLabel.copy(letterSpacing = 0.6.sp),
        color = XpTheme.colors.textLabel,
    )
}

@Composable
private fun LoadErrorPanel(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Text(
            text = message,
            style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = XpTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(16.dp))
        XpPrimaryButton(text = stringResource(R.string.retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "Add", showBackground = true, heightDp = 1500)
@Composable
private fun CardFormScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        CardFormScreen(
            state = CardFormUiState(label = "Personal", number = "652281434201", expiry = "07"),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
            onSectionClick = {},
        )
    }
}

@Preview(name = "Edit, unreadable", showBackground = true, heightDp = 1500)
@Composable
private fun CardFormScreenEditPreview() {
    XproKeyTheme(darkTheme = false) {
        CardFormScreen(
            state = CardFormUiState(
                isEditing = true,
                label = "Visa Test",
                holderName = "Shreyash Jadhav",
                expiry = "1230",
                secretsUnreadable = true,
                numberError = null,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
            onSectionClick = {},
        )
    }
}
