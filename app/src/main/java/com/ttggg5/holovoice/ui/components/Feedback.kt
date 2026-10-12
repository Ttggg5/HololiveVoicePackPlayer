package com.ttggg5.holovoice.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Shows app-wide snackbars, optionally with an Undo action. */
class UndoController(private val snackbar: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, onUndo: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = if (onUndo != null) "Undo" else null,
                withDismissAction = onUndo == null,
                duration = if (onUndo != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo?.invoke()
        }
    }
}

/** Provided by the root screen; null in previews. */
val LocalUndo = staticCompositionLocalOf<UndoController?> { null }
