package com.billfolder.android.di

import com.billfolder.android.data.preferences.DataStoreReminderPreferences
import com.billfolder.android.data.preferences.DataStoreThemePreferences
import com.billfolder.android.data.preferences.ReminderPreferences
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

    @Binds
    abstract fun bindReminderPreferences(impl: DataStoreReminderPreferences): ReminderPreferences
}
