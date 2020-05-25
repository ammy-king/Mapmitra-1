package com.mapmitra.mapmitra

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        title = getString(R.string.loginTitle)

        val auth = FirebaseAuth.getInstance()


        if (auth.currentUser != null) {
            goMainActivity()
        }

        txtSignUp.setOnClickListener {
            goRegisterActivity()
        }

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Password cannot be Empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            progressBar.visibility = VISIBLE
            //Firebase Authentication

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                btnLogin.isEnabled = true
                progressBar.visibility = GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                    goMainActivity()

                } else {
                    Log.e("TAG", "SignInFailed", task.exception)
                    Toast.makeText(this, "SignInFailed", Toast.LENGTH_SHORT).show()
                }
            }


        }


    }

    private fun goMainActivity() {

        Log.i("TAG", "goMainActivity")
        val intentMain = Intent(this, MainActivity::class.java)
        startActivity(intentMain)
        finish()
    }

    private fun goRegisterActivity() {
        Log.i("TAG", "goRegisterActivity")
        val intentReg = Intent(this, RegisterActivity::class.java)
        startActivity(intentReg)


    }
}