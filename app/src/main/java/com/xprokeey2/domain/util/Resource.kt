package com.xprokeey2.domain.util

/** Result of any operation that can fail: either the data, or a [DataError]. */
sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val error: DataError) : Resource<Nothing>
}
