package io.spixy.advancedmusicmanager.dagger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.spixy.advancedmusicmanager.MusicService
import io.spixy.advancedmusicmanager.Settings
import io.spixy.advancedmusicmanager.upload.UploadServer
import javax.inject.Singleton

@Module
class MusicModule(private val application: BaseApplication) {

    @Provides
    @Singleton
    fun provideMusicService(): Single<MusicService> {
        val subject = PublishSubject.create<MusicService>()
        val intent = Intent(application, MusicService::class.java)
        application.startService(intent)
        application.bindService(intent, object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) { }

            override fun onServiceConnected(p0: ComponentName, service: IBinder) {
                val binder = service as MusicService.LocalBinder
                subject.onNext(binder.getService())
                subject.onComplete()
            }
        }, Context.BIND_AUTO_CREATE)
        return subject.singleOrError().cache()
    }

    @Provides
    @Singleton
    fun provideUploadServer(): UploadServer{
        val uploadServer = UploadServer(application)
        uploadServer.getUploadFolder = {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
            val musicFolder = sharedPreferences.getString(Settings.MUSIC_FOLDER, "")
            if((arrayOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)) + application.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)).map { it.absolutePath }.contains(musicFolder)){
                musicFolder
            }else{
                ""
            }
        }
        return uploadServer
    }
}