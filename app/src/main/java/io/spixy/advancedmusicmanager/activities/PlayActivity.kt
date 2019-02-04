package io.spixy.advancedmusicmanager.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.spixy.advancedmusicmanager.MusicService
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.adapters.TrackPagerAdapter
import io.spixy.advancedmusicmanager.dagger.BaseApplication
import io.spixy.advancedmusicmanager.dialogs.ConfirmDialog
import io.spixy.advancedmusicmanager.dialogs.TextDialog
import kotlinx.android.synthetic.main.activity_play.*
import javax.inject.Inject

class PlayActivity : AppCompatActivity() {
    @Inject lateinit var musicService:Single<MusicService>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        BaseApplication.component.inject(this)

        musicService.subscribe { musicService->
            viewPager.adapter = TrackPagerAdapter(supportFragmentManager, musicService.currentPlayList)
            disposeOnDestroyComposite.add(musicService.playSubject.subscribe { currentTrack ->
                viewPager.setCurrentItem(musicService.currentPlayList.indexOf(currentTrack), false)
            })
            disposeOnDestroyComposite.add(musicService.trackRenamedPublish.subscribe {
                val currentItem = viewPager.currentItem
                viewPager.adapter = TrackPagerAdapter(supportFragmentManager, musicService.currentPlayList)
                viewPager.setCurrentItem(currentItem, false)
            })
            disposeOnDestroyComposite.add(musicService.trackDeletedPublish.subscribe {
                val currentItem = viewPager.currentItem
                viewPager.adapter = TrackPagerAdapter(supportFragmentManager, musicService.currentPlayList)
                viewPager.setCurrentItem(currentItem, false)
            })
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) { }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }

                override fun onPageSelected(position: Int) {
                    musicService.play(musicService.currentPlayList[position])
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.play_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_shuffle -> {
                musicService.subscribe { musicService->
                    val newCurrentPlayList = musicService.currentPlayList.toMutableList()
                    if(musicService.playSubject.hasValue()){
                        val currentPlay = musicService.playSubject.value
                        if(currentPlay != null) {
                            newCurrentPlayList.remove(currentPlay)
                            newCurrentPlayList.shuffle()
                            newCurrentPlayList.add(0, currentPlay)
                            viewPager.adapter = TrackPagerAdapter(supportFragmentManager, newCurrentPlayList)
                            musicService.currentPlayList = newCurrentPlayList
                            viewPager.setCurrentItem(0, false)
                        }
                    }
                }
                true
            }
            R.id.action_open_current_playlist -> {
                setResult(Activity.RESULT_OK, Intent("OPEN_CURRENT_PLAYLIST"))
                finish()
                true
            }
            R.id.action_rename -> {
                musicService.subscribe { musicService->
                    (viewPager.adapter as? TrackPagerAdapter)?.apply {
                        if(files.size>viewPager.currentItem){
                            val currentTrackName = files[viewPager.currentItem].name
                            TextDialog(this@PlayActivity, resources.getString(R.string.track_rename_dialog_title), initialValue = currentTrackName) { result ->
                                musicService.renameTrack(currentTrackName, result)
                            }
                        }
                    }
                }
                true
            }
            R.id.action_delete -> {
                musicService.subscribe { musicService ->
                    (viewPager.adapter as? TrackPagerAdapter)?.apply {
                        if(files.size>viewPager.currentItem){
                            ConfirmDialog(this@PlayActivity, resources.getString(R.string.file_delete_confirm)) {
                                musicService.deleteTrack(files[viewPager.currentItem].name)
                            }
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val disposeOnDestroyComposite = CompositeDisposable()
    override fun onDestroy() {
        super.onDestroy()
        disposeOnDestroyComposite.dispose()
    }
}
