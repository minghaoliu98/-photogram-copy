package edu.uw.zhzsimon.photogram

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase


class ImageGalleryFragment : Fragment() {
    private lateinit var viewModel: FirebaseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(
            R.layout.fragment_image_gallery,
            container, false
        )

        val floatActionButton =
            rootView.findViewById<FloatingActionButton>(R.id.floating_action_button) as FloatingActionButton
        viewModel = ViewModelProvider(this).get(FirebaseViewModel::class.java)

        // Set up recycler view
        val recycleView = rootView.findViewById<RecyclerView>(R.id.recycleview)
        recycleView.layoutManager = LinearLayoutManager(inflater.context)
        val testAdapter = MainAdapter(this)
        recycleView.adapter = testAdapter

        // Observe authentication state change as well as data change
        val authStateObserver = Observer<String> { authState ->
            if (authState == "Authenticated") {
                floatActionButton.show()
                testAdapter.notifyDataSetChanged()
                Log.v("test", Firebase.auth.currentUser.toString())
            } else {
                floatActionButton.hide()
                testAdapter.notifyDataSetChanged()
                Log.v("test", Firebase.auth.currentUser.toString())
            }
        }
        viewModel.authenticationState.observe(viewLifecycleOwner, authStateObserver)

        floatActionButton.setOnClickListener {
            val action = ImageGalleryFragmentDirections.actionToUploadFragment()
            it.findNavController().navigate(action)
        }

        return rootView
    }
}

// Adapter of the recycler view
class MainAdapter(lifecycleOwner: LifecycleOwner) : FirebaseRecyclerAdapter<ImageData,
        MainAdapter.ImageHolder>(buildOptions(lifecycleOwner)) {

    companion object {
        private fun buildOptions(lifecycleOwner: LifecycleOwner) =
            FirebaseRecyclerOptions.Builder<ImageData>()
                .setQuery(FirebaseDatabase.getInstance().reference, ImageData::class.java)
                .setLifecycleOwner(lifecycleOwner)
                .build()
    }

    class ImageHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.image_title_recycle)
        val image =
            view.findViewById<ImageView>(R.id.image_preview_recycle)
        val number = view.findViewById<TextView>(R.id.number_like)
        var button = view.findViewById<ImageButton>(R.id.like)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        return ImageHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_recyclerview_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int, data: ImageData) {
        val likeCount = holder.number
        val likeButton = holder.button

        // If there is no like
        if (data.likeOrNot.isNullOrEmpty()) {
            handleZeroLike(likeCount, likeButton, position, holder.view.context)
        } else {
            val list = data.likeOrNot.toMutableMap()
            if (Firebase.auth.currentUser != null) {
                val uid = Firebase.auth.currentUser!!.uid

                // Set images to star if the user likes this photo
                if (list.contains(uid)) {
                    likeButton.setImageResource(R.drawable.ic_baseline_star_24)
                    likeButton.tag = "liked"
                }
                likeButton.setOnClickListener {
                    if (likeButton.tag == "liked") {
                        dislikePhoto(likeButton, uid, list, likeCount)
                    } else {
                        likePhoto(likeButton, uid, list, likeCount)
                    }
                    updateList(position, list)
                }
            } else {
                likeButton.setImageResource(R.drawable.ic_baseline_add_24)
                holder.button.setOnClickListener {
                    Toast.makeText(
                        holder.view.context,
                        "Please login to like a photo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            likeCount.text = data.likeOrNot.size.toString()
        }
        holder.title.text = data.title
        Glide.with(holder.itemView.context)
            .load(data.downloadURL)
            .into(holder.image)
    }

    // If the user likes a photo, the number of like recevied of this photo is increased by 1.
    private fun likePhoto(likeButton: ImageButton, uid: String, list: MutableMap<String, Boolean>,
                          likeCount: TextView) {
        likeButton.tag = "liked"
        list[uid] = true
        likeButton.setImageResource(R.drawable.ic_baseline_star_24)
        likeCount.text = (likeCount.text.toString().toInt() + 1).toString()
    }

    // If the user dislikes a photo, the number of like recevied of this photo is decreased by 1.
    private fun dislikePhoto(likeButton: ImageButton, uid: String, list: MutableMap<String, Boolean>,
                             likeCount: TextView) {
        likeButton.tag = "0"
        list.remove(uid)
        likeButton.setImageResource(R.drawable.ic_baseline_add_24)
        likeCount.text = (likeCount.text.toString().toInt() - 1).toString()
    }

    // Update the databse
    private fun updateList(position: Int, list: MutableMap<String, Boolean>) {
        val ref = getRef(position).key
        // here is the part new list is the modified value
        val newList = list.toMap()
        val imageData = FirebaseDatabase.getInstance().getReference(ref!!).child("likeOrNot")
        imageData.setValue(newList)
    }

    private fun handleZeroLike(likeCount: TextView, likeButton: ImageButton,
                               position: Int, context: Context) {
        likeCount.text = "0"
        likeButton.setImageResource(R.drawable.ic_baseline_add_24)
        if (Firebase.auth.currentUser != null) {
            val list = mutableMapOf<String, Boolean>()
            val uid = Firebase.auth.currentUser!!.uid
            likeButton.setOnClickListener {
                likePhoto(likeButton, uid, list, likeCount)
                updateList(position, list)
            }
        } else {
            likeButton.setOnClickListener {
                Toast.makeText(
                    context,
                    "Please login to like a photo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}