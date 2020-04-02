package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.TabFragment
import kotlinx.android.synthetic.main.fragment_settings.*
import timber.log.Timber
import java.io.Serializable

class SettingsTabFragment : Fragment(R.layout.fragment_settings), TabFragment {

    private val nav: Navigation by lazy { Navigation.getInstance() }

    companion object {
        const val TESTING_MODE_PREFS = "TESTING_MODE"
    }

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences
    private var isThemeSelected: Boolean = false

    override fun getToolbar(): View = toolbar_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                if (!isThemeSelected)
                    isThemeSelected = true
                else
                    GlobalUtil.setTheme(sharedPrefs, pos)
            }
        }

        license_button.setOnClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.licenses))
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }

        sign_out_button.setOnClickListener {
            auth.signOut()
            nav.hideBottomNavBar?.let { it(true) }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()

            nav.clearBackstack()
        }
    }

    // Set ui elements based on user preferences
    private fun loadPreferences() {
        // Set spinner based on theme
        GlobalUtil.getThemePreference(sharedPrefs).let {
            theme_spinner.setSelection(it)
        }

        // Testing mode
        testing_mode_switch.isChecked = sharedPrefs.getBoolean(TESTING_MODE_PREFS, false)

        // app version
        app_version_value.text = BuildConfig.VERSION_NAME
    }
}