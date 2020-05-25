package com.mapmitra.mapmitra

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapmitra.mapmitra.models.User

import kotlinx.android.synthetic.main.activity_profile.*


class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        btnLogOut.setOnClickListener {
            Log.i("ProfileActivity","User Wants to Logout")
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }

        val firestoreDB = FirebaseFirestore.getInstance()
        firestoreDB.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                var signedInUser = userSnapshot.toObject(User::class.java)

                tvname.text = signedInUser?.name?.toUpperCase()
                tvEmail.text = signedInUser?.eMail
                tvMobile.text = signedInUser?.mobile.toString()
                Log.i("ProfileActivity","Signed in as $signedInUser")
            }
            .addOnFailureListener{exception->
                Log.e("ProfileAcrivity","Failure in Fetching Signed in User",exception)
            }

        tvChangeName.setOnClickListener { update("name") }
        tvChangeMobile.setOnClickListener { update("mobile") }
        tvChangeEmail.setOnClickListener { update("enail") }



    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater: MenuInflater = menuInflater
        menuInflater.inflate(R.menu.menu_profile_activity , menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.menu_about_us) {
            Log.i("ProfileActivity" , "User Wants to know about You")
            startActivity(Intent(this , AboutUs::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    private fun update(type:String){
        //StartActivity(Intent(this,//UpdateActivity::class.java))
    }

}