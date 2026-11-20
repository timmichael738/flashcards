package com.kotlinapps.flashcards.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single(DataStorePath) { androidContext().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath }
}
