package com.sarcoordinator.sarsolutions

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.models.Case
import kotlinx.android.synthetic.main.fragment_cases.*
import kotlinx.android.synthetic.main.list_view_item.view.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * This fragment displays the list of cases for the user
 */
class CasesFragment : Fragment() {

    private lateinit var viewModel: SharedViewModel
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: Adapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cases, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNetworkErrors()

        if (!viewModel.getCases().value.isNullOrEmpty()) {
            setupRecyclerView()
            // Observe cases takes care of removing shimmer layout
            observeCases()
        } else {
            // Process only if internet connection is available
            validateNetworkConnectivity()
        }
    }

    private fun validateNetworkConnectivity() {
        val cm =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            if (cm.activeNetworkInfo.isConnected) {
                disableRecyclerView(false)
            } else {
                disableRecyclerView(true)
                Snackbar.make(requireView(), "No network connection found", Snackbar.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            disableRecyclerView(true)
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
                validateNetworkConnectivity()
            }
        } else {
            swipe_refresh_layout.visibility = View.VISIBLE
            list_shimmer_layout.visibility = View.VISIBLE
            try_again_network_button.visibility = View.GONE

            setupRecyclerView()

            auth.currentUser!!.getIdToken(true).addOnSuccessListener {
                viewModel.mAuthToken = it.token!!
                observeCases()
                viewModel.refreshCases()
            }
        }
    }

    // Sets up recyclerview and refresh layout
    private fun setupRecyclerView() {
        viewManager = LinearLayoutManager(context)
        viewAdapter = Adapter()
        cases_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        swipe_refresh_layout.setColorSchemeColors(
            ContextCompat.getColor(
                requireContext(),
                R.color.orange
            )
        )
        swipe_refresh_layout.setOnRefreshListener {
            viewAdapter = Adapter()
            cases_recycler_view.adapter = viewAdapter
            viewModel.refreshCases()
        }
    }

    // Makes new network call and observes for case list
    private fun observeCases() {
        viewModel.getCases().observe(viewLifecycleOwner, Observer<ArrayList<Case>> { caseList ->
            if (list_shimmer_layout.visibility != View.GONE)
                list_shimmer_layout.visibility = View.GONE
            if (swipe_refresh_layout.isRefreshing)
                swipe_refresh_layout.isRefreshing = false
            viewAdapter.addCaseList(caseList)
        })
    }

    private fun observeNetworkErrors() {
        viewModel.getNetworkExceptionObservable().observe(viewLifecycleOwner, Observer { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(requireView(), "Error, pull down to try again", Snackbar.LENGTH_LONG)
                    .show()
                list_shimmer_layout.stopShimmer()
            }
        })
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
                itemView.date.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(Date(Timestamp(case.date * 1000).time))
                itemView.setOnClickListener {
                    itemView.findNavController()
                        .navigate(CasesFragmentDirections.actionCasesFragmentToTrackFragment(case.id))
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder =
                LayoutInflater.from(parent.context).inflate(R.layout.list_view_item, parent, false)
            return ViewHolder(holder)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindView(data[position])
        }

        fun addCaseList(list: ArrayList<Case>) {
            data.addAll(list)
            notifyDataSetChanged()
        }
    }
}
