package Com.app.cuts.plows.ui

import Com.app.cuts.plows.databinding.SplashScreenActivityBinding
import Com.app.cuts.plows.ui.Dashboard.HomeScreenActivity
import Com.app.cuts.plows.ui.login.LoginActivity
import Com.app.cuts.plows.utils.UserPreferences
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate

class SplashScreenActivity : Activity() {
    lateinit var binding: SplashScreenActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashScreenActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Thread {
            Thread.sleep(1000)
            callLoginScreen()
        }.start()
    }

    private fun callLoginScreen() {
        if (UserPreferences.getClassInstance(this).getKeepUserLoginFlag()) {
            val intent = Intent(this, HomeScreenActivity::class.java)
            startActivity(intent)
            this.finish()
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }
}