package com.example.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sarsolutions.models.Case
import kotlinx.android.synthetic.main.fragment_cases.*
import kotlinx.android.synthetic.main.list_view_item.view.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * This fragment displays the list of cases for the user
 */
class CasesFragment : Fragment() {

    private val viewModel: CasesViewModel by viewModel()
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cases, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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
                        .navigate(CasesFragmentDirections.actionCasesFragmentToMainFragment())
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
