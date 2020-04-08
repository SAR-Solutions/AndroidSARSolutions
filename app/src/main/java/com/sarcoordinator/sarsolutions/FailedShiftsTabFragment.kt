package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sarcoordinator.sarsolutions.adapters.CachedShiftAdapter
import com.sarcoordinator.sarsolutions.util.CustomFragment
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.fragment_failed_shifts.*

class FailedShiftsTabFragment : Fragment(R.layout.fragment_failed_shifts), CustomFragment {

    private lateinit var viewModel: SharedViewModel
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: CachedShiftAdapter
    private val nav: Navigation by lazy { Navigation.getInstance() }

    override fun getSharedElement(): View = toolbar_failed_shifts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).restoreSystemBars()
        nav.hideBottomNavBar?.let { it(false) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()

        viewModel.getAllShiftReports().observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                no_shifts_to_sync_view.visibility = View.VISIBLE
                failed_shifts_recycler_view.visibility = View.GONE
            } else {
                no_shifts_to_sync_view.visibility = View.GONE
                failed_shifts_recycler_view.visibility = View.VISIBLE
                viewAdapter.setList(it)
            }
        })
    }

    private fun setUpRecyclerView() {
        viewManager = LinearLayoutManager(context)
        viewAdapter = CachedShiftAdapter(nav)

        failed_shifts_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }
}