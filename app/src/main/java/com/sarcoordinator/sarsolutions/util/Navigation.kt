package com.sarcoordinator.sarsolutions.util

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sarcoordinator.sarsolutions.*
import java.util.*
import kotlin.collections.HashMap

// Navigation class singleton
// Responsible for handling backstacks for each tab and the tabs themselves
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

    var currentTab =
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
        if (comingFromFragment is ICustomToolbarFragment) {
            (comingFromFragment as ICustomToolbarFragment).getToolbar()?.let {
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

    private fun popFragment() {
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
    fun loginNavigation() {
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationView.selectedItemId = R.id.home_dest
    }

    fun logoutNavigation() {
        // Clear back stacks
        backStacks[BackStackIdentifiers.HOME]!!.clear()
        backStacks[BackStackIdentifiers.FAILED_SHIFTS]!!.clear()
        backStacks[BackStackIdentifiers.SETTINGS]!!.clear()
        tabBackStacks.clear()

        bottomNavigationView.visibility = View.GONE
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commitNow()
    }

    // Returns false if not handled
    fun handleOnBackPressed(): Boolean {
        if (backStacks[currentTab]!!.size <= 1) {
            if (tabBackStacks.size <= 1) {
                // Finish activity, nothing left to pop
                return false
            } else {
                // Nothing to pop from back stack, pop tab back stack
                popTabFragmentStack()
            }
        } else {
            popFragment()
        }
        return true
    }

    fun getCurrentFragment(): Fragment = backStacks[currentTab]!!.lastElement()
}