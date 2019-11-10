package com.example.sarsolutions

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_reset_password.*

/**
 * A simple [Fragment] subclass.
 */
class ResetPasswordFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reset_password, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        forgot_password_button.setOnClickListener {
            if (!Patterns.EMAIL_ADDRESS.matcher(email_input_text.text.toString()).matches()) { // Validate input is email
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email_input_text.text.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Email Sent.", Toast.LENGTH_LONG).show()
                        view!!.findNavController().popBackStack()
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
}
