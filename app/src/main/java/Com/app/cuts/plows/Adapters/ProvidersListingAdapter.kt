package Com.app.cuts.plows.Adapters

import Com.app.cuts.plows.Models.UserDetailsModel
import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.ui.UsersDirectoryActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProvidersListingAdapter(
    val context: Context,
    var providersList: ArrayList<UserDetailsModel>
) :
    RecyclerView.Adapter<ProvidersListingAdapter.ViewHolder>(), Filterable {
    val TAG = "ProvidersListingAdapter"
    var callerName: String = ""
    val originalprovidersList = providersList
    var bookingId: String = ""
    private val mFilter = ItemFilter()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.dictonaries_listrow, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binditem(providersList[position], context)
    }

    override fun getItemCount(): Int {
        return providersList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun binditem(userDetailsModel: UserDetailsModel, context: Context) {
            val providerProfilePicture =
                itemView.findViewById<ImageView>(R.id.providerProfilePicture)
            val providerNameTextView = itemView.findViewById<TextView>(R.id.providerNameTextView)
            val providerRoleTextView = itemView.findViewById<TextView>(R.id.providerRoleTextView)
            val providerContactButton = itemView.findViewById<ImageView>(R.id.callProviderImageView)
            val providerMessageButton =
                itemView.findViewById<ImageView>(R.id.messageProviderImageView)
            val providerDistanceTextView =
                itemView.findViewById<TextView>(R.id.providerDistanceTextView)
            val sendRequestToProviderButton = itemView.findViewById<Button>(R.id.sendRequestButton)
            if (userDetailsModel.userProfileImage.isNotEmpty() && !userDetailsModel.userProfileImage.equals(
                    "null",
                    ignoreCase = true
                )
            ) {
                Picasso.get()
                    .load("http://apis.cutsandplows.com/assets/profilepics/" + userDetailsModel.userProfileImage)
                    .into(providerProfilePicture)
            } else {
                Picasso.get()
                    .load(R.drawable.image_placeholder)
                    .into(providerProfilePicture)
            }
            providerNameTextView.text = userDetailsModel.userName
            providerRoleTextView.text = userDetailsModel.userRole
            providerContactButton.setOnClickListener {
                val phoneIntent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.fromParts("tel", userDetailsModel.userContactNo, null)
                )
                context.startActivity(phoneIntent)
            }

            if (callerName.isNotEmpty() && callerName == "HomeScreenFragment") {
                providerMessageButton.visibility = View.VISIBLE
                providerDistanceTextView.visibility = View.VISIBLE
                val distance = String.format("%.2f", userDetailsModel.userDistance)
                providerDistanceTextView.text = "$distance Km"

                sendRequestToProviderButton.visibility = View.VISIBLE
                sendRequestToProviderButton.setOnClickListener {
                    sendRequestToProviderAPI(userDetailsModel.providerUserId)
                }
            }
        }
    }


    inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val results = FilterResults()
            // We implement here the filter logic
            if (constraint.isEmpty()) {
                // No filter implemented we return all the list
                results.values = providersList
                results.count = providersList.size
            } else {
                // We perform filtering operation
                val nPlanetList: MutableList<UserDetailsModel> = ArrayList()
                for (p in originalprovidersList) {
                    if (p.userName.toUpperCase().contains(constraint.toString().toUpperCase())
                    ) nPlanetList.add(p)
                }
                results.values = nPlanetList
                results.count = nPlanetList.size
            }
            return results
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            providersList = results.values as ArrayList<UserDetailsModel>
            if (providersList.size == 0) {
                (context as UsersDirectoryActivity).noRecordFoundViewVisible()
            } else {
                (context as UsersDirectoryActivity).noRecordFoundViewGone()
            }
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        return mFilter
    }


    private fun sendRequestToProviderAPI(providerUserId: String) {
        val apiService = ApiClient.getClient(context)?.create(ApiInterface::class.java)
        val call = apiService?.acceptProvider(
            bookingId,
            providerUserId
        )
        (context as UsersDirectoryActivity).showProgressBar()
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                context.hideProgressBar()
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            context,
                            responseObject.getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    context.setResult(101)
                    context.finish()
                }

            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                context.hideProgressBar()
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }

        })
    }


}