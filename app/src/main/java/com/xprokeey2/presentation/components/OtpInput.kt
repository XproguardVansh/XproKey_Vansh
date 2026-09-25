package com.xprokeey2.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme

private val CellShape = RoundedCornerShape(12.dp)

/**
 * Row of single-digit boxes backed by one hidden text field, so typing,
 * backspace and pasting a whole code all work naturally.
 */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val description = stringResource(R.string.cd_otp_field)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        // Keep the caret pinned to the end so the "active" box is always the next empty one.
        value = TextFieldValue(text = value, selection = TextRange(value.length)),
        onValueChange = { newValue ->
            val digits = newValue.text.filter(Char::isDigit).take(length)
            if (digits != value) onValueChange(digits)
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { contentDescription = description },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val activeIndex = value.length.coerceAtMost(length - 1)
                repeat(length) { index ->
                    OtpCell(
                        digit = value.getOrNull(index),
                        isActive = isFocused && index == activeIndex,
                        isError = isError,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun OtpCell(
    digit: Char?,
    isActive: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = XpTheme.colors
    val accent = if (isError) colors.error else colors.primary
    val borderColor = when {
        isError -> colors.error
        isActive -> colors.primary
        else -> colors.fieldBorder
    }

    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .focusRing(visible = isActive, color = accent.copy(alpha = 0.18f), cornerRadius = 12.dp)
            .background(colors.fieldBackground, CellShape)
            .border(1.dp, borderColor, CellShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            digit != null -> Text(
                text = digit.toString(),
                style = XpTheme.typography.otpDigit,
                color = colors.textPrimary,
            )
            isActive -> BlinkingCaret()
        }
    }
}

@Composable
private fun BlinkingCaret() {
    val transition = rememberInfiniteTransition(label = "otpCaret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 530), RepeatMode.Reverse),
        label = "otpCaretAlpha",
    )
    Box(
        modifier = Modifier
            .width(1.5.dp)
            .height(22.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(XpTheme.colors.textPrimary),
    )
}
