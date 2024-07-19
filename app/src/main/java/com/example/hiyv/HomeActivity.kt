package com.example.hiyv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.hiyv.databinding.ActivityHomeBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val db = Firebase.firestore
    private val user = Firebase.auth.currentUser
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default fragment
        loadFragment(MailFragment(), "Child")

        // Set up listeners for both navbars
        setupNavbars()

        // Listen for changes in the "accountType" field
        user?.let { currentUser ->
            val docRef = db.collection("users").document(currentUser.uid)
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val accountType = snapshot.getString("accountType")
                    accountType?.let {
                        // Show the appropriate navbar based on account type
                        updateNavbars(it)
                    }
                    Log.d(TAG, "Current data: ${snapshot.data}")
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
        }
    }

    private fun setupNavbars() {
        binding.parentNavbar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_mail -> {
                    loadFragment(MailFragment(), "Parent")
                    true
                }
                R.id.navigation_tasks -> {
                    loadFragment(TasksFragment(), "Parent")
                    true
                }
                R.id.navigation_members -> {
                    loadFragment(MembersFragment(), "Parent")
                    true
                }
                R.id.navigation_logout -> {
                    signOut()
                    true
                }
                else -> false
            }
        }

        binding.childNavbar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_mail -> {
                    loadFragment(MailFragment(), "Child")
                    true
                }
                R.id.navigation_tasks -> {
                    loadFragment(TasksFragment(), "Child")
                    true
                }
                R.id.navigation_members -> {
                    loadFragment(MembersFragment(), "Child")
                    true
                }
                R.id.navigation_logout -> {
                    signOut()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateNavbars(accountType: String) {
        if (accountType == "Child") {
            binding.parentNavbar.visibility = View.GONE
            binding.childNavbar.visibility = View.VISIBLE
        } else {
            binding.childNavbar.visibility = View.GONE
            binding.parentNavbar.visibility = View.VISIBLE
        }
    }

    private fun loadFragment(fragment: Fragment, accountType: String) {
        val args = Bundle().apply {
            putString("ACCOUNT_TYPE", accountType)
        }
        fragment.arguments = args
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


    private fun signOut() {
        // Sign out from FirebaseAuth
        Firebase.auth.signOut()
        // Return to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showAccountTypeAlert(accountType: String) {
        Toast.makeText(this, "Account type: $accountType", Toast.LENGTH_SHORT).show()
    }
}
