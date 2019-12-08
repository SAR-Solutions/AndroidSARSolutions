package com.example.sarsolutions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.sarsolutions.api.Repository
import com.example.sarsolutions.models.Case

class CasesViewModel : ViewModel() {

    val cases: LiveData<ArrayList<Case>> = liveData {
        val cases = ArrayList<Case>()
        Repository.getCases().caseIds.forEach { id ->
            cases.add(Repository.getCaseDetail(id))
        }
        emit(cases)
    }
}