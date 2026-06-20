package com.kabarinpacar.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kabarinpacar.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val generatedCode: String = "",
    val enteredCode: String = "",
    val errorMessage: String = "",
    val isPaired: Boolean = false,
    val pairId: String = "",
    val isWaitingForPartner: Boolean = false,
    val nickname: String = ""
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val statusRepository: StatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(nickname = statusRepository.nickname))
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun updateNickname(name: String) {
        statusRepository.setNickname(name)
        _uiState.value = _uiState.value.copy(nickname = name)
    }

    fun generateCode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")
            try {
                val code = statusRepository.generatePairingCode()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedCode = code,
                    isWaitingForPartner = true
                )
                waitForPartnerToJoin(code)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal membuat kode: ${e.message}"
                )
            }
        }
    }

    private fun waitForPartnerToJoin(code: String) {
        statusRepository.listenForPairingConfirmation(code) { pairDocId, partnerId ->
            _uiState.value = _uiState.value.copy(
                isPaired = true,
                pairId = pairDocId,
                isWaitingForPartner = false
            )
        }
    }

    fun updateEnteredCode(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(enteredCode = code, errorMessage = "")
        }
    }

    fun joinWithCode() {
        val code = _uiState.value.enteredCode
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Kode harus 6 digit")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")
            val result = statusRepository.joinWithCode(code)
            result.fold(
                onSuccess = { pairDocId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPaired = true,
                        pairId = pairDocId
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Gagal bergabung"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }
}
