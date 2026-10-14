package com.billfolder.android.di

import com.billfolder.android.notifications.DueBillsNotifier
import com.billfolder.android.notifications.DueBillsScheduler
import com.billfolder.android.notifications.SystemDueBillsNotifier
import com.billfolder.android.notifications.WorkManagerDueBillsScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    abstract fun bindDueBillsScheduler(impl: WorkManagerDueBillsScheduler): DueBillsScheduler

    @Binds
    abstract fun bindDueBillsNotifier(impl: SystemDueBillsNotifier): DueBillsNotifier
}
