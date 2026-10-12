package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.data.ClipView

/** Sort button with a menu of [options]; the active one is checked. */
@Composable
fun <T> SortMenu(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort: ${label(selected)}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            options.forEach { option ->
                val active = option == selected
                DropdownMenuItem(
                    text = { Text(label(option), fontWeight = if (active) FontWeight.Bold else null) },
                    trailingIcon = { if (active) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** Switches clip collections between a picture grid and a compact list. */
@Composable
fun ViewToggle(view: ClipView, onChange: (ClipView) -> Unit) {
    IconButton(onClick = { onChange(if (view == ClipView.GRID) ClipView.LIST else ClipView.GRID) }) {
        if (view == ClipView.GRID) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Show as list")
        } else {
            Icon(Icons.Filled.GridView, contentDescription = "Show as grid")
        }
    }
}
