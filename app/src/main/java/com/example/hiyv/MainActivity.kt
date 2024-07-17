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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val fullName = dialogBinding.etFullName.text.toString()
            val email = dialogBinding.etEmail.text.toString()
            val password = dialogBinding.etPassword.text.toString()
            val confirmPassword = dialogBinding.etConfirmPassword.text.toString()

            // Example validation (you should add your own validation logic)
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                // Handle empty fields scenario
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                // Handle password mismatch scenario
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simulated account creation and login logic (replace with actual logic)
            // For simplicity, simulate a successful login after account creation
            // Replace with your actual account creation and login logic

            // Assuming successful account creation and login
            // You might want to store user session or credentials securely (e.g., SharedPreferences, etc.)
            val isLoggedIn = true // Simulated login status

            if (isLoggedIn) {
                // Example logic: Navigate to HomeActivity after successful login
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)

                // Dismiss the dialog after navigating
                dialog.dismiss()
            } else {
                // Handle login failure scenario (e.g., show error message)
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                // Optionally, you can clear fields or handle retry
            }
        }

        dialog.show()
    }


}
