package Com.app.cuts.plows.ui.Chat

import Com.app.cuts.plows.R
import Com.app.cuts.plows.utils.RECEIVER_TYPE_MESSAGE
import Com.app.cuts.plows.utils.SENDER_TYPE_MESSAGE
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessagesThreadAdapter(
    private val messagesThreadList: ArrayList<MessageThreadModel>,
    context: Context
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == SENDER_TYPE_MESSAGE) {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.sender_list_row, parent, false)
            SenderViewHolder(itemView)
        } else if (viewType == RECEIVER_TYPE_MESSAGE) {
            val itemView =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.receiver_list_row, parent, false)
            ReceiverViewHolder(itemView)
        } else {
            val itemView =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.date_listrow, parent, false)
            DateViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            getItemViewType(position) == SENDER_TYPE_MESSAGE -> {
                val senderViewHolder = holder as SenderViewHolder
                senderViewHolder.bindItem(messagesThreadList[position])
            }
            getItemViewType(position) == RECEIVER_TYPE_MESSAGE -> {
                val receiverViewHolder = holder as ReceiverViewHolder
                receiverViewHolder.bindItem(messagesThreadList[position])
            }
            else -> {
                val dateViewHolder = holder as DateViewHolder
                dateViewHolder.bindItem(messagesThreadList[position])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return messagesThreadList[position].messageType
    }

    override fun getItemCount() = messagesThreadList.size

    class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(messageThreadModel: MessageThreadModel) {
            val messageTextView = itemView.findViewById<TextView>(R.id.receiver_message_text_view)
            messageTextView.text = messageThreadModel.message
            val timeTextView = itemView.findViewById<TextView>(R.id.receiver_message_time)
            timeTextView.text = messageThreadModel.dateTime
        }
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(messageThreadModel: MessageThreadModel) {
            val dateTextView = itemView.findViewById<TextView>(R.id.dateText_view)
            dateTextView.text = messageThreadModel.dateTime
        }
    }

    class SenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(messageThreadModel: MessageThreadModel) {
            val messageTextView = itemView.findViewById<TextView>(R.id.sender_message_text_view)
            messageTextView.text = messageThreadModel.message
            val timeTextView = itemView.findViewById<TextView>(R.id.sender_message_time)
            timeTextView.text = messageThreadModel.dateTime
        }
    }
}