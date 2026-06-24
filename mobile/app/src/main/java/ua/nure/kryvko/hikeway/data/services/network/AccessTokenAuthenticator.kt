package ua.nure.kryvko.hikeway.data.services.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AccessTokenAuthenticator(
    private val refreshCoordinator: TokenRefreshCoordinator,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount() >= MAX_ATTEMPTS) return null

        val failedToken = response.request.header(AUTHORIZATION_HEADER)
            ?.removePrefix("Bearer ")
        val refreshedSession = refreshCoordinator.refreshAfterUnauthorized(failedToken)
            ?: return null
        return response.request.newBuilder()
            .header(AUTHORIZATION_HEADER, "Bearer ${refreshedSession.accessToken}")
            .build()
    }
}

private fun Response.responseCount(): Int {
    var count = 1
    var current = priorResponse
    while (current != null) {
        count++
        current = current.priorResponse
    }
    return count
}

private const val MAX_ATTEMPTS = 2
