package com.billfolder.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Estados da Home. Carrega no init; o usuário pode pedir refresh manual.
 *
 * Nota: optei por NÃO carregar o HomeResponse mapeado pra um "domain model"
 * próprio. Pra UI direta, o DTO tá bom — quando ganhar formatação custom
 * por feature ou virar offline-first com Room, aí extraímos.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val data: HomeResponse) : HomeUiState
    data class Error(val message: String) : HomeUiState
    /** Ciclo ainda não criado pelo usuário — backend retorna 404/erro específico. */
    data object NoCycle : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is HomeUiState.Loading) return
        _state.value = HomeUiState.Loading
        load()
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                HomeUiState.Content(homeRepository.getHome())
            } catch (e: HttpException) {
                // Backend retorna 404 quando não há ciclo aberto pra esse usuário.
                if (e.code() == HTTP_NOT_FOUND) {
                    HomeUiState.NoCycle
                } else {
                    HomeUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                HomeUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                HomeUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
