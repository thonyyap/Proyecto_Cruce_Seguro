package com.cruceseguro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AccessibleColors =
    darkColorScheme(
        primary = SafetyYellow,
        onPrimary = AccessibleBlack,
        secondary = WarningOrange,
        background = AccessibleBlack,
        onBackground = AccessibleWhite,
        surface = SurfaceDark,
        onSurface = AccessibleWhite,
        error = ErrorRed,
    )

@Composable
fun CruceSeguroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AccessibleColors,
        typography = AccessibleTypography,
        content = content,
    )
}
