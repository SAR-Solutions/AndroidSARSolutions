package com.example.sarsolutions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.sarsolutions.api.Repository
import com.example.sarsolutions.models.Case
import kotlinx.coroutines.Dispatchers

class CasesViewModel : ViewModel() {

    var isTestingEnabled = false
    lateinit var lastUpdatedText: String

    val cases: LiveData<ArrayList<Case>> = liveData(Dispatchers.IO) {
        val result = ArrayList<Case>()
        Repository.getCases().caseIds.forEach { id ->
            result.add(Repository.getCaseDetail(id))
        }
        emit(result)
    }
}