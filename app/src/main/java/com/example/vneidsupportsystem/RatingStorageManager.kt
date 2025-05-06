package com.example.vneidsupportsystem

import android.content.Context
import com.example.vneidsupportsystem.data.RatedResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RatingStorageManager {

    private const val PREF_NAME = "rated_responses"

    fun saveRatedResponse(context: Context, rated: RatedResponse) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("responses", "[]")
        val gson = Gson()
        val listType = object : TypeToken<MutableList<RatedResponse>>() {}.type
        val list: MutableList<RatedResponse> = gson.fromJson(jsonString, listType)

        // Nếu đã có câu hỏi đó thì cập nhật rating
        val existing = list.find { it.response == rated.response }
        if (existing != null) {
            existing.rating = rated.rating
        } else {
            list.add(rated)
        }

        sharedPreferences.edit()
            .putString("responses", gson.toJson(list))
            .apply()
    }

    fun getRatedResponse(context: Context, response: String): RatedResponse? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("responses", "[]")
        val gson = Gson()
        val listType = object : TypeToken<List<RatedResponse>>() {}.type
        val list: List<RatedResponse> = gson.fromJson(jsonString, listType)
        return list.find { it.response == response }
    }

}