package com.songbook.app

import android.app.Application
import com.songbook.app.data.SettingsStore
import com.songbook.app.data.SongbookDatabase
import com.songbook.app.data.SongbookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SongbookApp : Application() {
    lateinit var repo: SongbookRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = SongbookDatabase.build(this)
        repo = SongbookRepository(this, db.dao(), SettingsStore(this))
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repo.seedIfNeeded()
            repo.cleanupImages()
        }
    }
}
