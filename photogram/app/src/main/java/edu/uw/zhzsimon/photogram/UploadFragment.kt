package edu.uw.zhzsimon.photogram

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

data class ImageData(
        val downloadURL: String,
        val title: String,
        val uid: String,
        val likeOrNot: Map<String, Boolean>? //only store true value
) {
    constructor() : this ("", "", "", null)
}

class UploadFragment : Fragment() {
    private lateinit var database: DatabaseReference
    private lateinit var viewModel: FirebaseViewModel
    private lateinit var previewImage: ImageView
    private var imageUri: Uri = Uri.EMPTY
    private val SELECT_PHOTO_CODE = 3000
    private val TAG = "UploadFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_upload, container, false)
        previewImage = rootView.findViewById(R.id.image_preview)
        viewModel = ViewModelProvider(this).get(FirebaseViewModel::class.java)
        val authStateObserver = Observer<String> { authState ->
            if (authState == "Unauthenticated") {
                val action = UploadFragmentDirections.actionToImageGalleryFragment()
                findNavController().navigate(action)
            }
        }
        viewModel.authenticationState.observe(viewLifecycleOwner, authStateObserver)

        previewImage.setOnClickListener {
            pickPhoto()
        }

        val uploadButton = rootView.findViewById<Button>(R.id.upload_button)
        uploadButton.setOnClickListener {
            val title = rootView.findViewById<EditText>(R.id.image_title).text.toString()
            if (imageUri == Uri.EMPTY) {
                Toast.makeText(this.context, "Please select a photo", Toast.LENGTH_LONG).show()
            } else {
                uploadPhoto(title)
                val action = UploadFragmentDirections.actionToImageGalleryFragment()
                findNavController().navigate(action)
            }
        }

        return rootView
    }

    private fun uploadPhoto(title: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imageRef = storageRef.child("${imageUri.lastPathSegment}")
        Log.v(TAG, imageRef.toString())
        val task = imageRef.putFile(imageUri)
        task.addOnFailureListener {
            Toast.makeText(this.context, "Upload failed", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {
            val downloadUrl = it.storage.downloadUrl
            downloadUrl.addOnFailureListener() {
                Toast.makeText(this.context, "Cannot get download url", Toast.LENGTH_LONG).show()
            }.addOnSuccessListener {
                //Toast.makeText(this.context, "Download url is: ${downloadUrl.result}", Toast.LENGTH_LONG).show()
                database = Firebase.database.reference
                val newImage = ImageData(
                    downloadURL = downloadUrl.result.toString(),
                    title = title,
                    uid = Firebase.auth.currentUser!!.uid,
                    likeOrNot = mapOf(
                        Firebase.auth.currentUser!!.uid to true
                    )
                )
                database.push().setValue(newImage)
            }
        }
    }

    private fun pickPhoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            // Bring up gallery to select a photo
            startActivityForResult(intent, SELECT_PHOTO_CODE)
        } else {
            Log.d(TAG, "No supporting activity found")
        }
    }

    private fun loadFromUri(photoUri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT > 27) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, photoUri)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == SELECT_PHOTO_CODE) {
            val photoUri = data.data
            imageUri = photoUri!!
            val selectedImage = loadFromUri(photoUri)

            previewImage.setImageBitmap(selectedImage)
        }
    }
}