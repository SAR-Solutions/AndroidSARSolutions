package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.CacheDatabase
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.ITabFragment
import com.sarcoordinator.sarsolutions.util.LocalCacheRepository
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private var navBarSelectedProgrammatically = false

    private var currentTab = BackStackViewModel.BackStackIdentifiers.HOME

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences

    private val viewModel: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

    private val backStackViewModel: BackStackViewModel by lazy {
        ViewModelProvider(this)[BackStackViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme needs to be applied before calling super.onCreate
        // Otherwise a new instance of this activity will be created
        loadUserPreferences()
        super.onCreate(savedInstanceState)

        bottom_nav_bar.selectedItemId = R.id.home_dest

        val repo = LocalCacheRepository(CacheDatabase.getDatabase(application).casesDao())
        repo.allShiftReports.observeForever {
            it.forEach { obj ->
                obj.locationList
            }
        }

        // Disable navbar while shift is active
        viewModel.isShiftActive.observe(this, Observer {
            it?.let {
                bottom_nav_bar.menu.forEach { menuItem ->
                    menuItem.isEnabled = !it
                }
            }
        })

//         Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        if (savedInstanceState == null) {
            // Navigate to login screen if user isn't logged in
            if (auth.currentUser == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
                bottom_nav_bar.visibility = View.GONE
            } else {
                setSelectedTab(BackStackViewModel.BackStackIdentifiers.HOME)
                bottom_nav_bar.visibility = View.VISIBLE
            }
        }

        bottom_nav_bar.setOnNavigationItemSelectedListener {
            if (!navBarSelectedProgrammatically) {
                when (it.itemId) {
                    R.id.home_dest -> {
                        setSelectedTab(BackStackViewModel.BackStackIdentifiers.HOME)
                    }
                    R.id.failed_shifts_dest -> {
                        setSelectedTab(BackStackViewModel.BackStackIdentifiers.FAILED_SHIFTS)
                    }
                    R.id.settings_dest -> {
                        setSelectedTab(BackStackViewModel.BackStackIdentifiers.SETTINGS)
                    }
                }
            } else
                navBarSelectedProgrammatically = false
            return@setOnNavigationItemSelectedListener true
        }
    }

    fun loginSuccessNavigation() {
        bottom_nav_bar.visibility = View.VISIBLE
        setSelectedTab(BackStackViewModel.BackStackIdentifiers.HOME)
    }

    private fun setSelectedTab(identifier: BackStackViewModel.BackStackIdentifiers) {
        var comingFromFragment: Fragment? = null
        if (backStackViewModel.backStacks[currentTab]!!.size != 0)
            comingFromFragment = backStackViewModel.backStacks[currentTab]!!.lastElement()

        currentTab = identifier
        if (backStackViewModel.tabBackStacks.isEmpty())
            backStackViewModel.tabBackStacks.push(currentTab)
        else if (backStackViewModel.tabBackStacks.peek() != currentTab)
            backStackViewModel.tabBackStacks.push(currentTab)

        if (backStackViewModel.backStacks[identifier]!!.size == 0) {
            if (comingFromFragment != null && (comingFromFragment is ITabFragment)) {
                val toolbar = (comingFromFragment as ITabFragment).getToolbar()
                when (identifier) {
                    BackStackViewModel.BackStackIdentifiers.HOME -> pushFragment(
                        identifier,
                        CasesTabFragment(),
                        toolbar
                    )
                    BackStackViewModel.BackStackIdentifiers.FAILED_SHIFTS -> pushFragment(
                        identifier,
                        FailedShiftsTabFragment(),
                        toolbar
                    )
                    BackStackViewModel.BackStackIdentifiers.SETTINGS -> pushFragment(
                        identifier,
                        SettingsTabFragment(),
                        toolbar
                    )
                }
            } else {
                when (identifier) {
                    BackStackViewModel.BackStackIdentifiers.HOME -> pushFragment(
                        identifier,
                        CasesTabFragment()
                    )
                    BackStackViewModel.BackStackIdentifiers.FAILED_SHIFTS -> pushFragment(
                        identifier,
                        FailedShiftsTabFragment()
                    )
                    BackStackViewModel.BackStackIdentifiers.SETTINGS -> pushFragment(
                        identifier,
                        SettingsTabFragment()
                    )
                }
            }
        } else {
            loadTab(comingFromFragment, identifier)
        }
    }

    // If identifier is null, adds fragment to currentTab backstack
    fun pushFragment(
        identifier: BackStackViewModel.BackStackIdentifiers?,
        fragment: Fragment,
        vararg sharedTransitionViews: View
    ) {
        if (identifier == null)
            backStackViewModel.backStacks[currentTab]!!.add(fragment)
        else
            backStackViewModel.backStacks[identifier]!!.add(fragment)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment).apply {
                for (element in sharedTransitionViews)
                    this.addSharedElement(element, element.transitionName)
            }
            .commit()
    }

    private fun loadTab(
        comingFromFragment: Fragment?,
        goingTo: BackStackViewModel.BackStackIdentifiers
    ) {
        val fragmentToLoad = backStackViewModel.backStacks[goingTo]!!.lastElement()
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentToLoad)
        if (comingFromFragment is ITabFragment) {
            val toolbar = (comingFromFragment as ITabFragment).getToolbar()
            transaction.addSharedElement(toolbar, toolbar.transitionName)
        }
        transaction.commit()
    }

    private fun popFragment() {
        backStackViewModel.backStacks[currentTab]!!.pop()
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                backStackViewModel.backStacks[currentTab]!!.lastElement()
            )
            .commit()
    }

    // Set last tab as active/current tab and change bottom_nav_bar selection
    private fun popTabFragmentStack() {
        backStackViewModel.tabBackStacks.pop()
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                backStackViewModel.backStacks[backStackViewModel.tabBackStacks.lastElement()]!!.lastElement()
            )
            .commit()
        navBarSelectedProgrammatically = true
        currentTab = backStackViewModel.tabBackStacks.lastElement()
        when (currentTab) {
            BackStackViewModel.BackStackIdentifiers.HOME -> bottom_nav_bar.selectedItemId =
                R.id.home_dest
            BackStackViewModel.BackStackIdentifiers.FAILED_SHIFTS -> bottom_nav_bar.selectedItemId =
                R.id.failed_shifts_dest
            BackStackViewModel.BackStackIdentifiers.SETTINGS -> bottom_nav_bar.selectedItemId =
                R.id.settings_dest
        }
    }

    // Clear backstack and place given fragment on stack
    fun popFragmentClearBackStack(fragment: Fragment) {
        backStackViewModel.backStacks[currentTab]!!.clear()
        pushFragment(null, fragment)
    }

    override fun onBackPressed() {
        when {
            auth.currentUser == null -> {
                super.onBackPressed()
            }
            backStackViewModel.backStacks[currentTab]!!.size <= 1 -> {
                if (backStackViewModel.tabBackStacks.size <= 1)
                    finishAffinity()
                else
                    popTabFragmentStack()
            }
            else -> {
                popFragment()
            }
        }
    }

    // Load user preferences using shared preferences
    // NOTE: Call before super.onCreate as it sets the app theme
    private fun loadUserPreferences() {
        sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        // Get system default theme
        GlobalUtil.setCurrentTheme(sharedPrefs, resources)
        if (GlobalUtil.getTheme(sharedPrefs, resources) == GlobalUtil.THEME_DARK) {
            // Set navigationBarColor to elevated gray
            window.navigationBarColor = Color.parseColor("#2D2D2D")
            window.statusBarColor = resources.getColor(R.color.gray)
        }
    }
}

