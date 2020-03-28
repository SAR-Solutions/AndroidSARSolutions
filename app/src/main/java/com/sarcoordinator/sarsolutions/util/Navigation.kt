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
    private lateinit var hideBottomNavBar: (Boolean) -> Unit

    @Volatile
    private var instance: Navigation? = null

    // Instance set in MainActivity and reused by fragments
    fun getInstance(
        fragmentManager: FragmentManager? = null,
        bottomNavigationView: BottomNavigationView? = null,
        hideBottomNavBar: ((Boolean) -> Unit)? = null
    ): Navigation {
        if (fragmentManager != null && bottomNavigationView != null && hideBottomNavBar != null) {
            this.fragmentManager = fragmentManager
            this.bottomNavigationView = bottomNavigationView
            this.hideBottomNavBar = hideBottomNavBar
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

    private fun setSelectedTab(identifier: BackStackIdentifiers) {
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
        sharedTransitionView: View? = null
    ) {
        if (identifier == null)
            backStacks[currentTab]!!.add(fragment)
        else
            backStacks[identifier]!!.add(fragment)

        // Hide bottom nav bar in ImageDetailFragment
        if (fragment is ImageDetailFragment) {
            hideBottomNavBar(true)
        }

        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment).apply {
                sharedTransitionView?.let {
                    this.addSharedElement(sharedTransitionView, sharedTransitionView.transitionName)
                }
            }
            .setReorderingAllowed(true)
            .commit()
    }

    private fun loadTab(
        comingFromFragment: Fragment?,
        goingTo: BackStackIdentifiers
    ) {
        val fragmentToLoad = backStacks[goingTo]!!.lastElement()
        val transaction = fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentToLoad)
        if (comingFromFragment is ISharedElementFragment && fragmentToLoad is ISharedElementFragment) {
            (comingFromFragment as ISharedElementFragment).getSharedElement()?.let { element ->
                transaction.addSharedElement(element, element.transitionName)
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
        val transaction = fragmentManager.beginTransaction()
        val source = backStacks[currentTab]!!.pop()
        val destination = backStacks[currentTab]!!.lastElement()

        // Show nav bar again if coming back from ImageDetailFragment
        if (source is ImageDetailFragment)
            hideBottomNavBar(false)

        if (source is ISharedElementFragment) {
            source.getSharedElement()?.let { element ->
                transaction.addSharedElement(element, element.transitionName)
            }
        }
        transaction
            .replace(
                R.id.fragment_container,
                destination
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
        hideBottomNavBar(false)
        bottomNavigationView.selectedItemId = R.id.home_dest
    }

    fun logoutNavigation() {
        // Clear back stacks
        backStacks[BackStackIdentifiers.HOME]!!.clear()
        backStacks[BackStackIdentifiers.FAILED_SHIFTS]!!.clear()
        backStacks[BackStackIdentifiers.SETTINGS]!!.clear()
        tabBackStacks.clear()

        hideBottomNavBar(true)
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commitNow()
    }

    // Loads current tab and fragment again
    fun restoreState() {
        hideBottomNavBar(false)
        setSelectedTab(currentTab)

        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, getCurrentFragment())
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