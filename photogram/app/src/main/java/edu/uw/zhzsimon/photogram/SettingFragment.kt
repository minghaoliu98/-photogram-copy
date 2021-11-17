package edu.uw.zhzsimon.photogram

import android.content.SharedPreferences
import android.os.Bundle
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat

class SettingFragment : PreferenceFragmentCompat(),  OnSharedPreferenceChangeListener{

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val background = requireActivity().findViewById<LinearLayout>(R.id.background)
        if (key == "color_purple") {
            if (sharedPreferences.getBoolean(key, false)) {
                background.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
            } else {
                background.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}