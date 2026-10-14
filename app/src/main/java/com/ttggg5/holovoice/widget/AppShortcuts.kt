package com.ttggg5.holovoice.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ttggg5.holovoice.MainActivity
import com.ttggg5.holovoice.R
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.TalentSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Long-press shortcuts on the app icon. Continue, Shuffle all and Favorites are fixed
 * (res/xml/shortcuts.xml); this adds "Shuffle <talent>" for your two most played talents.
 */
object AppShortcuts {
    const val ACTION_CONTINUE = "com.ttggg5.holovoice.action.CONTINUE"
    const val ACTION_SHUFFLE_ALL = "com.ttggg5.holovoice.action.SHUFFLE_ALL"
    const val ACTION_SHUFFLE_FAVORITES = "com.ttggg5.holovoice.action.SHUFFLE_FAVORITES"
    const val ACTION_SHUFFLE_TALENT = "com.ttggg5.holovoice.action.SHUFFLE_TALENT"
    const val EXTRA_TALENT = "talent"

    suspend fun refreshTalents(context: Context, repository: PackRepository) = withContext(Dispatchers.IO) {
        val talents = TalentSort.PLAYS.sorted(repository.talentSummaries.first())
            .filter { it.slug != null && it.plays > 0 }
            .take(2)
        val shortcuts = talents.mapIndexed { i, t ->
            val name = t.name ?: t.slug!!
            val picture = t.imageUrl?.let { NowPlayingWidget.decode(context, it, 192) }?.let(NowPlayingWidget::circle)
            ShortcutInfoCompat.Builder(context, "talent_${t.slug}")
                .setShortLabel(name.substringBefore(' ').ifBlank { name })
                .setLongLabel("Shuffle $name")
                .setIcon(picture?.let(IconCompat::createWithBitmap) ?: IconCompat.createWithResource(context, R.drawable.ic_shortcut_shuffle))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(ACTION_SHUFFLE_TALENT)
                        .putExtra(EXTRA_TALENT, t.slug),
                )
                .setRank(i)
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
