package com.billfolder.android.di

import com.billfolder.android.data.preferences.DataStoreThemePreferences
import com.billfolder.android.data.preferences.ThemePreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    abstract fun bindThemePreferences(impl: DataStoreThemePreferences): ThemePreferences
}
