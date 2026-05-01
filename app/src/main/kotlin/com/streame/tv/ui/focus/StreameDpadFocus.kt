package com.streame.tv.ui.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import kotlin.Unit

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun Modifier.StreameDpadFocusGroup(
    restoreFocusRequester: FocusRequester? = null,
    enableFocusRestorer: Boolean = true
): Modifier {
    val restorer = when {
        !enableFocusRestorer -> Modifier
        restoreFocusRequester != null -> Modifier.focusRestorer { restoreFocusRequester }
        else -> Modifier.focusRestorer()
    }
    return this.then(restorer).focusGroup()
}

@OptIn(ExperimentalFoundationApi::class)
private object StreameNoOpBringIntoViewResponder : BringIntoViewResponder {
    override fun calculateRectForParent(localRect: Rect): Rect = localRect

    override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.StreameManualBringIntoViewBoundary(): Modifier {
    return bringIntoViewResponder(StreameNoOpBringIntoViewResponder)
}

fun isStreameDpadNavigationKey(key: Key): Boolean {
    return key == Key.DirectionLeft ||
        key == Key.DirectionRight ||
        key == Key.DirectionUp ||
        key == Key.DirectionDown
}

@Stable
class StreameDpadRepeatGate(
    private val minRepeatIntervalMs: Long
) {
    private var lastKeyCode: Int = Int.MIN_VALUE
    private var lastHandledAtMs: Long = 0L

    fun shouldSkip(keyCode: Int, repeatCount: Int, nowMs: Long): Boolean {
        if (repeatCount <= 0) {
            lastKeyCode = keyCode
            lastHandledAtMs = nowMs
            return false
        }

        val skip = keyCode == lastKeyCode && nowMs - lastHandledAtMs < minRepeatIntervalMs
        if (!skip) {
            lastKeyCode = keyCode
            lastHandledAtMs = nowMs
        }
        return skip
    }

    fun reset() {
        lastKeyCode = Int.MIN_VALUE
        lastHandledAtMs = 0L
    }
}

@Composable
fun rememberStreameDpadRepeatGate(
    minRepeatIntervalMs: Long = 82L
): StreameDpadRepeatGate {
    return remember(minRepeatIntervalMs) { StreameDpadRepeatGate(minRepeatIntervalMs) }
}
