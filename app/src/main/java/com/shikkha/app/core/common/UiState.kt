package com.shikkha.app.core.common

/**
 * Unified async state for screens (Spec 19.3: explicit loading / success / empty / error).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val fromCache: Boolean = false) : UiState<Nothing>
}
