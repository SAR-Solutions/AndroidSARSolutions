package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.sarcoordinator.sarsolutions.adapters.ImagesAdapter
import com.sarcoordinator.sarsolutions.adapters.VehiclesAdapter
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_shift_report.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

/**
 * Fragment for volunteer shift report at the end of a shift
 */
class ShiftReportFragment : Fragment(R.layout.fragment_shift_report), ISharedElementFragment {

    private val nav: Navigation = Navigation.getInstance()

    companion object ArgsTags {
        const val SHIFT_ID = "SHIFT_ID"
    }

    private lateinit var shiftId: String
    private lateinit var viewModel: SharedViewModel
    private var viewManager: RecyclerView.LayoutManager? = null
    private var viewAdapter: VehiclesAdapter? = null
    private var imagesViewManager: RecyclerView.LayoutManager? = null
    private var imagesViewAdapter: ImagesAdapter? = null

    // Detect swipe
    private val swipeHelper = ItemTouchHelper(object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            Snackbar.make(
                requireView(),
                "Removed ${viewModel.vehicleList[viewHolder.adapterPosition].name}",
                Snackbar.LENGTH_LONG
            ).show()
            viewModel.vehicleList.removeAt(viewHolder.adapterPosition)
            viewAdapter?.notifyItemRemoved(viewHolder.adapterPosition)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        shiftId = arguments?.getString(SHIFT_ID)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNetworkExceptions()

        setupVehicleRecyclerView()
        setupImagesCardView()
        initViewListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewManager = null
        viewAdapter = null
        imagesViewManager = null
        imagesViewAdapter = null
    }

    private fun hideVehicleSection() {
        vehicle_heading_text.visibility = View.GONE
        vehicle_recycler_view.visibility = View.GONE
    }

    private fun showVehicleSection() {
        vehicle_heading_text.visibility = View.VISIBLE
        vehicle_recycler_view.visibility = View.VISIBLE
    }

    private fun setupImagesCardView() {
        // Don't inflate images card view if nothing to show
        if (viewModel.getImageList().value!!.isNullOrEmpty()) {
            images_taken_card.visibility = View.GONE
            return
        }
        imagesViewManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        imagesViewAdapter =
            ImagesAdapter(
                nav,
                viewModel.getImageList().value!!
            )
        images_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = imagesViewManager
            adapter = imagesViewAdapter
        }
    }

    private fun setupVehicleRecyclerView() {
        viewManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        viewAdapter =
            VehiclesAdapter(viewModel)
        vehicle_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        swipeHelper.attachToRecyclerView(vehicle_recycler_view)

        if (viewModel.vehicleList.isNotEmpty())
            viewAdapter?.notifyDataSetChanged()
        else
            hideVehicleSection()
    }

    private fun addVehicle() {
        viewModel.addVehicle()
        if (!vehicle_heading_text.isVisible)
            showVehicleSection()
        viewAdapter?.notifyDataSetChanged()
    }

    private fun initViewListeners() {
//        endShiftButton.setOnClickListener {
//            endShiftButton.requestFocus()
//            endShiftButton.isEnabled = false
//
//            GlobalUtil.hideKeyboard(requireActivity())
//
//            // Validate form input
//            if (shift_hours_edit_text.text.isNullOrEmpty() || !areVehicleFormsValid()) {
//                endShiftButton.isEnabled = true
//                Snackbar.make(
//                    requireView(),
//                    "Complete shift report to proceed",
//                    Snackbar.LENGTH_LONG
//                ).show()
//            } else {
//                progress_bar.visibility = View.VISIBLE
//                shift_report_fab.hide()
//
//                // Only submit shift if internet connectivity is available
//                if (GlobalUtil.isNetworkConnectivityAvailable(
//                        requireActivity(),
//                        requireView(),
//                        false
//                    )
//                ) {
//                    viewModel.submitShiftReport(
//                        shiftId,
//                        shift_hours_edit_text.text.toString(),
//                        resources.getStringArray(R.array.vehicle_array).toList()
//                    ).invokeOnCompletion {
//                        CoroutineScope(Main).launch {
//                            delay(10000)
//                            viewModel.completeShiftReportSubmission()
//                            nav.popFragmentClearBackStack(CasesTabFragment())
//                        }
//                    }
//                } else {
//                    viewModel.addShiftReportToCache(
//                        shift_hours_edit_text.text.toString(),
//                        shiftId
//                    ).invokeOnCompletion {
//                        CoroutineScope(Main).launch {
//                            viewModel.completeShiftReportSubmission()
//
//                            nav.popFragmentClearBackStack(CasesTabFragment())
//
//                            Snackbar.make(
//                                requireView(),
//                                "No internet connection, cached shift report",
//                                Snackbar.LENGTH_LONG
//                            ).show()
//                        }
//                    }
//                }
//            }
//        }

        shift_report_fab.setOnClickListener {
            shift_report_fab.requestFocus()
            addVehicle()
        }
    }

    // True if all vehicles have required data set, false otherwise
    private fun areVehicleFormsValid(): Boolean {
        viewModel.vehicleList.forEach { vehicle ->
            if (vehicle.milesTraveled.isNullOrBlank())
                return false
        }
        return true
    }

    private fun observeNetworkExceptions() {
        viewModel.getNetworkExceptionObservable().observe(viewLifecycleOwner, Observer { error ->
            if (error != null && error.isNotEmpty())
                viewModel.clearNetworkExceptions()
            Snackbar.make(
                requireActivity().parent_layout,
                "Failed network call. Saving report to cache.",
                Snackbar.LENGTH_LONG
            ).show()

            viewModel.addShiftReportToCache(
                shift_hours_edit_text.text.toString(),
                shiftId
            ).invokeOnCompletion {
                CoroutineScope(Main).launch {
                    viewModel.completeShiftReportSubmission()
                    nav.popFragmentClearBackStack(CasesTabFragment())
                }
            }
        })
    }

    override fun getSharedElement(): View? {
        return toolbar_shift_report
    }

}
