package com.sarcoordinator.sarsolutions.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import kotlinx.android.synthetic.main.fragment_onboarding_first.*

class OnboardingFirstFragment : Fragment(R.layout.fragment_onboarding_first) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (GlobalUtil.getCurrentTheme(
                resources,
                requireActivity().getPreferences(Context.MODE_PRIVATE)
            ) ==
            GlobalUtil.THEME_DARK
        ) {
            background.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.dark_onboarding_1
                )
            )
        }
    }
}