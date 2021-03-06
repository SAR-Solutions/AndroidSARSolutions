package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialFadeThrough
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.util.CustomFragment
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.fragment_cases.*
import kotlinx.android.synthetic.main.list_view_item.view.*
import timber.log.Timber

/**
 * This fragment displays the list of cases for the user
 */
class CasesTabFragment : Fragment(R.layout.fragment_cases), CustomFragment {

    private val nav: Navigation by lazy { Navigation.getInstance() }

    private lateinit var viewModel: SharedViewModel
    private var viewManager: RecyclerView.LayoutManager? = null
    private var viewAdapter: Adapter? = null

    override fun getSharedElement(): View = toolbar_cases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        no_cases_found_view.visibility = View.GONE

        nav.hideBottomNavBar?.let { it(false) }
        (requireActivity() as MainActivity).enableTransparentStatusBar(false)

        // Adjust refresh layout progress view offset depending on toolbar height
        toolbar_cases.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                toolbar_cases?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                swipe_refresh_layout.setProgressViewOffset(
                    false,
                    toolbar_cases.height - 150,
                    toolbar_cases.height + 100
                )
            }
        })

        observeNetworkErrors()
        setupRecyclerView()
        observeCases()

        if (viewModel.getCases().value.isNullOrEmpty())
            refreshCaseList()
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
            if (caseList.size == 0) {
                no_cases_found_view.visibility = View.VISIBLE
                list_shimmer_layout.visibility = View.GONE
                if (swipe_refresh_layout.isRefreshing)
                    swipe_refresh_layout.isRefreshing = false
            } else {
                no_cases_found_view.visibility = View.GONE
                if (list_shimmer_layout.visibility != View.GONE)
                    list_shimmer_layout.visibility = View.GONE
                if (swipe_refresh_layout.isRefreshing)
                    swipe_refresh_layout.isRefreshing = false
                viewAdapter?.setCaseList(caseList)
            }
        })
    }

    // Sets up recyclerview and refresh layout
    private fun setupRecyclerView() {

        toolbar_cases.attachRecyclerView(cases_recycler_view)

        viewManager = LinearLayoutManager(context)
        viewAdapter = Adapter(nav, this)
        cases_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
            setHasFixedSize(true)
        }

        val primaryColor = TypedValue()
        val backgroundColor = TypedValue()
        requireActivity().theme.resolveAttribute(R.attr.colorPrimary, primaryColor, true)
        requireActivity().theme.resolveAttribute(R.attr.colorOnPrimary, backgroundColor, true)

        swipe_refresh_layout.setColorSchemeColors(primaryColor.data)
        swipe_refresh_layout.setProgressBackgroundColorSchemeColor(backgroundColor.data)

        swipe_refresh_layout.setOnRefreshListener {
            refreshCaseList()
        }
    }

    private fun observeNetworkErrors() {
        viewModel.getNetworkExceptionObservable().observe(viewLifecycleOwner, Observer { error ->
            if (error != null && error.isNotEmpty()) {
                Timber.e("Network error: $error")
                viewModel.clearNetworkExceptions()
                Toast.makeText(requireContext(), "Internet connection error", Toast.LENGTH_LONG)
                    .show()
                disableRecyclerView(true)
            }
        })
    }

    private fun refreshCaseList() {
        if (validateNetworkConnectivity()) {
            no_cases_found_view.visibility = View.GONE
            viewAdapter = Adapter(nav, this)
            cases_recycler_view.adapter = viewAdapter
            viewModel.refreshCases()
        }
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).restoreSystemBars()
        nav.hideBottomNavBar?.let { it(false) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewManager = null
        viewAdapter = null
    }

    /** Recycler view item stuff **/
    class Adapter(private val nav: Navigation, private val parent: CasesTabFragment) :
        RecyclerView.Adapter<Adapter.ViewHolder>() {
        private var data = ArrayList<Case>()

        class ViewHolder(
            itemView: View,
            private val nav: Navigation,
            private val parent: CasesTabFragment
        ) : RecyclerView.ViewHolder(itemView) {
            fun bindView(case: Case) {
                itemView.missing_person_text.text =
                    case.missingPersonName.toString().removeSurrounding("[", "]")
                itemView.person_avatar_view.setText(case.missingPersonName[0])
                itemView.case_name_text_view.text =
                    case.caseName
                itemView.date.text = GlobalUtil.convertEpochToDate(case.date)
                itemView.setOnClickListener {
                    val trackFragment = TrackFragment()
                    trackFragment.arguments = Bundle().apply {
                        putString(TrackFragment.CASE_ID, case.id)
                    }

                    parent.exitTransition = MaterialFadeThrough.create(parent.requireContext())

                    nav.pushFragment(
                        trackFragment,
                        Navigation.TabIdentifiers.HOME,
                        parent.getSharedElement()
                    )
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            // Buffer to make space for toolbar
            if (position == 0) {
                return 0
            }
            // Header to show county name
            if (position == 1) {
                return 1
            }
            return 2
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            var holder: View = View(parent.context)

            when (viewType) {
                0 -> {
                    holder =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.rv_buffer, parent, false)
                }
                1 -> {
                    holder =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.rv_header, parent, false)
                }
                2 -> {
                    holder =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.list_view_item, parent, false)
                }
            }
            return ViewHolder(holder, nav, this.parent)
        }

        override fun getItemCount(): Int {
            // Always return size + 1 for buffer; Only return size + 2 if data was found
            return if (data.isNullOrEmpty())
                data.size + 1
            else
                data.size + 2
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Timber.d("Binding at position: $position for list size: ${data.size}")
            if (position == 0 || position == 1)
                return
            holder.bindView(data[position - 2])
        }

        fun setCaseList(list: ArrayList<Case>) {
            data = list
            notifyDataSetChanged()
        }
    }
}
