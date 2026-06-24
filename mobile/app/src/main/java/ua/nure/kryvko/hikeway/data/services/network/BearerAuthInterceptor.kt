package ua.nure.kryvko.hikeway.data.services.network

import okhttp3.Interceptor
import okhttp3.Response
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager

class BearerAuthInterceptor(
    private val sessionManager: AuthSessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = sessionManager.validStoredSession()?.accessToken
        val request = if (accessToken == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header(AUTHORIZATION_HEADER, "Bearer $accessToken")
                .build()
        }
        return chain.proceed(request)
    }
}

internal const val AUTHORIZATION_HEADER = "Authorization"
