package com.kabarinpacar.app.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.kabarinpacar.app.data.model.ActivityType
import com.kabarinpacar.app.data.model.PartnerStatus
import com.kabarinpacar.app.data.model.StatusUpdate
import com.kabarinpacar.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class MainUiState(
    val selectedActivity: ActivityType? = null,
    val note: String = "",
    val locationName: String = "",
    val isLoadingLocation: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String = "",
    val locationOptIn: Boolean = false,
    val reminderIntervalHours: Int = StatusRepository.DEFAULT_REMINDER_INTERVAL
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            locationOptIn = statusRepository.locationOptIn,
            reminderIntervalHours = statusRepository.reminderIntervalHours
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState

    val todayStatuses = statusRepository.getTodayStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestStatus = statusRepository.getLatestStatusFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val partnerStatus: StateFlow<PartnerStatus?> = statusRepository.listenToPartnerStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPaired: Boolean get() = statusRepository.isPaired

    init {
        viewModelScope.launch {
            statusRepository.cleanOldRecords()
        }
    }

    fun selectActivity(activity: ActivityType) {
        _uiState.value = _uiState.value.copy(selectedActivity = activity, submitSuccess = false)
        if (_uiState.value.locationOptIn) {
            fetchLocation()
        }
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun toggleLocationOptIn(enabled: Boolean) {
        statusRepository.setLocationOptIn(enabled)
        _uiState.value = _uiState.value.copy(locationOptIn = enabled)
        if (enabled) fetchLocation()
        else _uiState.value = _uiState.value.copy(locationName = "")
    }

    fun setReminderInterval(hours: Int) {
        statusRepository.setReminderInterval(hours)
        _uiState.value = _uiState.value.copy(reminderIntervalHours = hours)
    }

    fun fetchLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _uiState.value = _uiState.value.copy(locationName = "Izin lokasi diperlukan")
            return
        }

        _uiState.value = _uiState.value.copy(isLoadingLocation = true)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModelScope.launch {
                    try {
                        val geocoder = Geocoder(context, Locale("id", "ID"))
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val area = if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            addr.subLocality ?: addr.locality ?: addr.subAdminArea ?: "Area tidak diketahui"
                        } else {
                            "Area tidak diketahui"
                        }
                        _uiState.value = _uiState.value.copy(
                            locationName = area,
                            isLoadingLocation = false
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            locationName = "",
                            isLoadingLocation = false
                        )
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    locationName = "",
                    isLoadingLocation = false
                )
            }
        }.addOnFailureListener {
            _uiState.value = _uiState.value.copy(
                locationName = "",
                isLoadingLocation = false
            )
        }
    }

    fun submitStatus() {
        val activity = _uiState.value.selectedActivity ?: return
        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = "")

        viewModelScope.launch {
            try {
                val status = StatusUpdate(
                    userId = statusRepository.userId,
                    activity = activity.name,
                    note = _uiState.value.note,
                    locationName = if (_uiState.value.locationOptIn) _uiState.value.locationName else "",
                    timestamp = System.currentTimeMillis()
                )
                statusRepository.insertStatus(status)
                statusRepository.pushStatusToFirestore(status)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true,
                    selectedActivity = null,
                    note = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Gagal kirim status: ${e.message}"
                )
            }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }
}
