package com.xprokeey2.data.remote.util

import com.xprokeey2.data.remote.dto.common.ErrorResponseDto
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** Runs a Retrofit call and converts every failure into a [DataError]. */
suspend fun <T> safeApiCall(json: Json, call: suspend () -> T): Resource<T> =
    try {
        Resource.Success(call())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Resource.Error(e.toDataError(json))
    } catch (e: SocketTimeoutException) {
        Resource.Error(DataError.Timeout)
    } catch (e: IOException) {
        Resource.Error(DataError.NoInternet)
    } catch (e: Exception) {
        Resource.Error(DataError.Unknown(e.message))
    }

private fun HttpException.toDataError(json: Json): DataError {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val errorDto = body?.let {
        runCatching { json.decodeFromString<ErrorResponseDto>(it) }.getOrNull()
    }
    val message = errorDto?.error ?: message()

    return if (errorDto?.isVerified == false) {
        DataError.AccountNotVerified(message)
    } else {
        DataError.Server(code = code(), message = message)
    }
}
