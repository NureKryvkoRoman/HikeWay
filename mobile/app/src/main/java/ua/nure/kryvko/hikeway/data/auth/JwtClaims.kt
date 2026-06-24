package ua.nure.kryvko.hikeway.data.auth

import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.util.Base64

private data class JwtPayload(
    val sub: String?,
)

class JwtDecoder(
    private val gson: Gson,
) {
    fun subject(accessToken: String): String {
        val payload = accessToken.split('.').getOrNull(1)
            ?: error("Access token is not a JWT.")
        val decodedPayload = runCatching {
            String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
        }.getOrElse {
            error("Access token payload is invalid.")
        }
        return gson.fromJson(decodedPayload, JwtPayload::class.java).sub
            ?: error("Access token does not contain subject.")
    }
}

fun extractJwtSubject(accessToken: String): String {
    return JwtDecoder(Gson()).subject(accessToken)
}
