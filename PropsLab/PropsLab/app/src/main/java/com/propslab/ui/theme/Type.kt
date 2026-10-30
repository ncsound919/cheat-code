package com.propslab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.propslab.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Display face for headlines and big numerals. */
val Bricolage = FontFamily(variable(R.font.bricolage, 400), variable(R.font.bricolage, 600), variable(R.font.bricolage, 800))

/** Text face. Tabular figures are switched on so columns of odds and lines align. */
val Manrope = FontFamily(variable(R.font.manrope, 400), variable(R.font.manrope, 500), variable(R.font.manrope, 600), variable(R.font.manrope, 700))

private fun body(size: Int, weight: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Manrope, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = spacing.sp, fontFeatureSettings = "tnum",
)

private fun display(size: Int, weight: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Bricolage, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = spacing.sp, fontFeatureSettings = "tnum",
)

val PropsTypography = Typography(
    displayLarge = display(64, 600, 66, -2.0),
    displayMedium = display(40, 600, 44, -1.0),
    headlineMedium = display(30, 600, 34, -0.6),
    headlineSmall = display(22, 600, 26, -0.3),
    titleLarge = display(20, 600, 24),
    titleMedium = body(16, 600, 22),
    titleSmall = body(14, 600, 20),
    bodyLarge = body(16, 400, 24),
    bodyMedium = body(14, 400, 21),
    bodySmall = body(12, 400, 17),
    labelLarge = body(14, 600, 18),
    labelMedium = body(12, 600, 16),
    labelSmall = body(11, 500, 14),
)
