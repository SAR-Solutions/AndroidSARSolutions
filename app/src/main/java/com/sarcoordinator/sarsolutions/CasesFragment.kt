package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import kotlinx.android.synthetic.main.fragment_cases.*
import kotlinx.android.synthetic.main.list_view_item.view.*

/**
 * This fragment displays the list of cases for the user
 */
class CasesFragment : Fragment(R.layout.fragment_cases) {

    private lateinit var viewModel: SharedViewModel
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust refresh layout progress view offset depending on toolbar height
        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object:
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                swipe_refresh_layout.setProgressViewOffset(false, toolbar.height - 150, toolbar.height + 100)
            }
        })

        // If service is ongoing, restore state
        //TODO:NAV
        if (viewModel.isShiftActive.value == true) {
            Toast.makeText(context, "TODO:Implement restore state", Toast.LENGTH_LONG).show()
        }

        observeNetworkErrors()
        setupRecyclerView()
        observeCases()

        if (viewModel.getCases().value.isNullOrEmpty()) {
            refreshCaseList()
        }
    }

    // Disables or enables recyclerview depending on network connectivity status
    private fun validateNetworkConnectivity(): Boolean {
        return if (GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), requireView())) {
            disableRecyclerView(false)
            true
        } else {
            disableRecyclerView(true)
            false
        }
    }

    // Disabled recyclerview and shows try again button
// Note: actually disables swipe refresh layout
    private fun disableRecyclerView(toDisable: Boolean) {
        if (toDisable) {
            swipe_refresh_layout.visibility = View.GONE
            list_shimmer_layout.visibility = View.GONE
            try_again_network_button.visibility = View.VISIBLE
            try_again_network_button.setOnClickListener {
                refreshCaseList()
            }
        } else {
            swipe_refresh_layout.visibility = View.VISIBLE
            list_shimmer_layout.visibility = View.VISIBLE
            try_again_network_button.visibility = View.GONE
        }
    }

    // Makes new network call and observes for case list
    private fun observeCases() {
        viewModel.getCases().observe(viewLifecycleOwner, Observer<ArrayList<Case>> { caseList ->
            if (list_shimmer_layout.visibility != View.GONE)
                list_shimmer_layout.visibility = View.GONE
            if (swipe_refresh_layout.isRefreshing)
                swipe_refresh_layout.isRefreshing = false
            viewAdapter.setCaseList(caseList)
        })
    }

    // Sets up recyclerview and refresh layout
    private fun setupRecyclerView() {
        viewManager = LinearLayoutManager(context)
        viewAdapter = Adapter()
        cases_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        swipe_refresh_layout.setColorSchemeColors(
            ContextCompat.getColor(
                requireContext(),
                R.color.newRed
            )
        )
        swipe_refresh_layout.setOnRefreshListener {
            refreshCaseList()
        }
    }

    private fun observeNetworkErrors() {
        viewModel.getNetworkExceptionObservable().observe(viewLifecycleOwner, Observer { error ->
            if (error != null && error.isNotEmpty()) {
                viewModel.clearNetworkExceptions()
                Toast.makeText(requireContext(), "Internet connection error", Toast.LENGTH_LONG)
                    .show()
                disableRecyclerView(true)
            }
        })
    }

    private fun refreshCaseList() {
        if (validateNetworkConnectivity()) {
            viewAdapter = Adapter()
            cases_recycler_view.adapter = viewAdapter
            viewModel.refreshCases()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cases_recycler_view.adapter = null
    }

    /** Recycler view item stuff **/
    class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        private var data = ArrayList<Case>()

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bindView(case: Case) {
                itemView.missing_person_text.text =
                    case.missingPersonName.toString().removeSurrounding("[", "]")
                itemView.person_avatar_view.setText(case.missingPersonName[0])
                itemView.date.text = GlobalUtil.convertEpochToDate(case.date)
                itemView.setOnClickListener {
                    Toast.makeText(it.context, "TODO:Implement navigation", Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0) {
                return 0
            }
//            return super.getItemViewType(position)
            return 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            var holder: View = View(parent.context)

            if (viewType == 0) {
                holder =
                    LayoutInflater.from(parent.context).inflate(R.layout.rv_header, parent, false)
            } else if (viewType == 1) {
                holder =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_view_item, parent, false)
            }
            return ViewHolder(holder)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position != 0)
                holder.bindView(data[position])
        }

        fun setCaseList(list: ArrayList<Case>) {
            data = list
            notifyDataSetChanged()
        }
    }
}
