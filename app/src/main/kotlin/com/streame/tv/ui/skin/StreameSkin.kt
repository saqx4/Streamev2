package com.streame.tv.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalStreameSkinTokens = staticCompositionLocalOf { StreameSkinTokens.defaults() }

@Composable
fun ProvideStreameSkin(
    tokens: StreameSkinTokens = StreameSkinTokens.defaults(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStreameSkinTokens provides tokens,
        content = content,
    )
}

object StreameSkin {
    val tokens: StreameSkinTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalStreameSkinTokens.current

    val colors: StreameColorTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.colors

    val spacing: StreameSpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.spacing

    val radius: StreameRadiusTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.radius

    val typography: StreameTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.typography

    val motion: StreameMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.motion

    val focus: StreameFocusTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.focus
}

