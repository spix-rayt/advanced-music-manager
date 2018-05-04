package io.spixy.advancedmusicmanager.activities

import android.app.Activity
import android.os.Bundle
import io.spixy.advancedmusicmanager.fragments.SettingsFragment


class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }
}