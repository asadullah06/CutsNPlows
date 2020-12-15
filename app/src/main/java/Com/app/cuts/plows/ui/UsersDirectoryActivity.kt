package Com.app.cuts.plows.ui

import Com.app.cuts.plows.Adapters.ProvidersListingAdapter
import Com.app.cuts.plows.Models.UserDetailsModel
import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.UsersDirectoryActivityBinding
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UsersDirectoryActivity : BaseActivity(), View.OnClickListener {
    val TAG = "UsersDirectoryActivity"
    lateinit var binding: UsersDirectoryActivityBinding
    var providersList: ArrayList<UserDetailsModel> = ArrayList()
    lateinit var providersListingAdapter: ProvidersListingAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UsersDirectoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Available provider(s)"


        binding.editTextSearchUser.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                charSequence: CharSequence,
                arg1: Int,
                arg2: Int,
                arg3: Int
            ) {
                // When user changed the Text
                if (arg3 == 0) {                    //add practice items to the array and put each array item in a hashmap
                    providersListingAdapter =
                        ProvidersListingAdapter(this@UsersDirectoryActivity, providersList)
                    binding.providersRecyclerView.adapter = providersListingAdapter
                } else {
                    if (charSequence.isNotEmpty()) {
                        if (this@UsersDirectoryActivity::providersListingAdapter.isLateinit) {
                            providersListingAdapter.filter.filter(binding.editTextSearchUser.text)
                        }
                    }
                }
            }

            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })
        if (intent.getStringExtra("className") == "HomeScreenFragment") {
            binding.editTextSearchUser.visibility = View.GONE
            binding.closeBookingButton.visibility = View.VISIBLE
            binding.closeBookingButton.setOnClickListener(this)
            providersList =
                intent.getSerializableExtra("providersList") as ArrayList<UserDetailsModel>
            if (providersList.size > 0) {
                binding.providersRecyclerView.layoutManager = LinearLayoutManager(this)
                providersListingAdapter = ProvidersListingAdapter(this, providersList)
                providersListingAdapter.callerName = "HomeScreenFragment"
                providersListingAdapter.bookingId = intent.getStringExtra("bookingId") ?: ""
                binding.providersRecyclerView.adapter = providersListingAdapter
            } else {
                noRecordFoundViewVisible()
            }
        } else if (intent.getStringExtra("className") == "ProfileFragment") {
            binding.providersRecyclerView.layoutManager = LinearLayoutManager(this)
            providersListingAdapter = ProvidersListingAdapter(this, providersList)
            binding.providersRecyclerView.adapter = providersListingAdapter
            getProvidersAPI()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.closeBookingButton -> {
                cancelRequestedBookingAPI()
            }
        }
    }

    private fun getProvidersAPI() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.getProviders()
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    val dataObjects = responseObject.getJSONArray("data")
                    for (i in 0 until dataObjects.length()) {
                        val userObject = dataObjects.getJSONObject(i)
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
                                userObject.getDouble("rating").toFloat()
                            )
                        )
                    }
                    if (providersList.size > 0) {
                        providersListingAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }

        })
    }

    fun noRecordFoundViewVisible() {
        binding.errorTextView.visibility = View.VISIBLE
    }

    fun noRecordFoundViewGone() {
        binding.errorTextView.visibility = View.GONE
    }

    fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE

    }

    fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun cancelRequestedBookingAPI() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.cancelBookingRequest(
            Integer.parseInt(intent.getStringExtra("bookingId") ?: "")
        )
        showProgressBar()
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                hideProgressBar()
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            this@UsersDirectoryActivity,
                            responseObject.getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                hideProgressBar()
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }

        })
    }
}