package Com.app.cuts.plows.NetworkCalls

import Com.app.cuts.plows.utils.BASE_URL
import Com.app.cuts.plows.utils.GOOGLE_MAP_BASE_URL
import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient {
    companion object {
        var retrofit: Retrofit? = null
        fun getOkHttpClient(): OkHttpClient {
            return try {
//                val interceptor = httpLoggingInterceptor()
//                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
                OkHttpClient.Builder()
//                .followRedirects(true)
//                .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .cache(null)
//                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
//                    .addInterceptor(interceptor)
                    .build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        fun getClient(context: Context): Retrofit? {
//            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder().serializeNulls().setLenient().create()
                        )
                    )
                    .build()
//            }
            return retrofit
        }
        fun getGoogleMapClient():Retrofit?{
//            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(GOOGLE_MAP_BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder().serializeNulls().setLenient().create()
                        )
                    )
                    .build()
//            }
            return retrofit
        }
    }
}