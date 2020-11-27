package Com.app.cuts.plows.ui.Profile

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.UserProfileActivityBinding
import Com.app.cuts.plows.ui.BaseActivity
import Com.app.cuts.plows.ui.SetNewPasswordActivity
import Com.app.cuts.plows.utils.UserPreferences
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserProfileActivity : BaseActivity(), View.OnClickListener {
    val TAG = "UserProfileActivity"
    lateinit var userPhoneNumber: String
    lateinit var userFirstName: String
    lateinit var userLastName: String
    private var userProfileImage = ""
    lateinit var binding: UserProfileActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.updateProfileButton.setOnClickListener(this)
        binding.updatePasswordButton.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        getUserProfile()
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
            R.id.updateProfileButton -> {
                val intent = Intent(this, UpdateProfileActivity::class.java)
                if (this::userFirstName.isInitialized)
                    intent.putExtra("first_name", userFirstName)
                if (this::userLastName.isInitialized)
                    intent.putExtra("last_name", userLastName)
                intent.putExtra("user_phone_no", userPhoneNumber)
                intent.putExtra("user_profile_image", userProfileImage)
                startActivity(intent)
            }
            R.id.updatePasswordButton -> {
                val intent = Intent(this, SetNewPasswordActivity::class.java)
                startActivity(intent)
            }
        }
    }



    private fun getUserProfile() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.getProfile(
            UserPreferences.getClassInstance(this).getUserId() ?: ""
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    val dataObjects = responseObject.getJSONObject("data")
                    if ((dataObjects.getString("fld_fname")
                            ?: "").isNotEmpty() and (dataObjects.getString("fld_fname") != "null")
                    ) {
                        userFirstName = dataObjects.getString("fld_fname")
                    }
                    if ((dataObjects.getString("fld_lname")
                            ?: "").isNotEmpty() and (dataObjects.getString(
                            "fld_lname"
                        ) != "null")
                    ) {
                        userLastName = dataObjects.getString("fld_lname")
                        binding.usernameTextView.text = "$userFirstName $userLastName"
                    }
                    if (dataObjects.getString("fld_profile_pic") != null) {
                        if (dataObjects.getString("fld_profile_pic")
                                .isNotEmpty() && !dataObjects.getString("fld_profile_pic")
                                .equals("null", ignoreCase = true)
                        ) {
                            UserPreferences.getClassInstance(this@UserProfileActivity)
                                .setUserProfile(dataObjects.getString("fld_profile_pic"))
                            userProfileImage =
                                "http://apis.cutsandplows.com/assets/profilepics/" + dataObjects.getString(
                                    "fld_profile_pic"
                                )
                            Picasso.get().load(userProfileImage)
                                .fit()
                                .into(binding.imageView3)
                        }
                    }
                    binding.userEmailTextView.text =
                        "Email : " + dataObjects.getString("fld_email")
                    userPhoneNumber = dataObjects.getString("fld_contact_number")
                    binding.userPhoneNumberTextView.text = "Phone : $userPhoneNumber"
                    if (dataObjects.getInt("fld_role") == 1) {
                        binding.userRoleTextView.text =
                            "Type : ${resources.getString(R.string.customer)}"
                    } else if (dataObjects.getInt("fld_role") == 2) {
                        binding.userRoleTextView.text =
                            "Type : ${resources.getString(R.string.service_provider)}"
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }

        })
    }




}