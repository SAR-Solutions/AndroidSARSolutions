package com.sarcoordinator.sarsolutions

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import kotlinx.android.synthetic.main.fragment_reset_password.*

class ResetPasswordFragment : Fragment(R.layout.fragment_reset_password) {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        imageView.doOnApplyWindowInsets { view, insets, initialState ->
//            view.setMargins(
//                initialState.margins.left + insets.systemGestureInsets.left,
//                initialState.margins.top + insets.systemGestureInsets.top,
//                initialState.margins.right + insets.systemGestureInsets.right,
//                initialState.margins.bottom + insets.systemGestureInsets.bottom
//            )
//        }

        // Set autofill hint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            email_input_text.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        }

        if (GlobalUtil.getCurrentTheme(
                resources,
                requireActivity().getPreferences(Context.MODE_PRIVATE)
            ) == GlobalUtil.THEME_DARK
        )
            imageView.setImageResource(R.mipmap.app_icon_white_text)

        forgot_password_button.setOnClickListener {
            if (!GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), requireView()))
                return@setOnClickListener

            // Validate input is email
            if (!Patterns.EMAIL_ADDRESS.matcher(email_input_text.text.toString()).matches()) {
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email_input_text.text.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Email Sent.", Toast.LENGTH_LONG).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(
                            context,
                            "No account found associated with the entered email",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)
    }
}
