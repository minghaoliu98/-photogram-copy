package edu.uw.zhzsimon.photogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseUser

class FirebaseViewModel : ViewModel() {
    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            "Authenticated"
        } else {
            "Unauthenticated"
        }
    }
}