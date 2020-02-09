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
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth = FirebaseAuth.getInstance()

    private var afm: AutofillManager? = null

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
        if (GlobalUtil.getThemeMode(resources) == GlobalUtil.THEME_DARK)
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

            it.isEnabled = false // Disable button

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
                            Toast.makeText(
                                context,
                                "${auth.currentUser!!.email} signed in",
                                Toast.LENGTH_LONG
                            ).show()

                            // Prompt auto fill service to save password
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                afm?.commit()
                            }

                            view?.findNavController()
                                ?.navigate(LoginFragmentDirections.actionLoginFragmentToCasesFragment())
                        } else {
                            it.isEnabled = true
                            Toast.makeText(
                                context,
                                "Invalid credentials. Try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                it.isEnabled = true
                Toast.makeText(context, "Unknown error occurred. Try again", Toast.LENGTH_LONG)
                    .show()
            }
        }

        forgot_password_button.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                email_text_layout to "email",
                imageView to "appImage",
                signin_button to "button"
            )
            findNavController()
                .navigate(
                    LoginFragmentDirections.actionLoginFragmentToResetPasswordFragment(),
                    extras
                )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

}
