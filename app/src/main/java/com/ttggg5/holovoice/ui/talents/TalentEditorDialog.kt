package com.ttggg5.holovoice.ui.talents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.talents.CustomTalents
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.ui.components.Artwork
import kotlinx.coroutines.launch

/**
 * Adds a talent who isn't on the hololive site (or edits one the user added): English and Japanese
 * names and an optional picture from the gallery. Saving also matches files named after them.
 * [onSaved] gets the stored talent.
 */
@Composable
fun TalentEditorDialog(
    existing: Talent? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSaved: (Talent) -> Unit,
) {
    val repository = LocalContext.current.appContainer.repository
    val scope = rememberCoroutineScope()
    var nameEn by rememberSaveable { mutableStateOf(existing?.nameEn ?: initialName.trim()) }
    var nameJa by rememberSaveable { mutableStateOf(existing?.nameJa.orEmpty()) }
    var picture by remember { mutableStateOf<Uri?>(null) }
    var removePicture by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            picture = uri
            removePicture = false
        }
    }
    val shown: Any? = picture ?: existing?.imageUrl?.takeUnless { removePicture || it.isEmpty() }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (existing == null) "Add a talent" else "Edit talent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Artwork(
                        shown,
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !saving) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        iconSize = 32.dp,
                        placeholder = if (shown == null) Icons.Filled.AddPhotoAlternate else Icons.Filled.Face,
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        TextButton(
                            onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !saving,
                        ) { Text(if (shown == null) "Choose picture" else "Change picture") }
                        if (shown != null) {
                            TextButton(
                                onClick = {
                                    picture = null
                                    removePicture = true
                                },
                                enabled = !saving,
                            ) { Text("Remove picture") }
                        }
                    }
                }
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Name (English / romaji)") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nameJa,
                    onValueChange = { nameJa = it },
                    label = { Text("日本語の名前 (optional)") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Files named after either name are matched to this talent automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            val saved = if (existing == null) {
                                repository.addTalent(nameEn, nameJa, picture)
                            } else {
                                repository.editTalent(existing.slug, nameEn, nameJa, picture, removePicture)
                            }
                            if (saved != null) onSaved(saved) else error = "This talent no longer exists"
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't save the talent"
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving && CustomTalents.isValidName(nameEn),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}
