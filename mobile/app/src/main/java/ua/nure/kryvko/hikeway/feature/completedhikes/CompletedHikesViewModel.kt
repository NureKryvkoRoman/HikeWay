package ua.nure.kryvko.hikeway.feature.completedhikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.ObserveCompletedHikesUseCase
import javax.inject.Inject

data class CompletedHikesUiState(
    val hikes: List<HikeLog> = emptyList(),
    val selectedHike: HikeLog? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class CompletedHikesViewModel @Inject constructor(
    private val observeCompletedHikes: ObserveCompletedHikesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompletedHikesUiState())
    val uiState: StateFlow<CompletedHikesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCompletedHikes()
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Could not load completed hikes.",
                        )
                    }
                }
                .collect { hikes ->
                    _uiState.update {
                        it.copy(
                            hikes = hikes,
                            isLoading = false,
                            errorMessage = null,
                            selectedHike = it.selectedHike?.let { selected ->
                                hikes.firstOrNull { hike -> hike.id == selected.id }
                            },
                        )
                    }
                }
        }
    }

    fun selectHike(hike: HikeLog) {
        _uiState.update { it.copy(selectedHike = hike) }
    }

    fun dismissSelectedHike() {
        _uiState.update { it.copy(selectedHike = null) }
    }

}
