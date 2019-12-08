package com.example.sarsolutions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * This fragment displays the list of cases for the user
 */
class CasesFragment : Fragment() {

    private val viewModel: CasesViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.cases.observe(this, Observer {
            Timber.d("Got something")
        })
    }

}
