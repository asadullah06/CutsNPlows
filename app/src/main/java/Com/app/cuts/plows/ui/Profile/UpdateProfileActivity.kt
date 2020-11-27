package Com.app.cuts.plows.ui.Profile

import Com.app.cuts.plows.NetworkCalls.ApiClient
import Com.app.cuts.plows.NetworkCalls.ApiInterface
import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.GetPictureBottomSheetBinding
import Com.app.cuts.plows.databinding.UpdateProfileActivityBinding
import Com.app.cuts.plows.ui.BaseActivity
import Com.app.cuts.plows.ui.Dashboard.HomeScreenActivity
import Com.app.cuts.plows.utils.CommonMethods
import Com.app.cuts.plows.utils.UserPreferences
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UpdateProfileActivity : BaseActivity(), View.OnClickListener {
    val TAG = "UpdateProfileActivity"
    lateinit var binding: UpdateProfileActivityBinding

    lateinit var photoURI: Uri
    lateinit var realPathOfImage: String
    private val CAMERA = 2
    private val GALLERY = 1
    private val cameraPermission = arrayOf(Manifest.permission.CAMERA)
    private val galleryPermission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private val MY_PERMISSIONS_REQUEST_CAMERA = 0
    private val MY_PERMISSIONS_REQUEST_GALLERY = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UpdateProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.updateProfileButton.setOnClickListener(this)
        binding.cardView.setOnClickListener(this)
        binding.countryPicker.registerCarrierNumberEditText(binding.editTextMobileNumber)

        if (intent.hasExtra("first_name")) {
            binding.firstNameEditText.setText(intent.getStringExtra("first_name"))
        }
        if (intent.hasExtra("last_name"))
            binding.lastNameEditText.setText(intent.getStringExtra("last_name"))
        binding.countryPicker.fullNumber = intent.getStringExtra("user_phone_no")

        if (intent.getStringExtra("user_profile_image").toString().isNotEmpty()) {
            Picasso.get().load(intent.getStringExtra("user_profile_image").toString())
                .fit()
                .into(binding.imageView3)
        }
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
                if (binding.firstNameEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter your first name", Toast.LENGTH_LONG).show()
                    return
                }
                if (binding.lastNameEditText.text.toString().isEmpty()) {
                    Toast.makeText(this, "Please enter your last name", Toast.LENGTH_LONG).show()
                    return
                }
                if (binding.editTextMobileNumber.toString().isEmpty()) {
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

                updateUserProfile()
            }
            R.id.cardView -> {
                showTakePhotoBottomSheet(this)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhotoFromCamera()
        } else if (requestCode == MY_PERMISSIONS_REQUEST_GALLERY && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            choosePhotoFromGallery()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CAMERA -> {
                binding.imageView3.setImageURI(photoURI)
//                val bitmapImage = CommonMethods.getUriToBitmapImage(photoURI, this)
//                val file = File(CommonMethods.compressImage(this, bitmapImage!!))
                val file = File(realPathOfImage)
                updateUserProfile(file)
            }
            GALLERY -> {
                if (data != null) {
                    binding.imageView3.setImageURI(data.data!!)
//                    val bitmapImage = CommonMethods.getUriToBitmapImage(data.data!!, this)
//                    val file = File(CommonMethods.compressImage(this, bitmapImage!!))
                    val file = File(CommonMethods.getRealPathFromURI(data.data!!, this))
                    updateUserProfile(file)
                }
            }
        }
    }

    private fun updateUserProfile(file: File? = null) {
        val userProfile = if (file == null) {
            null
        } else {
            MultipartBody.Part.createFormData(
                "fld_profile_pic", file.name, RequestBody.create(
                    MediaType.parse("multipart/form-data"), file
                )
            )

        }

        val firstName: RequestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            binding.firstNameEditText.text.toString()
        )
        val lastName: RequestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            binding.lastNameEditText.text.toString()
        )
        val contactNumber: RequestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            binding.countryPicker.fullNumberWithPlus
        )
        val userId: RequestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            UserPreferences.getClassInstance(this).getUserId() ?: ""
        )

        val apiService = ApiClient.getClient(this)?.create(ApiInterface::class.java)
        val call = apiService?.updateUserProfile(
            firstName,
            lastName,
            contactNumber,
            userId,
            userProfile
        )
        binding.progressBar.visibility = View.VISIBLE
        call?.enqueue(
            object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val responseObject = JSONObject(response.body()?.string() ?: "")
                        Toast.makeText(
                            this@UpdateProfileActivity,
                            responseObject.getString("message"),
                            Toast.LENGTH_LONG
                        ).show()
                        this@UpdateProfileActivity.finish()
                    } else {
                        val responseObject = JSONObject(response.errorBody()?.string() ?: "")
                        Toast.makeText(
                            this@UpdateProfileActivity,
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

    private fun showTakePhotoBottomSheet(mContext: Context) {
        val dialog = BottomSheetDialog(mContext)
        val bottomSheetBinding = GetPictureBottomSheetBinding.inflate(layoutInflater)
        dialog.setContentView(bottomSheetBinding.root)
        bottomSheetBinding.captureImageButton.setOnClickListener {
            dialog.dismiss()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    cameraPermission,
                    MY_PERMISSIONS_REQUEST_CAMERA
                )
            } else {
                takePhotoFromCamera()
            }
        }
        bottomSheetBinding.takeFromGalleryButton.setOnClickListener {
            dialog.dismiss()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    galleryPermission,
                    MY_PERMISSIONS_REQUEST_GALLERY
                )
            } else {
                choosePhotoFromGallery()
            }
        }
        dialog.show()
    }

    private fun takePhotoFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) { // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) { // Error occurred while creating the File
                ex.printStackTrace()
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "Com.app.cuts.plows", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? { // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        val image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",  /* suffix */
//                storageDir /* directory */
//        )

        val image = File(storageDir?.path + File.separator + "IMG_" + timeStamp + ".jpg");
        // Save a file: path for use with ACTION_VIEW intents
        realPathOfImage = image.absolutePath
        return image
    }


    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY)
    }
}