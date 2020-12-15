package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.ProfileFragmentBinding
import Com.app.cuts.plows.ui.Profile.UserProfileActivity
import Com.app.cuts.plows.ui.TermsAndConditionsActivity
import Com.app.cuts.plows.ui.UsersDirectoryActivity
import Com.app.cuts.plows.ui.login.LoginActivity
import Com.app.cuts.plows.utils.UserPreferences
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ProfileFragment : Fragment(), View.OnClickListener {
    lateinit var binding: ProfileFragmentBinding
    val TAG = "ProfileFragment"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ProfileFragmentBinding.inflate(inflater, container, false)
        binding.updateProfileTextView.setOnClickListener(this)
        binding.logoutTextView.setOnClickListener(this)
        binding.termsConditionsTextView.setOnClickListener(this)
        binding.directoryTextView.setOnClickListener(this)
        binding.usernameTextView.text = UserPreferences.getClassInstance(context!!).getUserName()
        if (UserPreferences.getClassInstance(context!!).getUserRole().isNotEmpty()) {
            binding.userroleTextView.text =
                UserPreferences.getClassInstance(context!!).getUserRole()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (UserPreferences.getClassInstance(context!!).getUserProfile()
                .isNotEmpty() && !UserPreferences.getClassInstance(context!!).getUserProfile()
                .equals("null", ignoreCase = true)
        ) {
            val userProfileImage =
                "http://apis.cutsandplows.com/assets/profilepics/" + UserPreferences.getClassInstance(
                    context!!
                ).getUserProfile()
            Picasso.get().load(userProfileImage)
                .fit()
                .into(binding.imageButton)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.updateProfileTextView -> {
                val intent = Intent(context, UserProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.logoutTextView -> {
                confirmationDialog()
            }
            R.id.termsConditionsTextView -> {
                val intent = Intent(context, TermsAndConditionsActivity::class.java)
                startActivity(intent)
            }
            R.id.directoryTextView -> {
                val intent = Intent(context, UsersDirectoryActivity::class.java)
                intent.putExtra("className", "ProfileFragment")
                startActivity(intent)
            }
        }
    }

    private fun confirmationDialog() {
        AlertDialog.Builder(context)
//            .setTitle("Title")
            .setMessage("Are you sure you want to logout?")
//            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                android.R.string.yes
            ) { dialog, whichButton ->
               logoutUserAPI()
            }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun logoutUserAPI() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.logoutUser(
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

                        UserPreferences.getClassInstance(context!!).clearUserPreferences()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
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
}