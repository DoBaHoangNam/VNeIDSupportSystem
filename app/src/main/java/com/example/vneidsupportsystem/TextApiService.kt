package com.example.vneidsupportsystem

import com.example.vneidsupportsystem.adapter.ChatAdapter
import com.example.vneidsupportsystem.data.ChatRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TextApiService {
    @POST("qa")
    fun sendTextMessage(@Body request: ChatRequest): Call<String>
}