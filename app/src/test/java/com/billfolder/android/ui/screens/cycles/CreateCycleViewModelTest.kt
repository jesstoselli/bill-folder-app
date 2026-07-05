package com.billfolder.android.ui.screens.cycles

import com.billfolder.android.data.dto.CreateCycleRequest
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CreateCycleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CyclesRepository(api, notifier)

    private fun cycleResponse() = CycleResponse(
        id = "cy1", startDate = "2026-06-01", endDate = "2026-06-30", label = "junho/2026",
        isRecurrenceGenerated = false, isCurrent = true,
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun http409() = HttpException(Response.error<Any>(409, ResponseBody.create(null, "")))

    private fun viewModel() = CreateCycleViewModel(repo)

    @Test
    fun `submit com label em branco nao chama o repo`() {
        val vm = viewModel()
        vm.onLabelChange("")

        vm.submit(
            labelEmptyMessage = "label obrigatoria",
            endBeforeStartMessage = "fim antes do inicio",
            duplicateStartMessage = "data duplicada",
        )

        assertEquals("label obrigatoria", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
    }

    @Test
    fun `submit com endDate menor ou igual ao startDate bloqueia`() {
        val vm = viewModel()
        vm.onLabelChange("meu ciclo")
        vm.onStartDateChange("2026-06-10")
        vm.onEndDateChange("2026-06-01")

        vm.submit(
            labelEmptyMessage = "label obrigatoria",
            endBeforeStartMessage = "fim antes do inicio",
            duplicateStartMessage = "data duplicada",
        )

        assertEquals("fim antes do inicio", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
    }

    @Test
    fun `submit valido chama create com o payload correto`() {
        var captured: CreateCycleRequest? = null
        api.onCreateCycle = { captured = it; cycleResponse() }
        val vm = viewModel()
        vm.onLabelChange("  julho/2026  ")
        vm.onStartDateChange("2026-07-01")
        vm.onEndDateChange("2026-07-31")

        vm.submit(
            labelEmptyMessage = "label obrigatoria",
            endBeforeStartMessage = "fim antes do inicio",
            duplicateStartMessage = "data duplicada",
        )

        assertNotNull(captured)
        assertEquals("2026-07-01", captured!!.startDate)
        assertEquals("2026-07-31", captured!!.endDate)
        assertEquals("julho/2026", captured!!.label) // trimado
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun `409 mapeia para a mensagem de data duplicada em PT`() {
        api.onCreateCycle = { throw http409() }
        val vm = viewModel()
        vm.onLabelChange("julho/2026")
        vm.onStartDateChange("2026-07-01")
        vm.onEndDateChange("2026-07-31")

        vm.submit(
            labelEmptyMessage = "label obrigatoria",
            endBeforeStartMessage = "fim antes do inicio",
            duplicateStartMessage = "data duplicada",
        )

        assertEquals("data duplicada", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }
}
