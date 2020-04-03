package com.sarcoordinator.sarsolutions.adapters

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.SharedViewModel
import com.sarcoordinator.sarsolutions.ShiftDetailFragment
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.loc_cache_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CachedShiftAdapter(
    private val viewModel: SharedViewModel,
    private val progressBar: ProgressBar,
    private val vehicleTypeArray: List<String>,
    private val activity: Activity,
    private val nav: Navigation
) : RecyclerView.Adapter<CachedShiftAdapter.LocationViewHolder>() {
    private var data = ArrayList<LocationsInShiftReport>()

    class LocationViewHolder(
        itemView: View,
        private val viewModel: SharedViewModel,
        private val progressBar: ProgressBar,
        private val vehicleTypeArray: List<String>,
        private val activity: Activity,
        private val nav: Navigation
    ) : RecyclerView.ViewHolder(itemView) {
        fun bindView(cachedObj: LocationsInShiftReport) {
            itemView.case_name.text = cachedObj.shiftReport.caseName
            itemView.cache_time.text = cachedObj.shiftReport.cacheTime

//            itemView.setOnClickListener {
//                if(GlobalUtil.isNetworkConnectivityAvailable(activity, itemView)) {
//                    progressBar.visibility = View.VISIBLE
//                    viewModel.submitShiftReportFromCache(cachedObj, vehicleTypeArray)
//                        .invokeOnCompletion {
//                            CoroutineScope(Dispatchers.Main).launch {
//                                if (viewModel.numberOfSyncsInProgress == 0)
//                                    progressBar.visibility = View.GONE
//                            }
//                        }
//                }
//            }

            itemView.setOnClickListener {
                val shiftDetailFragment = ShiftDetailFragment()
                shiftDetailFragment.arguments = Bundle().apply {
                    putSerializable(ShiftDetailFragment.CACHED_SHIFT, cachedObj)
                }
                nav.pushFragment(shiftDetailFragment)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.loc_cache_list_item, parent, false)
        return LocationViewHolder(view, viewModel, progressBar, vehicleTypeArray, activity, nav)
    }

    override fun getItemCount(): Int = data.size


    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bindView(data[position])
    }

    fun setList(list: List<LocationsInShiftReport>) {
        data = ArrayList(list)
        notifyDataSetChanged()
    }
}