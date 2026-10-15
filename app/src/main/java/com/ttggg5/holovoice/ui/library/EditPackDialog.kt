package com.ttggg5.holovoice.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.data.Pack
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.ui.components.TalentPickerDialog

/** Rename a topic, change its talent, set talents for all its clips, or delete it. */
@Composable
fun EditPackDialog(
    pack: Pack,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onSetTalents: (List<Talent>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(pack.name) }
    var talent by rememberSaveable { mutableStateOf(pack.talent) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickTalent by remember { mutableStateOf(false) }

    if (pickTalent) {
        TalentPickerDialog(
            title = "Talents for all clips",
            initialSlugs = emptyList(),
            onConfirm = onSetTalents,
            onDismiss = { pickTalent = false },
        )
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${pack.name}?") },
            text = {
                Text(
                    "The topic is removed from the library. Files imported from a ZIP are deleted; " +
                        "files in a picked folder are left untouched.",
                )
            },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = talent, onValueChange = { talent = it }, label = { Text("Talent") }, singleLine = true)
                TextButton(onClick = { pickTalent = true }) {
                    Text("Set talents for all clips…")
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete topic", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, talent) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
