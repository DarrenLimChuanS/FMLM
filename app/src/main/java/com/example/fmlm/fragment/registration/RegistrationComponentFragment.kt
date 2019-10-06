package com.example.fmlm.fragment.registration

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.example.fmlm.R
import com.example.fmlm.fragment.login.LoginComponentFragment
//import com.google.firebase.auth.FirebaseAuth

class RegistrationComponentFragment : Fragment() {

    companion object {
        fun newInstance() = RegistrationComponentFragment()
    }

    // Text inputs
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var emailEditText: EditText

    // Text Buttons
    private lateinit var backToLoginButton: TextView

    // Buttons
    private lateinit var registerButton: Button

    // Firebase
    //private  lateinit var mAuth: FirebaseAuth

    private lateinit var viewModel: RegistrationComponentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.registration_component_fragment, container, false)

        //mAuth = FirebaseAuth.getInstance()

        usernameEditText = v.findViewById(R.id.edittext_username)
        passwordEditText = v.findViewById(R.id.edittext_password)
        emailEditText = v.findViewById(R.id.edittext_email)
        registerButton = v.findViewById(R.id.button_register)
        backToLoginButton = v.findViewById(R.id.textView_back_to_login)

        registerButton.setOnClickListener {
            onRegisterPress()
        }

        backToLoginButton.setOnClickListener {
            onBackToLoginPress()
        }

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RegistrationComponentViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun onRegisterPress() {
        // Empty input checks
        if (emailEditText.text.isNullOrEmpty() || passwordEditText.text.isNullOrEmpty()) {
            Toast.makeText(activity, "Empty username or password.", Toast.LENGTH_LONG).show()
        }


    }

    private fun onBackToLoginPress() {
        val fragment: Fragment = LoginComponentFragment()
        val transaction = fragmentManager?.beginTransaction()!!
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}
