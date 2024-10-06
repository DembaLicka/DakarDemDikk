package com.example.dakardemdikk

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.view.animation.TranslateAnimation

class all_publication : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var topLayout: RelativeLayout
    lateinit var layoutbottom: ConstraintLayout
    private lateinit var nestedScrollView: NestedScrollView


    private var isLayoutBottomVisible = true // To track visibility of the layout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_publication)

        recyclerView = findViewById(R.id.recyclerView)
        topLayout = findViewById(R.id.layout_ascendant)
        layoutbottom = findViewById(R.id.layoutbottom)
        nestedScrollView = findViewById(R.id.nestedScrollView)

        recyclerView.layoutManager = LinearLayoutManager(this)


        loadMediaData()

        nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            // Si on scrolle vers le bas
            if (scrollY > oldScrollY && isLayoutBottomVisible) {
                hideLayoutBottom()
            }
            // Si on scrolle vers le haut
            else if (scrollY < oldScrollY && !isLayoutBottomVisible) {
                showLayoutBottom()
            }
        }
    }

    // Fonction pour cacher layoutbottom avec une animation
    private fun hideLayoutBottom() {
        layoutbottom.clearAnimation()
        val animate = TranslateAnimation(0f, 0f, 0f, layoutbottom.height.toFloat())
        animate.duration = 300
        layoutbottom.startAnimation(animate)
        layoutbottom.visibility = View.GONE
        isLayoutBottomVisible = false
    }

    private fun showLayoutBottom() {
        layoutbottom.clearAnimation()
        val animate = TranslateAnimation(0f, 0f, layoutbottom.height.toFloat(), 0f)
        animate.duration = 300
        layoutbottom.startAnimation(animate)
        layoutbottom.visibility = View.VISIBLE
        isLayoutBottomVisible = true
    }

    private fun loadMediaData() {
        val database = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val reference = database.getReference("data/$userId")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mediaList = mutableListOf<MediaData>()

                for (dataSnapshot in snapshot.children) {
                    val media = dataSnapshot.getValue(MediaData::class.java)
                    media?.let { mediaList.add(it) }
                }

                val adapter = MediaAdapter(mediaList)
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error: ${error.message}")
            }
        })
    }





}
