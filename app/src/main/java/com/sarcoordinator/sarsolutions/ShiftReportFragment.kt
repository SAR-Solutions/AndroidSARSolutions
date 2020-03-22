package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_shift_report.*
import kotlinx.android.synthetic.main.vehicle_material_card.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: Adapter

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
                "SWIPE: Removed ${viewModel.vehicleList[viewHolder.adapterPosition].name}",
                Snackbar.LENGTH_LONG
            ).show()
            viewModel.vehicleList.removeAt(viewHolder.adapterPosition)
            viewAdapter.removeVehicleAt(viewHolder.adapterPosition)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.numberOfVehicles = 0
        shiftId = arguments?.getString(SHIFT_ID)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNetworkExceptions()

        setupRecyclerView()
        initViewListeners()
        if (viewModel.vehicleList.isNotEmpty())
            viewAdapter.setData(viewModel.vehicleList)
        else
            hideVehicleSection()
    }

    private fun hideVehicleSection() {
        vehicle_heading_text.visibility = View.GONE
        vehicle_recycler_view.visibility = View.GONE
    }

    private fun showVehicleSection() {
        vehicle_heading_text.visibility = View.VISIBLE
        vehicle_recycler_view.visibility = View.VISIBLE
    }


    private fun addVehicle() {
        viewModel.addVehicle()
        if (!vehicle_heading_text.isVisible)
            showVehicleSection()
        viewAdapter.add(viewModel.vehicleList.last())
    }

    private fun initViewListeners() {
        endShiftButton.setOnClickListener {
            endShiftButton.isEnabled = false

            GlobalUtil.hideKeyboard(requireActivity())

            // Validate form input
            if (shiftHoursEditText.text.isNullOrEmpty() || !areVehicleFormsValid()) {
                endShiftButton.isEnabled = true
                Snackbar.make(
                    requireView(),
                    "Complete shift report to proceed",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                progress_bar.visibility = View.VISIBLE
                shift_report_fab.hide()
                // Only submit shift if internet connectivity is available
                if (GlobalUtil.isNetworkConnectivityAvailable(
                        requireActivity(),
                        requireView(),
                        false
                    )
                ) {
                    viewModel.submitShiftReport(
                        shiftId,
                        shiftHoursEditText.text.toString(),
                        resources.getStringArray(R.array.vehicle_array).toList()
                    ).invokeOnCompletion {
                        CoroutineScope(Main).launch {
                            viewModel.completeShiftReportSubmission()

                            nav.popFragmentClearBackStack(CasesTabFragment())
                        }
                    }
                } else {
                    viewModel.addShiftReportToCache(
                        shiftHoursEditText.text.toString(),
                        shiftId
                    ).invokeOnCompletion {
                        CoroutineScope(Main).launch {
                            viewModel.completeShiftReportSubmission()

                            nav.popFragmentClearBackStack(CasesTabFragment())

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

        shift_report_fab.setOnClickListener {
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

    private fun setupRecyclerView() {
        viewManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        viewAdapter = Adapter(viewModel)
        vehicle_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        swipeHelper.attachToRecyclerView(vehicle_recycler_view)
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
                shiftHoursEditText.text.toString(),
                shiftId
            ).invokeOnCompletion {
                CoroutineScope(Main).launch {
                    viewModel.completeShiftReportSubmission()
                    nav.popFragmentClearBackStack(CasesTabFragment())
                }
            }
        })
    }

    override fun getSharedElements(): Array<View> {
        return arrayOf(toolbar_shift_report)
    }

    /*
     * Vehicle Adapter
     */
    class Adapter(private val viewModel: SharedViewModel) :
        RecyclerView.Adapter<Adapter.Viewholder>() {

        private var data = ArrayList<SharedViewModel.VehicleCardContent>()

        class Viewholder(itemView: View, private val viewModel: SharedViewModel) :
            RecyclerView.ViewHolder(itemView) {

            init {
                // Listeners
                itemView.county_vehicle_check_box.setOnCheckedChangeListener { _, isChecked ->
                    val vehicleCardContent = viewModel.vehicleList[adapterPosition]
                    vehicleCardContent.isCountyVehicle = isChecked
                    viewModel.updateVehicleAtPosition(position, vehicleCardContent)
                }

                itemView.personal_vehicle_check_box.setOnCheckedChangeListener { _, isChecked ->
                    val vehicleCardContent = viewModel.vehicleList[adapterPosition]
                    vehicleCardContent.isPersonalVehicle = isChecked
                    viewModel.updateVehicleAtPosition(position, vehicleCardContent)
                }

                itemView.vehicle_type_spinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            Timber.e("Nothing selected")
                        }

                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            pos: Int,
                            d: Long
                        ) {
                            val vehicleCardContent = viewModel.vehicleList[adapterPosition]
                            vehicleCardContent.vehicleType = pos
                            viewModel.updateVehicleAtPosition(position, vehicleCardContent)
                        }
                    }

                itemView.miles_traveled_edit_text.addTextChangedListener {
                    val vehicleCardContent = viewModel.vehicleList[adapterPosition]
                    vehicleCardContent.milesTraveled = it.toString()
                    viewModel.updateVehicleAtPosition(adapterPosition, vehicleCardContent)
                    Timber.d("Logged at ${adapterPosition}")
                }
            }

            fun bindView(
                vehicleCardContent: SharedViewModel.VehicleCardContent
            ) {

                // Init spinner options
                itemView.vehicle_type_spinner.adapter = itemView.context?.let {
                    ArrayAdapter(
                        it,
                        R.layout.support_simple_spinner_dropdown_item,
                        it.resources.getStringArray(R.array.vehicle_array)
                    )
                }

                // Set content
                itemView.vehicle_title_text_view.text = vehicleCardContent.name
                itemView.county_vehicle_check_box.isChecked = vehicleCardContent.isCountyVehicle
                itemView.personal_vehicle_check_box.isChecked = vehicleCardContent.isPersonalVehicle
                itemView.miles_traveled_edit_text.setText(vehicleCardContent.milesTraveled)
                itemView.vehicle_type_spinner.setSelection(vehicleCardContent.vehicleType)


            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.vehicle_material_card, parent, false)

            return Viewholder(view, viewModel)
        }


        override fun onBindViewHolder(holder: Viewholder, position: Int) {
            holder.bindView(data[position])
        }

        override fun getItemCount() = data.size

        fun add(vehicleCardContent: SharedViewModel.VehicleCardContent) {
            data.add(vehicleCardContent)
            notifyDataSetChanged()
//            notifyItemInserted(data.size)
        }

        fun setData(list: ArrayList<SharedViewModel.VehicleCardContent>) {
            data = list
            notifyDataSetChanged()
        }

        fun removeVehicleAt(position: Int) {
            data.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
