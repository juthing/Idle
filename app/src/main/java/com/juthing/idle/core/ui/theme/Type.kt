package com.juthing.idle.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.juthing.idle.R

/**
 * Google Sans Flex, bundled under the SIL Open Font License.
 *
 * Three static instances are shipped rather than the variable font: they cover every
 * weight the design uses and keep rendering identical across all supported API levels.
 */
val GoogleSans = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold),
)

/**
 * Material 3 type scale rebuilt on [GoogleSans].
 *
 * Only the font family, weight and letter spacing are customised; sizes and line
 * heights follow the Material 3 defaults so that components keep their intended rhythm.
 */
val IdleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.withGoogleSans(FontWeight.Normal, (-0.5).sp),
        displayMedium = displayMedium.withGoogleSans(FontWeight.Normal),
        displaySmall = displaySmall.withGoogleSans(FontWeight.Normal),
        headlineLarge = headlineLarge.withGoogleSans(FontWeight.Normal),
        headlineMedium = headlineMedium.withGoogleSans(FontWeight.Normal),
        headlineSmall = headlineSmall.withGoogleSans(FontWeight.Normal),
        titleLarge = titleLarge.withGoogleSans(FontWeight.Medium),
        titleMedium = titleMedium.withGoogleSans(FontWeight.Medium),
        titleSmall = titleSmall.withGoogleSans(FontWeight.Medium),
        bodyLarge = bodyLarge.withGoogleSans(FontWeight.Normal),
        bodyMedium = bodyMedium.withGoogleSans(FontWeight.Normal),
        bodySmall = bodySmall.withGoogleSans(FontWeight.Normal),
        labelLarge = labelLarge.withGoogleSans(FontWeight.Medium),
        labelMedium = labelMedium.withGoogleSans(FontWeight.Medium),
        labelSmall = labelSmall.withGoogleSans(FontWeight.Medium),
    )
}

private fun TextStyle.withGoogleSans(
    weight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = this.letterSpacing,
): TextStyle = copy(
    fontFamily = GoogleSans,
    fontWeight = weight,
    letterSpacing = letterSpacing,
)
