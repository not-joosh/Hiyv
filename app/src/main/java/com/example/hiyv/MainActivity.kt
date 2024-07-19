package com.example.hiyv

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.hiyv.databinding.ActivityMainBinding
import com.example.hiyv.databinding.DialogCreateAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        Glide.with(this).load(R.drawable.backdrop_authentication).into(binding.gifContainer)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmailInput.text.toString().trim()
            val password = binding.etPasswordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        val exception = task.exception
                        val errorMessage = when (exception) {
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                            else -> "Authentication failed: ${exception?.message}"
                        }
                        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }

        val clickHereText = binding.clickHere
        val spannableString = SpannableString(clickHereText.text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showCreateAccountDialog()
            }
        }
        val start = clickHereText.text.indexOf("Click Here!")
        val end = start + "Click Here!".length
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        clickHereText.text = spannableString
        clickHereText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showCreateAccountDialog() {
        val dialogBinding = DialogCreateAccountBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Account")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCreateAccount.setOnClickListener {
            val fullName = dialogBinding.etFullName.text.toString().trim()
            val email = dialogBinding.etEmail.text.toString().trim()
            val password = dialogBinding.etPassword.text.toString().trim()
            val confirmPassword = dialogBinding.etConfirmPassword.text.toString().trim()
            val selectedAccountTypeId = dialogBinding.radioGroupAccountType.checkedRadioButtonId

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || selectedAccountTypeId == -1) {
                Toast.makeText(this, "Please fill in all fields and select an account type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val accountType = dialogBinding.root.findViewById<RadioButton>(selectedAccountTypeId).text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        saveUserToFirestore(fullName, accountType, user, email)
                        updateUI(user)
                        dialog.dismiss()
                    } else {
                        val exception = task.exception
                        val errorMessage = when (exception) {
                            is FirebaseAuthUserCollisionException -> "This email address is already in use."
                            is FirebaseAuthWeakPasswordException -> "The password is too weak."
                            is FirebaseAuthInvalidCredentialsException -> "The email address is malformed."
                            else -> "Authentication failed: ${exception?.message}"
                        }
                        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }

        dialog.show()
    }

    private fun saveUserToFirestore(fullName: String, accountType: String, user: FirebaseUser?, email: String) {
        user?.let {
            val userMap = hashMapOf(
                "Name" to fullName,
                "userID" to it.uid,
                "accountType" to accountType,
                "family" to emptyList<String>(),
                "email" to email,
                "invites" to emptyList<String>(),
                "tasks" to emptyList<String>(),
                "scheduled_tasks" to emptyList<ScheduledTask>(),
            )

            db.collection("users").document(it.uid).set(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error creating account: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Clear input fields if necessary
        }
    }
}
