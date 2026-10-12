package com.ttggg5.holovoice

import android.app.Application
import android.content.Context
import com.ttggg5.holovoice.data.AppDatabase
import com.ttggg5.holovoice.data.PackImporter
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HoloVoiceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Hand-rolled dependency container shared by the whole app. */
class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)
    val repository = PackRepository(db, PackImporter(context, db))
    val player = PlayerController(context)

    /** For writes that must finish even if the screen that started them goes away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

val Context.appContainer: AppContainer get() = (applicationContext as HoloVoiceApp).container
