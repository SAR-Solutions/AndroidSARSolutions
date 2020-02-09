package com.sarcoordinator.sarsolutions

import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Patterns
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import kotlinx.android.synthetic.main.fragment_reset_password.*

/**
 * A simple [Fragment] subclass.
 */
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

        // Set autofill hint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            email_input_text.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        }

        if (GlobalUtil.getThemeMode(resources) == GlobalUtil.THEME_DARK)
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
                        view.findNavController().popBackStack()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }
}
