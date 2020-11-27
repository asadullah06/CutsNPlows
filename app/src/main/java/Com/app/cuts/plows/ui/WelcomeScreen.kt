package Com.app.cuts.plows.ui

import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.WelcomeScreenBinding
import Com.app.cuts.plows.ui.registration.RegistrationFormActivity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View

class WelcomeScreen : BaseActivity(), View.OnClickListener {
    lateinit var binding: WelcomeScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WelcomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.createAccountButton.setOnClickListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

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
            R.id.createAccountButton -> {
                val intent = Intent(this, RegistrationFormActivity::class.java)
                startActivity(intent)
            }
        }
    }
}