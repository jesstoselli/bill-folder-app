package com.billfolder.android.ui.screens.account

import com.billfolder.android.data.dto.UserDto
import com.billfolder.android.data.preferences.ThemeMode
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.data.repository.UsersRepository
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.FakeDueBillsScheduler
import com.billfolder.android.testutil.FakeReminderPreferences
import com.billfolder.android.testutil.FakeThemePreferences
import com.billfolder.android.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val themePreferences = FakeThemePreferences()
    private val reminderPreferences = FakeReminderPreferences()
    private val scheduler = FakeDueBillsScheduler()

    // AuthRepository exige TokenStorage (android Context); o mock basta para
    // verificar que o "sair" delega para ele.
    private val authRepository: AuthRepository = mockk(relaxed = true)

    private val jess = UserDto(id = "user-1", email = "jess@example.com", displayName = "Jess")

    private fun viewModel() = AccountViewModel(
        UsersRepository(api),
        authRepository,
        themePreferences,
        reminderPreferences,
        scheduler,
    )

    @Test
    fun `carrega o usuario logado`() {
        api.onGetMe = { jess }

        val vm = viewModel()

        assertEquals(AccountUserState.Loaded(jess), vm.user.value)
    }

    @Test
    fun `falha de rede vira mensagem e tentar de novo recarrega`() {
        var calls = 0
        api.onGetMe = { if (++calls == 1) throw IOException("offline") else jess }
        val vm = viewModel()

        assertEquals(AccountUserState.Error("Sem conexão. Tenta de novo."), vm.user.value)

        vm.loadUser()

        assertEquals(AccountUserState.Loaded(jess), vm.user.value)
        assertEquals(2, calls)
    }

    @Test
    fun `aparencia comeca no automatico e salva a escolha na hora`() {
        api.onGetMe = { jess }
        val vm = viewModel()
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)

        vm.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)
    }

    @Test
    fun `sair desloga e so depois avisa a tela`() {
        api.onGetMe = { jess }
        val vm = viewModel()
        var done = false

        vm.logout(onDone = { done = true })

        coVerify(exactly = 1) { authRepository.logout() }
        assertTrue(done)
    }

    @Test
    fun `inicial do avatar usa o nome e cai para o e-mail`() {
        assertEquals("J", jess.initial())
        assertEquals("X", jess.copy(displayName = "  ", email = "xavier@example.com").initial())
    }

    @Test
    fun `ligar o lembrete agenda e desligar cancela`() {
        api.onGetMe = { jess }
        val checked = java.time.LocalDate.of(2026, 10, 4)
        reminderPreferences.lastCheckedDate.value = checked
        val vm = viewModel()
        assertEquals(false, vm.dueRemindersEnabled.value)

        vm.setDueRemindersEnabled(true)

        assertEquals(true, vm.dueRemindersEnabled.value)
        assertEquals(listOf<java.time.LocalDate?>(checked), scheduler.scheduled)

        vm.setDueRemindersEnabled(false)

        assertEquals(false, vm.dueRemindersEnabled.value)
        assertEquals(1, scheduler.cancelled)
    }
}
