package ua.nure.kryvko.hikeway.data.services.backend

import retrofit2.http.Body
import retrofit2.http.POST
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest

data class SignUpRequestDto(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val username: String,
)

interface BackendAuthService {
    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequestDto)
}

fun SignUpRequest.toDto() = SignUpRequestDto(
    email = email,
    password = password,
    firstName = firstName,
    lastName = lastName,
    username = username,
)
