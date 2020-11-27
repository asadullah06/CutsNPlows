package Com.app.cuts.plows.ui

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.SetNewPasswordFormBinding
import Com.app.cuts.plows.ui.Dashboard.HomeScreenActivity
import Com.app.cuts.plows.utils.UserPreferences
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SetNewPasswordActivity : BaseActivity(), View.OnClickListener {
    lateinit var binding: SetNewPasswordFormBinding
    val TAG = "SetNewPasswordActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SetNewPasswordFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.submitButton.setOnClickListener(this)
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
            R.id.submitButton -> {
                if (binding.currentPasswordEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter correct old password", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (binding.newPasswordEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter correct new password", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (binding.confirmPasswordEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter correct confirm password", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (binding.newPasswordEditText.text.toString() != binding.confirmPasswordEditText.text.toString()) {
                    Toast.makeText(this, "Both passwords should be same", Toast.LENGTH_LONG).show()
                    return
                }
                updatePassword()
            }
        }
    }

    private fun updatePassword() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.changePassword(
            binding.currentPasswordEditText.text.toString(),
            binding.newPasswordEditText.text.toString(),
            binding.confirmPasswordEditText.text.toString(),
            UserPreferences.getClassInstance(this).getUserId() ?: ""
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        this@SetNewPasswordActivity,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this@SetNewPasswordActivity, HomeScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@SetNewPasswordActivity,
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
}