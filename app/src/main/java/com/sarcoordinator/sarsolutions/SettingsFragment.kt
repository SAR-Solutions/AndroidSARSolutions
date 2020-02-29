package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.fragment_settings.*
import timber.log.Timber

class SettingsFragment : Fragment() {

    companion object {
        const val TESTING_MODE_PREFS = "TESTING_MODE"
    }

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.doOnApplyWindowInsets { view, insets, initialState ->
            view.updatePadding(
                top = initialState.paddings.top + insets.systemWindowInsetTop,
                bottom = initialState.paddings.bottom
            )
        }

        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        toolbar.title = getString(R.string.settings)

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
                GlobalUtil.setTheme(sharedPrefs, pos)
            }
        }

        license_button.setOnClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.licenses))
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }

        sign_out_button.setOnClickListener {
            auth.signOut()
            findNavController().navigate(
                R.id.loginFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )

        }
    }

    // Set ui elements based on user preferences
    private fun loadPreferences() {
        // Set spinner based on theme
        GlobalUtil.getTheme(sharedPrefs, resources).let {
            theme_spinner.setSelection(it)
        }

        // Testing mode
        testing_mode_switch.isChecked = sharedPrefs.getBoolean(TESTING_MODE_PREFS, false)

        // app version
        app_version_value.text = BuildConfig.VERSION_NAME
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }
}