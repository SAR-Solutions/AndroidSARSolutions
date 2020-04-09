package com.sarcoordinator.sarsolutions

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Patterns
import android.view.View
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.setMargins
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth = FirebaseAuth.getInstance()

    private var afm: AutofillManager? = null

    private val nav: Navigation = Navigation.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        imageView.doOnApplyWindowInsets { v, insets, initialState ->
//            v.setMargins(
//                initialState.margins.left + insets.systemGestureInsets.left,
//                initialState.margins.top + insets.systemGestureInsets.top,
//                initialState.margins.right + insets.systemGestureInsets.right,
//                initialState.margins.bottom + insets.systemGestureInsets.bottom
//            )
//        }
//
        privacy_policy_text.setOnApplyWindowInsetsListener { v, insets ->
            privacy_policy_text.setMargins(
                insets.systemGestureInsets.left,
                insets.systemGestureInsets.top,
                insets.systemGestureInsets.right,
                insets.systemGestureInsets.bottom
            )
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            afm = requireContext().getSystemService(AutofillManager::class.java)
        }

        // Set app icon based on app theme
        if (GlobalUtil.getCurrentTheme(
                resources,
                requireActivity().getPreferences(Context.MODE_PRIVATE)
            ) == GlobalUtil.THEME_DARK
        )
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

            // Disable UI elements
            enableUIElements(false)

            // Hide keyboard
            activity?.window?.decorView?.rootView?.let {
                val imm =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }

            // Validate input is email
            if (!Patterns.EMAIL_ADDRESS.matcher(email_input_text.text.toString()).matches()) {
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_LONG).show()
                enableUIElements(true)
                return@setOnClickListener
            }

            // Validate password format
            if (password_input_text.text.isNullOrEmpty()) {
                Toast.makeText(context, "Password can't be empty", Toast.LENGTH_LONG).show()
                enableUIElements(true)
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
                            nav.hideBottomNavBar?.let { it(false) }
                            nav.selectTab(Navigation.TabIdentifiers.HOME)
                        } else {
                            enableUIElements(true)
                            Toast.makeText(
                                context,
                                "Invalid credentials. Try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                enableUIElements(true)
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

        privacy_policy_text.movementMethod = LinkMovementMethod.getInstance()

    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)
    }

    private fun enableUIElements(enable: Boolean) {
        signin_button.isEnabled = enable
        forgot_password_button.isEnabled = enable
        email_input_text.isEnabled = enable
        password_input_text.isEnabled = enable
    }

}
