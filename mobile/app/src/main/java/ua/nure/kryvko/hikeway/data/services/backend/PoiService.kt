package ua.nure.kryvko.hikeway.data.services.backend

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import ua.nure.kryvko.hikeway.data.pois.remote.PageResponseDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiCreateRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiCommentDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiCommentRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiDetailDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiNearbySummaryDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiPhotoDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiPhotoFinalizeRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiPhotoUpdateRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiPhotoUploadRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiRatingDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiRatingRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiSummaryDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiUpdateRequestDto
import ua.nure.kryvko.hikeway.data.pois.remote.PoiUploadDto

interface PoiService {
    @GET("pois")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PageResponseDto<PoiSummaryDto>

    @GET("pois/nearby")
    suspend fun nearby(
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("radiusMeters") radiusMeters: Double,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PageResponseDto<PoiNearbySummaryDto>

    @GET("pois/{id}")
    suspend fun get(@Path("id") id: Long): PoiDetailDto

    @POST("pois")
    suspend fun create(@Body request: PoiCreateRequestDto): PoiDetailDto

    @PATCH("pois/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body request: PoiUpdateRequestDto,
    ): PoiDetailDto

    @DELETE("pois/{id}")
    suspend fun delete(@Path("id") id: Long)

    @PUT("pois/{id}/rating")
    suspend fun rate(
        @Path("id") id: Long,
        @Body request: PoiRatingRequestDto,
    ): PoiRatingDto

    @DELETE("pois/{id}/rating")
    suspend fun removeRating(@Path("id") id: Long): PoiRatingDto

    @GET("pois/{id}/comments")
    suspend fun comments(
        @Path("id") id: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PageResponseDto<PoiCommentDto>

    @POST("pois/{id}/comments")
    suspend fun addComment(
        @Path("id") id: Long,
        @Body request: PoiCommentRequestDto,
    ): PoiCommentDto

    @PATCH("pois/{id}/comments/{commentId}")
    suspend fun updateComment(
        @Path("id") id: Long,
        @Path("commentId") commentId: Long,
        @Body request: PoiCommentRequestDto,
    ): PoiCommentDto

    @DELETE("pois/{id}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("id") id: Long,
        @Path("commentId") commentId: Long,
    )

    @POST("pois/{id}/photos/uploads")
    suspend fun createPhotoUpload(
        @Path("id") id: Long,
        @Body request: PoiPhotoUploadRequestDto,
    ): PoiUploadDto

    @POST("pois/{id}/photos")
    suspend fun finalizePhoto(
        @Path("id") id: Long,
        @Body request: PoiPhotoFinalizeRequestDto,
    ): PoiPhotoDto

    @PATCH("pois/{id}/photos/{photoId}")
    suspend fun updatePhoto(
        @Path("id") id: Long,
        @Path("photoId") photoId: Long,
        @Body request: PoiPhotoUpdateRequestDto,
    ): PoiPhotoDto

    @DELETE("pois/{id}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("id") id: Long,
        @Path("photoId") photoId: Long,
    )

    @PUT
    suspend fun uploadPhotoBytes(
        @Url uploadUrl: String,
        @Body body: RequestBody,
    ): ResponseBody
}
