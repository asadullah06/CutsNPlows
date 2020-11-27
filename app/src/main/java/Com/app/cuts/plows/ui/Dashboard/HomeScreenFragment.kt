package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.Models.UserDetailsModel
import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.HomeScreenFragmentBinding
import Com.app.cuts.plows.databinding.StartEndJobDialogBinding
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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
                Toast.makeText(requireContext(), "New Notification received", Toast.LENGTH_LONG)
                    .show()

                val action = intent.action
                if (action != null) {
                    if (action == "finish_activity_pathwayMain") {
                    }
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
                    requestBookingAPI()
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
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
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

    private fun requestBookingAPI() {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.requestBooking(
            UserPreferences.getClassInstance(requireContext()).getUserId() ?: "",
            googleApiClass.latitude,
            googleApiClass.longitude
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val dataObjects = responseObject.getJSONObject("data")
                    findProvider(dataObjects.getInt("fld_bid"))
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

    private fun findProvider(mbookingId: Int) {
        val apiService = ApiClient.getClient(requireContext())?.create(ApiInterface::class.java)
        val call = apiService?.findProvider(
            mbookingId
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
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
                                "${bookingObject.getString("fld_fname")} ${bookingObject.getString("fld_lname")}"
                            val providerContact = bookingObject.getString("fld_contact_number")
                            val providerPicture = bookingObject.getString("fld_profile_pic")
                            val providerDistance = bookingObject.getDouble("distance")
                            val providerId = bookingObject.getString("fld_userid")
                            foundProvidersList.add(
                                UserDetailsModel(
                                    providerName,
                                    resources.getString(R.string.service_provider),
                                    providerContact,
                                    providerPicture ?: "",
                                    providerDistance,
                                    providerId
                                )
                            )
                        }
                        binding.findProviderButton.text = getString(R.string.view_found_providers)
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
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
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
                                "${bookingObject.getString("fld_fname")} ${bookingObject.getString("fld_lname")}"
                            val providerContact = bookingObject.getString("fld_contact_number")
                            val providerPicture = bookingObject.getString("fld_profile_pic")
                            val providerDistance = bookingObject.getDouble("distance")
                            val providerId = bookingObject.getString("fld_userid")
                            foundProvidersList.add(
                                UserDetailsModel(
                                    providerName,
                                    resources.getString(R.string.service_provider),
                                    providerContact,
                                    providerPicture ?: "",
                                    providerDistance,
                                    providerId
                                )
                            )
                        }
                        binding.findProviderButton.text = getString(R.string.view_found_providers)
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
                        if (bookingStatus == FINISH_JOB_REQUEST || bookingStatus == STATUS_JOB_STARTED) {
                            showStartEndJobDialog(bookingId, bookingStatus)
                            return
                        }
                        val providerName =
                            "${bookingObject.getString("fld_fname")} ${bookingObject.getString("fld_lname")}"
                        val providerContact = bookingObject.getString("fld_contact_number")
                        val providerPicture = bookingObject.getString("fld_profile_pic")
                        val providerLat = bookingObject.getDouble("fld_lat")
                        val providerLong = bookingObject.getDouble("fld_lng")

                        showProviderDetailsDialog(
                            providerName,
                            providerContact,
                            providerPicture,
                            providerLat,
                            providerLong,
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
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
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
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    val dataObjects = responseObject.getJSONObject("data")
                    val bookingStatus = dataObjects.getInt("tbl_status")
                    val bookingId = dataObjects.getInt("fld_bid")
                    if (bookingStatus == STATUS_ARRIVED) {
                        showStartEndJobDialog(bookingId, bookingStatus)
                    } else if (bookingStatus == STATUS_JOB_STARTED) {
                        showStartEndJobDialog(bookingId, bookingStatus)
                        return
                    }

                    val customerName =
                        "${dataObjects.getString("fld_fname")} ${dataObjects.getString("fld_lname")}"
                    val customerContact = dataObjects.getString("fld_contact_number")
                    val customerPicture = dataObjects.getString("fld_profile_pic")
                    val customerLat = dataObjects.getDouble("fld_lat")
                    val customerLong = dataObjects.getDouble("fld_lng")

                    showCustomerDetailsDialog(
                        customerName,
                        customerContact,
                        customerPicture,
                        customerLat,
                        customerLong,
                        bookingId,
                        bookingStatus
                    )
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
        customerName: String,
        customerContact: String,
        customerPicture: String,
        customerLat: Double,
        customerLong: Double,
        bookingId: Int,
        bookingStatus: Int
    ) {

        binding.userDetailsLayout.root.visibility = View.VISIBLE

        binding.userDetailsLayout.userRoleTextView.text = resources.getString(R.string.customer)
        binding.userDetailsLayout.acceptRejectLayout.visibility = View.VISIBLE
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
            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("sms:$customerContact")
            startActivity(smsIntent)
        }
        binding.userDetailsLayout.userNameTextView.text = customerName
        if (customerPicture.isNotEmpty() && !customerPicture.equals("null", ignoreCase = true)) {
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
            val uri: String =
                java.lang.String.format(Locale.ENGLISH, "geo:%f,%f", customerLat, customerLong)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            context!!.startActivity(intent)
        }
        val currentLocation = LatLng(customerLat, customerLong)
        val mp = MarkerOptions()
        mp.position(currentLocation)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_marker))
            .title(customerName)

        googleMap.addMarker(mp)
        val cameraPosition = CameraPosition.Builder().target(currentLocation).zoom(12f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        /*binding.userDetailsLayout.navigateButton.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }*/
    }

    private fun showProviderDetailsDialog(
        providerName: String,
        providerContact: String,
        providerPicture: String,
        providerLat: Double,
        providerLong: Double,
        bookingStatus: Int
    ) {
        binding.findProviderButton.visibility = View.GONE

        binding.userDetailsLayout.root.visibility = View.VISIBLE


        when (bookingStatus) {
            ONLINE_STATUS_AVAILABLE -> {
                binding.userDetailsLayout.userRoleTextView.text =
                    "${resources.getString(R.string.provider)} is on its way"
                binding.userDetailsLayout.acceptRejectLayout.visibility = View.VISIBLE
                binding.userDetailsLayout.acceptCustomerButton.visibility = View.GONE
                binding.userDetailsLayout.navigateButton.visibility = View.VISIBLE
                binding.userDetailsLayout.otherButtonsLayout.visibility = View.GONE
            }
            STATUS_ARRIVED -> {
                binding.userDetailsLayout.userRoleTextView.text =
                    "${resources.getString(R.string.provider)} has arrived"
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
            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("sms:$providerContact")
            startActivity(smsIntent)
        }
        binding.userDetailsLayout.userNameTextView.text = providerName
        if (providerPicture.isNotEmpty() && !providerPicture.equals("null", ignoreCase = true)) {
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
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
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
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val responseObject = JSONObject(response.body()?.string() ?: "")
                    Toast.makeText(
                        requireContext(),
                        responseObject.getString("message"),
                        Toast.LENGTH_LONG
                    ).show()
                    if (bookingStatus == STATUS_ARRIVED) {
                        binding.userDetailsLayout.acceptCustomerButton.visibility = View.GONE
                        binding.userDetailsLayout.arrivedCustomerButton.visibility = View.GONE
                        binding.userDetailsLayout.navigateButton.visibility = View.GONE
                        binding.userDetailsLayout.startJobButton.visibility = View.VISIBLE

                        showStartEndJobDialog(bookingId, STATUS_ARRIVED)
                    } else if (bookingStatus == STATUS_JOB_STARTED) {
                        showStartEndJobDialog(bookingId, STATUS_JOB_STARTED)
                        binding.userDetailsLayout.root.visibility = View.GONE
                    } else if (bookingStatus == FINISH_JOB_REQUEST) {

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
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = StartEndJobDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogViewBinding.root)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialogViewBinding.descriptionTextView.text =
            "Congratulations! The vendor you selected has\n arrived and is on site ready to serve you"
        if (bookingStatus == STATUS_ARRIVED)
            dialogViewBinding.bookingStatusTextView.text = "Arrived"
        else if (bookingStatus == STATUS_JOB_STARTED && UserPreferences.getClassInstance(
                requireContext()
            ).getUserRole() == resources.getString(R.string.service_provider)
        ) {
            dialogViewBinding.bookingStatusTextView.text = "Job in Progress"
            dialogViewBinding.startJobButton.visibility = View.GONE
            dialogViewBinding.finishJobButton.alpha = 1f
            dialogViewBinding.finishJobButton.isEnabled = true
            dialogViewBinding.finishJobButton.tag = "0"
            dialog.setCancelable(false)
        } else if (bookingStatus == STATUS_JOB_STARTED) {
            dialogViewBinding.bookingStatusTextView.text = "Job in Progress"
            dialogViewBinding.startJobButton.visibility = View.GONE
            dialogViewBinding.finishJobButton.isEnabled = false
            dialogViewBinding.finishJobButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.green
                )
            )
            dialog.setCancelable(false)
            dialogViewBinding.descriptionTextView.text =
                "The vendor you selected has arrived and serving you now."
        } else if (bookingStatus == FINISH_JOB_REQUEST) {
            dialogViewBinding.bookingStatusTextView.text = "Job in Progress"
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
            dialog.setCancelable(false)
            dialogViewBinding.descriptionTextView.text =
                "The vendor you selected has arrived and serving you now."
        }


        dialogViewBinding.startJobButton.setOnClickListener {
            dialog.dismiss()
            updateBookingStatusForCustomerAPI(bookingId, STATUS_JOB_STARTED)
        }
        dialogViewBinding.finishJobButton.setOnClickListener {
            dialog.dismiss()
            if (dialogViewBinding.finishJobButton.tag == "0")
                updateBookingStatusForCustomerAPI(bookingId, FINISH_JOB_REQUEST)
            else if (dialogViewBinding.finishJobButton.tag == "1")
                acceptJobFinishedRequestAPI(bookingId)


        }
    }

    private fun showCollectCashDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = StartEndJobDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogViewBinding.root)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialogViewBinding.bookingStatusTextView.text = "Collect Cash"
        dialogViewBinding.descriptionTextView.text = "The job is finished, please collect cash"
        dialogViewBinding.innerLayout.visibility = View.GONE
        dialogViewBinding.paymentCollectLayout.visibility = View.VISIBLE

        dialogViewBinding.processButton.setOnClickListener {
            if (dialogViewBinding.cashEditText.text.toString().isNotEmpty()) {

            }
        }
    }

    private fun showRatingDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogViewBinding = StartEndJobDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogViewBinding.root)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialogViewBinding.bookingStatusTextView.visibility = View.GONE
        dialogViewBinding.descriptionTextView.visibility = View.GONE
        dialogViewBinding.ratingLayout.visibility = View.VISIBLE
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

}