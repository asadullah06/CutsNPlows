package Com.app.cuts.plows

import Com.app.cuts.plows.databinding.ActivityMainBinding
import Com.app.cuts.plows.ui.BaseActivity
import android.os.Bundle

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindind.root)
    }
}