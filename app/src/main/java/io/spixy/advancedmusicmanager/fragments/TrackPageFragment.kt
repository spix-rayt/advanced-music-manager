package io.spixy.advancedmusicmanager.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.spixy.advancedmusicmanager.MusicService
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.TrackFile
import io.spixy.advancedmusicmanager.activities.TagsActivity
import io.spixy.advancedmusicmanager.dagger.BaseApplication
import kotlinx.android.synthetic.main.track_page.*
import javax.inject.Inject


class TrackPageFragment : Fragment() {
    @Inject lateinit var musicService: Single<MusicService>

    var trackFile: TrackFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseApplication.component.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.track_page, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        musicService.subscribe { musicService->
            (arguments[ARGUMENT_NAME_TRACK] as? String)?.let { trackName->
                trackFile = musicService.musicFiles.first { it.name == trackName }
            }

            var seekUpdateSubscribe: Disposable? = null

            disposeOnDestroyComposite.add(musicService.playSubject.subscribe {
                if (it == trackFile) {
                    seek.visibility = View.VISIBLE
                    seekUpdateSubscribe = musicService.progressPlaySubject.subscribe { progress ->
                        seek?.let {
                            it.post{ it.progress = (progress*10000).toInt() }
                        }
                    }
                    seekUpdateSubscribe?.let { disposeOnDestroyComposite.add(it) }
                } else {
                    seek.visibility = View.INVISIBLE
                    seekUpdateSubscribe?.dispose()
                }
            })

            disposeOnDestroyComposite.add(musicService.playingStateChanges.subscribe { playing ->
                if(playing){
                    image_track_control.setImageResource(R.drawable.ic_pause_black)
                }else{
                    image_track_control.setImageResource(R.drawable.ic_play_arrow_black)
                }
            })

            seek.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) { }

                override fun onStartTrackingTouch(p0: SeekBar?) { }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    musicService.mediaPlayer.apply {
                        seekTo((seekBar.progress/10000.0*duration).toInt())
                    }
                }
            })
        }
        updateInfo()
        tags.setOnClickListener {
            musicService.subscribe { musicService ->
                if(musicService.mediaPlayer.isPlaying){
                    musicService.pause()
                }else{
                    musicService.start()
                }
            }
        }

        tags.setOnLongClickListener { _ ->
            trackFile?.let { track ->
                val intent = Intent(context, TagsActivity::class.java)
                intent.putExtra(TagsActivity.INTENT_DATA_NAME_PATH, track.name)
                startActivityForResult(intent, 0)
            }
            true
        }
    }

    private fun updateInfo(){
        trackFile?.let { track ->
            track_name.text = track.name
            tags.text = track.getTags().joinToString(separator = ", ") { it.name }
        }
    }


    private val disposeOnDestroyComposite = CompositeDisposable()
    override fun onDestroy() {
        super.onDestroy()
        disposeOnDestroyComposite.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        updateInfo()
    }

    companion object {
        val ARGUMENT_NAME_TRACK = "TRACK"
    }
}