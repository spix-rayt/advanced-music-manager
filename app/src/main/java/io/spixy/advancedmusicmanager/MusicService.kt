package io.spixy.advancedmusicmanager

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.spixy.advancedmusicmanager.dagger.BaseApplication
import io.spixy.advancedmusicmanager.db.Track
import io.spixy.advancedmusicmanager.upload.UploadServer
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MusicService : Service() {
    val mediaPlayer = MediaPlayer()
    val musicFiles = arrayListOf<TrackFile>()
    val musicFileNamesSet = hashSetOf<String>()
    var currentPlayList = listOf<TrackFile>()

    val musicFilesPublish = PublishSubject.create<TrackFile>()
    val playSubject = BehaviorSubject.create<TrackFile>()
    val progressPlaySubject = BehaviorSubject.create<Double>().toSerialized()
    val trackRenamedPublish = PublishSubject.create<Pair<TrackFile, TrackFile>>()
    val trackDeletedPublish = PublishSubject.create<TrackFile>()
    val playingStateChanges = BehaviorSubject.create<Boolean>().toSerialized()
    val audioManager: AudioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    @Inject lateinit var uploadServer: UploadServer

    private var trackDuration = 0

    override fun onBind(p0: Intent): IBinder? {
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    private val tracksInDB = Observable.fromCallable({
        Track.fetchAll().map { it.path to it }.toMap()
    })

    override fun onCreate() {
        super.onCreate()
        BaseApplication.component.inject(this)

        rescanFolders()

        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay({
            synchronized(mediaPlayer){
                if(trackDuration != 0){
                    progressPlaySubject.onNext(mediaPlayer.currentPosition.toDouble() / trackDuration)
                }
            }
        }, 250, 250, TimeUnit.MILLISECONDS)

        mediaPlayer.setOnCompletionListener { playNext() }

        registerReceiver(object : BroadcastReceiver(){
            override fun onReceive(context: Context, intent: Intent) {
                if(AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action){
                    pause()
                }
            }
        }, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    var disposable: Disposable? = null
    fun rescanFolders(){
        disposable?.dispose()
        musicFiles.clear()
        musicFileNamesSet.clear()
        val tracksFromDisk = Observable.fromCallable {
            val defaultMusicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val externalFilesDirs = getExternalFilesDirs(Environment.DIRECTORY_MUSIC)
            externalFilesDirs.forEach { if(!it.exists()) it.mkdirs() }
            val defaultMusicFolders = if (defaultMusicFolder.exists()) {
                defaultMusicFolder.walk(FileWalkDirection.TOP_DOWN).filter { it.isDirectory }.toList()
            } else {
                emptyList()
            }
            val dirs = externalFilesDirs + defaultMusicFolders
            dirs.flatMap { it.listFiles().toList() }.filter { it.isFile && it.name.endsWith(".mp3") }.map { file -> TrackFile(file) }
        }

        tracksInDB.subscribe { tracks ->
            disposable = tracksFromDisk
                    .flatMap { Observable.fromIterable(it) }
                    .mergeWith(uploadServer.uploadedFiles.map { TrackFile(it) })
                    .subscribeOn(Schedulers.newThread())
                    .subscribe { newFile ->
                        if (!musicFileNamesSet.contains(newFile.name)) {
                            if (!tracks.containsKey(newFile.name)) {
                                Track().apply {
                                    path = newFile.name
                                }.save()
                            }
                            musicFiles.add(newFile)
                            musicFileNamesSet.add(newFile.name)
                            musicFilesPublish.onNext(newFile)
                        }
                    }
        }
    }

    val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { i ->
        when(i){
            AudioManager.AUDIOFOCUS_LOSS -> pause()
        }
    }

    fun play(track: TrackFile){
        if(!playSubject.hasValue() || playSubject.value != track){
            synchronized(mediaPlayer) {
                trackDuration = 0
                mediaPlayer.reset()
            }
            mediaPlayer.setDataSource(applicationContext, Uri.fromFile(track.file))
            mediaPlayer.setOnPreparedListener { mediaPlayer ->
                trackDuration = mediaPlayer.duration
                start()
                playSubject.onNext(track)
            }
            mediaPlayer.prepareAsync()
        }
    }

    fun start(){
        if(!mediaPlayer.isPlaying){
            val result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                mediaPlayer.start()
                playingStateChanges.onNext(true)
            }
        }
    }

    fun pause(){
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            audioManager.abandonAudioFocus(audioFocusChangeListener)
            playingStateChanges.onNext(false)
        }
    }

    fun playNext(){
        if(playSubject.hasValue()){
            val currentTrack = playSubject.value
            val currentPlayIndex = currentPlayList.indexOfFirst { currentTrack == it }
            if(currentPlayList.size > 0){
                play(currentPlayList[(currentPlayIndex+1) % currentPlayList.size])
            }
        }
    }

    fun renameTrack(oldName:String, newName:String){
        musicFiles.find { it.name == oldName }?.let { oldTrackFile ->
            val newFile = File(oldTrackFile.file.parent + File.separator + newName)
            if(!newFile.exists()){
                if(oldTrackFile.file.renameTo(newFile)){
                    Track.delete(newName)
                    val track = Track.fetchWithPath(oldName)
                    track.path = newName
                    track.save()
                    val index = musicFiles.indexOfFirst { it == oldTrackFile }
                    if(index!=-1){
                        val newTrackFile = TrackFile(newFile)
                        musicFiles[index] = newTrackFile
                        currentPlayList = currentPlayList.toMutableList().let {
                            it[it.indexOf(oldTrackFile)] = newTrackFile
                            it
                        }
                        trackRenamedPublish.onNext(Pair(oldTrackFile, newTrackFile))
                    }
                }
            }
        }
    }

    fun deleteTrack(name:String){
        val files = musicFiles.filter { it.name == name }
        if(files.isNotEmpty()){
            Track.delete(name)
        }
        if(playSubject.hasValue()){
            if(playSubject.value.name == name){
                playNext()
            }
        }
        currentPlayList = currentPlayList.toMutableList().apply {
            removeAll { it.name == name }
        }
        files.forEach {
            it.file.delete()
            trackDeletedPublish.onNext(it)
        }
    }
}