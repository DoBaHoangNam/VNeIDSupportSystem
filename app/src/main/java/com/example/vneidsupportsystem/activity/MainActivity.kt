package com.example.vneidsupportsystem.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vneidsupportsystem.Mode
import com.example.vneidsupportsystem.R
import com.example.vneidsupportsystem.RetrofitClient
import com.example.vneidsupportsystem.adapter.ChatAdapter
import com.example.vneidsupportsystem.data.ChatRequest
import com.example.vneidsupportsystem.data.ChatStructuredResponse
import com.example.vneidsupportsystem.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.util.Calendar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private var currentMode: Mode = Mode.STRUCTURED


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cập nhật lời chào theo giờ
        updateGreeting()

        // Khởi tạo RecyclerView
        chatAdapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }

        // Xử lý sự kiện gửi tin nhắn
        binding.buttonSend.setOnClickListener {
            val userMessage = ChatRequest(binding.editTextMessage.text.toString())
            chatAdapter.addUserMessage(binding.editTextMessage.text.toString())
            binding.editTextMessage.text.clear()

            if (currentMode == Mode.TEXT) {
                RetrofitClient.getTextApiService().sendTextMessage(userMessage)
                    .enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.isSuccessful) {
                                val reply = response.body() ?: "Không nhận được phản hồi từ hệ thống."
                                Log.d("check_API", "Response Success: $reply")
                                chatAdapter.addBotMessageText(reply)
                            } else {
                                Log.d("check_API", "Response Error: ${response.errorBody()?.string()}")
                                chatAdapter.addBotMessageText("Lỗi hệ thống")
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            Log.d("check_API", "onFailure TEXT: ${t.message}")
                            t.printStackTrace()  // Log chi tiết về lỗi
                            chatAdapter.addBotMessageText("Lỗi kết nối Vui lòng kiểm tra mạng và thử lại.")
                        }
                    })
            } else {
//                test with JSON file
                val reply = readChatStructuredResponseFromAssets(this@MainActivity)
                if (reply != null) {
                    Log.d("check_API", "Response Success: Title = ${reply.Title}, Step = ${reply.steps}")
                    chatAdapter.addBotStructMessage(reply.Title, reply.steps)
                } else {
                    Log.d("check_API", "Response Empty: No data received")
                    chatAdapter.addBotStructMessage("Không nhận được dữ liệu", emptyList())
                }



//              recieve data from BE
//                RetrofitClient.getStructuredApiService().sendStructuredMessage(userMessage)
//                    .enqueue(object : Callback<ChatStructuredResponse> {
//                        override fun onResponse(call: Call<ChatStructuredResponse>, response: Response<ChatStructuredResponse>) {
//                            Log.d("check_API", response.toString())
//                            Log.d("check_API", userMessage.toString() + "  user")
//                            if (response.isSuccessful) {
//                                val reply = response.body()
//                                if (reply != null) {
//                                    Log.d("check_API", "Response Success: Title = ${reply.Title}, Step = ${reply.steps}")
//                                    chatAdapter.addBotStructMessage(reply.Title, reply.steps)
//                                } else {
//                                    Log.d("check_API", "Response Empty: No data received")
//                                    chatAdapter.addBotStructMessage("Không nhận được dữ liệu", emptyList())
//                                }
//                            } else {
//                                Log.d("check_API", "Response Error: ${response.errorBody()?.string()}")
//                                chatAdapter.addBotStructMessage("Không tồn tại câu trả lời", emptyList())
//                            }
//
//
//                        }
//
//                        override fun onFailure(call: Call<ChatStructuredResponse>, t: Throwable) {
//                            Log.d("check_API", "onFailure STRUCTURED: ${t.message}")
//                            t.printStackTrace()  // Log chi tiết về lỗi
//                            chatAdapter.addBotStructMessage("Lỗi kết nối", emptyList())
//                        }
//                    })
            }
        }


        binding.btnDongGop.setOnClickListener {
            val intent = Intent(this, ActivityContribute::class.java)
            startActivity(intent)
        }

        selectTab(binding.tab1)

        binding.tab1.setOnClickListener { v -> selectTab(binding.tab1) }
        binding.tab2.setOnClickListener { v -> selectTab(binding.tab2) }
    }


    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greetingText: String
        val animationRes: Int

        when (hour) {
            in 5..12 -> {
                greetingText = "Chào buổi sáng"
                animationRes = R.raw.sun_loti
            }
            in 13..18 -> {
                greetingText = "Chào buổi chiều"
                animationRes = R.raw.sun_loti
            }
            else -> {
                greetingText = "Chào buổi tối"
                animationRes = R.raw.moon_loti
            }
        }

        binding.welcomeTv.text = greetingText
        binding.statusLoti.setAnimation(animationRes)
        binding.statusLoti.playAnimation()
    }


    private fun selectTab(selectedTab: TextView) {
        val tabs = listOf(binding.tab1, binding.tab2)

        // Reset tất cả tab
        for (tab in tabs) {
            tab.isSelected = false
            tab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start()
        }

        // Chọn tab mới
        selectedTab.isSelected = true
        selectedTab.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .start()

        currentMode = when (selectedTab) {
            binding.tab1 -> Mode.STRUCTURED
            binding.tab2 -> Mode.TEXT
            else -> currentMode
        }
    }

    private fun readChatStructuredResponseFromAssets(context: Context): ChatStructuredResponse? {
        return try {
            val inputStream = context.assets.open("response.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            Gson().fromJson(jsonString, ChatStructuredResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }


    }
}
