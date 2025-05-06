package com.example.vneidsupportsystem

import androidx.recyclerview.widget.DiffUtil
import com.example.vneidsupportsystem.data.ChatRequest
import com.example.vneidsupportsystem.data.ChatResponseText
import com.example.vneidsupportsystem.data.ChatStructuredResponse
import com.example.vneidsupportsystem.data.Message

class MessageDiffCallback(
    private val oldList: List<Message>,
    private val newList: List<Message>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when {
            oldItem is ChatRequest && newItem is ChatRequest -> oldItem.text == newItem.text
            oldItem is ChatStructuredResponse && newItem is ChatStructuredResponse-> oldItem.Title == newItem.Title && oldItem.steps == newItem.steps
            oldItem is ChatResponseText && newItem is ChatResponseText-> oldItem.message == newItem.message
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
