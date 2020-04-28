package com.sarcoordinator.sarsolutions.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.TabCasesFragment
import com.sarcoordinator.sarsolutions.TabFailedSyncsFragment
import com.sarcoordinator.sarsolutions.TabSettingsFragment
import java.util.*
import kotlin.collections.HashMap

// Navigation class singleton
// Responsible for handling backstacks for each tab and the tabs themselves
object Navigation {

    enum class TabIdentifiers {
        HOME, FAILED_SHIFTS, SETTINGS
    }

    private var saveCurrentViewState: Boolean = true
    private var fragmentManager: FragmentManager? = null
    private var bottomNavBar: BottomNavigationView? = null
    var hideBottomNavBar: ((Boolean) -> Unit)? = null

    private var tabBackStack = HashMap<TabIdentifiers, Stack<String>>().apply {
        TabIdentifiers.values().forEach {
            this[it] = Stack<String>()
        }
    }
    private var tabStack = Stack<TabIdentifiers>()

    var currentTab: TabIdentifiers = TabIdentifiers.HOME

    private var fragmentStateMap = HashMap<String, Fragment.SavedState?>()

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
            if(!saveCurrentViewState){
                saveCurrentViewState = true
            } else {
                saveCurrentFragmentState()
            }
            when (it.itemId) {
                R.id.home_dest -> loadTab(TabIdentifiers.HOME)
                R.id.failed_syncs_dest -> loadTab(TabIdentifiers.FAILED_SHIFTS)
                R.id.settings_dest -> loadTab(TabIdentifiers.SETTINGS)
                else -> return@setOnNavigationItemSelectedListener false
            }
            return@setOnNavigationItemSelectedListener true
        }
    }

    fun selectTab(tabIdentifier: TabIdentifiers) {
        bottomNavBar?.selectedItemId = when(tabIdentifier) {
            TabIdentifiers.HOME -> R.id.home_dest
            TabIdentifiers.FAILED_SHIFTS -> R.id.failed_syncs_dest
            TabIdentifiers.SETTINGS -> R.id.settings_dest
        }
    }

    // Loads tab stack into current view, makes a new parent tab fragment if stack is null
    private fun loadTab(tabIdentifier: TabIdentifiers) {

        if (currentTab == tabIdentifier && tabBackStack[tabIdentifier]!!.isNotEmpty())
        //TODO: Implement popping up to home/parent fragment
            return

        currentTab = tabIdentifier

        // Manage tabStack
        // Add to tabStack
        if (tabStack.contains(tabIdentifier))
            tabStack.remove(tabIdentifier)
        tabStack.add(tabIdentifier)

        if (tabBackStack[tabIdentifier].isNullOrEmpty()) {
            val currentFragment = getCurrentFragment()
            var toolbar: View? = null
            if(currentFragment is CustomFragment) {
                toolbar = (currentFragment as CustomFragment).getSharedElement()
            }
            when (tabIdentifier) {
                TabIdentifiers.HOME -> pushFragment(TabCasesFragment(), tabIdentifier, toolbar)
                TabIdentifiers.FAILED_SHIFTS -> pushFragment(
                    TabFailedSyncsFragment(),
                    tabIdentifier,
                    toolbar
                )
                TabIdentifiers.SETTINGS -> pushFragment(
                    TabSettingsFragment(),
                    tabIdentifier,
                    toolbar
                )
            }
        } else {
            showTab(tabIdentifier)
        }
    }

    /**
     * fragment: Fragment to put into container
     * saveState: Whether or not to save previous fragment state
     * tab: Tab to put fragment into; null will push to current tab
     * sharedElements: views to be included in shared element transition
     */
    fun pushFragment(fragment: Fragment, tab: TabIdentifiers? = null, vararg sharedElements: View?) {

        // Check to a void making another instance of top fragment
        if(tab != null && currentTab == tab) {
            val currentTabBackstack = tabBackStack[currentTab]!!
            // Nothing to do if current fragment is the same as to-be-replaced fragment
            if (currentTabBackstack.isNotEmpty() &&
                currentTabBackstack.peek()
                == fragment::class.qualifiedName
            )
                return

            currentTab = tab
        }

        val backStack = tabBackStack[currentTab]!!

        // Save current fragment state
        saveCurrentFragmentState()

        // Restore state if previous fragment state exists
        if (fragmentStateMap.containsKey(fragment.javaClass.kotlin.qualifiedName)) {
            fragment.setInitialSavedState(fragmentStateMap[fragment.javaClass.kotlin.qualifiedName])
        }

        // If fragment was found in backstack, remove it; Will be added to the top
        if (backStack.contains(fragment::class.qualifiedName)) {
            backStack.remove(fragment::class.qualifiedName)
        }

        backStack.push(fragment::class.qualifiedName)

        val transaction = fragmentManager?.beginTransaction()

        sharedElements.forEach { element ->
            element?.let {
                val transitionName = ViewCompat.getTransitionName(it) ?: ""
                transaction?.addSharedElement(it, transitionName)
            }
        }

        transaction
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

                saveCurrentViewState = false
                // onItemSelected listener will be called
                bottomNavBar?.selectedItemId =
                    when (tabStack.peek()) {
                        TabIdentifiers.HOME -> R.id.home_dest
                        TabIdentifiers.FAILED_SHIFTS -> R.id.failed_syncs_dest
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

    // Show current tab and restore its state; if null, loads current tab
    private fun showTab(tabIdentifier: TabIdentifiers? = null) {
        if (tabIdentifier != null)
            currentTab = tabIdentifier

        val backStack = tabBackStack[currentTab]!!
        val fragmentToShow: Fragment = Class.forName(backStack.peek()).newInstance() as Fragment
        fragmentToShow.setInitialSavedState(fragmentStateMap[fragmentToShow::class.qualifiedName ?: throw Exception("Unexpected class")])

        val currentFragment = getCurrentFragment()
        val transaction = fragmentManager?.beginTransaction()

        if(currentFragment is CustomFragment) {
            val toolbar = (currentFragment as CustomFragment).getSharedElement()
            transaction?.addSharedElement(toolbar, toolbar.transitionName)
        }

        transaction
            ?.replace(R.id.fragment_container, fragmentToShow)
            ?.commit()
    }

    // Save current fragment state in fragmentStateMap
    fun saveCurrentFragmentState() {
        val currentFragment = fragmentManager?.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            fragmentStateMap[currentFragment::class.qualifiedName!!] =
                fragmentManager?.saveFragmentInstanceState(currentFragment)
        }
    }

    private fun getCurrentFragment(): Fragment? = fragmentManager?.findFragmentById(R.id.fragment_container)

    // Stores state of current fragment and loses fragmentManager reference
    // To be called from MainActivity
    fun onActivityDestoryed() {
        fragmentManager = null
        bottomNavBar = null
        hideBottomNavBar = null
    }

    fun clearBackstack() {
        tabBackStack.apply {
            TabIdentifiers.values().forEach {
                this[it] = Stack<String>()
            }
        }
        tabStack.clear()
        fragmentStateMap.clear()
    }

    /** Process death related stuff **/

    fun getBackStack(): HashMap<TabIdentifiers, Stack<String>> {
        return tabBackStack
    }

    fun setBackStack(backStack: HashMap<*, Collection<String>>) {
        TabIdentifiers.values().forEach { tab ->
            val stackToAdd = Stack<String>()
            backStack[tab]?.forEach {
                stackToAdd.add(it)
            }
            tabBackStack[tab] = stackToAdd
        }
    }

    fun getTabStack(): Stack<TabIdentifiers> {
        return tabStack
    }

    fun setTabStack(tabStack: Stack<TabIdentifiers>) {
        this.tabStack = tabStack
        if (tabStack.isNotEmpty())
            currentTab = tabStack.peek()
    }

    fun getFragmentStateMap(): HashMap<String, Fragment.SavedState?> {
        return fragmentStateMap
    }

    fun setFragmentStateMap(fragmentStates: HashMap<String, Fragment.SavedState?>) {
        this.fragmentStateMap = HashMap<String, Fragment.SavedState?>()
        fragmentStates.forEach {
            fragmentStateMap[it.key] = it.value
        }
    }
}