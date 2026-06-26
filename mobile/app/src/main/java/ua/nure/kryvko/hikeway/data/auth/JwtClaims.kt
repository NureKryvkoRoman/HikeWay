package ua.nure.kryvko.hikeway.data.auth

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.nio.charset.StandardCharsets
import java.util.Base64

private data class JwtPayload(
    val sub: String?,
    @SerializedName("realm_access")
    val realmAccess: JwtRoleAccess? = null,
    @SerializedName("resource_access")
    val resourceAccess: Map<String, JwtRoleAccess>? = null,
)

private data class JwtRoleAccess(
    val roles: List<String>? = null,
)

class JwtDecoder(
    private val gson: Gson,
) {
    fun subject(accessToken: String): String {
        return payload(accessToken).sub
            ?: error("Access token does not contain subject.")
    }

    fun roles(accessToken: String): Set<String> {
        val payload = payload(accessToken)
        return buildSet {
            payload.realmAccess?.roles.orEmpty().forEach(::add)
            payload.resourceAccess.orEmpty().values
                .flatMap { it.roles.orEmpty() }
                .forEach(::add)
        }
    }

    private fun payload(accessToken: String): JwtPayload {
        val payload = accessToken.split('.').getOrNull(1)
            ?: error("Access token is not a JWT.")
        val decodedPayload = runCatching {
            String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
        }.getOrElse {
            error("Access token payload is invalid.")
        }
        return gson.fromJson(decodedPayload, JwtPayload::class.java)
    }
}

fun extractJwtSubject(accessToken: String): String {
    return JwtDecoder(Gson()).subject(accessToken)
}
