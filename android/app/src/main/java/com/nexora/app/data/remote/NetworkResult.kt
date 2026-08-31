package com.nexora.app.data.remote

/**
 * Generic result wrapper for handling network states in UI and Repository layers.
 */
sealed class NetworkResult<out T> {

    /**
     * Successful result containing data of type [T].
     */
    data class Success<out T>(val data: T) : NetworkResult<T>()

    /**
     * Failure result containing a [NetworkError].
     */
    data class Error(val error: NetworkError) : NetworkResult<Nothing>()

    /**
     * In-progress loading state.
     */
    data object Loading : NetworkResult<Nothing>()

    fun <R> map(transform: (T) -> R): NetworkResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(error)
            is Loading -> Loading
        }
    }

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrElse(default: @UnsafeVariance T): T = (this as? Success)?.data ?: default

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (NetworkError) -> Unit): NetworkResult<T> {
        if (this is Error) action(error)
        return this
    }

    inline fun onLoading(action: () -> Unit): NetworkResult<T> {
        if (this is Loading) action()
        return this
    }
}
