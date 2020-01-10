package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_shift_report.*

/**
 * Fragment for volunteer shift report at the end of a shift
 */
class ShiftReportFragment : Fragment(), AdapterView.OnItemSelectedListener {

    //todo: Remove dummy data
    val vehicleTypes = arrayOf("Ford Explorer", "Snowmobile", "ATV")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shift_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinner?.adapter = this.context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                resources.getStringArray(R.array.vehicle_array)
            )
        }
        spinner.onItemSelectedListener = this

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
            if (isChecked) {
                spinner.visibility = View.VISIBLE
                personalVehicleCheckBox.isEnabled = false
            } else {
                spinner.visibility = View.GONE
                personalVehicleCheckBox.isEnabled = true
            }
        }

        personalVehicleCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                spinner.visibility = View.VISIBLE
                countyVehicleCheckBox.isEnabled = false
            } else {
                spinner.visibility = View.GONE
                countyVehicleCheckBox.isEnabled = true
            }
        }

        endShiftButton.setOnClickListener {
            findNavController().navigate(ShiftReportFragmentDirections.actionShiftReportFragmentToCasesFragment())
        }
    }
}