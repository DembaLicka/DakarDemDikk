package com.example.dakardemdikk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class MainActivity2 : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var namelieu : TextView
    lateinit var target : RelativeLayout
    lateinit var signaler : RelativeLayout
    lateinit var rela1 : RelativeLayout
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_MEDIA_PICK = 1
    private var isVideo: Boolean = false
    private var videoUri: Uri? = null
    private lateinit var imagedulieu: ImageView
    private lateinit var imagechoisie: ImageView
    lateinit var videoView: VideoView
    lateinit var progressBar : ProgressBar

    private lateinit var valider: ImageView
    private lateinit var cancel: RelativeLayout
    private lateinit var ok: RelativeLayout
    private lateinit var editTextDescription: EditText
    private var imageUri: Uri? = null
    private var selectedUri: Uri? = null

    private lateinit var storageReference: StorageReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        mAuth = FirebaseAuth.getInstance()

        namelieu = findViewById(R.id.latlong)
        target = findViewById(R.id.target)
        signaler = findViewById(R.id.rela2)
        imagedulieu = findViewById(R.id.imagedulieu)
        rela1 = findViewById(R.id.rela1)

        storageReference = FirebaseStorage.getInstance().reference
        target.setOnClickListener {
            moveToCurrentLocation()
        }
        rela1.setOnClickListener {
            val intent = Intent(this , all_publication::class.java)
            startActivity(intent)
        }

        signaler.setOnClickListener {
           showCustomDialog()
        }

        if (!Places.isInitialized()){
            Places.initialize(applicationContext, "AIzaSyArQQSYw7uI2PErSntnx_0i8e8vITToO5Q") // Replace with your API key
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(
            CollectionUtils.listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // When a place is selected, move the map and add a marker
                val latLng = place.latLng
                if (latLng != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    googleMap.addMarker(MarkerOptions().position(latLng).title(place.name))
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("MapsActivity", "An error occurred: $status")
            }
        })

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val textView = findViewById<TextView>(R.id.name)

        val auth = Firebase.auth
        val user = auth.currentUser

        if (user != null) {
            val userName = user.displayName
            textView.text = "Welcome, " + userName
        } else {

        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // Check location permissions

        val defaultLocation = LatLng(14.69278, -17.44672)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        moveToCurrentLocation()


    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                openGallery()
                onMapReady(googleMap) // Call this method again to get the location
            } else {
                // Permission denied
                Log.e("MapsActivity", "Location permission denied")
            }
        }
    }
    private fun moveToCurrentLocation() {
        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Get the last known location and move the camera there
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    // Animate camera movement to the user's current location
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f), 2000, null)

                    // Add marker with a slight animation effect (Optional)
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Vous etes ici")
                    )

                    // Get the address and update the TextView
                    getAddressFromLocation(location)
                } else {
                    Log.e("MapsActivity", "Location is null")
                }
            }
        } else {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }
    private fun getAddressFromLocation(location: Location) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address> =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)!!
            if (addresses != null) {
                val address = addresses[0]
                val thoroughfare = address.thoroughfare ?: "" // Rue
                val subLocality = address.subLocality ?: "" // Quartier
                val locality = address.locality ?: "" // Région
                val country = address.countryName ?: "" // Pays

                // Construction de l'adresse complète
                var fullAddress = thoroughfare
                if (locality!= null) fullAddress += " $locality"
                if (country!= null) fullAddress += ", $country"

                namelieu.text = if (fullAddress!= null) fullAddress else "Adresse non disponible"

            } else {
                Log.e("MapsActivity", "No address found")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Accepter tous les types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) // Limiter aux images et vidéos
        startActivityForResult(intent, REQUEST_MEDIA_PICK)
    }
    @SuppressLint("SuspiciousIndentation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val mimeType = contentResolver.getType(uri)
                selectedUri = uri
                if (mimeType?.startsWith("video/") == true) {
                    videoUri = uri
                    isVideo = true
                    videoView.setVideoURI(videoUri)
                    videoView.visibility = View.VISIBLE
                    videoView.start() // Démarre la vidéo immédiatement
                    imagechoisie.visibility = View.GONE
                } else {
                    imagechoisie.setImageURI(uri)
                    isVideo = false
                    imagechoisie.visibility = View.VISIBLE
                    videoView.visibility = View.GONE
                }
            }
        }
    }
    private fun uploadData(isVideo: Boolean) {
        // Récupérer le texte depuis l'EditText
        val description = editTextDescription.text.toString()

        val storageRef = FirebaseStorage.getInstance().reference
        val userId = mAuth.currentUser?.uid ?: "unknown_user"

        if (isVideo) {
            // Gérer l'envoi de la vidéo
            val videoRef = storageRef.child("videos/$userId/${UUID.randomUUID()}.mp4")

            // Vérifier que videoUri n'est pas null
            videoUri?.let { uri ->
                videoRef.putFile(uri).addOnSuccessListener {
                    videoRef.downloadUrl.addOnSuccessListener { uri ->
                        val videoUrl = uri.toString()
                        saveDataToDatabase(null, videoUrl, description, true)
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de l'envoi de la vidéo", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Aucune vidéo sélectionnée", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Gérer l'envoi de l'image
            selectedUri?.let { uri ->
                imagechoisie.isDrawingCacheEnabled = true
                imagechoisie.buildDrawingCache()
                val bitmap = (imagechoisie.drawable as BitmapDrawable).bitmap

                // Convertir le bitmap en tableau d'octets
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                val imageRef = storageRef.child("images/$userId/${UUID.randomUUID()}.jpg")

                imageRef.putBytes(data).addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        saveDataToDatabase(imageUrl, null, description, false)
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de l'envoi de l'image", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Aucune image sélectionnée", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveDataToDatabase(imageUrl: String?, videoUrl: String?, description: String, isVideo: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("data")
        val userId = mAuth.currentUser?.uid

        if (userId != null) {
            val dataRef = reference.child(userId).push()
            val dataMap = mapOf(
                "imageUrl" to imageUrl,
                "videoUrl" to videoUrl,
                "description" to description,
                "isVideo" to isVideo
            )
            dataRef.setValue(dataMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.INVISIBLE
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de l'envoi des données", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showCustomDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom, null)
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)


        val dialog = builder.create()

        // Access UI elements in the dialog
         editTextDescription = dialogView.findViewById(R.id.textInputLayoutDescription)
        imagechoisie = dialogView.findViewById<ShapeableImageView>(R.id.imagechoisie)
         videoView = dialogView.findViewById(R.id.videoView)
         valider = dialogView.findViewById(R.id.valider)
         progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        cancel = dialogView.findViewById(R.id.cancel)
         ok = dialogView.findViewById(R.id.ok)

        ok.setOnClickListener {

            progressBar.visibility = View.VISIBLE

            uploadData(isVideo)
        }

        cancel.setOnClickListener {
            // Handle Cancel button click
            dialog.dismiss()
        }

        valider.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
            } else {
                openGallery()
            }
        }
        dialog.show()
    }

}
