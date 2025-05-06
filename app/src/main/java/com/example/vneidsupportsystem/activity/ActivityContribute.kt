package com.example.vneidsupportsystem.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import com.example.vneidsupportsystem.databinding.ActivityContributeBinding
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

class ActivityContribute : AppCompatActivity() {
    private lateinit var binding: ActivityContributeBinding
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContributeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Xử lý chọn hình ảnh
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        // Thêm vào sự kiện nhấn nút "Gửi ý kiến"
        binding.btnSubmit.setOnClickListener {
            val description = binding.edtDescription.text.toString()
            if (description.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mô tả vấn đề!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển ảnh sang Base64 (nếu có)
            val base64Image = selectedImageUri?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmapToBase64(bitmap)
                }
            } ?: ""

            // Tạo đối tượng JSON
            val feedbackJson = JSONObject().apply {
                put("text", description)
                put("image", base64Image)
            }

            // Lưu JSON vào file
            saveJsonToExternalStorage(feedbackJson)
            saveFeedbackToFirebase(description,base64Image)

            Toast.makeText(this, "Cảm ơn bạn đã đóng góp!", Toast.LENGTH_SHORT).show()
            finish()
        }


    }

    // Nhận ảnh từ thư viện
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            binding.imgSelected.setImageURI(selectedImageUri)
            binding.imgSelected.visibility = View.VISIBLE
        }
    }

    // Hàm chuyển ảnh Bitmap sang Base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveJsonToExternalStorage(jsonObject: JSONObject) {
        val directory = getExternalFilesDir(null) // Thư mục lưu file ngoài bộ nhớ riêng của app
        val file = File(directory, "feedback.json")

        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(jsonObject.toString().toByteArray())
            }
            Log.d("FilePath", "File saved at: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveFeedbackToFirebase(content: String, image: String) {
        val ref = FirebaseDatabase.getInstance().getReference("feedbacks")
        val newKey = ref.push().key
        if (newKey != null) {
            val feedback = mapOf(
                "content" to content,
                "image" to image
            )

            ref.child(newKey).setValue(feedback)
                .addOnSuccessListener {
                    // Thành công
                    Log.d("Firebase", "Feedback uploaded successfully")
                }
                .addOnFailureListener { e ->
                    // Thất bại
                    Log.e("Firebase", "Error uploading feedback", e)
                }
        } else {
            Log.e("Firebase", "Failed to generate unique key")
        }
    }

}