package com.sarcoordinator.sarsolutions

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
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

    private var afm: AutofillManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            afm = requireContext().getSystemService(AutofillManager::class.java)
        }
        // Set autofill hints
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            email_input_text.setAutofillHints(View.AUTOFILL_HINT_USERNAME)
            password_input_text.setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        }

        signin_button.setOnClickListener {
            it.isEnabled = false // Disable button

            // Hide keyboard
            activity?.window?.decorView?.rootView?.let {
                val imm =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email_input_text.text.toString()).matches()) { // Validate input is email
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_LONG).show()
                it.isEnabled = true
                return@setOnClickListener
            }

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
            view?.findNavController()
                ?.navigate(LoginFragmentDirections.actionLoginFragmentToResetPasswordFragment())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

}
