package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.Adapters.ProvidersListingAdapter
import Com.app.cuts.plows.Models.UserDetailsModel
import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.MessagingFragmentBinding
import Com.app.cuts.plows.utils.UPDATE_MESSAGES
import Com.app.cuts.plows.utils.UserPreferences
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessagingFragment : Fragment() {
    val TAG = "MessagingFragment"
    lateinit var binding: MessagingFragmentBinding
    var providersList: ArrayList<UserDetailsModel> = ArrayList()
    lateinit var providersListingAdapter: ProvidersListingAdapter
    lateinit var broadcastReceiver: BroadcastReceiver
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MessagingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getConversationsList()
        registerBroadCastReceiver()
    }

    private fun getConversationsList() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.getConversationsList(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        val dataArray = responseObject.getJSONArray("data")
                        populateList(dataArray)
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        context,
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

    private fun populateList(dataArray: JSONArray) {
        providersList.clear()
        for (i in 0 until dataArray.length()) {
            val userObject = dataArray.getJSONObject(i)
            val userRole: String = when {
                userObject.getInt("fld_role") == 1 -> resources.getString(R.string.customer)
                userObject.getInt("fld_role") == 2 -> resources.getString(R.string.service_provider)
                else -> ""
            }
            providersList.add(
                UserDetailsModel(
                    userObject.getString("fld_fname") + " " + userObject.getString("fld_lname"),
                    userRole,
                    userObject.getString("fld_contact_number"),
                    userObject.getString("fld_profile_pic") ?: "",
                    userObject.getDouble("rating").toFloat(),
                    userObject.getString("fld_userid")
                )
            )
        }
        binding.customersRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        providersListingAdapter =
            ProvidersListingAdapter(requireContext(), providersList)
        providersListingAdapter.callerName = "MessagingFragment"
        binding.customersRecyclerView.adapter = providersListingAdapter
    }

    private fun registerBroadCastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                getConversationsList()
            }
        }

        requireContext().registerReceiver(broadcastReceiver, IntentFilter(UPDATE_MESSAGES))
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(broadcastReceiver)
    }
}