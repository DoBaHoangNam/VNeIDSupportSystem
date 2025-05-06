package com.example.vneidsupportsystem

import com.example.vneidsupportsystem.adapter.ChatAdapter
import com.example.vneidsupportsystem.data.ChatRequest
import com.example.vneidsupportsystem.data.ChatStructuredResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface StructuredApiService {
    @POST("hdsd")
    fun sendStructuredMessage(@Body request: ChatRequest): Call<ChatStructuredResponse>
}