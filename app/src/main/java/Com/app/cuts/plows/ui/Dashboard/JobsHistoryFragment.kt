package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.databinding.JobsHistoryFragmentBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class JobsHistoryFragment : Fragment() {
    lateinit var binding: JobsHistoryFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = JobsHistoryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}