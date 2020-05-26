package com.mapmitra.mapmitra

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity


class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        //Code to start timer and take action after the timer ends
        Handler().postDelayed({ //Do any action here. Now we are moving to next page
            val mySuperIntent = Intent(this@StartActivity , LoginActivity::class.java)
            startActivity(mySuperIntent)

            //This 'finish()' is for exiting the app when back button pressed from Home page which is ActivityHome
            finish()
        } , 3000)
    }
}