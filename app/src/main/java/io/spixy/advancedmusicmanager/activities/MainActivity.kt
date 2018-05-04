package io.spixy.advancedmusicmanager.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import io.spixy.advancedmusicmanager.*
import io.spixy.advancedmusicmanager.adapters.MusicListAdapter
import io.spixy.advancedmusicmanager.adapters.TagWrapper
import io.spixy.advancedmusicmanager.dagger.BaseApplication
import io.spixy.advancedmusicmanager.db.Track
import io.spixy.advancedmusicmanager.dialogs.ActionsListDialog
import io.spixy.advancedmusicmanager.dialogs.ConfirmDialog
import io.spixy.advancedmusicmanager.dialogs.TextDialog
import io.spixy.advancedmusicmanager.upload.UploadServer
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigInteger
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject lateinit var musicService: Single<MusicService>
    @Inject lateinit var uploadServer: UploadServer

    val musicListAdapter = MusicListAdapter(arrayListOf())
    val currentTrackList = BehaviorSubject.create<List<TrackFile>>().toSerialized()
    var filter = hashMapOf<Long, TagWrapper.Status>()
    var currentPlayListShowed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BaseApplication.component.inject(this)

        if(savedInstanceState != null){
            filter = savedInstanceState.getSerializable("FILTER") as HashMap<Long, TagWrapper.Status>
            currentPlayListShowed = savedInstanceState.getBoolean("CURRENT_PLAYLIST_SHOWED")
        }

        musicService.subscribe { musicService ->
            slide_mode.setOnClickListener {
                val intent = Intent(this, PlayActivity::class.java)
                startActivity(intent)
            }

            musicService.musicFilesPublish
                    .throttleLast(500, TimeUnit.MILLISECONDS)
                    .subscribe {
                        filterByTag()
                    }

            disposeOnDestroyComposite.add(musicService.trackRenamedPublish.subscribe{ (oldTrackFile, newTrackFile) ->
                val index = musicListAdapter.files.indexOfFirst { it == oldTrackFile }
                if(index != -1){
                    musicListAdapter.files[index] = newTrackFile
                    musicListAdapter.notifyDataSetChanged()
                }
            })

            disposeOnDestroyComposite.add(musicService.trackDeletedPublish.subscribe{ deletedTrackFile->
                musicListAdapter.files.remove(deletedTrackFile)
                musicListAdapter.notifyDataSetChanged()
            })
        }

        Observable.combineLatest<CharSequence, List<TrackFile>, List<TrackFile>>(
                RxTextView.textChanges(text_filter),
                currentTrackList,
                BiFunction { textFilter, tracks ->
                    tracks.filter { it.lowerCaseName.contains(textFilter.toString().toLowerCase()) }
                }
        ).observeOn(AndroidSchedulers.mainThread()).subscribe {
            musicListAdapter.files.clear()
            musicListAdapter.files.addAll(it)
            musicListAdapter.notifyDataSetChanged()
        }

        if(currentPlayListShowed){
            loadCurrentPlaylist()
        }else{
            filterByTag()
        }

        musicRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            musicService.subscribe { musicService ->
                adapter = musicListAdapter.apply {
                    clicks.subscribe {
                        musicService.play(it)
                        musicService.currentPlayList = musicListAdapter.files.toList()
                        val intent = Intent(this@MainActivity, PlayActivity::class.java)
                        startActivityForResult(intent, 0)
                    }

                    longClicks.subscribe {
                        ActionsListDialog(this@MainActivity, resources.getString(R.string.choose_action), arrayListOf(
                                Pair(resources.getString(R.string.track_action_rename), {
                                    TextDialog(this@MainActivity, resources.getString(R.string.track_rename_dialog_title), initialValue = it.name) { result ->
                                        musicService.renameTrack(it.name, result)
                                    }
                                }),
                                Pair(resources.getString(R.string.track_action_delete), {
                                    ConfirmDialog(this@MainActivity, resources.getString(R.string.file_delete_confirm)) {
                                        musicService.deleteTrack(it.name)
                                    }
                                })
                        ))
                    }
                    disposeOnDestroyComposite.add(musicService.playSubject.subscribe {
                        val index = files.indexOf(it)
                        if(index != -1){
                            this.currentTrack = it
                            notifyDataSetChanged()
                        }
                    })
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("FILTER", filter)
        outState.putBoolean("CURRENT_PLAYLIST_SHOWED", currentPlayListShowed)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun filterByTag(){
        musicService.subscribe { musicService->
            currentPlayListShowed = false
            val tracks = Track.filterByTag(filter)
            musicService.musicFiles.sortedWith(compareBy { it.lowerCaseName }).let { musicFiles ->
                val pathHashSet = tracks.map { it.path }.toHashSet()
                val trackFiles = musicFiles.filter { pathHashSet.contains(it.name) }
                currentTrackList.onNext(trackFiles)
            }
        }
    }

    private fun loadCurrentPlaylist(){
        musicService.subscribe { musicService->
            currentPlayListShowed = true
            currentTrackList.onNext(musicService.currentPlayList)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_filter_by_tag -> {
                val intent = Intent(this@MainActivity, FilterByTagsActivity::class.java)
                startActivityForResult(intent, 0)
                true
            }
            R.id.action_enable_upload_server -> {
                uploadServer.startOrProlong(60*5)

                val builder = AlertDialog.Builder(this)
                builder.setTitle(resources.getString(R.string.upload_server_started))
                val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


                builder.setMessage(resources.getString(
                        R.string.upload_server_short_info,
                        InetAddress.getByAddress(BigInteger.valueOf(Integer.reverseBytes(wifi.connectionInfo.ipAddress).toLong()).toByteArray()).hostAddress,
                        uploadServer.listeningPort.toString()
                ))

                builder.setPositiveButton("OK") { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                builder.show()
                true
            }
            R.id.action_reload_files -> {
                musicService.subscribe { musicService->
                    musicService.rescanFolders()
                }
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK && data != null){
            if(data.action == "OPEN_CURRENT_PLAYLIST"){
                loadCurrentPlaylist()
            }else{
                filter = data.getSerializableExtra(FilterByTagsActivity.RESULT_FILTER) as HashMap<Long, TagWrapper.Status>
                filterByTag()
            }
        }
    }

    private val disposeOnDestroyComposite = CompositeDisposable()
    override fun onDestroy() {
        super.onDestroy()
        disposeOnDestroyComposite.dispose()
    }
}
