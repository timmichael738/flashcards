package com.kotlinapps.flashcards.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.kotlinapps.flashcards.AppViewModel
import com.kotlinapps.flashcards.core.data.deck.BundledDeckSource
import com.kotlinapps.flashcards.core.data.deck.DeckRepository
import com.kotlinapps.flashcards.core.data.deck.DeckSource
import com.kotlinapps.flashcards.core.data.deck.DefaultDeckRepository
import com.kotlinapps.flashcards.core.data.repository.DataStoreScoreRepository
import com.kotlinapps.flashcards.core.data.repository.DataStoreUserPreferencesRepository
import com.kotlinapps.flashcards.core.data.repository.ScoreRepository
import com.kotlinapps.flashcards.core.data.repository.UserPreferencesRepository
import com.kotlinapps.flashcards.feature.decks.DecksViewModel
import com.kotlinapps.flashcards.feature.quiz.QuizViewModel
import com.kotlinapps.flashcards.feature.settings.SettingsViewModel
import com.kotlinapps.flashcards.feature.study.StudyViewModel
import okio.Path.Companion.toPath
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.random.Random

/** Koin qualifier for the absolute path of the preferences DataStore file. */
internal val DataStorePath = named("dataStorePath")
internal const val DATASTORE_FILE_NAME = "flashcards.preferences_pb"

/** Platform-specific pieces: the DataStore file path (qualified with [DataStorePath]). */
internal expect val platformModule: Module

internal val dataModule = module {
    // DataStore must be a singleton per file; two instances for one file throw at runtime.
    // Settings and best scores share it, under distinct keys.
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(produceFile = { get<String>(DataStorePath).toPath() })
    }
    single<DeckSource> { BundledDeckSource() }
    single<DeckRepository> { DefaultDeckRepository(get()) }
    single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get()) }
    single<ScoreRepository> { DataStoreScoreRepository(get()) }
}

internal val viewModelModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::DecksViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { params -> StudyViewModel(deckId = params.get(), deckRepository = get(), random = Random.Default) }
    viewModel { params ->
        QuizViewModel(
            deckId = params.get(),
            deckRepository = get(),
            scoreRepository = get(),
            preferencesRepository = get(),
            random = Random.Default,
        )
    }
}

/** Single entry point used by both the Android Application and the iOS app. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, dataModule, viewModelModule)
    }
}
