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
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * A simple [Fragment] subclass.
 */
class LoginFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (auth.currentUser != null) {
            Toast.makeText(context, "${auth.currentUser!!.email} is logged in", Toast.LENGTH_LONG)
                .show()
            view!!.findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }

        signin_button.setOnClickListener {
            if (!Patterns.EMAIL_ADDRESS.matcher(email_input_text.text.toString()).matches()) { // Validate input is email
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(
                email_input_text.text.toString(),
                password_input_text.text.toString()
            )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            context,
                            "${auth.currentUser!!.email} has signed in!",
                            Toast.LENGTH_LONG
                        ).show()
                        view!!.findNavController()
                            .navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        Toast.makeText(
                            context,
                            "Invalid credentials. Try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        forgot_password_button.setOnClickListener {
            view!!.findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }
    }

}
