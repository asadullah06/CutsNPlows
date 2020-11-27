package Com.app.cuts.plows.utils

import android.content.Context
import android.content.SharedPreferences

class UserPreferences {

    companion object {
        lateinit var preferences: SharedPreferences
        private lateinit var instance: UserPreferences

        fun getClassInstance(context: Context): UserPreferences {
            instance = UserPreferences()
            preferences = context.getSharedPreferences(USER_PREF, Context.MODE_PRIVATE)
            return instance
        }
    }

    fun setUserId(userId: String) {
        preferences.edit().putString("user_id", userId).apply()
    }

    fun getUserId(): String? {
        return preferences.getString("user_id", "")
    }

    fun setKeepUserLoginFlag() {
        preferences.edit().putBoolean("user_login", true).apply()
    }

    fun getKeepUserLoginFlag(): Boolean {
        return preferences.getBoolean("user_login", false)
    }

    fun clearUserPreferences() {
        preferences.edit().clear().apply()
    }

    fun setUserName(userName: String) {
        preferences.edit().putString("user_name", userName).apply()
    }

    fun getUserName(): String? {
        return preferences.getString("user_name", "")
    }

    fun setUserRole(userRole: String) {
        preferences.edit().putString("user_role", userRole).apply()
    }

    fun getUserRole(): String {
        return preferences.getString("user_role", "") ?: ""
    }

    fun setUserProfile(imageUrl: String) {
        preferences.edit().putString("user_profile_image", imageUrl).apply()
    }

    fun getUserProfile(): String {
        return preferences.getString("user_profile_image", "") ?: ""
    }

    fun getUserAvailability(): Int {
        return preferences.getInt("user_available", -1)
    }

    fun setUserAvailability(status: Int) {
        preferences.edit().putInt("user_available", status).apply()
    }
}