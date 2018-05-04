package io.spixy.advancedmusicmanager.fragments

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.preference.ListPreference
import android.preference.PreferenceFragment
import io.reactivex.Single
import io.spixy.advancedmusicmanager.MusicService
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.Settings
import io.spixy.advancedmusicmanager.dagger.BaseApplication
import java.math.BigDecimal
import javax.inject.Inject


class SettingsFragment : PreferenceFragment() {
    @Inject lateinit var musicService: Single<MusicService>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseApplication.component.inject(this)

        addPreferencesFromResource(R.xml.preferences)

        (findPreference(Settings.MUSIC_FOLDER) as? ListPreference)?.let { musicFolderPreference ->
            updateMusicFolderPreferenceData(musicFolderPreference)
            musicFolderPreference.setOnPreferenceClickListener {
                updateMusicFolderPreferenceData(musicFolderPreference)
                true
            }
        }
    }

    private fun updateMusicFolderPreferenceData(preference: ListPreference){
        val dirs = (arrayOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)) + activity.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)).map {
            Pair(it, StatFs(it.absolutePath))
        }

        preference.entries = dirs.map {
            "${BigDecimal(it.second.freeBytes/1024.0/1024.0/1024.0).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()} GiB / ${BigDecimal(it.second.totalBytes/1024.0/1024.0/1024.0).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()} GiB\n${it.first.absolutePath}"
        }.toTypedArray()

        preference.entryValues = dirs.map { it.first.absolutePath }.toTypedArray()

        preference.setDefaultValue(dirs.last().first.absolutePath)
    }
}