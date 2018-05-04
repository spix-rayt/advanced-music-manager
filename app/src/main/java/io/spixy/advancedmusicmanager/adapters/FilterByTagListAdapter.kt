package io.spixy.advancedmusicmanager.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.spixy.advancedmusicmanager.*
import io.spixy.advancedmusicmanager.db.Tag
import kotlinx.android.synthetic.main.filter_by_tag_list_item_view.view.*

class FilterByTagListAdapter(val tags: MutableList<TagWrapper>): RecyclerView.Adapter<FilterByTagListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.filter_by_tag_list_item_view, parent, false)
        val viewHolder = ViewHolder(view)
        view.setOnClickListener {
            viewHolder.tag?.apply {
                status = when(status){
                    TagWrapper.Status.INCLUDED -> TagWrapper.Status.EXCLUDED
                    TagWrapper.Status.EXCLUDED -> TagWrapper.Status.NONE
                    else -> TagWrapper.Status.INCLUDED
                }
                viewHolder.updateImage(this)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTag(tags[position])
    }

    override fun getItemCount(): Int {
        return tags.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tag:TagWrapper? = null

        fun bindTag(tag: TagWrapper){
            this.tag = tag
            itemView.text_tag_name.text = tag.tag.name
            itemView.text_count.text = tag.count.toString()
            updateImage(tag)
        }

        fun updateImage(tag: TagWrapper){
            when(tag.status){
                TagWrapper.Status.EXCLUDED -> itemView.image_view.setImageResource(R.drawable.ic_close_black_24dp)
                TagWrapper.Status.INCLUDED -> itemView.image_view.setImageResource(R.drawable.ic_check_black_24dp)
                else -> itemView.image_view.setImageResource(android.R.color.transparent)
            }
        }
    }
}