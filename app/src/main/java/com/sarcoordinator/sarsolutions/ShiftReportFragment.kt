package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.sarcoordinator.sarsolutions.adapters.ImagesAdapter
import com.sarcoordinator.sarsolutions.adapters.VehiclesAdapter
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.applyAllInsets
import kotlinx.android.synthetic.main.card_images.*
import kotlinx.android.synthetic.main.card_images.view.*
import kotlinx.android.synthetic.main.fragment_shift_report_modern.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

/**
 * Fragment for volunteer shift report at the end of a shift
 */
class ShiftReportFragment : Fragment(R.layout.fragment_shift_report_modern) {

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
    private lateinit var bottomSheet: BottomSheetBehavior<MaterialCardView>

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
            Toast.makeText(
                requireContext(),
                "Removed ${viewModel.vehicleList[viewHolder.adapterPosition].name}",
                Toast.LENGTH_LONG
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
        shiftId = viewModel.currentShiftId ?: savedInstanceState?.getString(SHIFT_ID)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.bottom_sheet))

        // Set insets
        shift_report_parent_layout.applyAllInsets()
//        shift_report_parent_layout.children.forEach { child ->
//            child.doOnApplyWindowInsets { childView, insets, initialState ->
//                if (childView.id != R.id.bottom_sheet) {
//                    childView.setMargins(
//                        initialState.margins.left + insets.systemGestureInsets.left,
//                        initialState.margins.top + insets.systemGestureInsets.top,
//                        initialState.margins.right + insets.systemGestureInsets.right,
//                        initialState.margins.bottom + insets.systemGestureInsets.bottom
//                    )
//                }
//            }
//        }

        observeNetworkExceptions()
        setupVehicleRecyclerView()
        setupImagesCardView()
        initViewListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SHIFT_ID, shiftId)
    }

    override fun onStart() {
        super.onStart()
        nav.hideBottomNavBar?.let { it(true) }
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewManager = null
        viewAdapter = null
        imagesViewManager = null
        imagesViewAdapter = null
    }

    private fun setupImagesCardView() {
        // Don't inflate images card view if nothing to show
        if (viewModel.getImageList().value!!.isNullOrEmpty()) {
            bottom_sheet.images_recycler_view.visibility = View.GONE
            no_images_view.visibility = View.VISIBLE
            return
        }
        imagesViewManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        imagesViewAdapter =
            ImagesAdapter(
                nav,
                viewModel.getImageList().value!!
            )
        bottom_sheet.images_recycler_view.apply {
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
    }

    private fun addVehicle() {
        viewModel.addVehicle()
        viewAdapter?.notifyDataSetChanged()
    }

    private fun initViewListeners() {
        submit_shift_report_fab.setOnClickListener {
            submit_shift_report_fab.requestFocus()
            submit_shift_report_fab.isClickable = false

            GlobalUtil.hideKeyboard(requireActivity())

            // Validate form input
            if (shift_hours_edit_text.text.isNullOrEmpty() || !areVehicleFormsValid()) {
                submit_shift_report_fab.isClickable = true
                Toast.makeText(
                    requireContext(),
                    "Complete shift report to proceed",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                progress_bar.visibility = View.VISIBLE
                add_vehicle_button.isEnabled = false

                // Only submit shift if internet connectivity is available
                if (GlobalUtil.isNetworkConnectivityAvailable(
                        requireActivity(),
                        requireView(),
                        false
                    )
                ) {
                    viewModel.submitShiftReport(
                        shiftId,
                        shift_hours_edit_text.text.toString(),
                        resources.getStringArray(R.array.vehicle_array).toList()
                    ).invokeOnCompletion {
                        CoroutineScope(Main).launch {
                            viewModel.completeShiftReportSubmission()
                            nav.clearBackstack()
                            nav.selectTab(Navigation.TabIdentifiers.HOME)
                        }
                    }
                } else {
                    viewModel.addShiftReportToCache(
                        shift_hours_edit_text.text.toString()
                    ).invokeOnCompletion {
                        CoroutineScope(Main).launch {
                            viewModel.completeShiftReportSubmission()

                            nav.clearBackstack()
                            nav.selectTab(Navigation.TabIdentifiers.HOME)

                            Snackbar.make(
                                requireView(),
                                "No internet connection, cached shift report",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        add_vehicle_button.setOnClickListener {
            add_vehicle_button.requestFocus()
            addVehicle()
            requireView().requestLayout()
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
            Toast.makeText(
                requireContext(),
                "Failed network call. Saving report to cache.",
                Toast.LENGTH_LONG
            ).show()

            viewModel.addShiftReportToCache(
                shift_hours_edit_text.text.toString()
            ).invokeOnCompletion {
                CoroutineScope(Main).launch {
                    viewModel.completeShiftReportSubmission()
                    nav.clearBackstack()
                    nav.selectTab(Navigation.TabIdentifiers.HOME)
                }
            }
        })
    }
}
