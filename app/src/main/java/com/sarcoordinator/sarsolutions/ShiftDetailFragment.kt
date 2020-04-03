package com.sarcoordinator.sarsolutions

import android.view.View
import androidx.fragment.app.Fragment
import com.sarcoordinator.sarsolutions.util.CustomFragment
import kotlinx.android.synthetic.main.fragment_shift_detail.*

class ShiftDetailFragment : Fragment(R.layout.fragment_shift_detail), CustomFragment {

    override fun getSharedElement(): View {
        return toolbar_detailed_shift
    }
}