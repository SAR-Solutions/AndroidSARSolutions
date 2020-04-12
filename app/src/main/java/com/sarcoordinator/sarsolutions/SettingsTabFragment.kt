package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.transition.TransitionInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.CustomFragment
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.fragment_settings.*
import timber.log.Timber

class SettingsTabFragment : Fragment(R.layout.fragment_settings), CustomFragment {

    private val nav: Navigation by lazy { Navigation.getInstance() }

    companion object {
        const val TESTING_MODE_PREFS = "TESTING_MODE"
        const val LOW_BANDWIDTH_PREFS = "LOW_BANDWIDTH"
        const val MAP_LIGHT_THEME_PREFS = "MAP_LIGHT_THEME"
        const val MAP_DARK_THEME_PREFS = "MAP_DARK_THEME"
    }

    enum class MapLightThemes {
        STANDARD, SNOW
    }

    enum class MapDarkThemes {
        STANDARD, Night
    }

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences
    private var isThemeSelected: Boolean = false

    override fun getSharedElement(): View = toolbar_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        toolbar_settings.attachRecyclerView(parent_scroll_view)

        // Init and set adapter for theme spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.theme_array,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            theme_spinner.adapter = it
        }

        setupDebugSettingsCard()

        loadPreferences()

        // Set onClick listeners for release settings
        theme_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                Timber.d("Nothing selected")
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, d: Long) {
                Timber.d("$pos selected in theme spinner")
                if (!isThemeSelected)
                    isThemeSelected = true
                else
                    GlobalUtil.setTheme(sharedPrefs, pos)
            }
        }

        low_bandwidth_switch.setOnClickListener {
            with(sharedPrefs.edit()) {
                putBoolean(LOW_BANDWIDTH_PREFS, (it as SwitchMaterial).isChecked)
                commit()
            }
        }

        map_light_radio_group.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.std_light_map_radio -> putStringInSharedPref(
                    MAP_LIGHT_THEME_PREFS,
                    MapLightThemes.STANDARD.name
                )
                R.id.snow_light_map_radio -> putStringInSharedPref(
                    MAP_LIGHT_THEME_PREFS,
                    MapLightThemes.SNOW.name
                )
                else -> Toast.makeText(
                    requireContext(),
                    "Something went wrong changing light theme",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        map_dark_radio_group.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.std_dark_map_radio -> putStringInSharedPref(
                    MAP_DARK_THEME_PREFS,
                    MapDarkThemes.STANDARD.name
                )
                R.id.night_dark_map_radio -> putStringInSharedPref(
                    MAP_DARK_THEME_PREFS,
                    MapDarkThemes.Night.name
                )
                else -> Toast.makeText(
                    requireContext(),
                    "Something went wrong changing dark theme",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        license_button.setOnClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.licenses))
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }

        sign_out_button.setOnClickListener {
            auth.signOut()
            nav.hideBottomNavBar?.let { it(true) }
            nav.clearBackstack()

            requireActivity().viewModelStore.clear()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        privacy_policy_button.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupDebugSettingsCard() {
        if (BuildConfig.DEBUG) {
            testing_mode_switch.setOnClickListener {
                with(sharedPrefs.edit()) {
                    putBoolean(TESTING_MODE_PREFS, (it as SwitchMaterial).isChecked)
                    commit()
                }
            }
        } else {
            // Release
            debug_settings_card.visibility = View.GONE
        }
    }

    // Set ui elements based on user preferences
    private fun loadPreferences() {
        // Set spinner based on theme
        GlobalUtil.getThemePreference(sharedPrefs).let {
            theme_spinner.setSelection(it)
        }

        // Low bandwidth mode
        low_bandwidth_switch.isChecked = sharedPrefs.getBoolean(LOW_BANDWIDTH_PREFS, false)

        // Testing mode
        testing_mode_switch.isChecked = sharedPrefs.getBoolean(TESTING_MODE_PREFS, false)

        // App version
        app_version_value.text = BuildConfig.VERSION_NAME

        // Light map theme
        when (sharedPrefs.getString(MAP_LIGHT_THEME_PREFS, MapLightThemes.STANDARD.name)) {
            MapLightThemes.STANDARD.name -> std_light_map_radio.isChecked = true
            MapLightThemes.SNOW.name -> snow_light_map_radio.isChecked = true
        }

        // Dark map theme
        when (sharedPrefs.getString(MAP_DARK_THEME_PREFS, MapDarkThemes.STANDARD.name)) {
            MapDarkThemes.STANDARD.name -> std_dark_map_radio.isChecked = true
            MapDarkThemes.Night.name -> night_dark_map_radio.isChecked = true
        }
    }

    private fun putStringInSharedPref(key: String, value: String) {
        with(sharedPrefs.edit()) {
            putString(key, value)
            commit()
        }
    }
}