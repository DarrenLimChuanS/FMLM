package com.example.fmlm.fragment.profile

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast

import com.example.fmlm.R
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ProfileComponentFragment : Fragment() {
    // Text Inputs
    private lateinit var textName: EditText
    private lateinit var textAge: EditText
    private lateinit var textReason: EditText

    // Spinner
    private lateinit var spinnerGender: Spinner
    private lateinit var spinnerMethod: Spinner

    // Document Reference
    private lateinit var mDocRef: DocumentReference

    companion object {
        fun newInstance() = ProfileComponentFragment()
    }

    private lateinit var viewModel: ProfileComponentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.profile_component_fragment, container, false)
        // Text Inputs
        textName = v.findViewById(R.id.edittext_name)
        textAge = v.findViewById(R.id.edittext_age)
        textReason = v.findViewById(R.id.edittext_reason)

        // Spinner
        spinnerGender = v.findViewById(R.id.spinner_gender)
        spinnerMethod = v.findViewById(R.id.spinner_method)

        // Button
        val button = v.findViewById(R.id.button_submit) as Button
        button.setOnClickListener {
            confirmInput()
        }

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Document References
        mDocRef = FirebaseFirestore.getInstance().document("FMLM/ProfileComponent")

        viewModel = ViewModelProviders.of(this).get(ProfileComponentViewModel::class.java)
    }

    // Function called when the submit button if pressed
    fun confirmInput() {
        // Check if required fields are filled
        if (!textName.text.isNullOrEmpty() &&
            !textAge.text.isNullOrEmpty() &&
            !textReason.text.isNullOrEmpty() &&
            spinnerGender.selectedItemPosition != 0 &&
            spinnerMethod.selectedItemPosition != 0
        ) {
            // Place data into a map
            val data = HashMap<String, String>()
            data["Name"] = textName.text.toString()
            data["Gender"] = spinnerGender.selectedItem.toString()
            data["Age"] = textAge.text.toString()
            data["Method"] = spinnerMethod.selectedItem.toString()
            data["Commute Reason"] = textReason.text.toString()

            // Save data to firecloud and show a toast message and reset inputs when successful
            mDocRef.collection("Users").add(data)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Submitted Successfully", Toast.LENGTH_LONG).show()

                    // Clear inputs
                    textName.text.clear()
                    textAge.text.clear()
                    textReason.text.clear()
                    spinnerGender.setSelection(0)
                    spinnerMethod.setSelection(0)

                }.addOnFailureListener { exception: java.lang.Exception ->
                Toast.makeText(activity, exception.toString(), Toast.LENGTH_LONG).show()
            }
        } else
            Toast.makeText(activity, "Required Fields Not Filled", Toast.LENGTH_LONG).show()
    }

}
