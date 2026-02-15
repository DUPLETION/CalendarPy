package com.pylearn.app.di

import com.pylearn.app.data.repository.ProgressRepository
import com.pylearn.app.ui.editor.EditorViewModel
import com.pylearn.app.ui.main.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ProgressRepository.createEncryptedPrefs(androidContext()) }
    single { ProgressRepository(androidContext(), get()) }
    
    viewModel { MainViewModel(get(), androidContext()) }
    viewModel { EditorViewModel(androidContext()) }
}
