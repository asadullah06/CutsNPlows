package Com.app.cuts.plows.ui

import Com.app.cuts.plows.databinding.TermsAndConditionsActivityBinding
import android.os.Bundle
import android.view.MenuItem

class TermsAndConditionsActivity : BaseActivity() {
    lateinit var binding: TermsAndConditionsActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TermsAndConditionsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}