package ua.nure.kryvko.hikeway.data.auth

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
)

interface AuthApi {
    suspend fun login(username: String, password: String): TokenResponse
    suspend fun refresh(refreshToken: String): TokenResponse
    suspend fun signUp(request: SignUpRequest)
}

class HttpAuthApi(
    private val backendBaseUrl: String,
    private val keycloakBaseUrl: String,
) : AuthApi {
    override suspend fun login(username: String, password: String): TokenResponse {
        return postTokenForm(
            form = keycloakLoginForm(username = username, password = password),
            errorPrefix = "Could not log in",
        )
    }

    override suspend fun refresh(refreshToken: String): TokenResponse {
        return postTokenForm(
            form = keycloakRefreshForm(refreshToken),
            errorPrefix = "Could not refresh session",
        )
    }

    override suspend fun signUp(request: SignUpRequest) {
        val response = post(
            url = "$backendBaseUrl/auth/signup",
            body = request.toSignUpJson(),
            contentType = "application/json",
        )
        if (response.status !in 200..299) {
            throw AuthNetworkException(response.body.ifBlank { "Could not sign up" })
        }
    }

    private suspend fun postTokenForm(
        form: Map<String, String>,
        errorPrefix: String,
    ): TokenResponse {
        val response = post(
            url = "$keycloakBaseUrl/realms/hikeway-keycloak/protocol/openid-connect/token",
            body = form.toFormBody(),
            contentType = "application/x-www-form-urlencoded",
        )
        if (response.status !in 200..299) {
            throw AuthNetworkException(response.body.extractJsonString("error_description") ?: errorPrefix)
        }
        return response.body.toTokenResponse()
    }

    private suspend fun post(
        url: String,
        body: String,
        contentType: String,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            HttpResponse(status = status, body = stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } catch (exception: IOException) {
            throw AuthNetworkException("Network error: ${exception.message ?: "request failed"}")
        } finally {
            connection.disconnect()
        }
    }
}

class AuthNetworkException(message: String) : RuntimeException(message)

data class HttpResponse(
    val status: Int,
    val body: String,
)

fun keycloakLoginForm(username: String, password: String): Map<String, String> {
    return mapOf(
        "client_id" to "backend-test",
        "grant_type" to "password",
        "username" to username,
        "password" to password,
    )
}

fun keycloakRefreshForm(refreshToken: String): Map<String, String> {
    return mapOf(
        "client_id" to "backend-test",
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
    )
}

fun SignUpRequest.toSignUpJson(): String {
    return buildString {
        append("{")
        appendJsonField("email", email)
        append(",")
        appendJsonField("password", password)
        append(",")
        appendJsonField("firstName", firstName)
        append(",")
        appendJsonField("lastName", lastName)
        append(",")
        appendJsonField("username", username)
        append("}")
    }
}

private fun StringBuilder.appendJsonField(name: String, value: String) {
    append("\"")
    append(name)
    append("\":\"")
    append(value.escapeJson())
    append("\"")
}

private fun String.escapeJson(): String {
    return flatMap { char ->
        when (char) {
            '\\' -> listOf('\\', '\\')
            '"' -> listOf('\\', '"')
            '\n' -> listOf('\\', 'n')
            '\r' -> listOf('\\', 'r')
            '\t' -> listOf('\\', 't')
            else -> listOf(char)
        }
    }.joinToString(separator = "")
}

private fun Map<String, String>.toFormBody(): String {
    return entries.joinToString(separator = "&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
}

private fun String.urlEncode(): String {
    return URLEncoder.encode(this, Charsets.UTF_8.name())
}

private fun String.toTokenResponse(): TokenResponse {
    return TokenResponse(
        accessToken = extractJsonString("access_token") ?: throw AuthNetworkException("Missing access token"),
        refreshToken = extractJsonString("refresh_token"),
        expiresInSeconds = extractJsonLong("expires_in") ?: 0L,
    )
}

private fun String.extractJsonString(name: String): String? {
    val pattern = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"])*)"""")
    return pattern.find(this)?.groupValues?.get(1)?.unescapeJson()
}

private fun String.extractJsonLong(name: String): Long? {
    val pattern = Regex(""""${Regex.escape(name)}"\s*:\s*(\d+)""")
    return pattern.find(this)?.groupValues?.get(1)?.toLongOrNull()
}

private fun String.unescapeJson(): String {
    return replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
}
