package Com.app.cuts.plows.ui.registration

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.SignUpFormBinding
import Com.app.cuts.plows.ui.BaseActivity
import Com.app.cuts.plows.ui.Dashboard.HomeScreenActivity
import Com.app.cuts.plows.ui.SelectRoleActivity
import Com.app.cuts.plows.ui.TermsAndConditionsActivity
import Com.app.cuts.plows.ui.login.LoginActivity
import Com.app.cuts.plows.utils.CommonMethods
import Com.app.cuts.plows.utils.UserPreferences
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RegistrationFormActivity : BaseActivity(), View.OnClickListener {
    val TAG = "RegistrationFormActivity"
    lateinit var binding: SignUpFormBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignUpFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        binding.signupButton.setOnClickListener(this)
        binding.textViewSignIn.setOnClickListener(this)
        binding.countryPicker.registerCarrierNumberEditText(binding.editTextMobileNumber)

        val ss = SpannableString(resources.getString(R.string.terms_and_condition_check))
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val intent =
                    Intent(this@RegistrationFormActivity, TermsAndConditionsActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }
        ss.setSpan(clickableSpan, 27, 48, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.termsConditionCheckbox.text = ss
        binding.termsConditionCheckbox.movementMethod = LinkMovementMethod.getInstance()
        binding.termsConditionCheckbox.highlightColor = Color.TRANSPARENT


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
            R.id.signupButton -> {
                if (binding.firstNameEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter your first name", Toast.LENGTH_LONG).show()
                    return
                }
                if (binding.lastNameEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter your last name", Toast.LENGTH_LONG).show()
                    return
                }
                if (!CommonMethods.isValidEmail(binding.editTextEmailAddress.text.toString())) {
                    Toast.makeText(this, "Please enter correct  email address", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (binding.editTextMobileNumber.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter your mobile number", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (!binding.countryPicker.isValidFullNumber) {
                    Toast.makeText(
                        this,
                        "Please enter correct mobile number format",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return
                }
                if (binding.editTextPassowrd.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter correct password", Toast.LENGTH_LONG).show()
                    return
                }
                if (binding.editTextPassowrdConfirm.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter correct confirm password", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (binding.editTextPassowrd.text.toString() != binding.editTextPassowrdConfirm.text.toString()) {
                    Toast.makeText(this, "Both passwords should be same", Toast.LENGTH_LONG).show()
                    return
                }
                if (!binding.termsConditionCheckbox.isChecked) {
                    Toast.makeText(
                        this,
                        "please read and agree terms and conditions",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return
                }
                checkEnteredValuesAPI()
            }
            R.id.textViewSignIn -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showMessageDialog() {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
        builder1.setTitle("Disclaimer")
        builder1.setMessage("The information provided by Cuts 'n' Plows is for general informational purposes only. All information on this application is provided in good faith, however we make no representation or warranty of any kind, express or implied, regarding the accuracy, adequacy, validity, reliability, availability or completeness of any information on this mobile application.")
        builder1.setPositiveButton(
            "ACCEPT"
        ) { dialog, id ->
            dialog.cancel()
            val intent = Intent(this, SelectRoleActivity::class.java)
            intent.putExtra("user_first_name", binding.firstNameEditText.text.toString())
            intent.putExtra("user_last_name", binding.lastNameEditText.text.toString())
            intent.putExtra("user_email", binding.editTextEmailAddress.text.toString())
            intent.putExtra("user_mobile_no", binding.countryPicker.fullNumberWithPlus)
            intent.putExtra("user_password", binding.editTextPassowrd.text.toString())
            intent.putExtra(
                "user_confirm_password", binding.editTextPassowrdConfirm.text.toString()
            )
            startActivity(intent)
        }

        /*builder1.setNegativeButton(
            "No",
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })*/

        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }

    private fun checkEnteredValuesAPI() {
        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.velidateUser(
            binding.firstNameEditText.text.toString(),
            binding.lastNameEditText.text.toString(),
            binding.editTextEmailAddress.text.toString(),
            binding.countryPicker.fullNumberWithPlus,
            binding.editTextPassowrd.text.toString(),
            binding.editTextPassowrdConfirm.text.toString()
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    showMessageDialog()
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        this@RegistrationFormActivity,
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
}