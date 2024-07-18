package com.example.hiyv

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
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
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Load the image using Glide
        Glide.with(this).load(R.drawable.backdrop_authentication).into(binding.gifContainer)

        // Set up the ClickableSpan for "Click Here!"
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

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
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

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user = auth.currentUser
                        saveUserToFirestore(fullName, user)
                        updateUI(user)
                        dialog.dismiss()
                    } else {
                        // If sign in fails, display a message to the user.
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

    private fun saveUserToFirestore(fullName: String, user: FirebaseUser?) {
        user?.let {
            val userMap = hashMapOf(
                "Name" to fullName,
                "userID" to it.uid,
                "family" to emptyList<String>()
            )

            db.collection("users").document(it.uid)
                .set(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to register user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // If user is not signed in, show a message or keep the current screen
        }
    }
}
