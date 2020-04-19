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
import com.sarcoordinator.sarsolutions.MainActivity
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.util.setMargins
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.fragment_onboarding.*

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private lateinit var adapter: OnboardingAdapter

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("Current", onboarding_view_pager.currentItem)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = OnboardingAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onboarding_view_pager.adapter = adapter

        setupOnboardingIndicators()

        setupInsets()

        button.setOnClickListener {
            onboarding_view_pager.apply {
                setCurrentItem(currentItem + 1, true)
            }
        }

        onboarding_view_pager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setOnboardingIndicator(position)

                when (position) {
                    in 0..1 -> {
                        button.text = getString(R.string.next)
                        button.visibility = View.VISIBLE
                        onboarding_indicators_parent.visibility = View.VISIBLE
                    }
                    2 -> {
                        button.text = getString(R.string.login)
                        button.visibility = View.VISIBLE
                        onboarding_indicators_parent.visibility = View.VISIBLE
                    }
                    3 -> {
                        button.visibility = View.GONE
                        onboarding_indicators_parent.visibility = View.GONE
                    }
                }
            }
        })

        savedInstanceState?.let {
            onboarding_view_pager.setCurrentItem(savedInstanceState.getInt("Current"), false)
        }
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)
    }

    private fun setupInsets() {
        button.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }

        onboarding_indicators_parent.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }
    }

    private fun setupOnboardingIndicators() {
        val indicators = arrayOfNulls<ImageView>(3)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)

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