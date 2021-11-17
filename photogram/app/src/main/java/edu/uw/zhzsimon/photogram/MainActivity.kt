package edu.uw.zhzsimon.photogram

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    private lateinit var itemLogIn: MenuItem
    private lateinit var itemLogOut: MenuItem
    private val LOG_IN_CODE = 1000

    private lateinit var navController: NavController

    private val COLOR_KEY = "color"
    private var mPreferences: SharedPreferences? = null
    private var mColor = 0

    private lateinit var background: LinearLayout

    // Name of shared preferences file
    private var sharedPrefFile: String? = "edu.uw.zhzsimon.photogram"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        navController = navHostFragment.findNavController()
        setupActionBarWithNavController(navController)

        mColor = ContextCompat.getColor(this, R.color.white)
        background = findViewById(R.id.background)
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE)
        if (mPreferences != null) {
            mColor = mPreferences!!.getInt(COLOR_KEY, mColor)
            background.setBackgroundColor(mColor)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        itemLogIn = menu!!.findItem(R.id.menu_item_log_in)
        itemLogOut = menu.findItem(R.id.menu_item_log_out)
        return true
    }

    // Handle behaviors of menu item when selected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_log_in -> {
                startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false).build(),
                    LOG_IN_CODE
                )
                true
            }
            R.id.setting_fragment -> {
                item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
            }
            R.id.menu_item_log_out -> {
                AuthUI.getInstance().signOut(this).addOnCompleteListener {
                    invalidateOptionsMenu()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Display the menu
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (FirebaseAuth.getInstance().currentUser == null) {
            itemLogIn.isVisible = true
            itemLogOut.isVisible = false
        } else {
            itemLogIn.isVisible = false
            itemLogOut.isVisible = true
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Update menu if user has successfully login
        if (requestCode == LOG_IN_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                invalidateOptionsMenu()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Store the current background color
        val preferencesEditor: SharedPreferences.Editor? = mPreferences?.edit()
        val drawable = background.background as ColorDrawable
        mColor = drawable.color
        preferencesEditor?.putInt(COLOR_KEY, mColor)
        preferencesEditor?.apply()
    }
}
