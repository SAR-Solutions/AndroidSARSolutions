package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.fragment_settings.*
import timber.log.Timber

class SettingsFragment : Fragment() {

    companion object {
        const val TESTING_MODE_PREFS = "TESTING_MODE"
        const val THEME_PREFS = "THEME_PREFS"
        const val THEME_LIGHT = "THEME_LIGHT"
        const val THEME_DARK = "THEME_DARK"
        const val THEME_DEFAULT = "THEME_DEFAULT"
    }

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Init and set adapter for theme spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.theme_array,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            theme_spinner.adapter = it
        }

        testing_mode_switch.setOnClickListener {
            with(sharedPrefs.edit()) {
                putBoolean(TESTING_MODE_PREFS, (it as SwitchMaterial).isChecked)
                commit()
            }
        }

        loadPreferences()

        theme_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                Timber.d("Nothing selected")
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, d: Long) {
                Timber.d("$pos selected in theme spinner")
                when (pos) {
                    0 -> setThemePref(THEME_LIGHT)
                    1 -> setThemePref(THEME_DARK)
                    2 -> setThemePref(THEME_DEFAULT)
                    else -> throw Exception("Invalid position($pos) selected in theme spinner")
                }
            }
        }
    }

    // Set ui elements based on user preferences
    private fun loadPreferences() {
        // Theme
        sharedPrefs.getString(THEME_PREFS, THEME_DEFAULT).let {
            when (it) {
                THEME_LIGHT -> theme_spinner.setSelection(0)
                THEME_DARK -> theme_spinner.setSelection(1)
                THEME_DEFAULT -> theme_spinner.setSelection(2)
                else -> throw Exception("SharedPreferences returned unexpected value for current theme")
            }
        }

        // Testing mode
        testing_mode_switch.isChecked = sharedPrefs.getBoolean(TESTING_MODE_PREFS, false)
    }

    // Sets theme preference to parameter and changes theme
    private fun setThemePref(theme: String) {
        with(sharedPrefs.edit()) {
            putString(THEME_PREFS, theme)
            commit()
        }
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_DEFAULT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> throw Exception("Invalid argument passed to setThemePref: argument = $theme")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }
}