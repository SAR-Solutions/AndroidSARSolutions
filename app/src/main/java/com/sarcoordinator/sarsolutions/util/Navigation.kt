package com.sarcoordinator.sarsolutions.util

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sarcoordinator.sarsolutions.CasesTabFragment
import com.sarcoordinator.sarsolutions.FailedShiftsTabFragment
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.SettingsTabFragment
import java.util.*
import kotlin.collections.HashMap

// Navigation class singleton
object Navigation {

    private lateinit var fragmentManager: FragmentManager
    private lateinit var bottomNavigationView: BottomNavigationView

    @Volatile
    private var instance: Navigation? = null

    // Instance set in MainActivity and reused by fragments
    fun getInstance(
        fragmentManager: FragmentManager? = null,
        bottomNavigationView: BottomNavigationView? = null
    ): Navigation {
        if (fragmentManager != null && bottomNavigationView != null) {
            this.fragmentManager = fragmentManager
            this.bottomNavigationView = bottomNavigationView
            instance = this
            setup()
        }
        return instance!!
    }

    enum class BackStackIdentifiers {
        HOME, FAILED_SHIFTS, SETTINGS
    }

    private var tabBackStacks = Stack<BackStackIdentifiers>()

    private var backStacks = HashMap<BackStackIdentifiers, Stack<Fragment>>().apply {
        this[BackStackIdentifiers.HOME] = Stack<Fragment>()
        this[BackStackIdentifiers.FAILED_SHIFTS] = Stack<Fragment>()
        this[BackStackIdentifiers.SETTINGS] = Stack<Fragment>()
    }

    private var currentTab =
        BackStackIdentifiers.HOME

    private var navBarSelectedProgrammatically = false

    private fun setup() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            if (!navBarSelectedProgrammatically) {
                when (it.itemId) {
                    R.id.home_dest -> {
                        setSelectedTab(BackStackIdentifiers.HOME)
                    }
                    R.id.failed_shifts_dest -> {
                        setSelectedTab(BackStackIdentifiers.FAILED_SHIFTS)
                    }
                    R.id.settings_dest -> {
                        setSelectedTab(BackStackIdentifiers.SETTINGS)
                    }
                }
            } else
                navBarSelectedProgrammatically = false
            return@setOnNavigationItemSelectedListener true
        }
    }

    fun setSelectedTab(identifier: BackStackIdentifiers) {
        var comingFromFragment: Fragment? = null
        if (backStacks[currentTab]!!.size != 0)
            comingFromFragment = backStacks[currentTab]!!.lastElement()

        currentTab = identifier
        if (tabBackStacks.isEmpty())
            tabBackStacks.push(currentTab)
        else if (tabBackStacks.peek() != currentTab)
            tabBackStacks.push(currentTab)

        if (backStacks[identifier]!!.size == 0) {
            when (identifier) {
                BackStackIdentifiers.HOME -> pushFragment(
                    identifier,
                    CasesTabFragment()
                )
                BackStackIdentifiers.FAILED_SHIFTS -> pushFragment(
                    identifier,
                    FailedShiftsTabFragment()
                )
                BackStackIdentifiers.SETTINGS -> pushFragment(
                    identifier,
                    SettingsTabFragment()
                )
            }
        } else {
            loadTab(comingFromFragment, identifier)
        }
    }

    // If identifier is null, adds fragment to currentTab backstack
    fun pushFragment(
        identifier: BackStackIdentifiers?,
        fragment: Fragment,
        vararg sharedTransitionViews: View
    ) {
        if (identifier == null)
            backStacks[currentTab]!!.add(fragment)
        else
            backStacks[identifier]!!.add(fragment)

        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment).apply {
                for (element in sharedTransitionViews)
                    this.addSharedElement(element, element.transitionName)
            }
            .commit()
    }

    private fun loadTab(
        comingFromFragment: Fragment?,
        goingTo: BackStackIdentifiers
    ) {
        val fragmentToLoad = backStacks[goingTo]!!.lastElement()
        val transaction = fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentToLoad)
        if (comingFromFragment is ITabFragment) {
            (comingFromFragment as ITabFragment).getToolbar()?.let {
                transaction.addSharedElement(it, it.transitionName)
            }
        }
        transaction.commit()
    }

    // Set last tab as active/current tab and change bottom_nav_bar selection
    private fun popTabFragmentStack() {
        tabBackStacks.pop()
        fragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                backStacks[tabBackStacks.lastElement()]!!.lastElement()
            )
            .commit()
        navBarSelectedProgrammatically = true
        currentTab = tabBackStacks.lastElement()
        when (currentTab) {
            BackStackIdentifiers.HOME -> bottomNavigationView.selectedItemId =
                R.id.home_dest
            BackStackIdentifiers.FAILED_SHIFTS -> bottomNavigationView.selectedItemId =
                R.id.failed_shifts_dest
            BackStackIdentifiers.SETTINGS -> bottomNavigationView.selectedItemId =
                R.id.settings_dest
        }
    }

    fun popFragment() {
        backStacks[currentTab]!!.pop()
        fragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                backStacks[currentTab]!!.lastElement()
            )
            .commit()
    }

    // Clear backstack and place given fragment on stack
    fun popFragmentClearBackStack(fragment: Fragment) {
        backStacks[currentTab]!!.clear()
        pushFragment(null, fragment)
    }

    // Navigate to cases after successful login
    fun loginSuccessNavigation() {
        bottomNavigationView.visibility = View.VISIBLE
        setSelectedTab(BackStackIdentifiers.HOME)
    }

    // Returns false if not handled
    fun handleOnBackPressed(): Boolean {
        if (backStacks[currentTab]!!.size <= 1) {
            if (tabBackStacks.size <= 1) {
                return false
            } else {
                popTabFragmentStack()
            }
        } else {
            popFragment()
        }
        return true
    }

}