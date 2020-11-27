package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.R
import Com.app.cuts.plows.databinding.HomeScreenActivityBinding
import Com.app.cuts.plows.ui.BaseActivity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.tabs.TabLayout


class HomeScreenActivity : BaseActivity() {
    lateinit var builder: HomeScreenActivityBinding
    lateinit var tabAdapter: TabAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        builder = HomeScreenActivityBinding.inflate(layoutInflater)
        setContentView(builder.root)

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)

        tabAdapter = TabAdapter(supportFragmentManager)
        tabAdapter.addFragment(JobsHistoryFragment(), "Job History")
        tabAdapter.addFragment(HomeScreenFragment(), "Home")
        tabAdapter.addFragment(MessagingFragment(), "Chat")
        tabAdapter.addFragment(ProfileFragment(), "Profile")

        builder.viewPager.adapter = tabAdapter
        builder.tabLayout.setupWithViewPager(builder.viewPager)
        builder.tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_job_history)
        builder.tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_home)
        builder.tabLayout.getTabAt(2)?.setIcon(R.drawable.chat)
        builder.tabLayout.getTabAt(3)?.setIcon(R.drawable.ic_profile)



        builder.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                val labelTextView =
                    supportActionBar?.customView?.findViewById<TextView>(R.id.tvTitle)
                when (position) {
                    0 -> labelTextView?.text = "Job History"
                    1 -> labelTextView?.text = "Home"
                    2 -> labelTextView?.text = "Chat"
                    3 -> labelTextView?.text = "Profile"
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })


        val tab: TabLayout.Tab? = builder.tabLayout.getTabAt(1)
        tab?.select()

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}