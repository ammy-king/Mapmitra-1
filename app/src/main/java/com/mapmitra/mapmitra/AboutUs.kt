package com.mapmitra.mapmitra

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mapmitra.mapmitra.models.Developers
import kotlinx.android.synthetic.main.activity_about_us.*

private const val TAG = "AboutUs"

class AboutUs : AppCompatActivity() {

    lateinit var firestoreDB: FirebaseFirestore
    lateinit var Devs: MutableList<Developers>
    lateinit var adapter: DevsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        Devs = mutableListOf()
        adapter = DevsAdapter(this , Devs)

        rvDeveloper.adapter = adapter
        rvDeveloper.layoutManager = LinearLayoutManager(this)


        firestoreDB = FirebaseFirestore.getInstance()


        val DevReference = firestoreDB
            .collection("Developers")
            .orderBy("Name" , Query.Direction.DESCENDING)


        DevReference.addSnapshotListener { snapshot , exception ->
            if (exception != null || snapshot == null) {
                Log.e(TAG , "Exception while Fetching Developers" , exception)
                return@addSnapshotListener
            }
            val devsList = snapshot.toObjects(Developers::class.java)
            Devs.clear()
            Devs.addAll(devsList)
            adapter.notifyDataSetChanged()
            for (dev in devsList) {
                Log.i(TAG , "Developer $dev, ${dev.Email} ")
            }
        }
    }
}
