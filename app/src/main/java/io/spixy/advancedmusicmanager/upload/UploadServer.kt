package io.spixy.advancedmusicmanager.upload

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import io.reactivex.subjects.PublishSubject
import io.spixy.advancedmusicmanager.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.util.HashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UploadServer(val context: Context) : NanoHTTPD(9144) {
    val uploadedFiles = PublishSubject.create<File>().toSerialized()
    private var started = false
    private var closeNanoTime = 0L

    init {
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay({
            synchronized(this){
                if(System.nanoTime() > closeNanoTime && started){
                    started = false
                    closeAllConnections()
                    stop()
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS)
    }

    fun startOrProlong(seconds: Long){
        synchronized(this){
            if(!started){
                started = true
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            closeNanoTime = System.nanoTime() + seconds*1000000000L
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        val method = session.method
        if (Method.PUT == method || Method.POST == method) {
            try {
                session.parseBody(files)
            } catch (ioe: IOException) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.message)
            } catch (re: ResponseException) {
                return newFixedLengthResponse(re.status, NanoHTTPD.MIME_PLAINTEXT, re.message)
            }
        }

        when(session.uri){
            "/dropzone.js" -> return newFixedLengthResponse(context.assets.open("dependencies/dropzone.js").bufferedReader().use { it.readText() })
            "/dropzone.css" -> return newFixedLengthResponse(context.assets.open("dependencies/dropzone.css").bufferedReader().use { it.readText() })
            "/upload" -> {
                val musicFolder = getUploadFolder()
                if(musicFolder!=""){
                    session.parms["encodedFileName"]?.let { fileName->
                        val newFile = File(musicFolder, URLDecoder.decode(fileName, "UTF-8"))
                        if(newFile.absolutePath.endsWith(".mp3")){
                            files["file"]?.let { tmpFile ->
                                moveFile(File(tmpFile), newFile)
                                if(newFile.exists()){
                                    uploadedFiles.onNext(newFile)
                                    startOrProlong(60*5)
                                }
                            }
                        }
                    }
                }
                return newFixedLengthResponse("")
            }
            else -> {
                val musicFolder = getUploadFolder()
                return if(musicFolder != ""){
                    newFixedLengthResponse(context.assets.open("index.html").bufferedReader().use { it.readText() })
                }else{
                    newFixedLengthResponse(context.resources.getString(R.string.music_folder_not_set))
                }
            }
        }
    }

    var getUploadFolder = { "" }

    private fun moveFile(file: File, newFile: File) {
        FileOutputStream(newFile).channel.use { outputChannel ->
            FileInputStream(file).channel.use { inputChannel ->
                inputChannel.transferTo(0, inputChannel.size(), outputChannel)
                inputChannel.close()
                file.delete()
            }
        }
    }
}