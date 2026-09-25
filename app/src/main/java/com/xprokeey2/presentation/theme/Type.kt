package com.xprokeey2.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xprokeey2.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Text styles matching the Figma auth screens. */
@Immutable
data class XpTypography(
    val brand: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.36).sp),
    val headline: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.7).sp),
    val subtitle: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, lineHeight = 20.25.sp),
    val fieldLabel: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.4.sp),
    val fieldText: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    val body: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 17.sp),
    val bodyBold: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 17.sp),
    val checkboxLabel: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    val button: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    val buttonSecondary: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    val divider: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    val caption: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 10.sp),
    val otpDigit: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    val mono: TextStyle = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 1.sp),
    val dialogTitle: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    val dialogBody: TextStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp),
)

private val default = Typography()

/** Material typography with Manrope everywhere, so stock Material components match the brand. */
val MaterialTypography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = Manrope),
    displayMedium = default.displayMedium.copy(fontFamily = Manrope),
    displaySmall = default.displaySmall.copy(fontFamily = Manrope),
    headlineLarge = default.headlineLarge.copy(fontFamily = Manrope),
    headlineMedium = default.headlineMedium.copy(fontFamily = Manrope),
    headlineSmall = default.headlineSmall.copy(fontFamily = Manrope),
    titleLarge = default.titleLarge.copy(fontFamily = Manrope),
    titleMedium = default.titleMedium.copy(fontFamily = Manrope),
    titleSmall = default.titleSmall.copy(fontFamily = Manrope),
    bodyLarge = default.bodyLarge.copy(fontFamily = Manrope),
    bodyMedium = default.bodyMedium.copy(fontFamily = Manrope),
    bodySmall = default.bodySmall.copy(fontFamily = Manrope),
    labelLarge = default.labelLarge.copy(fontFamily = Manrope),
    labelMedium = default.labelMedium.copy(fontFamily = Manrope),
    labelSmall = default.labelSmall.copy(fontFamily = Manrope),
)
