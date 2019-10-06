package com.example.fmlm.fragment.login

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.example.fmlm.R
import com.example.fmlm.fragment.registration.RegistrationComponentFragment

class LoginComponentFragment : Fragment() {

    companion object {
        fun newInstance() = LoginComponentFragment()
    }

    private lateinit var viewModel: LoginComponentViewModel

    // Text inputs
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

    // Text Buttons
    private lateinit var registerButton: TextView

    // Buttons
    private lateinit var loginButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.login_component_fragment, container, false)
        loginButton = v.findViewById(R.id.button_register)
        usernameEditText = v.findViewById(R.id.edittext_username)
        passwordEditText = v.findViewById(R.id.edittext_password)
        registerButton = v.findViewById(R.id.textView_register)

        // Set on click listeners
        loginButton.setOnClickListener {
            onLoginButtonPress()
        }

        registerButton.setOnClickListener {
            onRegisterButtonPress()
        }

        activity?.actionBar?.hide()

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LoginComponentViewModel::class.java)
    }

    private fun onLoginButtonPress() {
        // Empty input checks
        if (usernameEditText.text.isNullOrEmpty() || passwordEditText.text.isNullOrEmpty()) {
            Log.e("Login", "Empty Inputs!")
        }
    }

    private fun onRegisterButtonPress() {
        val fragment: Fragment = RegistrationComponentFragment()
        val transaction = fragmentManager?.beginTransaction()!!
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
