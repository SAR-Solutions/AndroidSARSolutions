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
import com.sarcoordinator.sarsolutions.util.LocalCacheRepository
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private var navBarSelectedProgrammatically = false

    private var tabBackStacks = Stack<BackStackIdentifiers>()

    private var backStacks = HashMap<BackStackIdentifiers, Stack<Fragment>>().apply {
        this[BackStackIdentifiers.HOME] = Stack<Fragment>()
        this[BackStackIdentifiers.FAILED_SHIFTS] = Stack<Fragment>()
        this[BackStackIdentifiers.SETTINGS] = Stack<Fragment>()
    }

    private var currentTab = BackStackIdentifiers.HOME

    enum class BackStackIdentifiers {
        HOME, FAILED_SHIFTS, SETTINGS
    }

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences

    private var bottomBarSelectedProgrammatically = false

    private val viewModel: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
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
                setSelectedTab(BackStackIdentifiers.HOME)
                bottom_nav_bar.visibility = View.VISIBLE
            }
        }

        bottom_nav_bar.setOnNavigationItemSelectedListener {
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

    fun loginSuccessNavigation() {
        bottom_nav_bar.visibility = View.VISIBLE
        setSelectedTab(BackStackIdentifiers.HOME)
    }

    private fun setSelectedTab(identifier: BackStackIdentifiers) {
        currentTab = identifier
        if (tabBackStacks.isEmpty())
            tabBackStacks.push(currentTab)
        else if (tabBackStacks.peek() != currentTab)
            tabBackStacks.push(currentTab)

        if (backStacks[identifier]!!.size == 0) {
            when (identifier) {
                BackStackIdentifiers.HOME -> pushFragment(identifier, CasesFragment())
                BackStackIdentifiers.FAILED_SHIFTS -> pushFragment(
                    identifier,
                    FailedShiftsFragment()
                )
                BackStackIdentifiers.SETTINGS -> pushFragment(identifier, SettingsFragment())
            }
        } else {
            loadTab(identifier)
        }
    }

    // If identifier is null, adds fragment to currentTab backstack
    fun pushFragment(identifier: BackStackIdentifiers?, fragment: Fragment) {
        if(identifier == null)
            backStacks[currentTab]!!.add(fragment)
        else
            backStacks[identifier]!!.add(fragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadTab(identifier: BackStackIdentifiers) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, backStacks[identifier]!!.lastElement())
            .commit()
    }

    private fun popFragment() {
        backStacks[currentTab]!!.pop()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, backStacks[currentTab]!!.lastElement())
            .commit()
    }

    // Set last tab as active/current tab and change bottom_nav_bar selection
    private fun popTabFragmentStack() {
        tabBackStacks.pop()
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                backStacks[tabBackStacks.lastElement()]!!.lastElement()
            )
            .commit()
        navBarSelectedProgrammatically = true
        currentTab = tabBackStacks.lastElement()
        when (currentTab) {
            BackStackIdentifiers.HOME -> bottom_nav_bar.selectedItemId = R.id.home_dest
            BackStackIdentifiers.FAILED_SHIFTS -> bottom_nav_bar.selectedItemId =
                R.id.failed_shifts_dest
            BackStackIdentifiers.SETTINGS -> bottom_nav_bar.selectedItemId = R.id.settings_dest
        }
    }

    // Clear backstack and place given fragment on stack
    fun popFragmentClearBackStack(fragment: Fragment) {
        backStacks[currentTab]!!.clear()
        pushFragment(null, fragment)
    }

    override fun onBackPressed() {
        when {
            auth.currentUser == null -> {
                super.onBackPressed()
            }
            backStacks[currentTab]!!.size <= 1 -> {
                if (tabBackStacks.size <= 1)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("backStack", backStacks)
        outState.putSerializable("tabsBackStack", tabBackStacks)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        backStacks =
            savedInstanceState.getSerializable("backStack") as HashMap<BackStackIdentifiers, Stack<Fragment>>
        tabBackStacks =
            savedInstanceState.getSerializable("tabsBackStack") as Stack<BackStackIdentifiers>
    }
}

