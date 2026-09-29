package com.cruceseguro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AccessibleTypography = Typography(
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
)
