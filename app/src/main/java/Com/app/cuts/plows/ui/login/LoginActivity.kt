package Com.app.cuts.plows.ui.login

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.LoginActivityBinding
import Com.app.cuts.plows.ui.BaseActivity
import Com.app.cuts.plows.ui.Dashboard.HomeScreenActivity
import Com.app.cuts.plows.ui.ForgetPasswordActivity
import Com.app.cuts.plows.ui.WelcomeScreen
import Com.app.cuts.plows.utils.UserPreferences
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity(), View.OnClickListener {
    val TAG = "LoginActivity"
    lateinit var binding: LoginActivityBinding
    private var userFCMToken = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.resetPasswordButton.setOnClickListener(this)
        binding.requredSignupButton.setOnClickListener(this)
        binding.loginButton.setOnClickListener(this)

        val ss = SpannableString(resources.getString(R.string.required_signup_text))
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val intent =
                    Intent(this@LoginActivity, WelcomeScreen::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }
        ss.setSpan(clickableSpan, 23, 31, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.requredSignupButton.text = ss
        binding.requredSignupButton.movementMethod = LinkMovementMethod.getInstance()
        binding.requredSignupButton.highlightColor = Color.TRANSPARENT

        getFCMToken()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.loginButton -> {
                if (binding.editTextTextEmailAddress.text.toString().isEmpty()) {
                    return
                }
                if (binding.editTextTextPassword.text.toString().isEmpty()) {
                    return
                }
                loginUserAPI()
            }
            R.id.resetPasswordButton -> {
                val intent = Intent(this, ForgetPasswordActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun loginUserAPI() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.loginUser(
            binding.editTextTextEmailAddress.text.toString(),
            binding.editTextTextPassword.text.toString(),
            userFCMToken
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    val dataObjects = responseObject.getJSONObject("data")
                    Toast.makeText(
                        this@LoginActivity,
                        responseObject.getString("message") ?: "",
                        Toast.LENGTH_LONG
                    ).show()
                    UserPreferences.getClassInstance(this@LoginActivity)
                        .setUserId(dataObjects.getString("fld_userid"))
                    if (binding.rememberMeCheckBox.isChecked) {
                        UserPreferences.getClassInstance(this@LoginActivity).setKeepUserLoginFlag()
                    }
                    UserPreferences.getClassInstance(this@LoginActivity).setUserName(
                        dataObjects.getString("fld_fname") + " " + dataObjects.getString("fld_lname")
                    )
                    UserPreferences.getClassInstance(this@LoginActivity)
                        .setUserProfile(dataObjects.getString("fld_profile_pic"))
                    if (dataObjects.getInt("fld_role") == 1) {
                        UserPreferences.getClassInstance(this@LoginActivity)
                            .setUserRole(resources.getString(R.string.customer))
                    } else if (dataObjects.getInt("fld_role") == 2) {
                        UserPreferences.getClassInstance(this@LoginActivity)
                            .setUserRole(resources.getString(R.string.service_provider))
                    }
                    UserPreferences.getClassInstance(this@LoginActivity)
                        .setUserAvailability(dataObjects.getInt("fld_online_status"))

                    val intent = Intent(this@LoginActivity, HomeScreenActivity::class.java)
                    startActivity(intent)
                    this@LoginActivity.finish()
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@LoginActivity,
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

    private fun getFCMToken() {

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                userFCMToken = task.result

                Log.d(TAG, userFCMToken)
            })

    }
}