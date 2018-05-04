package io.spixy.advancedmusicmanager.adapters

import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.TrackFile
import kotlinx.android.synthetic.main.music_list_item_view.view.*
import org.jetbrains.anko.backgroundColor

class MusicListAdapter(val files: MutableList<TrackFile>): RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {
    val clicks = PublishSubject.create<TrackFile>()
    val longClicks = PublishSubject.create<TrackFile>()
    var currentTrack:TrackFile? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.music_list_item_view, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            tag_name.text = files[position].name
            tag_name.setOnClickListener {
                clicks.onNext(files[position])
            }
            tag_name.setOnLongClickListener {
                longClicks.onNext(files[position])
                true
            }
            backgroundColor = if(files[position] == currentTrack){
                ResourcesCompat.getColor(resources, R.color.colorAccent,null)
            }else{
                ResourcesCompat.getColor(resources, R.color.colorWhite,null)
            }
        }
    }

    override fun getItemCount() = files.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}