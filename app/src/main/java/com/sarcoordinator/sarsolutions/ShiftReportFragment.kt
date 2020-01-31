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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_shift_report.*
import kotlinx.android.synthetic.main.vehicle_material_card.view.*
import timber.log.Timber

/**
 * Fragment for volunteer shift report at the end of a shift
 */
class ShiftReportFragment : Fragment(R.layout.fragment_shift_report) {

    private lateinit var viewModel: ShiftReportViewModel
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ShiftReportViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initViewListeners()
        if (viewModel.vehicleList.isNotEmpty())
            viewAdapter.addAll(viewModel.vehicleList)
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
        viewAdapter.addItem(viewModel.vehicleList.last())
    }

    private fun initViewListeners() {
        endShiftButton.setOnClickListener {
            findNavController().navigate(ShiftReportFragmentDirections.actionShiftReportFragmentToCasesFragment())
        }

        shift_report_fab.setOnClickListener {
            addVehicle()
        }
    }

    private fun setupRecyclerView() {
        viewManager = LinearLayoutManager(context)
        viewAdapter = Adapter(viewModel)
        vehicle_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    class Adapter(val viewModel: ShiftReportViewModel) :
        RecyclerView.Adapter<Adapter.Viewholder>() {

        private var data = ArrayList<ShiftReportViewModel.VehicleCardContent>()

        class Viewholder(itemView: View, private val viewModel: ShiftReportViewModel) :
            RecyclerView.ViewHolder(itemView) {

            fun bindView(
                vehicleCardContent: ShiftReportViewModel.VehicleCardContent,
                position: Int
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

                // Listeners
                itemView.county_vehicle_check_box.setOnCheckedChangeListener { _, isChecked ->
                    vehicleCardContent.isCountyVehicle = isChecked
                    viewModel.updateAtPosition(position, vehicleCardContent)
                }

                itemView.personal_vehicle_check_box.setOnCheckedChangeListener { _, isChecked ->
                    vehicleCardContent.isPersonalVehicle = isChecked
                    viewModel.updateAtPosition(position, vehicleCardContent)
                }

                itemView.miles_traveled_edit_text.addTextChangedListener {
                    vehicleCardContent.milesTraveled = it.toString()
                    viewModel.updateAtPosition(position, vehicleCardContent)
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
                            vehicleCardContent.vehicleType = pos
                            viewModel.updateAtPosition(position, vehicleCardContent)
                        }
                    }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
            val holder = LayoutInflater.from(parent.context)
                .inflate(R.layout.vehicle_material_card, parent, false)
            return Viewholder(holder, viewModel)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: Viewholder, position: Int) {
            holder.bindView(data[position], position)
        }

        fun addItem(item: ShiftReportViewModel.VehicleCardContent) {
            val position = data.size
            data.add(item)
            notifyItemInserted(position)
        }

        fun addAll(list: ArrayList<ShiftReportViewModel.VehicleCardContent>) {
            data.addAll(list)
            notifyDataSetChanged()
        }
    }
}