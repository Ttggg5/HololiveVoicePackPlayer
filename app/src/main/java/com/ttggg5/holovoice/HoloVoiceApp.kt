package com.ttggg5.holovoice

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.ttggg5.holovoice.data.AppDatabase
import com.ttggg5.holovoice.data.PackImporter
import com.ttggg5.holovoice.data.LibraryRoots
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.UiPrefs
import com.ttggg5.holovoice.alarm.AlarmRepository
import com.ttggg5.holovoice.alarm.AlarmScheduler
import com.ttggg5.holovoice.data.talents.TalentDirectory
import com.ttggg5.holovoice.data.talents.TalentPictures
import com.ttggg5.holovoice.data.talents.TalentLinker
import com.ttggg5.holovoice.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HoloVoiceApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Every image (covers, talent pictures) fades in instead of popping. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).crossfade(250).build()
}

/** Hand-rolled dependency container shared by the whole app. */
class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)
    val uiPrefs = UiPrefs(context)
    private val talentLinker = TalentLinker(db, TalentDirectory(context, TalentPictures(context)))
    val repository = PackRepository(
        db,
        PackImporter(context, db, talentLinker, autoMatch = { uiPrefs.autoMatchTalents.value }),
        talentLinker,
        LibraryRoots(context),
    )
    val player = PlayerController(context, uiPrefs)
    val alarms = AlarmRepository(db.alarmDao(), AlarmScheduler(context))

    /** For writes that must finish even if the screen that started them goes away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

val Context.appContainer: AppContainer get() = (applicationContext as HoloVoiceApp).container
