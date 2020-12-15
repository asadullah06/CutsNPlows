package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.JobsHistoryFragmentBinding
import Com.app.cuts.plows.utils.UserPreferences
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
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat


class JobsHistoryFragment : Fragment() {
    lateinit var binding: JobsHistoryFragmentBinding
    val TAG = "JobsHistoryFragment"
    lateinit var jobHistoryListAdapter: JobHistoryListAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = JobsHistoryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.jobsHistoryList.layoutManager = LinearLayoutManager(requireContext())

        if (UserPreferences.getClassInstance(requireContext())
                .getUserRole() == resources.getString(
                R.string.customer
            )
        ) {
            getCustomerJobHistoryAPI()
        } else if (UserPreferences.getClassInstance(requireContext())
                .getUserRole() == resources.getString(R.string.service_provider)
        ) {
            getProviderJobHistoryAPI()
        }

    }

    private fun getProviderJobHistoryAPI() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.getProviderJobHistory(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (context == null)
                    return
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
                        requireContext(),
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

    private fun getCustomerJobHistoryAPI() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.getCustomerJobHistory(
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
                    val errorResponseObject = response.errorBody()?.string() ?: ""
                    if (!isJSONValid(errorResponseObject))
                        return
                    val responseObject = JSONObject(errorResponseObject)
                    if (context == null)
                        return
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
        val jobsList = ArrayList<JobHistoryListModel>()

        for (i in 0 until dataArray.length()) {
            val historyObject = dataArray.getJSONObject(i)
            val userName =
                "${historyObject.getString("fld_fname")} ${historyObject.getString("fld_lname")}"
            val jobAmount = historyObject.getString("fld_amount")
            val jobStatus = historyObject.getString("fld_booking_status")
            var jobFinishedDate = historyObject.getString("fld_created_date")
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val sourceDate = dateFormat.parse(jobFinishedDate)
                val targetFormat = SimpleDateFormat("MMM dd")
                jobFinishedDate = targetFormat.format(sourceDate)
            } catch (exception: ParseException) {
                exception.printStackTrace()
            }
            jobsList.add(JobHistoryListModel(userName, jobAmount, jobStatus, jobFinishedDate))
        }
        jobHistoryListAdapter = JobHistoryListAdapter(requireContext(), jobsList)
        binding.jobsHistoryList.adapter = jobHistoryListAdapter
    }

    fun isJSONValid(test: String?): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }
}
