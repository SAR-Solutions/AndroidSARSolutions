package com.sarcoordinator.sarsolutions.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.SharedViewModel
import kotlinx.android.synthetic.main.vehicle_material_card.view.*
import timber.log.Timber


/*
 * Vehicle Adapter
 */
class VehiclesAdapter(private val viewModel: SharedViewModel) :
    RecyclerView.Adapter<VehiclesAdapter.Viewholder>() {

    private var data = ArrayList<SharedViewModel.VehicleCardContent>()

    init {
        data = viewModel.vehicleList
        Timber.d("INITED VehicleAdapter: $this")
    }

    class Viewholder(itemView: View, private val viewModel: SharedViewModel) :
        RecyclerView.ViewHolder(itemView) {

        init {
            // Listeners
            itemView.county_vehicle_check_box.setOnCheckedChangeListener { _, isChecked ->
                val vehicleCardContent = viewModel.vehicleList[adapterPosition]
                vehicleCardContent.isCountyVehicle = isChecked
                viewModel.updateVehicleAtPosition(adapterPosition, vehicleCardContent)
            }

            itemView.personal_vehicle_check_box.setOnCheckedChangeListener { _, isChecked ->
                val vehicleCardContent = viewModel.vehicleList[adapterPosition]
                vehicleCardContent.isPersonalVehicle = isChecked
                viewModel.updateVehicleAtPosition(adapterPosition, vehicleCardContent)
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

        return Viewholder(
            view,
            viewModel
        )
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.bindView(data[position])
    }

    override fun getItemCount() = data.size

}