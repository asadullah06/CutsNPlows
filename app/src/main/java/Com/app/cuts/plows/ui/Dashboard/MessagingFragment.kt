package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.databinding.MessagingFragmentBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class MessagingFragment :Fragment(){
lateinit var binding:MessagingFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MessagingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}