package com.sarcoordinator.sarsolutions

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth = FirebaseAuth.getInstance()

    private var afm: AutofillManager? = null

    private val nav: Navigation = Navigation.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            afm = requireContext().getSystemService(AutofillManager::class.java)
        }

        // Set app icon based on app theme
        if (GlobalUtil.getCurrentTheme(resources) == GlobalUtil.THEME_DARK)
            imageView.setImageResource(R.mipmap.app_icon_white_text)

        password_text_layout.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f).duration =
                resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        }

        // Set autofill hints
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            email_input_text.setAutofillHints(View.AUTOFILL_HINT_USERNAME)
            password_input_text.setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        }

        signin_button.setOnClickListener {

            // Proceed only if network connection is available
            if (!GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), requireView())) {
                return@setOnClickListener
            }

            // Disable buttons
            it.isEnabled = false
            forgot_password_button.isEnabled = false

            // Hide keyboard
            activity?.window?.decorView?.rootView?.let {
                val imm =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }

            // Validate input is email
            if (!Patterns.EMAIL_ADDRESS.matcher(email_input_text.text.toString()).matches()) {
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_LONG).show()
                it.isEnabled = true
                return@setOnClickListener
            }

            // Validate password format
            if (password_input_text.text.isNullOrEmpty()) {
                Toast.makeText(context, "Password can't be empty", Toast.LENGTH_LONG).show()
                it.isEnabled = true
                return@setOnClickListener
            }

            try {
                auth.signInWithEmailAndPassword(
                    email_input_text.text.toString(),
                    password_input_text.text.toString()
                )
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Prompt auto fill service to save password
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                afm?.commit()
                            }
                            nav.loginNavigation()
                        } else {
                            it.isEnabled = true
                            Toast.makeText(
                                context,
                                "Invalid credentials. Try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            forgot_password_button.isEnabled = true
                        }
                    }
            } catch (e: Exception) {
                it.isEnabled = true
                Toast.makeText(context, "Unknown error occurred. Try again", Toast.LENGTH_LONG)
                    .show()
            }
        }

        forgot_password_button.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, ResetPasswordFragment())
                .addSharedElement(email_text_layout, email_text_layout.transitionName)
                .addSharedElement(imageView, imageView.transitionName)
                .addSharedElement(signin_button, signin_button.transitionName)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

}
