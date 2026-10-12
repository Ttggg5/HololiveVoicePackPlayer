package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.talents.Talent
import com.ttggg5.holovoice.data.talents.TalentMatcher

/**
 * Searchable, multi-select list of all talents with their pictures. Picks keep the order they were
 * chosen in (the first one is the clip's main picture). [onConfirm] gets an empty list for "no talent".
 */
@Composable
fun TalentPickerDialog(
    title: String,
    initialSlugs: List<String>,
    onConfirm: (List<Talent>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { context.appContainer.repository }
    var talents by remember { mutableStateOf<List<Talent>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val picked = remember { mutableStateListOf<String>().apply { addAll(initialSlugs) } }

    LaunchedEffect(reload) {
        error = null
        talents = null
        talents = try {
            repository.talents(forceRefresh = reload > 0)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't download the talent list"
            emptyList()
        }
    }
    val bySlug = talents.orEmpty().associateBy { it.slug }

    fun toggle(slug: String) {
        if (slug in picked) picked.remove(slug) else picked.add(slug)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f))
                IconButton(onClick = { reload++ }, enabled = talents != null) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh talent list")
                }
            }
        },
        text = {
            Column {
                if (picked.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    ) {
                        picked.forEach { slug ->
                            val t = bySlug[slug]
                            InputChip(
                                selected = true,
                                onClick = { picked.remove(slug) },
                                label = { Text(t?.nameEn ?: slug, maxLines = 1) },
                                avatar = { Artwork(t?.imageUrl, Modifier.size(24.dp).clip(CircleShape), iconSize = 14.dp) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search (English or 日本語)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = talents
                when {
                    list == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    list.isEmpty() -> Text(
                        error ?: "The talent list is empty. Connect to the internet and tap refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    else -> {
                        val filtered = remember(list, query) { list.filter { TalentMatcher.matchesSearch(query, it) } }
                        LazyColumn(Modifier.padding(top = 8.dp).height(340.dp)) {
                            items(filtered, key = { it.slug }) { t ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { toggle(t.slug) }.padding(vertical = 6.dp),
                                ) {
                                    Checkbox(checked = t.slug in picked, onCheckedChange = { toggle(t.slug) })
                                    Artwork(t.imageUrl, Modifier.size(40.dp).clip(CircleShape), iconSize = 20.dp)
                                    Column(Modifier.padding(start = 12.dp)) {
                                        Text(t.nameEn, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        t.nameJa?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked.mapNotNull { bySlug[it] }) }, enabled = talents != null) {
                Text(if (picked.isEmpty()) "Save (no talent)" else "Save ${picked.size}")
            }
        },
        dismissButton = {
            Row {
                if (picked.isNotEmpty()) TextButton(onClick = { picked.clear() }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
