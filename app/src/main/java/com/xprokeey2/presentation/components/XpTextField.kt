package com.xprokeey2.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme

private val FieldShape = RoundedCornerShape(11.dp)

/**
 * Figma input: optional uppercase label above, tinted box, primary border + halo when focused.
 * [supportingText] is a small hint under the field, replaced by [error] when there is one.
 */
@Composable
fun XpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    @DrawableRes labelIcon: Int? = null,
    placeholder: String = "",
    supportingText: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    required: Boolean = false,
    contentType: ContentType? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = XpTheme.typography.fieldText,
    placeholderStyle: TextStyle = XpTheme.typography.fieldText,
    /** More than 1 makes a multi-line field (e.g. notes) that grows with its text. */
    minLines: Int = 1,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val accent = if (error != null) colors.error else colors.primary
    val borderColor = when {
        error != null -> colors.error
        isFocused -> colors.primary
        else -> colors.fieldBorder
    }

    Column(modifier = modifier) {
        if (label != null) {
            FieldLabel(label = label, required = required, icon = labelIcon)
            Spacer(Modifier.height(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (contentType != null) Modifier.semantics { this.contentType = contentType } else Modifier)
                .focusRing(visible = isFocused, color = accent.copy(alpha = 0.12f), cornerRadius = 11.dp)
                .background(if (isFocused) colors.surface else colors.fieldBackground, FieldShape)
                .border(1.dp, borderColor, FieldShape),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.copy(color = if (readOnly) colors.textLabel else colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(start = 16.dp, end = if (trailingContent != null) 6.dp else 16.dp)
                        .then(if (minLines > 1) Modifier.padding(vertical = 14.dp) else Modifier),
                    verticalAlignment = if (minLines > 1) Alignment.Top else Alignment.CenterVertically,
                ) {
                    if (leadingContent != null) {
                        leadingContent()
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = placeholderStyle,
                                color = colors.textPlaceholder,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.invoke()
                }
            },
        )
        val below = error ?: supportingText
        if (below != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = below,
                style = typography.body.copy(fontSize = 11.5.sp),
                color = if (error != null) colors.error else colors.textLabel,
            )
        }
    }
}

@Composable
fun XpPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    required: Boolean = false,
    contentType: ContentType? = ContentType.Password,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = XpTheme.colors
    val baseStyle = XpTheme.typography.fieldText
    XpTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        error = error,
        enabled = enabled,
        required = required,
        contentType = contentType,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = keyboardActions,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        // Figma spaces out the bullets (letter-spacing ≈ 2.5px) while the password is hidden.
        textStyle = if (isPasswordVisible) baseStyle else baseStyle.copy(letterSpacing = 2.5.sp),
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onToggleVisibility),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    contentDescription = stringResource(
                        if (isPasswordVisible) R.string.cd_hide_password else R.string.cd_show_password
                    ),
                    tint = colors.textPlaceholder,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

/**
 * Uppercase field label; required fields get a red asterisk like the web signup form. The card
 * forms put a small icon in front of it.
 */
@Composable
private fun FieldLabel(label: String, required: Boolean, @DrawableRes icon: Int?) {
    val colors = XpTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = buildAnnotatedString {
                append(label.uppercase())
                if (required) {
                    append(" ")
                    withStyle(SpanStyle(color = colors.error)) { append("*") }
                }
            },
            style = XpTheme.typography.fieldLabel,
            color = colors.textLabel,
        )
    }
}
