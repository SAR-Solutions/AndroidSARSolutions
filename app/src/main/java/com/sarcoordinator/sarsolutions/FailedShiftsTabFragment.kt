package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import kotlinx.android.synthetic.main.fragment_failed_shifts.*
import kotlinx.android.synthetic.main.loc_cache_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.io.Serializable

class FailedShiftsTabFragment : Fragment(R.layout.fragment_failed_shifts), ISharedElementFragment, Serializable {

    private lateinit var viewModel: SharedViewModel
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: LocationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
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
        viewAdapter = LocationAdapter(
            viewModel,
            progress_bar,
            resources.getStringArray(R.array.vehicle_array).toList()
        )

        failed_shifts_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    class LocationAdapter(
        private val viewModel: SharedViewModel,
        private val progressBar: ProgressBar,
        private val vehicleTypeArray: List<String>
    ) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {
        private var data = ArrayList<LocationsInShiftReport>()

        class LocationViewHolder(
            itemView: View,
            private val viewModel: SharedViewModel,
            private val progressBar: ProgressBar,
            private val vehicleTypeArray: List<String>
        ) : RecyclerView.ViewHolder(itemView) {
            fun bindView(cachedObj: LocationsInShiftReport) {
                itemView.case_name.text = cachedObj.shiftReport.caseName
                itemView.cache_time.text = cachedObj.shiftReport.cacheTime

                itemView.setOnClickListener {
                    progressBar.visibility = View.VISIBLE
                    viewModel.submitShiftReportFromCache(cachedObj, vehicleTypeArray)
                        .invokeOnCompletion {
                            CoroutineScope(Main).launch {
                                if (viewModel.numberOfSyncsInProgress == 0)
                                    progressBar.visibility = View.GONE
                            }
                        }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.loc_cache_list_item, parent, false)
            return LocationViewHolder(view, viewModel, progressBar, vehicleTypeArray)
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

    override fun getSharedElement(): View? {
        return toolbar_failed_shifts
    }
}