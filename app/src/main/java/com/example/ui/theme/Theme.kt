package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = TrackDarkPrimary,
    onPrimary = TrackDarkOnPrimary,
    primaryContainer = TrackDarkPrimaryContainer,
    onPrimaryContainer = TrackDarkOnPrimaryContainer,
    secondary = TrackDarkSecondary,
    onSecondary = TrackDarkOnSecondary,
    secondaryContainer = TrackDarkSecondaryContainer,
    onSecondaryContainer = TrackDarkOnSecondaryContainer,
    tertiary = TrackDarkTertiary,
    onTertiary = TrackDarkOnTertiary,
    tertiaryContainer = TrackDarkTertiaryContainer,
    onTertiaryContainer = TrackDarkOnTertiaryContainer,
    error = TrackError,
    onError = TrackOnError,
    errorContainer = TrackErrorContainer,
    onErrorContainer = TrackOnErrorContainer,
    background = TrackDarkBackground,
    onBackground = TrackDarkOnBackground,
    surface = TrackDarkSurface,
    onSurface = TrackDarkOnSurface,
    surfaceVariant = TrackDarkSurfaceVariant,
    onSurfaceVariant = TrackDarkOnSurfaceVariant,
    outline = TrackDarkOutline,
    outlineVariant = TrackDarkOutlineVariant,
    surfaceContainerLowest = TrackDarkSurfaceContainerLowest,
    surfaceContainerLow = TrackDarkSurfaceContainerLow,
    surfaceContainer = TrackDarkSurfaceContainer,
    surfaceContainerHigh = TrackDarkSurfaceContainerHigh,
    surfaceContainerHighest = TrackDarkSurfaceContainerHighest,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TrackPrimary,
    onPrimary = TrackOnPrimary,
    primaryContainer = TrackPrimaryContainer,
    onPrimaryContainer = TrackOnPrimaryContainer,
    secondary = TrackSecondary,
    onSecondary = TrackOnSecondary,
    secondaryContainer = TrackSecondaryContainer,
    onSecondaryContainer = TrackOnSecondaryContainer,
    tertiary = TrackTertiary,
    onTertiary = TrackOnTertiary,
    tertiaryContainer = TrackTertiaryContainer,
    onTertiaryContainer = TrackOnTertiaryContainer,
    error = TrackError,
    onError = TrackOnError,
    errorContainer = TrackErrorContainer,
    onErrorContainer = TrackOnErrorContainer,
    background = TrackBackground,
    onBackground = TrackOnBackground,
    surface = TrackSurface,
    onSurface = TrackOnSurface,
    surfaceVariant = TrackSurfaceVariant,
    onSurfaceVariant = TrackOnSurfaceVariant,
    outline = TrackOutline,
    outlineVariant = TrackOutlineVariant,
    surfaceContainerLowest = TrackSurfaceContainerLowest,
    surfaceContainerLow = TrackSurfaceContainerLow,
    surfaceContainer = TrackSurfaceContainer,
    surfaceContainerHigh = TrackSurfaceContainerHigh,
    surfaceContainerHighest = TrackSurfaceContainerHighest,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
