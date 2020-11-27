package Com.app.cuts.plows.ui

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.ForgetPasswordFormBinding
import android.app.AlertDialog
import android.content.DialogInterface
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


class ForgetPasswordActivity : BaseActivity(), View.OnClickListener {
    val TAG = "ForgetPasswordActivity"
    lateinit var binding: ForgetPasswordFormBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ForgetPasswordFormBinding.inflate(layoutInflater)
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
                if (binding.editTextEmailAddress.text.toString().isEmpty()) {
                    return
                }
                requestToGetNewPassword()
            }
        }
    }

    private fun requestToGetNewPassword() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.forgetPassword(
            binding.editTextEmailAddress.text.toString()
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    showMessageDialog(responseObject.getString("message"))
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@ForgetPasswordActivity,
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

    private fun showMessageDialog(message: String) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
        builder1.setMessage(message)
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            "Okay",
            DialogInterface.OnClickListener { dialog, id ->
                 dialog.cancel()
                finish()
            })

        /*builder1.setNegativeButton(
            "No",
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })*/

        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }
}