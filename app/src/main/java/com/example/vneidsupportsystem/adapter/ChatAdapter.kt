package com.example.vneidsupportsystem.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.vneidsupportsystem.MessageDiffCallback
import com.example.vneidsupportsystem.R
import com.example.vneidsupportsystem.RatingStorageManager
import com.example.vneidsupportsystem.data.ChatRequest
import com.example.vneidsupportsystem.data.ChatResponseText
import com.example.vneidsupportsystem.data.ChatStructuredResponse
import com.example.vneidsupportsystem.data.Message
import com.example.vneidsupportsystem.data.RatedResponse
import com.example.vneidsupportsystem.data.Step
import com.example.vneidsupportsystem.databinding.ItemChatMessageBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val messages = mutableListOf<Message>()


    class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding =
            ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]


        Log.d("ChatAdapter", "Binding message at position $position: $message")


        holder.binding.stepContainer.removeAllViews()

        when (message) {
            is ChatRequest -> {
                Log.d("ChatAdapter", "ChatRequest: ${message.text}")
                holder.binding.userMessage.apply {
                    text = message.text
                    visibility = View.VISIBLE
                }
                holder.binding.botMessageCard.visibility = View.GONE
            }

            is ChatStructuredResponse -> {
                Log.d(
                    "ChatAdapter",
                    "ChatStructuredResponse: Title=${message.Title}, Steps=${message.steps.size}"
                )
                holder.binding.botMessageCard.visibility = View.VISIBLE
                holder.binding.userMessage.visibility = View.GONE

                holder.binding.titleText.apply {
                    text = message.Title
                    visibility = if (message.Title.isNullOrEmpty()) View.GONE else View.VISIBLE
                }

                holder.binding.noteText.apply {
                    visibility = View.GONE
                }


                if (message.steps.isEmpty()) {
                    Log.d("ChatAdapter", "No steps available.")
                    holder.binding.stepContainer.visibility = View.GONE
                } else {
                    Log.d("ChatAdapter", "Adding ${message.steps.size} steps.")
                    holder.binding.stepContainer.visibility = View.VISIBLE
                    addStepViews(holder.binding.stepContainer, message.steps)
                }

                val ratingBar = holder.binding.ratingBar
                ratingBar.visibility = View.VISIBLE
                val savedRating = RatingStorageManager.getRatedResponse(holder.binding.root.context,
                    message.Title.toString()
                )
                if (savedRating != null) {
                    ratingBar.rating = savedRating.rating
                }


                // Khi người dùng đánh giá
                ratingBar.setOnRatingBarChangeListener { _, newRating, fromUser ->
                    Log.d("ChatAdapter", "User rated '$message.Title.toString()' with $newRating stars")
                    if (fromUser) {
                        val response = message.Title

                        val existing = RatingStorageManager.getRatedResponse(holder.binding.root.context,
                            response.toString()
                        )

                        if (existing != null && existing.rating != newRating) {
                            // Xóa cũ khỏi Firebase
                            deleteOldRatingFromFirebase(response.toString(), existing.rating)
                        }

                        // Lưu mới vào local
                        RatingStorageManager.saveRatedResponse(
                            holder.binding.root.context,
                            RatedResponse(response.toString(), newRating)
                        )

                        // Gửi mới lên Firebase
                        saveRatingToFirebase(response.toString(), newRating)
                    }
                }

            }

            is ChatResponseText -> {
                Log.d("ChatAdapter", "ChatResponseText: ${message.message}")
                holder.binding.botMessageCard.visibility = View.VISIBLE
                holder.binding.userMessage.visibility = View.GONE


                holder.binding.titleText.apply {
                    text = message.message
                    visibility = if (message.message.isNullOrEmpty()) View.GONE else View.VISIBLE
                }

                val ratingBar = holder.binding.ratingBar
                ratingBar.visibility = View.VISIBLE
                val savedRating = RatingStorageManager.getRatedResponse(holder.binding.root.context,
                    message.message.toString()
                )
                if (savedRating != null) {
                    ratingBar.rating = savedRating.rating
                }

                // Khi người dùng đánh giá
                ratingBar.setOnRatingBarChangeListener { _, newRating, fromUser ->
                    Log.d("ChatAdapter", "User rated '$message.Title.toString()' with $newRating stars")
                    if (fromUser) {
                        val response = message.message

                        val existing = RatingStorageManager.getRatedResponse(holder.binding.root.context,
                            response.toString()
                        )

                        if (existing != null && existing.rating != newRating) {
                            // Xóa cũ khỏi Firebase
                            deleteOldRatingFromFirebase(response.toString(), existing.rating)
                        }

                        // Lưu mới vào local
                        RatingStorageManager.saveRatedResponse(
                            holder.binding.root.context,
                            RatedResponse(response.toString(), newRating)
                        )

                        // Gửi mới lên Firebase
                        saveRatingToFirebase(response.toString(), newRating)
                    }

                }

            }
        }
    }


    override fun getItemCount() = messages.size

    fun addUserMessage(text: String) {
        updateMessages(messages + ChatRequest(text))
    }

    fun addBotStructMessage(title: String?, steps: List<Step>) {
        updateMessages(messages + ChatStructuredResponse(title, steps))
    }

    fun addBotMessageText(message: String) {
        updateMessages(messages + ChatResponseText(message))
    }


    private fun updateMessages(newList: List<Message>) {
        Log.d(
            "ChatAdapter",
            "Updating messages. New list size: ${newList.size}, Old list size: ${messages.size}"
        )

        val diffCallback = MessageDiffCallback(messages, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newList)
        diffResult.dispatchUpdatesTo(this)

        Log.d("ChatAdapter", "Messages updated.")
    }


    private fun addStepViews(stepContainer: LinearLayout, steps: List<Step>) {
        val context = stepContainer.context
        stepContainer.removeAllViews() // Xóa nội dung cũ trước khi thêm mới

        Log.d("ChatAdapter", "Adding ${steps.size} steps to container.")

        steps.forEach { step ->
            val stepView =
                LayoutInflater.from(context).inflate(R.layout.item_step, stepContainer, false)
            val stepTitle = stepView.findViewById<TextView>(R.id.stepTitle)
            val stepContent = stepView.findViewById<TextView>(R.id.stepContent)
            val imageContainer = stepView.findViewById<LinearLayout>(R.id.imageContainer)

            stepTitle.text = step.name_of_step
            stepContent.text = step.full_content_of_step


            Log.d(
                "ChatAdapter",
                "Step: Title=${step.name_of_step}, Content=${step.full_content_of_step}, ImageBase64=${step.image.base64.size}"
            )


            imageContainer.removeAllViews()

            if (step.image.base64.isNotEmpty()) {
                imageContainer.visibility = View.VISIBLE
                step.image.base64.forEach { base64 ->
                    val imageView = ImageView(context)
                    imageView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }

                    // Giải mã base64 thành Bitmap
                    decodeBase64ToBitmap(base64)?.let {
                        imageView.setImageBitmap(it)
                        imageContainer.addView(imageView)
                        Log.d("ChatAdapter", "Added image from base64 string.")
                    }
                }
            } else {
                imageContainer.visibility = View.GONE
            }

            stepContainer.addView(stepView)
        }
    }


    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }



    fun saveRatingToFirebase(title: String, newRating: Float) {
        // Chuyển rating thành chuỗi hợp lệ cho Firebase key
        val safeRating = newRating.toString().replace(".", "_")
        val starKey = "${safeRating}_star"
        Log.e("Rating", "new rating ${newRating}")

        // Tạo reference đến rating hiện tại
        val ratingRef = FirebaseDatabase.getInstance()
            .getReference("ratings")
            .child(title)
            .child(starKey)

        ratingRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentCount = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentCount + 1 // Cộng thêm 1 vào số lượng rating hiện tại
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    Log.e("Rating", "Failed to update rating: ${error.message}")
                } else {
                    Log.d("Rating", "Rating updated successfully")
                }
            }
        })
    }


    fun deleteOldRatingFromFirebase(title: String, oldRating: Float) {
        val safeRating = oldRating.toString().replace(".", "_")
        val starKey = "${safeRating}_star"
        Log.e("Rating", "Old rating ${oldRating}")

        val ratingRef = FirebaseDatabase.getInstance()
            .getReference("ratings")
            .child(title)
            .child(starKey)

        ratingRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentCount = currentData.getValue(Int::class.java) ?: 0
                if (currentCount > 1) {
                    currentData.value = currentCount - 1
                } else {
                    currentData.value = null // nếu chỉ còn 1 thì xóa nút luôn
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    Log.e("Rating", "Failed to remove old rating: ${error.message}")
                } else {
                    Log.d("Rating", "Old rating removed")
                }
            }
        })
    }








}
