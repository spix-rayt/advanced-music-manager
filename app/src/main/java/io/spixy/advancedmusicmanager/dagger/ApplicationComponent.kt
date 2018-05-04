package io.spixy.advancedmusicmanager.dagger

import dagger.Component
import io.spixy.advancedmusicmanager.MusicService
import io.spixy.advancedmusicmanager.fragments.TrackPageFragment
import io.spixy.advancedmusicmanager.activities.FilterByTagsActivity
import io.spixy.advancedmusicmanager.activities.MainActivity
import io.spixy.advancedmusicmanager.activities.PlayActivity
import io.spixy.advancedmusicmanager.fragments.SettingsFragment
import javax.inject.Singleton

@Singleton
@Component(modules = [(MusicModule::class)])
interface ApplicationComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(filterByTagsActivity: FilterByTagsActivity)
    fun inject(playActivity: PlayActivity)
    fun inject(trackPageFragment: TrackPageFragment)
    fun inject(musicService: MusicService)
    fun inject(settingsFragment: SettingsFragment)
}