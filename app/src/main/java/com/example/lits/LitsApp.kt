package com.example.lits

import android.app.Application
import com.example.lits.data.LevelRepositoryImpl
import com.example.lits.logic.LevelRepository

class LitsApp : Application() {
    val levelRepository: LevelRepository by lazy { LevelRepositoryImpl(this) }
}
