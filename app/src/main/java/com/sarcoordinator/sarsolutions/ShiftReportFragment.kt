package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_shift_report.*

/**
 * Fragment for volunteer shift report at the end of a shift
 */
class ShiftReportFragment : Fragment(R.layout.fragment_shift_report),
    AdapterView.OnItemSelectedListener {

    //todo: Remove dummy data
    val vehicleTypes = arrayOf("Ford Explorer", "Snowmobile", "ATV")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSpinners()
        initViewListeners()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent != null) {
            var selection = parent.getItemAtPosition(position)
        }
    }

    private fun initViewListeners() {
        countyVehicleCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) personalVehicleCheckBox.isChecked = !isChecked
        }
        countyVehicle2CheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) personalVehicle2CheckBox.isChecked = !isChecked
        }
        countyVehicle3CheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) personalVehicle3CheckBox.isChecked = !isChecked
        }

        //Only one type of vehicle checked at a time
        personalVehicleCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) countyVehicleCheckBox.isChecked = !isChecked
        }
        personalVehicle2CheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) countyVehicle2CheckBox.isChecked = !isChecked
        }
        personalVehicle3CheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) countyVehicle3CheckBox.isChecked = !isChecked
        }

        endShiftButton.setOnClickListener {
            findNavController().navigate(ShiftReportFragmentDirections.actionShiftReportFragmentToCasesFragment())
        }
    }

    private fun initSpinners(){
        spinner?.adapter = this.context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                resources.getStringArray(R.array.vehicle_array)
            )
        }
        spinner.onItemSelectedListener = this

        spinner2?.adapter = this.context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                resources.getStringArray(R.array.vehicle_array)
            )
        }
        spinner2.onItemSelectedListener = this

        spinner3?.adapter = this.context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                resources.getStringArray(R.array.vehicle_array)
            )
        }
        spinner3.onItemSelectedListener = this
    }
}