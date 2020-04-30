package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.sarcoordinator.sarsolutions.util.LocationServiceManager
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.setMargins
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.fragment_track_offline.*
import kotlinx.android.synthetic.main.view_circular_button.view.*
import timber.log.Timber

class TrackOfflineFragment : Fragment() {

    val nav: Navigation by lazy { Navigation.getInstance() }

    private lateinit var bottomSheet: BottomSheetBehavior<MaterialCardView>

    private val locationServiceManager: LocationServiceManager by lazy {
        LocationServiceManager.getInstance(requireActivity() as MainActivity)
    }
    private val sharedPrefs: SharedPreferences by lazy {
        requireActivity().getPreferences(Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_offline, container, false)

        bottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.images_card))

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyViewInsets()

        // Hide image fab
        capture_photo_fab.hide()

        // Init fab click listener
        location_service_fab.setOnClickListener {
            if (!locationServiceManager.getServiceStatus()) {
                // Start shift
                locationServiceManager.startLocationService(
                    sharedPrefs.getBoolean(TabSettingsFragment.TESTING_MODE_PREFS, false)
                )
            } else {
                // End shift
                Timber.d("Offline Shift Result = ${locationServiceManager.stopOfflineLocationService()}")
            }
        }

        back_button_view.image_button.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_arrow_back_24))
        back_button_view.image_button.setOnClickListener { requireActivity().onBackPressed() }
    }

    override fun onStart() {
        super.onStart()
        nav.hideBottomNavBar?.let { it(true) }
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)
    }

    private fun applyViewInsets() {
        back_button_view.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom
            )
        }
        capture_photo_fab.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom
            )
        }
        location_service_fab.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom
            )
        }
    }
}
