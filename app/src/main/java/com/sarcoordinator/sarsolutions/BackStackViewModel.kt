package com.sarcoordinator.sarsolutions

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import java.util.*
import kotlin.collections.HashMap

class BackStackViewModel : ViewModel() {

    enum class BackStackIdentifiers {
        HOME, FAILED_SHIFTS, SETTINGS
    }

    var tabBackStacks = Stack<BackStackIdentifiers>()

    var backStacks = HashMap<BackStackIdentifiers, Stack<Fragment>>().apply {
        this[BackStackIdentifiers.HOME] = Stack<Fragment>()
        this[BackStackIdentifiers.FAILED_SHIFTS] = Stack<Fragment>()
        this[BackStackIdentifiers.SETTINGS] = Stack<Fragment>()
    }
}