package Com.app.cuts.plows.NetworkCalls

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {

    @FormUrlEncoded
    @POST("signup")
    fun registerUser(
        @Field("fld_fname") userFirstName: String,
        @Field("fld_lname") userLastName: String,
        @Field("fld_email") userEmail: String,
        @Field("fld_contact_number") userPhoneNumber: String,
        @Field("fld_password") userPassword: String,
        @Field("confirm_password") userConfirmPassword: String,
        @Field("fld_role") userRole: Int,
        @Field("fld_bio") providerBio:String,
        @Field("fld_device_token") deviceToken: String = "",
        @Field("fld_lat") userLat: Double = 0.0,
        @Field("fld_lng") userLong: Double = 0.0
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("signupvalidate")
    fun velidateUser(
        @Field("fld_fname") userFirstName: String,
        @Field("fld_lname") userLastName: String,
        @Field("fld_email") userEmail: String,
        @Field("fld_contact_number") userPhoneNumber: String,
        @Field("fld_password") userPassword: String,
        @Field("confirm_password") userConfirmPassword: String,
        @Field("fld_lat") userLat: Double = 0.0,
        @Field("fld_lng") userLong: Double = 0.0
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("signin")
    fun loginUser(
        @Field("fld_email") userEmail: String,
        @Field("fld_password") userPassword: String,
        @Field("fld_device_token") deviceToken: String = "",
        @Field("fld_device_type") deviceType: String,
        @Field("fld_lat") userLat: Double = 0.0,
        @Field("fld_lng") userLong: Double = 0.0
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("getProfile")
    fun getProfile(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("changePassword")
    fun changePassword(
        @Field("fld_old_password") oldPassword: String,
        @Field("fld_password") newPassword: String,
        @Field("confirm_password") confirmPassword: String,
        @Field("fld_userid") userId: String
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("forgetPassword")
    fun forgetPassword(
        @Field("fld_email") userEmail: String
    ): Call<ResponseBody?>

    @Multipart
    @POST("updateProfile")
    fun updateUserProfile(
        @Part("fld_fname") userFirstName: RequestBody,
        @Part("fld_lname") userLastName: RequestBody,
        @Part("fld_contact_number") userPhoneNumber: RequestBody,
        @Part("fld_userid") userId: RequestBody,
        @Part file: MultipartBody.Part?
    ): Call<ResponseBody?>

    @GET("getProviders")
    fun getProviders(): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("upd_online_status")
    fun updateOnlineStatus(
        @Field("fld_userid") userId: String,
        @Field("fld_online_status") onlineStatus: Int
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("addbooking")
    fun requestBooking(
        @Field("fld_userid") userId: String,
        @Field("fld_lat") userLat: Double,
        @Field("fld_lng") userLong: Double
    ): Call<ResponseBody?>


    @FormUrlEncoded
    @POST("findprovider")
    fun findProvider(
        @Field("fld_radius") radius: String,
        @Field("fld_bid") bookingId: Int
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("customerdashboard")
    fun getCustomerDashboard(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("providerdashboard")
    fun getProviderDashboard(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("updatelatlng")
    fun updateLocation(
        @Field("fld_userid") userId: String,
        @Field("fld_lat") latitude: Double,
        @Field("fld_lng") longitude: Double,
        @Field("fld_device_token") deviceToken: String = ""
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("acceptrequest")
    fun acceptCustomer(
        @Field("fld_userid") userId: String,
        @Field("fld_bid") bookingId: Int,
        @Field("fld_lat") latitude: Double,
        @Field("fld_lng") longitude: Double
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("rejectrequest")
    fun rejectCustomer(
        @Field("fld_userid") userId: String,
        @Field("fld_bid") bookingId: Int
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("updatebkgtatus")
    fun updateBookingStatus(
        @Field("fld_bid") bookingId: Int,
        @Field("fld_userid") userId: String,
        @Field("fld_bk_status") bookingStatus: Int
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @GET("getBooking")
    fun getBooking(@Field("fld_bid") bookingId: Int): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("sendrequest")
    fun acceptProvider(
        @Field("fld_bid") bookingId: String,
        @Field("fld_userid") userId: String
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("cancelrequest")
    fun cancelBookingRequest(
        @Field("fld_bid") bookingId: Int
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("updatebooking")
    fun acceptJobFinishedRequest(
        @Field("fld_bid") bookingId: Int
    ): Call<ResponseBody?>

    @FormUrlEncoded
    @POST("requestpayment")
    fun providerRequestPayment(
        @Field("fld_userid") userId: String,
        @Field("fld_bid") bookingId: Int,
        @Field("fld_amount") paymentAmount: Float
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("acceptpayment")
    fun customerAcceptPaymentRequest(
        @Field("fld_userid") userId: String,
        @Field("fld_bid") bookingId: Int
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("providerreview")
    fun customerReviewProvider(
        @Field("fld_bid") bookingId: Int,
        @Field("fld_rating") rating: Float,
        @Field("fld_review") reviewString: String,
        @Field("fld_userid") userId: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("customerreview")
    fun providerReviewCustomer(
        @Field("fld_bid") bookingId: Int,
        @Field("fld_rating") rating: Float,
        @Field("fld_review") reviewString: String,
        @Field("fld_userid") userId: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("logout")
    fun logoutUser(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("providerbookinghistory")
    fun getProviderJobHistory(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("customerbookinghistory")
    fun getCustomerJobHistory(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody>

    @GET("/maps/api/directions/json")
    fun getDirectionJson(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("sensor") sensor: Boolean,
        @Query("mode") mode: String,
        @Query("key") apiKey: String

    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("chatlist")
    fun getConversationsList(
        @Field("fld_userid") userId: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("getmessages")
    fun getMessagesThread(
        @Field("fld_sender_id") senderId: String,
        @Field("fld_receiver_id") receiverId: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("messagesend")
    fun sendMessage(
        @Field("fld_message") message: String,
        @Field("fld_sender_id") senderId: String,
        @Field("fld_receiver_id") receiverId: String
    ): Call<ResponseBody>
}