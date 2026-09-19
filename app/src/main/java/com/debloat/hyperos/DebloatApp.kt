package com.debloat.hyperos

import android.app.Application
import com.debloat.hyperos.data.db.AppDatabase
import com.debloat.hyperos.repository.DebloatRepository
import com.debloat.hyperos.shizuku.ShizukuManager

class DebloatApp : Application() {

    lateinit var repository: DebloatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.initialize()

        val db = AppDatabase.getInstance(this)
        repository = DebloatRepository(this, db.debloatDao())
    }

    override fun onTerminate() {
        ShizukuManager.teardown()
        super.onTerminate()
    }
}
