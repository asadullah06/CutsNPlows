package Com.app.cuts.plows.ui.Chat

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.MessageThreadActivityBinding
import Com.app.cuts.plows.ui.BaseActivity
import Com.app.cuts.plows.utils.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException

class MessageThreadActivity : BaseActivity() {
    val TAG = "MessageThreadActivity"
    lateinit var binding: MessageThreadActivityBinding
    var messagingThreadList = ArrayList<MessageThreadModel>()
    lateinit var broadcastReceiver: BroadcastReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MessageThreadActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = intent.getStringExtra("user_name") ?: ""
        supportActionBar?.subtitle = intent.getStringExtra("user_role") ?: ""


        binding.createMessageEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                charSequence: CharSequence,
                arg1: Int,
                arg2: Int,
                arg3: Int
            ) {
                if (binding.createMessageEditText.text.toString().isNotEmpty()) {
                    binding.sendIcon.setImageResource(R.drawable.ic_send_message)
                } else {
                    binding.sendIcon.setImageResource(R.drawable.ic_send_message_default)
                }
            }

            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.sendIcon.setOnClickListener {
            if (binding.createMessageEditText.text.toString().isNotEmpty()) {
                binding.createMessageEditText.hideKeyboard()
                sendMessageAPI()
            }
        }
        getMessagesThreadAPI()
        registerBroadCastReceiver()
    }

    override fun onResume() {
        super.onResume()
        CommonMethods.activityResumed()
    }

    override fun onPause() {
        super.onPause()
        CommonMethods.activityPausedOrFinished()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getMessagesThreadAPI() {

        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.getMessagesThread(
            UserPreferences.getClassInstance(this).getUserId() ?: "",
            intent.getStringExtra("receiver_id") ?: ""
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            this@MessageThreadActivity,
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        val dataArray = responseObject.getJSONArray("data")
                        populateMessagesList(dataArray)
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@MessageThreadActivity,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }

    private fun sendMessageAPI() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.sendMessage(
            binding.createMessageEditText.text.toString(),
            UserPreferences.getClassInstance(this).getUserId() ?: "",
            intent.getStringExtra("receiver_id") ?: ""
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            this@MessageThreadActivity,
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        val dataArray = responseObject.getJSONArray("data")
                        binding.createMessageEditText.setText("")
                        populateMessagesList(dataArray)
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@MessageThreadActivity,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }

    private fun populateMessagesList(dataArray: JSONArray) {
        messagingThreadList.clear()

        for (i in dataArray.length() - 1 downTo 0) {
            val messageObject = dataArray.getJSONObject(i)
            val message = messageObject.getString("fld_message")
            val senderId = messageObject.getString("fld_sender_id")
            val originalDate = messageObject.getString("fld_created_date")
            var formattedTime = ""
            var formattedDate = ""
            formattedTime = try {
                CommonMethods.formatDateFromDateString(
                    "yyyy-MM-dd HH:mm:ss",
                    "hh:mm a",
                    originalDate
                )
            } catch (exception: ParseException) {
                exception.printStackTrace()
                originalDate
            }
            try {
                formattedDate = CommonMethods.formatDateFromDateString(
                    "yyyy-MM-dd HH:mm:ss",
                    "dd-MMM-yyyy",
                    originalDate
                )
            } catch (exception: ParseException) {
                exception.printStackTrace()
            }
            if (formattedDate.isNotEmpty() && i == dataArray.length() - 1) {
                messagingThreadList.add(
                    MessageThreadModel(
                        "",
                        DATE_TYPE_LABEL,
                        formattedDate
                    )
                )
            } else if (formattedDate.isNotEmpty() && i < dataArray.length() - 1) {
                val previousDate = CommonMethods.formatDateFromDateString(
                    "yyyy-MM-dd HH:mm:ss",
                    "dd-MMM-yyyy",
                    dataArray.getJSONObject(i + 1).getString("fld_created_date")
                )
                if (formattedDate != previousDate) {
                    messagingThreadList.add(
                        MessageThreadModel(
                            "",
                            DATE_TYPE_LABEL,
                            formattedDate
                        )
                    )
                }
            }
            val messageType =
                if (senderId == UserPreferences.getClassInstance(this@MessageThreadActivity)
                        .getUserId() ?: ""
                )
                    SENDER_TYPE_MESSAGE
                else
                    RECEIVER_TYPE_MESSAGE
            messagingThreadList.add(
                MessageThreadModel(
                    message,
                    messageType,
                    formattedTime
                )
            )
        }
        if (messagingThreadList.size > 0) {
            val messagesThreadAdapter = MessagesThreadAdapter(
                messagingThreadList,
                this@MessageThreadActivity
            )
            val linearLayoutManager = LinearLayoutManager(this)
            binding.conversationRecyclerview.layoutManager = linearLayoutManager
            binding.conversationRecyclerview.adapter = messagesThreadAdapter
            binding.conversationRecyclerview.scrollToPosition(messagingThreadList.size - 1)
        }
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun registerBroadCastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                getMessagesThreadAPI()
            }
        }

        registerReceiver(broadcastReceiver, IntentFilter(UPDATE_MESSAGES))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        CommonMethods.activityPausedOrFinished()
    }
}