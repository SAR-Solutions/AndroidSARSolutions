package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_failed_shifts.*

class FailedShiftsFragment : Fragment(R.layout.fragment_failed_shifts) {

    private lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.failed_shfits)

        viewModel.getAllLocationCaseIdsFromCache().observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                no_shifts_to_sync_view.visibility = View.VISIBLE
                failed_shifts_recycler_view.visibility = View.GONE
            } else {
                no_shifts_to_sync_view.visibility = View.GONE
                failed_shifts_recycler_view.visibility = View.VISIBLE
            }
        })

    }
}