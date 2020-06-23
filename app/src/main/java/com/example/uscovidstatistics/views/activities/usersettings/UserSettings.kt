package com.example.uscovidstatistics.views.activities.usersettings

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.example.uscovidstatistics.databinding.ActivitySettingsBinding
import com.example.uscovidstatistics.views.dialogs.BottomDialog
import com.example.uscovidstatistics.views.activities.BaseActivity
import com.example.uscovidstatistics.views.activities.SettingsFragment
import kotlinx.android.synthetic.main.app_toolbar.view.*

class UserSettings : BaseActivity(), ViewBinding {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(root)

        supportFragmentManager
            .beginTransaction()
            .replace(binding.settingsFragment.id, SettingsFragment())
            .commit()

        setNavOptions()
    }

    private fun setNavOptions() {
        binding.regionBackBtn.setOnClickListener {
            onBackPressed()
        }
    }

    override fun getRoot(): View {
        return binding.root
    }
}
