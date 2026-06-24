package ua.nure.kryvko.hikeway.data.services.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import retrofit2.HttpException

class ApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

private data class ApiErrorDto(
    @SerializedName("error_description")
    val errorDescription: String? = null,
    val message: String? = null,
    val error: String? = null,
)

fun Throwable.toApiException(
    gson: Gson,
    fallbackMessage: String,
): ApiException {
    if (this is ApiException) return this
    val message = when (this) {
        is HttpException -> response()?.errorBody()?.string()
            ?.takeIf(String::isNotBlank)
            ?.let { body ->
                runCatching { gson.fromJson(body, ApiErrorDto::class.java) }
                    .getOrNull()
                    ?.let { it.errorDescription ?: it.message ?: it.error }
                    ?: body
            }
            ?: "$fallbackMessage (${code()})"
        is IOException -> "Network error: ${message ?: fallbackMessage}"
        else -> message ?: fallbackMessage
    }
    return ApiException(message, this)
}
