package com.billfolder.android.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Substitui Dispatchers.Main por um TestDispatcher durante o teste — necessário
 * porque `viewModelScope` usa Dispatchers.Main.immediate, que não existe num
 * unit test JVM puro.
 *
 * UnconfinedTestDispatcher roda as corrotinas ansiosamente (eager), então o
 * `init { load() }` de um VM completa de forma síncrona quando o FakeBillFolderApi
 * responde na hora — logo, o state já está em Content assim que o construtor
 * retorna, sem precisar avançar o scheduler manualmente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
