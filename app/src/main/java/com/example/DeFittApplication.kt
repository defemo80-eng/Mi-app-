package com.example

import android.app.Application
import com.example.data.local.DeFittDatabase
import com.example.data.local.SessionPreferences
import com.example.data.repository.DeFittRepository
import com.example.worker.MidnightScheduler

class DeFittApplication : Application() {
    lateinit var database: DeFittDatabase
        private set
    lateinit var sessionPreferences: SessionPreferences
        private set
    lateinit var repository: DeFittRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = DeFittDatabase.getInstance(this)
        sessionPreferences = SessionPreferences(this)
        repository = DeFittRepository(database, sessionPreferences)
        MidnightScheduler.scheduleWork(this)
    }
}
