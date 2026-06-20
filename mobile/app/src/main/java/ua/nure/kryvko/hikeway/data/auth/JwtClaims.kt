package ua.nure.kryvko.hikeway.data.auth

import java.nio.charset.StandardCharsets
import java.util.Base64

fun extractJwtSubject(accessToken: String): String {
    val payload = accessToken.split('.').getOrNull(1)
        ?: error("Access token is not a JWT.")
    val decodedPayload = runCatching {
        String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
    }.getOrElse {
        error("Access token payload is invalid.")
    }
    return jsonStringClaim(decodedPayload, "sub")
        ?: error("Access token does not contain subject.")
}

private fun jsonStringClaim(json: String, name: String): String? {
    val escapedName = Regex.escape(name)
    return Regex(""""$escapedName"\s*:\s*"((?:\\.|[^"\\])*)"""")
        .find(json)
        ?.groupValues
        ?.get(1)
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")
}
