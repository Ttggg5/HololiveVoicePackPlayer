package com.ttggg5.holovoice.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.holovoice.data.Pack
import com.ttggg5.holovoice.data.PackImporter
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.talents.Talent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: PackRepository) : ViewModel() {
    val packs: StateFlow<List<PackWithCount>?> =
        repository.packs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _importing = MutableStateFlow(false)

    /** Busy with an import, a talent match, or the automatic folder sync. */
    val importing: StateFlow<Boolean> = combine(_importing, repository.syncing) { a, b -> a || b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = merge(_messages.receiveAsFlow(), repository.syncMessages)

    fun importFolder(uri: Uri) = runImport { repository.importFolder(uri) }

    fun importZip(uri: Uri) = runImport { repository.importZip(uri) }

    private fun runImport(block: suspend () -> PackImporter.SyncResult) {
        viewModelScope.launch {
            _importing.value = true
            val message = try {
                val r = block()
                when {
                    r.created -> "Imported \"${r.topicName}\" (${r.added} clips)"
                    r.changed -> "\"${r.topicName}\" is already in your library — updated: ${r.added} new, ${r.removed} removed"
                    else -> "No new audio files in \"${r.topicName}\""
                }
            } catch (e: Exception) {
                "Import failed: ${e.message ?: e::class.simpleName}"
            } finally {
                _importing.value = false
            }
            _messages.send(message)
        }
    }

    /** Re-runs automatic talent matching on the whole library. */
    fun matchTalents() {
        viewModelScope.launch {
            _importing.value = true
            val message = try {
                val (matched, total) = repository.rematchAllTalents()
                "Matched a talent for $matched of $total clips"
            } catch (e: Exception) {
                e.message ?: "Talent matching failed"
            } finally {
                _importing.value = false
            }
            _messages.send(message)
        }
    }

    /** Downloads the talent list again, then re-matches. */
    fun refreshTalents() {
        viewModelScope.launch {
            _importing.value = true
            val message = try {
                val count = repository.talents(forceRefresh = true).size
                val (matched, total) = repository.rematchAllTalents()
                "Talent list updated ($count talents). Matched $matched of $total clips"
            } catch (e: Exception) {
                "Couldn't update the talent list: ${e.message ?: e::class.simpleName}"
            } finally {
                _importing.value = false
            }
            _messages.send(message)
        }
    }

    fun setPackTalent(pack: Pack, talent: Talent?) {
        viewModelScope.launch {
            repository.setPackTalent(pack, talent)
            _messages.send(if (talent == null) "Cleared talent for ${pack.name}" else "Set ${talent.nameEn} for ${pack.name}")
        }
    }

    fun rename(pack: Pack, name: String, talent: String) {
        viewModelScope.launch { repository.rename(pack, name, talent) }
    }

    fun delete(pack: Pack) {
        viewModelScope.launch {
            repository.delete(pack.id)
            _messages.send("Deleted ${pack.name}")
        }
    }
}
