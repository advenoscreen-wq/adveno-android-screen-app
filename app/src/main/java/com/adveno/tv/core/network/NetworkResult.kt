package com.adveno.tv.core.network

/**
 * Clean Architecture wrapper for all remote data operations.
 */
sealed interface NetworkResult<out T> {

    data class Success<out T>(val data: T) : NetworkResult<T>

    data class Error(val error: NetworkError) : NetworkResult<Nothing>

    data object Loading : NetworkResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error, Loading -> null
    }

    fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (NetworkError) -> Unit): NetworkResult<T> {
        if (this is Error) action(error)
        return this
    }
}


