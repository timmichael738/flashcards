package com.kotlinapps.flashcards

import android.app.Application
import com.kotlinapps.flashcards.di.initKoin
import org.koin.android.ext.koin.androidContext

class FlashcardsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@FlashcardsApplication)
        }
    }
}
