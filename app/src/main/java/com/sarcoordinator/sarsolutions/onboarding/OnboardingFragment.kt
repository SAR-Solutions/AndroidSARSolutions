package com.sarcoordinator.sarsolutions.onboarding

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.sarcoordinator.sarsolutions.LoginFragment
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.fragment_onboarding.*

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onboarding_view_pager.adapter = OnboardingAdapter(this)

        setupOnboardingIndicators()

        onboarding_view_pager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setOnboardingIndicator(position)
            }
        })
    }

    private fun setupOnboardingIndicators() {
        val indicators = arrayOfNulls<ImageView>(3)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 8, 8, 8)

        indicators.forEachIndexed { index, imageView ->
            indicators[index] = ImageView(requireContext())
            val indicatorDrawable = getDrawable(requireContext(), R.drawable.onboarding_indicator)
            indicators[index]!!.setImageDrawable(indicatorDrawable)
            indicators[index]!!.layoutParams = layoutParams
            onboarding_indicators_parent.addView(indicators[index])
        }
    }

    private fun setOnboardingIndicator(position: Int) {
        onboarding_indicators_parent.children.forEachIndexed { index, view ->
            if (index == position) {
                (view as ImageView).drawable.alpha = 255
            } else {
                (view as ImageView).drawable.alpha = 64 //25%
            }
        }
    }

    private inner class OnboardingAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int {
            return 4
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OnboardingFirstFragment()
                1 -> OnboardingSecondFragment()
                2 -> OnboardingThirdFragment()
                else -> LoginFragment()
            }
        }
    }
}