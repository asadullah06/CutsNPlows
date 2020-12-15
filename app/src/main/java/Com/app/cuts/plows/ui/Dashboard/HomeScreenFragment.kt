package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.Models.UserDetailsModel
import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.HomeScreenFragmentBinding
import Com.app.cuts.plows.databinding.SelectRadiousDialogBinding
import Com.app.cuts.plows.databinding.StartEndJobDialogBinding
import Com.app.cuts.plows.ui.Chat.MessageThreadActivity
import Com.app.cuts.plows.ui.UsersDirectoryActivity
import Com.app.cuts.plows.utils.*
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.util.rangeTo
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class HomeScreenFragment : Fragment(), View.OnClickListener {
    private var userFCMToken = ""
    val TAG = "HomeScreenFragment"
    lateinit var binding: HomeScreenFragmentBinding
    lateinit var googleMap: GoogleMap
    lateinit var googleApiClass: GoogleApiClass
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1
    val foundProvidersList: ArrayList<UserDetailsModel> = ArrayList()
    lateinit var bookingId: String
    private val permission = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var multiPurposeDialog: Dialog
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = HomeScreenFragmentBinding.inflate(inflater, container, false)

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.onResume() // needed to get the map to display immediately
        try {
            MapsInitializer.initialize(activity!!.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        getFCMToken()

        // For showing a move to my location button
        if (checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                permission,
                MY_PERMISSIONS_REQUEST_LOCATION
            )
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
            mapsConfiguration()
        }

        binding.availabilityLayout.availableButton.setOnClickListener(this)
        binding.availabilityLayout.availableRadioButton.setOnClickListener(this)
        binding.availabilityLayout.notAvailableButton.setOnClickListener(this)
        binding.availabilityLayout.notAvailableRadioButton.setOnClickListener(this)
        binding.findProviderButton.setOnClickListener(this)

        registerBroadCastReceiver()
        return binding.root
    }

    private fun registerBroadCastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, intent: Intent) {
                if (UserPreferences.getClassInstance(context!!)
                        .getUserRole() == resources.getString(R.string.service_provider)
                ) {
                    if (::multiPurposeDialog.isInitialized && multiPurposeDialog.isShowing)
                        multiPurposeDialog.dismiss()
                    getProviderDashboardAPI()
                } else if (UserPreferences.getClassInstance(context!!)
                        .getUserRole() == resources.getString(R.string.customer)
                ) {
                    if (::multiPurposeDialog.isInitialized && multiPurposeDialog.isShowing)
                        multiPurposeDialog.dismiss()
                    getCustomerDashboardAPI()
                }
            }
        }
        context?.registerReceiver(broadcastReceiver, IntentFilter(UPDATE_UI_AGAINST_NOTIFICATION))
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.availableRadioButton,
            R.id.availableButton -> {
                updateAvailableButton()
                updateOnlineStatusAPI(ONLINE_STATUS_AVAILABLE)
            }
            R.id.notAvailableRadioButton,
            R.id.notAvailableButton -> {
                updateNotAvailableButton()
                updateOnlineStatusAPI(ONLINE_STATUS_NOY_AVAILABLE)
            }
            R.id.findProviderButton -> {
                if (binding.findProviderButton.text == resources.getString(R.string.find_provider))
                    showSelectRadiusDialog()
                else {
                    val intent = Intent(context, UsersDirectoryActivity::class.java)
                    intent.putExtra("providersList", foundProvidersList)
                    intent.putExtra("bookingId", bookingId)
                    intent.putExtra("className", "HomeScreenFragment")
                    startActivityForResult(intent, REQUEST_CODE_TO_SHOW_PROVIDERS)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TO_SHOW_PROVIDERS && resultCode == Activity.RESULT_OK) {
            updateUIForRoles()
        } else if (requestCode == REQUEST_CODE_TO_SHOW_PROVIDERS && resultCode == 101) {
            getCustomerDashboardAPI(0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION && (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
            mapsConfiguration()
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()


    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun mapsConfiguration() {
        binding.mapView.getMapAsync { mMap ->
            googleMap = mMap
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@getMapAsync
            }

//            googleMap.isMyLocationEnabled = true


            // For dropping a marker at a point on the Map
            /*val sydney = LatLng((-34).toDouble(), (151).toDouble())
            val markerOptions = MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.app_icon_small))
                .position(sydney)
                .title("Marker Title").snippet("Marker Description")
            googleMap.addMarker(markerOptions)*/

//             For zooming automatically to the location of the marker
            /*val cameraPosition = CameraPosition.Builder().target(sydney).zoom(12f).build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))*/
        }


        googleApiClass = GoogleApiClass(context) { mLocation: Location? ->
            googleMap.clear()
            val currentLocation = LatLng(mLocation!!.latitude, mLocation.longitude)
            val mp = MarkerOptions()
            mp.position(currentLocation)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker))
                .title("my position")

            googleMap.addMarker(mp)
            val cameraPosition = CameraPosition.Builder().target(currentLocation).zoom(12f).build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            /*googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(mLocation.latitude, mLocation.longitude), 16f
                )
            )*/

            updateLocationAPI()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleApiClass.destroyApiConnection()
    }

    private fun updateOnlineStatusAPI(availabilityStatus: Int) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.updateOnlineStatus(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            availabilityStatus
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        context,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    UserPreferences.getClassInstance(requireContext())
                        .setUserAvailability(availabilityStatus)
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


    private fun updateAvailableButton() {
        binding.availabilityLayout.availableButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
        )
        binding.availabilityLayout.availableButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.green
            )
        )

        binding.availabilityLayout.notAvailableButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.green
            )
        )
        binding.availabilityLayout.notAvailableButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.transparent
            )
        )

        binding.availabilityLayout.availableRadioButton.isChecked = true
        binding.availabilityLayout.notAvailableRadioButton.isChecked = false
    }

    private fun updateNotAvailableButton() {
        binding.availabilityLayout.notAvailableButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
        )
        binding.availabilityLayout.notAvailableButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.green
            )
        )

        binding.availabilityLayout.availableButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.green
            )
        )
        binding.availabilityLayout.availableButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.transparent
            )
        )

        binding.availabilityLayout.availableRadioButton.isChecked = false
        binding.availabilityLayout.notAvailableRadioButton.isChecked = true
    }

    private fun requestBookingAPI(selectedRadius: String) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.requestBooking(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            googleApiClass.latitude,
            googleApiClass.longitude
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val dataObjects = responseObject.getJSONObject("data")
                    findProvider(dataObjects.getInt("fld_bid"), selectedRadius)
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

    private fun findProvider(mbookingId: Int, selectedRadius: String) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.findProvider(
            selectedRadius,
            mbookingId
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val dataObjects = responseObject.getJSONObject("data")
//                    if (dataObjects.has("booking")) {
//                        val bookingObject = dataObjects.getJSONObject("booking")
//                        bookingId = (bookingObject.getString("fld_bid") ?: "")
//                    }
                    if (dataObjects.has("bookingprovider")) {
                        val bookingArray = dataObjects.getJSONArray("bookingprovider")
                        foundProvidersList.clear()
                        for (i in 0 until bookingArray.length()) {
                            val bookingObject = bookingArray.getJSONObject(i)
                            val providerName =
                                "${bookingObject.getString("fld_fname")} ${
                                    bookingObject.getString(
                                        "fld_lname"
                                    )
                                }"
                            val providerContact = bookingObject.getString("fld_contact_number")
                            val providerPicture = bookingObject.getString("fld_profile_pic")
                            val providerDistance = bookingObject.getDouble("distance")
                            val providerId = bookingObject.getString("fld_userid")
                            val providerStatus = bookingObject.getString("tbl_status")
                            val providerRating = bookingObject.getDouble("rating")
                            foundProvidersList.add(
                                UserDetailsModel(
                                    providerName,
                                    resources.getString(R.string.service_provider),
                                    providerContact,
                                    providerPicture ?: "",
                                    providerRating.toFloat(),
                                    providerId,
                                    providerStatus,
                                    providerDistance
                                )
                            )
                        }
                        binding.findProviderButton.text =
                            getString(R.string.view_found_providers)
                        bookingId = mbookingId.toString()
                        val intent = Intent(context, UsersDirectoryActivity::class.java)
                        intent.putExtra("providersList", foundProvidersList)
                        intent.putExtra("bookingId", bookingId)
                        intent.putExtra("className", "HomeScreenFragment")
                        startActivityForResult(intent, REQUEST_CODE_TO_SHOW_PROVIDERS)
                    }

                    /*val dataArray = responseObject.getJSONArray("data")
                    val dataObjects = dataArray.getJSONObject(0)
                    val providerName =
                        "${dataObjects.getString("fld_fname")} ${dataObjects.getString("fld_lname")}"
                    val providerContact = dataObjects.getString("fld_contact_number")
                    val providerPicture = dataObjects.getString("fld_profile_pic")
                    val providerLat = dataObjects.getDouble("fld_lat")
                    val providerLong = dataObjects.getDouble("fld_lng")

                    showProviderDetailsDialog(
                        providerName,
                        providerContact,
                        providerPicture,
                        providerLat,
                        providerLong
                    )*/
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

    private fun getCustomerDashboardAPI(flag: Int = -1) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.getCustomerDashboard(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: ""
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    val dataObjects = responseObject.getJSONObject("data")
                    if (dataObjects.has("booking")) {
                        val bookingObject = dataObjects.getJSONObject("booking")
                        bookingId = (bookingObject.getString("fld_bid") ?: "")
                    }
                    // just to reset the button text in case user cancel the booking.
                    binding.findProviderButton.text = getString(R.string.find_provider)
                    foundProvidersList.clear()
                    if (dataObjects.has("bookingprovider")) {
                        val bookingArray = dataObjects.getJSONArray("bookingprovider")
                        for (i in 0 until bookingArray.length()) {
                            val bookingObject = bookingArray.getJSONObject(i)
                            val providerName =
                                "${bookingObject.getString("fld_fname")} ${
                                    bookingObject.getString(
                                        "fld_lname"
                                    )
                                }"
                            val providerContact = bookingObject.getString("fld_contact_number")
                            val providerPicture = bookingObject.getString("fld_profile_pic")
                            val providerDistance = bookingObject.getDouble("distance")
                            val providerId = bookingObject.getString("fld_userid")
                            val providerStatus = bookingObject.getString("tbl_status")
                            val providerRating = bookingObject.getDouble("rating")
                            foundProvidersList.add(
                                UserDetailsModel(
                                    providerName,
                                    resources.getString(R.string.service_provider),
                                    providerContact,
                                    providerPicture ?: "",
                                    providerRating.toFloat(),
                                    providerId,
                                    providerStatus,
                                    providerDistance
                                )
                            )
                        }
                        binding.findProviderButton.text =
                            getString(R.string.view_found_providers)
                        if (flag == -1) {
                            val intent = Intent(context, UsersDirectoryActivity::class.java)
                            intent.putExtra("providersList", foundProvidersList)
                            intent.putExtra("bookingId", bookingId)
                            intent.putExtra("className", "HomeScreenFragment")
                            startActivityForResult(intent, REQUEST_CODE_TO_SHOW_PROVIDERS)
                        }

                    } else if (dataObjects.has("booking")) {
                        val bookingObject = dataObjects.getJSONObject("booking")
                        val bookingId = bookingObject.getInt("fld_bid")
                        val bookingStatus = bookingObject.getInt("tbl_status")
                        val providerName =
                            "${bookingObject.getString("fld_fname")} ${bookingObject.getString("fld_lname")}"
                        val bookingJobStatus = bookingObject.getString("fld_booking_status")
                        val providerPicture = bookingObject.getString("fld_profile_pic")
                        val providerRating = bookingObject.getDouble("rating")
                        var paymentStatus = ""
                        var paymentAmount = 0f
                        if (bookingStatus == REQUEST_TO_VERIFY_PAYMENT) {
                            if (bookingObject.has("fld_payment_status")) {
                                paymentStatus = bookingObject.getString("fld_payment_status")
                            }
                            if (bookingObject.has("fld_amount")) {
                                paymentAmount = bookingObject.getDouble("fld_amount").toFloat()
                            }
                        }
                        if ((bookingStatus == FINISH_JOB_REQUEST && bookingJobStatus != "payment") || bookingStatus == STATUS_JOB_STARTED) {
                            showStartEndJobDialog(bookingId, bookingStatus)
                            return
                        } else if (bookingStatus == REQUEST_TO_VERIFY_PAYMENT && (paymentStatus == "0" || paymentStatus.isEmpty())) {
                            showCollectCashDialog(
                                bookingId,
                                paymentStatus,
                                paymentAmount,
                                providerName,
                                providerPicture
                            )
                            return
                        } else if (bookingStatus == REQUEST_TO_VERIFY_PAYMENT && bookingJobStatus == "completed") {
                            showRatingDialog(bookingId, providerName, providerPicture)
                            return
                        }
                        val providerId = bookingObject.getString("fld_provider_id")
                        val providerContact = bookingObject.getString("fld_contact_number")
                        val providerLat = bookingObject.getDouble("fld_lat")
                        val providerLong = bookingObject.getDouble("fld_lng")
                        showProviderDetailsDialog(
                            providerId,
                            providerName,
                            providerContact,
                            providerPicture,
                            providerLat,
                            providerLong,
                            providerRating.toFloat(),
                            bookingStatus
                        )
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

    private fun updateLocationAPI() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.updateLocation(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            googleApiClass.latitude,
            googleApiClass.longitude,
            userFCMToken
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    updateUIForRoles()
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

    private fun getProviderDashboardAPI() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.getProviderDashboard(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: ""
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (context == null)
                    return
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val dataObjects = responseObject.getJSONObject("data")
                    if (dataObjects.has("booking")) {
                        val bookingObject = dataObjects.getJSONObject("booking")
                        val customerName =
                            "${bookingObject.getString("fld_fname")} ${bookingObject.getString("fld_lname")}"
                        val customerPicture = bookingObject.getString("fld_profile_pic")
                        var paymentStatus = ""
                        var paymentAmount = 0f
                        val bookingStatus = bookingObject.getInt("tbl_status")
                        val bookingId = bookingObject.getInt("fld_bid")
                        val bookingJobStatus = bookingObject.getString("fld_booking_status")
                        if (bookingStatus == REQUEST_TO_VERIFY_PAYMENT) {
                            if (bookingObject.has("fld_payment_status")) {
                                paymentStatus = bookingObject.getString("fld_payment_status")
                            }
                            if (bookingObject.has("fld_amount")) {
                                paymentAmount = bookingObject.getDouble("fld_amount").toFloat()
                            }
                        }

                        if (bookingStatus == STATUS_ARRIVED) {
                            showStartEndJobDialog(bookingId, bookingStatus)
                        } else if (bookingStatus == STATUS_JOB_STARTED) {
                            showStartEndJobDialog(bookingId, bookingStatus)
                            return
                        } else if (bookingStatus == FINISH_JOB_REQUEST || (bookingStatus == REQUEST_TO_VERIFY_PAYMENT && bookingJobStatus == "accepted")) {
                            paymentStatus = bookingJobStatus
                            showCollectCashDialog(bookingId, paymentStatus, paymentAmount)
                            return
                        } else if (bookingStatus == REQUEST_TO_VERIFY_PAYMENT && bookingJobStatus == "completed") {
                            showRatingDialog(bookingId, customerName, customerPicture)
                            return
                        }
                        val customerContact = bookingObject.getString("fld_contact_number")
                        val customerLat = bookingObject.getDouble("fld_lat")
                        val customerLong = bookingObject.getDouble("fld_lng")
                        val customerRating = bookingObject.getDouble("rating")
                        val customerId = bookingObject.getString("fld_customer_id")
                        val customerDistance = bookingObject.getDouble("distance")
                        showCustomerDetailsDialog(
                            customerId,
                            customerName,
                            customerContact,
                            customerPicture,
                            customerLat,
                            customerLong,
                            customerRating.toFloat(),
                            customerDistance,
                            bookingId,
                            bookingStatus
                        )
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    binding.userDetailsLayout.root.visibility = View.GONE
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

    private fun updateUIForRoles() {
        if (UserPreferences.getClassInstance(requireContext()).getUserRole().isNotEmpty()) {
            if (UserPreferences.getClassInstance(requireContext())
                    .getUserRole() == resources.getString(R.string.service_provider)
            ) {
                binding.availabilityLayout.root.visibility = View.VISIBLE
                binding.findProviderButton.visibility = View.GONE

                if (UserPreferences.getClassInstance(requireContext())
                        .getUserAvailability() == ONLINE_STATUS_AVAILABLE
                ) {
                    updateAvailableButton()
                } else if (UserPreferences.getClassInstance(requireContext())
                        .getUserAvailability() == ONLINE_STATUS_NOY_AVAILABLE
                ) {
                    updateNotAvailableButton()
                }
                getProviderDashboardAPI()

            } else if (UserPreferences.getClassInstance(requireContext())
                    .getUserRole() == resources.getString(R.string.customer)
            ) {
                binding.availabilityLayout.root.visibility = View.GONE
                binding.findProviderButton.visibility = View.VISIBLE

                getCustomerDashboardAPI()
            }
        }
    }

    private fun showCustomerDetailsDialog(
        customerId: String,
        customerName: String,
        customerContact: String,
        customerPicture: String,
        customerLat: Double,
        customerLong: Double,
        customerRating: Float,
        customerDistance: Double,
        bookingId: Int,
        bookingStatus: Int
    ) {

        binding.userDetailsLayout.root.visibility = View.VISIBLE

        binding.userDetailsLayout.userRoleTextView.text = resources.getString(R.string.customer)
        binding.userDetailsLayout.ratingbarView.rating = customerRating
        binding.userDetailsLayout.acceptRejectLayout.visibility = View.VISIBLE
        if (bookingStatus == ONLINE_STATUS_NOY_AVAILABLE) {
            binding.userDetailsLayout.userRoleTextView.text =
                "${binding.userDetailsLayout.userRoleTextView.text} ${
                    String.format(
                        "%.2f",
                        customerDistance
                    )
                } km away"
        }
        if (bookingStatus == STATUS_ACCEPTED) {
            binding.userDetailsLayout.acceptCustomerButton.visibility = View.GONE
            binding.userDetailsLayout.rejectCustomerButton.visibility = View.GONE
            binding.userDetailsLayout.arrivedCustomerButton.visibility = View.VISIBLE
            binding.userDetailsLayout.navigateButton.visibility = View.VISIBLE
        } else if (bookingStatus == STATUS_ARRIVED) {
            binding.userDetailsLayout.acceptCustomerButton.visibility = View.GONE
            binding.userDetailsLayout.rejectCustomerButton.visibility = View.GONE
            binding.userDetailsLayout.arrivedCustomerButton.visibility = View.GONE
            binding.userDetailsLayout.startJobButton.visibility = View.VISIBLE
            binding.userDetailsLayout.navigateButton.visibility = View.GONE
        }
        binding.userDetailsLayout.otherButtonsLayout.visibility = View.GONE

        binding.userDetailsLayout.callUserButton.setOnClickListener {
            val phoneIntent = Intent(
                Intent.ACTION_DIAL,
                Uri.fromParts("tel", customerContact, null)
            )
            startActivity(phoneIntent)
        }
        binding.userDetailsLayout.messageUserButton.setOnClickListener {
            /*val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("sms:$customerContact")
            startActivity(smsIntent)*/
            val intent = Intent(context, MessageThreadActivity::class.java)
            intent.putExtra("receiver_id", customerId)
            intent.putExtra("user_name", customerName)
            intent.putExtra("user_role", resources.getString(R.string.customer))
            intent.putExtra("user_image", customerPicture)
            startActivity(intent)
        }
        binding.userDetailsLayout.userNameTextView.text = customerName
        if (customerPicture.isNotEmpty() && !customerPicture.equals(
                "null",
                ignoreCase = true
            )
        ) {
            val userProfileImage =
                "http://apis.cutsandplows.com/assets/profilepics/" + UserPreferences.getClassInstance(
                    context!!
                ).getUserProfile()
            Picasso.get().load(userProfileImage)
                .fit()
                .into(binding.userDetailsLayout.imageButton)
        }
        binding.userDetailsLayout.acceptCustomerButton.setOnClickListener {
            acceptCustomerAPI(bookingId)
        }
        binding.userDetailsLayout.arrivedCustomerButton.setOnClickListener {
            updateBookingStatusForCustomerAPI(bookingId, STATUS_ARRIVED)
        }
        binding.userDetailsLayout.rejectCustomerButton.setOnClickListener {
            rejectCustomerAPI(bookingId)
        }
        binding.userDetailsLayout.startJobButton.setOnClickListener {
            showStartEndJobDialog(bookingId, STATUS_ARRIVED)
        }
        binding.userDetailsLayout.navigateButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?q=loc: $customerLat,$customerLong ( my location)")
            )
            startActivity(intent)
        }
        val currentLocation = LatLng(customerLat, customerLong)
        val mp = MarkerOptions()
        mp.position(currentLocation)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_marker))
            .title(customerName)

        googleMap.addMarker(mp)
        val cameraPosition = CameraPosition.Builder().target(currentLocation).zoom(12f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        val currentLatLng = "${googleApiClass.latitude},${googleApiClass.longitude}"
        val remoteUserLatLong = "$customerLat,$customerLong"
        getDirectionsAPI(currentLatLng, remoteUserLatLong)

        /*binding.userDetailsLayout.navigateButton.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }*/
    }

    private fun showProviderDetailsDialog(
        providerId: String,
        providerName: String,
        providerContact: String,
        providerPicture: String,
        providerLat: Double,
        providerLong: Double,
        providerRating: Float,
        bookingStatus: Int
    ) {
        binding.findProviderButton.visibility = View.GONE

        binding.userDetailsLayout.root.visibility = View.VISIBLE

        binding.userDetailsLayout.ratingbarView.rating = providerRating
        when (bookingStatus) {
            ONLINE_STATUS_AVAILABLE -> {
                binding.userDetailsLayout.userRoleTextView.text =
                    "${resources.getString(R.string.provider)} is on its way"
                binding.userDetailsLayout.acceptRejectLayout.visibility = View.GONE

//                binding.userDetailsLayout.acceptRejectLayout.visibility = View.GONE
//                binding.userDetailsLayout.acceptCustomerButton.visibility = View.GONE
//                binding.userDetailsLayout.navigateButton.visibility = View.VISIBLE
//                binding.userDetailsLayout.otherButtonsLayout.visibility = View.GONE
            }
            STATUS_ARRIVED -> {
                binding.userDetailsLayout.userRoleTextView.text =
                    "${resources.getString(R.string.provider)} has arrived"
                binding.userDetailsLayout.acceptRejectLayout.visibility = View.GONE
            }
            FINISH_JOB_REQUEST -> {
                binding.userDetailsLayout.userRoleTextView.text =
                    "${resources.getString(R.string.provider)} deciding payment"
                binding.userDetailsLayout.acceptRejectLayout.visibility = View.GONE

            }
            STATUS_JOB_STARTED -> {
                binding.userDetailsLayout.userRoleTextView.text =
                    "${resources.getString(R.string.provider)} started working"
                binding.userDetailsLayout.acceptRejectLayout.visibility = View.GONE
            }
        }


        binding.userDetailsLayout.callUserButton.setOnClickListener {
            val phoneIntent = Intent(
                Intent.ACTION_DIAL,
                Uri.fromParts("tel", providerContact, null)
            )
            startActivity(phoneIntent)
        }
        binding.userDetailsLayout.messageUserButton.setOnClickListener {
            /*val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("sms:$providerContact")
            startActivity(smsIntent)*/

            val intent = Intent(context, MessageThreadActivity::class.java)
            intent.putExtra("receiver_id", providerId)
            intent.putExtra("user_name", providerName)
            intent.putExtra("user_role", resources.getString(R.string.service_provider))
            intent.putExtra("user_image", providerPicture)
            startActivity(intent)
        }
        binding.userDetailsLayout.userNameTextView.text = providerName
        if (providerPicture.isNotEmpty() && !providerPicture.equals(
                "null",
                ignoreCase = true
            )
        ) {
            val userProfileImage =
                "http://apis.cutsandplows.com/assets/profilepics/" + UserPreferences.getClassInstance(
                    context!!
                ).getUserProfile()

            Picasso.get().load(userProfileImage)
                .fit()
                .into(binding.userDetailsLayout.imageButton)
        }

        val currentLocation = LatLng(providerLat, providerLong)
        val mp = MarkerOptions()
        mp.position(currentLocation)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_marker))
            .title(providerName)

        googleMap.addMarker(mp)
        val cameraPosition = CameraPosition.Builder().target(currentLocation).zoom(12f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        val currentLatLng = "${googleApiClass.latitude},${googleApiClass.longitude}"
        val remoteUserLatLong = "$providerLat,$providerLong"
        getDirectionsAPI(currentLatLng, remoteUserLatLong)

        binding.userDetailsLayout.navigateButton.setOnClickListener {
            val uri: String =
                java.lang.String.format(Locale.ENGLISH, "geo:%f,%f", providerLat, providerLong)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            context!!.startActivity(intent)
        }
    }

    private fun acceptCustomerAPI(bookingId: Int) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.acceptCustomer(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            bookingId,
            googleApiClass.latitude,
            googleApiClass.longitude
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.userDetailsLayout.acceptCustomerButton.visibility = View.GONE
                    binding.userDetailsLayout.rejectCustomerButton.visibility = View.GONE
                    binding.userDetailsLayout.arrivedCustomerButton.visibility = View.VISIBLE
                    binding.userDetailsLayout.navigateButton.visibility = View.VISIBLE
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

    private fun updateBookingStatusForCustomerAPI(bookingId: Int, bookingStatus: Int) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.updateBookingStatus(
            bookingId,
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            bookingStatus
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    when (bookingStatus) {
                        STATUS_ARRIVED -> {
                            binding.userDetailsLayout.acceptCustomerButton.visibility =
                                View.GONE
                            binding.userDetailsLayout.arrivedCustomerButton.visibility =
                                View.GONE
                            binding.userDetailsLayout.navigateButton.visibility = View.GONE
                            binding.userDetailsLayout.startJobButton.visibility = View.VISIBLE

                            showStartEndJobDialog(bookingId, STATUS_ARRIVED)
                        }
                        STATUS_JOB_STARTED -> {
                            showStartEndJobDialog(bookingId, STATUS_JOB_STARTED)
                            binding.userDetailsLayout.root.visibility = View.GONE
                        }
                        FINISH_JOB_REQUEST -> {
                            showCollectCashDialog(bookingId)
                        }
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

    private fun rejectCustomerAPI(bookingId: Int) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.rejectCustomer(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            bookingId,
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        googleMap.clear()
                        val currentLocation = LatLng(
                            googleApiClass.latitude,
                            googleApiClass.longitude
                        )
                        val mp = MarkerOptions()
                        mp.position(currentLocation)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker))
                            .title("my position")
                        googleMap.addMarker(mp)
                        val cameraPosition = CameraPosition.Builder().target(currentLocation).zoom(
                            12f
                        ).build()
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                        getProviderDashboardAPI()
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

    private fun acceptJobFinishedRequestAPI(bookingId: Int) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.acceptJobFinishedRequest(
            bookingId
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        getCustomerDashboardAPI()
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        context,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    showStartEndJobDialog(bookingId, FINISH_JOB_REQUEST)
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }


    private fun showStartEndJobDialog(bookingId: Int, bookingStatus: Int) {
        multiPurposeDialog = Dialog(requireContext())
        multiPurposeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = StartEndJobDialogBinding.inflate(layoutInflater)
        multiPurposeDialog.setContentView(dialogViewBinding.root)
        multiPurposeDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        multiPurposeDialog.show()
        dialogViewBinding.descriptionTextView.text =
            "Congratulations! The vendor you selected has\n arrived and is on site ready to serve you"
        if (bookingStatus == STATUS_ARRIVED)
            dialogViewBinding.bookingStatusTextView.text = getString(R.string.arrive)
        else if (bookingStatus == STATUS_JOB_STARTED && UserPreferences.getClassInstance(
                requireContext()
            ).getUserRole() == resources.getString(R.string.service_provider)
        ) {
            dialogViewBinding.bookingStatusTextView.text = getString(R.string.job_in_progress)
            dialogViewBinding.startJobButton.visibility = View.GONE
            dialogViewBinding.finishJobButton.alpha = 1f
            dialogViewBinding.finishJobButton.isEnabled = true
            dialogViewBinding.finishJobButton.tag = "0"
            multiPurposeDialog.setCancelable(false)
            startTimer(
                dialogViewBinding.hoursTextView,
                dialogViewBinding.minuteTextView,
                dialogViewBinding.secondsTextView
            )

        } else if (bookingStatus == STATUS_JOB_STARTED) {
            dialogViewBinding.bookingStatusTextView.text = getString(R.string.job_in_progress)
            dialogViewBinding.startJobButton.visibility = View.GONE
            dialogViewBinding.finishJobButton.isEnabled = false
            dialogViewBinding.finishJobButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.green
                )
            )
            multiPurposeDialog.setCancelable(false)
            dialogViewBinding.descriptionTextView.text =
                "The vendor you selected has arrived and serving you now."
            startTimer(
                dialogViewBinding.hoursTextView,
                dialogViewBinding.minuteTextView,
                dialogViewBinding.secondsTextView
            )
        } else if (bookingStatus == FINISH_JOB_REQUEST) {
            dialogViewBinding.bookingStatusTextView.text = getString(R.string.job_in_progress)
            dialogViewBinding.startJobButton.visibility = View.GONE
            dialogViewBinding.finishJobButton.isEnabled = true
            dialogViewBinding.finishJobButton.alpha = 1f
            dialogViewBinding.finishJobButton.tag = "1"
            dialogViewBinding.finishJobButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.green
                )
            )
            multiPurposeDialog.setCancelable(false)
            dialogViewBinding.descriptionTextView.text =
                "The vendor you selected has arrived and serving you now."
        }


        dialogViewBinding.startJobButton.setOnClickListener {
            multiPurposeDialog.dismiss()
            updateBookingStatusForCustomerAPI(bookingId, STATUS_JOB_STARTED)
        }
        dialogViewBinding.finishJobButton.setOnClickListener {
            multiPurposeDialog.dismiss()
            if (dialogViewBinding.finishJobButton.tag == "0")
                updateBookingStatusForCustomerAPI(bookingId, FINISH_JOB_REQUEST)
            else if (dialogViewBinding.finishJobButton.tag == "1")
                acceptJobFinishedRequestAPI(bookingId)


        }
    }

    private fun showCollectCashDialog(
        bookingId: Int,
        paymentStatus: String = "",
        paymentAmount: Float = 0f,
        userName: String = "",
        userProfile: String = ""
    ) {
        multiPurposeDialog = Dialog(requireContext())
        multiPurposeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = StartEndJobDialogBinding.inflate(layoutInflater)
        multiPurposeDialog.setContentView(dialogViewBinding.root)
        multiPurposeDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        multiPurposeDialog.setCancelable(false)
        multiPurposeDialog.show()
        dialogViewBinding.bookingStatusTextView.text = getString(R.string.collect_cash)
        dialogViewBinding.descriptionTextView.text = "The job is finished, please collect cash"
        dialogViewBinding.innerLayout.visibility = View.GONE
        dialogViewBinding.paymentCollectLayout.visibility = View.VISIBLE
        if (UserPreferences.getClassInstance(requireContext())
                .getUserRole() == resources.getString(R.string.customer)
        ) {
            dialogViewBinding.bookingStatusTextView.text = getString(R.string.pay_cash)
        }
        if (paymentStatus.isNotEmpty()) {

            dialogViewBinding.cashEditText.setText(paymentAmount.toString())
            dialogViewBinding.cashEditText.isFocusable = false
            dialogViewBinding.cashEditText.isLongClickable = false
            if (UserPreferences.getClassInstance(requireContext())
                    .getUserRole() == resources.getString(R.string.service_provider)
            ) {
//                dialogViewBinding.descriptionTextView.text = "${dialogViewBinding.descriptionTextView.text}\npayment request is pending at customer side"
                dialogViewBinding.descriptionTextView.text = "payment request is pending at customer side"
                dialogViewBinding.processButton.isEnabled = false
                dialogViewBinding.processButton.alpha = 0.5f
            }
        }
        dialogViewBinding.processButton.setOnClickListener {
            if (dialogViewBinding.cashEditText.text.toString().isNotEmpty()) {
                multiPurposeDialog.dismiss()
                if (UserPreferences.getClassInstance(requireContext())
                        .getUserRole() == resources.getString(R.string.service_provider)
                ) {
                    providerRequestPaymentAPI(
                        bookingId,
                        dialogViewBinding.cashEditText.text.toString().toFloatOrNull()
                    )
                } else if (UserPreferences.getClassInstance(requireContext())
                        .getUserRole() == resources.getString(R.string.customer)
                ) {
                    customerAcceptPaymentRequestAPI(bookingId, userName, userProfile)
                }
            } else {
                Toast.makeText(requireContext(), "Must enter amount", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRatingDialog(bookingId: Int, userName: String, userProfile: String) {
        multiPurposeDialog = Dialog(requireContext())
        multiPurposeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = StartEndJobDialogBinding.inflate(layoutInflater)
        multiPurposeDialog.setContentView(dialogViewBinding.root)
        multiPurposeDialog.setCancelable(false)
        multiPurposeDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        multiPurposeDialog.show()
        dialogViewBinding.bookingStatusTextView.visibility = View.GONE
        dialogViewBinding.descriptionTextView.visibility = View.GONE
        dialogViewBinding.innerLayout.visibility = View.GONE
        dialogViewBinding.ratingLayout.visibility = View.VISIBLE

        dialogViewBinding.userNameTextView.text = userName
        if (UserPreferences.getClassInstance(requireContext())
                .getUserRole() == resources.getString(
                R.string.customer
            )
        ) {
            dialogViewBinding.userRoleTextView.text =
                resources.getString(R.string.service_provider)
        } else if (UserPreferences.getClassInstance(requireContext())
                .getUserRole() == resources.getString(R.string.service_provider)
        ) {
            dialogViewBinding.userRoleTextView.text = resources.getString(R.string.customer)
        }
        if (userProfile.isNotEmpty() && !userProfile.equals("null", ignoreCase = true)) {
            val userProfileImage =
                "http://apis.cutsandplows.com/assets/profilepics/$userProfile"

            Picasso.get().load(userProfileImage)
                .fit()
                .into(dialogViewBinding.imageButton)
        }
        dialogViewBinding.submitButton.setOnClickListener {
            if (dialogViewBinding.ratingbarView.rating > 0f) {
                val reviewString = dialogViewBinding.reviewEditText.text.toString()
                multiPurposeDialog.dismiss()
                if (UserPreferences.getClassInstance(requireContext())
                        .getUserRole() == resources.getString(
                        R.string.customer
                    )
                ) {

                    customerReviewProviderAPI(
                        userName,
                        userProfile,
                        bookingId,
                        dialogViewBinding.ratingbarView.rating,
                        reviewString
                    )
                } else if (UserPreferences.getClassInstance(requireContext())
                        .getUserRole() == resources.getString(R.string.service_provider)
                ) {
                    providerReviewCustomerAPI(
                        bookingId,
                        dialogViewBinding.ratingbarView.rating,
                        reviewString
                    )
                }
            } else {
                Toast.makeText(requireContext(), "Rating user is must", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun providerRequestPaymentAPI(
        bookingId: Int,
        paymentAmount: Float?
    ) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.providerRequestPayment(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            bookingId,
            paymentAmount ?: 0f
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        showCollectCashDialog(bookingId, "0", paymentAmount ?: 0f)
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        context,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    showCollectCashDialog(bookingId)
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }

    private fun customerAcceptPaymentRequestAPI(
        bookingId: Int,
        userName: String,
        userProfile: String
    ) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.customerAcceptPaymentRequest(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            bookingId
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        showRatingDialog(bookingId, userName, userProfile)
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        context,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    showCollectCashDialog(bookingId)
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }

    private fun customerReviewProviderAPI(
        userName: String,
        userProfile: String,
        bookingId: Int,
        rating: Float,
        reviewString: String
    ) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.customerReviewProvider(
            bookingId,
            rating,
            reviewString,
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        mapsConfiguration()
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        context,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    showRatingDialog(bookingId, userName, userProfile)
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }

    private fun providerReviewCustomerAPI(bookingId: Int, rating: Float, reviewString: String) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.providerReviewCustomer(
            bookingId,
            rating,
            reviewString,
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    if (responseObject.getString("type") == "success") {
                        Toast.makeText(
                            requireContext(),
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        mapsConfiguration()
                    }
                } else {
                    val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                    Toast.makeText(
                        context,
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    showCollectCashDialog(bookingId)
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

    companion object {
        const val REQUEST_CODE_TO_SHOW_PROVIDERS = 1
    }


    /// ---------////
    private fun getDirectionsAPI(origin: String, destination: String) {
        val apiService = ApiClient.getGoogleMapClient()?.create(ApiInterface::class.java)
        val call = apiService?.getDirectionJson(
            origin,
            destination,
            false,
            "driving",
            getString(R.string.google_api_key)
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                directionResults: Response<ResponseBody>
            ) {
                binding.progressBar.visibility = View.GONE
                if (directionResults.isSuccessful) {
                    var polyLineToDraw = ""
                    val responseObject = JSONObject(directionResults.body()?.string() ?: "")
                    if (responseObject.has("routes")) {
                        val routesArray = responseObject.getJSONArray("routes")
                        if (routesArray.length() > 0) {
                            val routeIndexOne = routesArray.getJSONObject(0)
                            if (routeIndexOne.has("overview_polyline")) {
                                val polyLineObject =
                                    routeIndexOne.getJSONObject("overview_polyline")
                                if (polyLineObject.has("points")) {
                                    polyLineToDraw =
                                        polyLineObject.getString("points")
                                }
                            }
                            /*else if (routeIndexOne.has("legs")) {
                                val legsArray = routeIndexOne.getJSONArray("legs")
                                if (legsArray.length() > 0) {
                                    val legFirstIndex = legsArray.getJSONObject(0)
                                    if (legFirstIndex.has("steps")) {
                                        val stepsArray = legFirstIndex.getJSONArray("steps")
                                        if (stepsArray.length() > 0) {
                                            val stepFirstIndex = stepsArray.getJSONObject(0)
                                            if (stepFirstIndex.has("polyline")) {
                                                val polyLineObject =
                                                    stepFirstIndex.getJSONObject("polyline")
                                                if (polyLineObject.has("points")) {
                                                    polyLineToDraw =
                                                        polyLineObject.getString("points")
                                                }
                                            }
                                        }
                                    }
                                }
                            }*/
                        }
                    }

                    /*val routelist = ArrayList<LatLng>()
                    if (directionResults.body()?.routes?.size!! > 0) {
                        var decodelist: ArrayList<LatLng>
                        val routeA = directionResults.body()?.routes?.get(0)!!
                        Log.i("zacharia", "Legs length : " + routeA.legs.size)
                        if (routeA.legs.size > 0) {
                            val steps: List<Steps> = routeA.legs[0].steps
                            Log.i("zacharia", "Steps size :" + steps.size)
                            var step: Steps
                            var locationModel: LocationModel
                            var polyline: String?
                            for (i in steps.indices) {
                                step = steps[i]
                                if (step.start_locationModel == null)
                                    continue
                                locationModel = step.start_locationModel
                                routelist.add(LatLng(locationModel.lat, locationModel.lng))
                                Log.i(
                                    "zacharia",
                                    "Start Location :" + locationModel.lat.toString() + ", " + locationModel.lng
                                )
                                polyline = step.polyline.getPoints()
                                decodelist = RouteDecode.decodePoly(polyline)
                                routelist.addAll(decodelist)
                                locationModel = step.end_locationModel
                                routelist.add(LatLng(locationModel.lat, locationModel.lng))
                                Log.i(
                                    "zacharia",
                                    "End Location :" + locationModel.lat.toString() + ", " + locationModel.lng
                                )
                            }
                        }
                    }
                    Log.i("zacharia", "routelist size : " + routelist.size)
                    if (routelist.size > 0) {
                        val rectLine = PolylineOptions().width(10f).color(
                            Color.RED
                        )
                        for (i in 0 until routelist.size) {
                            rectLine.add(routelist[i])
                        }
                        // Adding route on the map
                        googleMap.addPolyline(rectLine)
//                        markerOptions.position(toPosition)
//                        markerOptions.draggable(true)
//                        googleMap.addMarker(markerOptions)
                    }*/

                    if (polyLineToDraw.isNotEmpty()) {
                        val rectLine = PolylineOptions().width(5f).color(
                            Color.RED
                        )
                        val decodelist = RouteDecode.decodePoly(polyLineToDraw)
                        if (decodelist.size > 0) {
                            for (i in 0 until decodelist.size)
                                rectLine.add(decodelist[i])
                            googleMap.addPolyline(rectLine)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Error Message: " + throwable.localizedMessage)
            }
        })
    }

    private fun showSelectRadiusDialog() {
        multiPurposeDialog = Dialog(requireContext())
        multiPurposeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = SelectRadiousDialogBinding.inflate(layoutInflater)
        multiPurposeDialog.setContentView(dialogViewBinding.root)
        multiPurposeDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        multiPurposeDialog.show()
        dialogViewBinding.cancelButton.setOnClickListener {
            multiPurposeDialog.dismiss()
        }
        dialogViewBinding.searchButton.setOnClickListener {
            if (dialogViewBinding.radiusEditText.text.isNotEmpty()) {
                multiPurposeDialog.dismiss()
                requestBookingAPI(dialogViewBinding.radiusEditText.text.toString())
            } else
                Toast.makeText(
                    context!!,
                    "Must enter radius to search provider(s)",
                    Toast.LENGTH_SHORT
                ).show()
        }
        dialogViewBinding.maxRadiusButton.setOnClickListener {
            multiPurposeDialog.dismiss()
            requestBookingAPI("max")
        }
    }

    private fun startTimer(
        hoursTextView: TextView,
        minutesTextView: TextView,
        secondsTextView: TextView
    ) {
        val totalSeconds: Long = 120
        val intervalSeconds: Long = 1

        val timer: CountDownTimer =
            object : CountDownTimer(totalSeconds * 1000, intervalSeconds * 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    Log.d(
                        "seconds elapsed: ",
                        ((totalSeconds * 1000 - millisUntilFinished) / 1000).toString()
                    )
                    val seconds: Int =
                        ((totalSeconds * 1000 - millisUntilFinished) / 1000).toInt() % 60
                    var hours: Int =
                        ((totalSeconds * 1000 - millisUntilFinished) / 1000).toInt() / 60
                    val minutes = hours % 60
                    hours /= 60

                    if (seconds < 10)
                        secondsTextView.text = "0$seconds"
                    else
                        secondsTextView.text = "$seconds"

                    if (minutes < 10)
                        minutesTextView.text = "0$minutes"
                    else
                        minutesTextView.text = "$minutes"

                    if (hours < 10)
                        hoursTextView.text = "0$hours"
                    else
                        hoursTextView.text = "$hours"

                }

                override fun onFinish() {
                    Log.d("done!", "Time's up!")
                }
            }
        timer.start()
    }
}