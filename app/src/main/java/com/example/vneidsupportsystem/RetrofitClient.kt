package com.example.vneidsupportsystem

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

//    private val loggingInterceptor = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Thời gian kết nối là 60 giây
        .readTimeout(60, TimeUnit.SECONDS)     // Thời gian đọc dữ liệu là 60 giây
        .writeTimeout(60, TimeUnit.SECONDS)    // Thời gian ghi dữ liệu là 60 giây
        .build()

    private fun getRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Tạo API service cho từng chế độ
    fun getTextApiService(): TextApiService {
        return getRetrofit("https://2e5d-2001-ee0-470e-d110-c05-f3eb-4fa7-11c2.ngrok-free.app/").create(TextApiService::class.java)
    }

    fun getStructuredApiService(): StructuredApiService {
        return getRetrofit("https://2e5d-2001-ee0-470e-d110-c05-f3eb-4fa7-11c2.ngrok-free.app/").create(StructuredApiService::class.java)
    }
}
