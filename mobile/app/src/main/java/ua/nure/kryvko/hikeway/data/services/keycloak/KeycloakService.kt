package ua.nure.kryvko.hikeway.data.services.keycloak

import com.google.gson.annotations.SerializedName
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

data class TokenResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_in")
    val expiresInSeconds: Long,
)

interface KeycloakService {
    @FormUrlEncoded
    @POST("realms/{realm}/protocol/openid-connect/token")
    suspend fun login(
        @Path("realm") realm: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "password",
        @Field("username") username: String,
        @Field("password") password: String,
    ): TokenResponseDto

    @FormUrlEncoded
    @POST("realms/{realm}/protocol/openid-connect/token")
    suspend fun refresh(
        @Path("realm") realm: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
    ): TokenResponseDto
}
