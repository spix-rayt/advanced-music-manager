package io.spixy.advancedmusicmanager.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import io.spixy.advancedmusicmanager.TrackFile
import io.spixy.advancedmusicmanager.fragments.TrackPageFragment
import org.jetbrains.anko.support.v4.withArguments

class TrackPagerAdapter(fm: FragmentManager, val files: List<TrackFile>): FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return TrackPageFragment().withArguments(Pair(TrackPageFragment.ARGUMENT_NAME_TRACK, files[position].name))
    }

    override fun getCount(): Int {
        return files.size
    }
}