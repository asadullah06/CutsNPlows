package Com.app.cuts.plows.ui

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.SelectRoleActivityBinding
import Com.app.cuts.plows.ui.Dashboard.HomeScreenActivity
import Com.app.cuts.plows.utils.UserPreferences
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SelectRoleActivity : BaseActivity(), View.OnClickListener {
    val TAG = "SelectRoleActivity"
    var selectedRole = -1
    lateinit var binding: SelectRoleActivityBinding
    private var deviceFCMToken: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SelectRoleActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        binding.customerRoleButton.setOnClickListener(this)
        binding.serviceProviderButton.setOnClickListener(this)
        binding.proceedButton.setOnClickListener(this)
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
            R.id.customerRoleButton -> {
                selectedRole = 1
                binding.customerRoleButton.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
                binding.customerRoleButton.setTextColor(ContextCompat.getColor(this, R.color.green))
                binding.customerRoleButton.strokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))

                binding.serviceProviderButton.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.green
                    )
                )
                binding.serviceProviderButton.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
                binding.serviceProviderButton.strokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_green))
            }
            R.id.serviceProviderButton -> {
                selectedRole = 2
                binding.serviceProviderButton.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
                binding.serviceProviderButton.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.green
                    )
                )
                binding.serviceProviderButton.strokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))

                binding.customerRoleButton.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.green
                    )
                )
                binding.customerRoleButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.customerRoleButton.strokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_green))

            }
            R.id.proceedButton -> {
                if (selectedRole != -1) {
                    registerUserAPI()
                }
            }
        }
    }

    private fun registerUserAPI() {

        getFCMToken()
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.registerUser(
            intent.getStringExtra("user_first_name") ?: "",
            intent.getStringExtra("user_last_name") ?: "",
            intent.getStringExtra("user_email") ?: "",
            intent.getStringExtra("user_mobile_no") ?: "",
            intent.getStringExtra("user_password") ?: "",
            intent.getStringExtra("user_confirm_password") ?: "",
            selectedRole,
            deviceFCMToken
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        this@SelectRoleActivity,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val dataObjects = responseObject.getJSONObject("data")
                    UserPreferences.getClassInstance(this@SelectRoleActivity)
                        .setUserId(dataObjects.getString("fld_userid"))
                    UserPreferences.getClassInstance(this@SelectRoleActivity).setUserName(
                        dataObjects.getString("fld_fname") + " " + dataObjects.getString("fld_lname")
                    )
                    UserPreferences.getClassInstance(this@SelectRoleActivity)
                        .setUserProfile(dataObjects.getString("fld_profile_pic"))
                    if (dataObjects.getInt("fld_role") == 1) {
                        UserPreferences.getClassInstance(this@SelectRoleActivity)
                            .setUserRole(resources.getString(R.string.customer))
                    } else if (dataObjects.getInt("fld_role") == 2) {
                        UserPreferences.getClassInstance(this@SelectRoleActivity)
                            .setUserRole(resources.getString(R.string.service_provider))
                    }
                    UserPreferences.getClassInstance(this@SelectRoleActivity)
                        .setUserAvailability(dataObjects.getInt("fld_online_status"))
                    UserPreferences.getClassInstance(this@SelectRoleActivity).setKeepUserLoginFlag()
                    val intent = Intent(this@SelectRoleActivity, HomeScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    this@SelectRoleActivity.finish()
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@SelectRoleActivity,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage ?: "")
            }

        })
    }

    private fun getFCMToken() {

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                deviceFCMToken = task.result

                Log.d(TAG, deviceFCMToken)
            })

    }
}