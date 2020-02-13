package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sarcoordinator.sarsolutions.models.RoomLocation
import kotlinx.android.synthetic.main.fragment_failed_shifts.*
import kotlinx.android.synthetic.main.loc_cache_list_item.view.*

class FailedShiftsFragment : Fragment(R.layout.fragment_failed_shifts) {

    private lateinit var viewModel: SharedViewModel
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: LocationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.failed_shfits)

        setUpRecyclerView()

        viewModel.getAllLocationCaseIdsFromCache().observe(viewLifecycleOwner, Observer {
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
        viewAdapter = LocationAdapter(viewModel, progress_bar)
        failed_shifts_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    class LocationAdapter(
        private val viewModel: SharedViewModel,
        private val progressBar: ProgressBar
    ) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {
        private var data = ArrayList<RoomLocation>()

        class LocationViewHolder(
            itemView: View,
            private val viewModel: SharedViewModel,
            private val progressBar: ProgressBar
        ) : RecyclerView.ViewHolder(itemView) {
            fun bindView(cachedObj: RoomLocation) {
                itemView.case_name.text = cachedObj.caseName
                itemView.cache_time.text = cachedObj.cacheTime

                itemView.setOnClickListener {
                    viewModel.postLocations(cachedObj.shiftId)
                        .invokeOnCompletion {
                            progressBar.visibility = View.GONE
                        }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            val holder = LayoutInflater.from(parent.context)
                .inflate(R.layout.loc_cache_list_item, parent, false)
            return LocationViewHolder(holder, viewModel, progressBar)
        }

        override fun getItemCount(): Int = data.size


        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            holder.bindView(data[position])
        }

        fun setList(list: List<RoomLocation>) {
            data = ArrayList(list)
            notifyDataSetChanged()
        }

    }
}