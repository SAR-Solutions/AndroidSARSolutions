package com.sarcoordinator.sarsolutions.util

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sarcoordinator.sarsolutions.CasesTabFragment
import com.sarcoordinator.sarsolutions.FailedShiftsTabFragment
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.SettingsTabFragment
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

// Navigation class singleton
// Responsible for handling backstacks for each tab and the tabs themselves
object Navigation {

    enum class TabIdentifiers {
        HOME, FAILED_SHIFTS, SETTINGS
    }

    private var fragmentManager: FragmentManager? = null
    private var bottomNavBar: BottomNavigationView? = null
    var hideBottomNavBar: ((Boolean) -> Unit)? = null

    private val tabBackStack = HashMap<TabIdentifiers, Stack<KClass<Fragment>>>().apply {
        TabIdentifiers.values().forEach {
            this[it] = Stack<KClass<Fragment>>()
        }
    }
    private val tabStack = Stack<TabIdentifiers>()

    var currentTab: TabIdentifiers = TabIdentifiers.HOME

    private val fragmentStateMap = HashMap<KClass<Fragment>, Fragment.SavedState?>()

    @Volatile
    private lateinit var instance: Navigation

    fun getInstance(
        fragmentManager: FragmentManager? = null,
        bottomNavigationView: BottomNavigationView? = null,
        hideBottomNavBar: ((Boolean) -> Unit)? = null
    ): Navigation {
        if (fragmentManager != null && bottomNavigationView != null && hideBottomNavBar != null) {
            this.fragmentManager = fragmentManager
            this.bottomNavBar = bottomNavigationView
            this.hideBottomNavBar = hideBottomNavBar
            setupBottomNavBar()
            instance = this
        }
        return instance
    }

    private fun setupBottomNavBar() {
        bottomNavBar?.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home_dest -> loadTab(TabIdentifiers.HOME)
                R.id.failed_shifts_dest -> loadTab(TabIdentifiers.FAILED_SHIFTS)
                R.id.settings_dest -> loadTab(TabIdentifiers.SETTINGS)
                else -> return@setOnNavigationItemSelectedListener false
            }
            return@setOnNavigationItemSelectedListener true
        }
    }

    // Loads tab stack into current view, makes a new parent tab fragment if stack is null
    fun loadTab(tabIdentifier: TabIdentifiers) {

        if (currentTab == tabIdentifier && tabBackStack[tabIdentifier]!!.isNotEmpty())
        //TODO: Implement popping up to home/parent fragment
            return

        // Manage tabStack
        // Add to tabStack
        if (tabStack.contains(tabIdentifier))
            tabStack.remove(tabIdentifier)
        tabStack.add(tabIdentifier)

        if (tabBackStack[tabIdentifier].isNullOrEmpty()) {
            when (tabIdentifier) {
                TabIdentifiers.HOME -> pushFragment(CasesTabFragment(), tabIdentifier)
                TabIdentifiers.FAILED_SHIFTS -> pushFragment(
                    FailedShiftsTabFragment(),
                    tabIdentifier
                )
                TabIdentifiers.SETTINGS -> pushFragment(SettingsTabFragment(), tabIdentifier)
            }
        } else {
            showTab(tabIdentifier)
        }
    }

    /**
     * fragment: Fragment to put into container
     * saveState: Whether or not to save previous fragment state
     * tab: Tab to put fragment into
     */
    fun pushFragment(fragment: Fragment, tab: TabIdentifiers) {

        Log.d("HOAL", "Putting ${fragment.javaClass.name} into ${currentTab.name}")

        // Check to a void making another instance of top fragment
        if (currentTab == tab) {
            val currentTabBackstack = tabBackStack[currentTab]!!
            // Nothing to do if current fragment is the same as to-be-replaced fragment
            if (currentTabBackstack.isNotEmpty() &&
                currentTabBackstack.peek()
                == fragment::class
            )
                return
        }

        currentTab = tab
        val backStack = tabBackStack[currentTab]!!

        // Save current fragment state
        saveCurrentFragmentState()

        // Restore state if previous fragment state exists
        if (fragmentStateMap.containsKey(fragment.javaClass.kotlin)) {
            fragment.setInitialSavedState(fragmentStateMap[fragment.javaClass.kotlin])
        }

        // If fragment was found in backstack, remove it; Will be added to the top
        if (backStack.contains(fragment::class)) {
            backStack.remove(fragment::class)
        }

        backStack.push(fragment.javaClass.kotlin)

        fragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, fragment)
            ?.commit()
    }

    // Returns true if handled, false otherwise
    fun popFragment(): Boolean {
        val backStack = tabBackStack[currentTab]!!

        // 1) Check backStack
        //      If nothing to pop, step 2
        //      If something to pop, pop RETURN TRUE
        // 2) Check tabStack
        //      If nothing to pop, RETURN FALSE
        //      If something to pop, pop to current tab and show it

        if (backStack.size > 1) {
            val fragmentRemoved = backStack.pop()
            fragmentStateMap.remove(fragmentRemoved)
            showTab(currentTab)
            return true
        } else {
            // Clear out backstack if not empty
            if (backStack.isNotEmpty()) {
                val fragmentRemoved = backStack.pop()
                fragmentStateMap.remove(fragmentRemoved)
            }

            return if (tabStack.size > 1) {
                tabStack.pop()
                currentTab = tabStack.peek()

                // onItemSelected listener will be called
                bottomNavBar?.selectedItemId =
                    when (currentTab) {
                        TabIdentifiers.HOME -> R.id.home_dest
                        TabIdentifiers.FAILED_SHIFTS -> R.id.failed_shifts_dest
                        TabIdentifiers.SETTINGS -> R.id.settings_dest
                    }
                true
            } else {
                // Clear out backstack if not empty
                if (backStack.isNotEmpty()) {
                    val fragmentRemoved = backStack.pop()
                    fragmentStateMap.remove(fragmentRemoved)
                }
                false
            }
        }
    }

    // Show current tab and restore state its state; if null, loads current tab
    fun showTab(tabIdentifier: TabIdentifiers? = null) {
        if (tabIdentifier != null)
            currentTab = tabIdentifier

        val backStack = tabBackStack[currentTab]!!
        val fragmentToShow = backStack.peek().java.newInstance()
        fragmentToShow.setInitialSavedState(fragmentStateMap[fragmentToShow.javaClass.kotlin])
        fragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, fragmentToShow)
            ?.commitNow()
    }

    // Save current fragment state in fragmentStateMap
    fun saveCurrentFragmentState() {
        val currentFragment = fragmentManager?.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            fragmentStateMap[currentFragment.javaClass.kotlin] =
                fragmentManager?.saveFragmentInstanceState(currentFragment)
        }
    }

    // Stores state of current fragment and loses fragmentManager reference
    // To be called from MainActivity
    fun onFragmentManagerDestroy() {
        fragmentManager = null
        bottomNavBar = null
        hideBottomNavBar = null
    }

    fun clearBackstack() {
        currentTab = TabIdentifiers.HOME
        tabBackStack.apply {
            TabIdentifiers.values().forEach {
                this[it] = Stack<KClass<Fragment>>()
            }
        }
        tabStack.clear()
    }
}