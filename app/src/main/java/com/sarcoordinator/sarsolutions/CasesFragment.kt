package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
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
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_cases, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        (activity as MainActivity).supportActionBar?.title = "Tis work liek magik"

        viewManager = LinearLayoutManager(context)
        viewAdapter = Adapter()
        cases_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        viewModel.cases.observe(this, Observer {
            viewAdapter.addCaseList(it)
        })
    }

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
                        .navigate(CasesFragmentDirections.actionCasesFragmentToTrackFragment())
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out -> {
                if (viewModel.getBinder().value == null) {
                    auth.signOut()
                    findNavController().navigate(CasesFragmentDirections.actionCasesFragmentToLoginFragment())
                    true
                } else {
                    Snackbar.make(view!!, "Stop tracking to sign out", Snackbar.LENGTH_LONG).show()
                    true
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
