package ua.nure.kryvko.hikeway.data.services.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {
    fun create(
        baseUrl: String,
        gson: Gson,
        client: OkHttpClient = OkHttpClient(),
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl.withTrailingSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}

fun String.withTrailingSlash(): String = if (endsWith('/')) this else "$this/"
