package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.CustomFragment
import com.sarcoordinator.sarsolutions.util.MapFragment
import kotlinx.android.synthetic.main.fragment_shift_detail.*

class ShiftDetailFragment : Fragment(R.layout.fragment_shift_detail), CustomFragment {

    companion object ARGS {
        val CACHED_SHIFT = "CACHED_SHIFT"
    }

    private lateinit var cachedShiftReport: LocationsInShiftReport

    override fun getSharedElement(): View = toolbar_detailed_shift

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Fetch from savedInstace if arguments is null
        cachedShiftReport = arguments?.getSerializable(CACHED_SHIFT) as LocationsInShiftReport

        toolbar_detailed_shift.setHeading(cachedShiftReport.shiftReport.caseName.toString())

        val mapFragment = MapFragment()

        cachedShiftReport.locationList?.let {
            mapFragment.arguments = Bundle().apply {
                putParcelableArrayList(MapFragment.LOCATIONS_KEY, ArrayList(it))
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.map_container, mapFragment)
            .commit()

    }
}