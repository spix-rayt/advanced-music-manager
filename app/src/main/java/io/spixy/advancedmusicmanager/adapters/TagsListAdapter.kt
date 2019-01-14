package io.spixy.advancedmusicmanager.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.db.TagTrackRelation
import io.spixy.advancedmusicmanager.dialogs.ActionsListDialog
import io.spixy.advancedmusicmanager.dialogs.ConfirmDialog
import io.spixy.advancedmusicmanager.dialogs.TextDialog
import kotlinx.android.synthetic.main.tags_list_item_view.view.*

class TagsListAdapter(val tags: MutableList<TagWrapper>): RecyclerView.Adapter<TagsListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.tags_list_item_view, parent, false)
        val viewHolder = ViewHolder(view)
        view.setOnClickListener {
            viewHolder.itemView.checkbox.isChecked = !viewHolder.itemView.checkbox.isChecked
        }
        view.setOnLongClickListener {
            ActionsListDialog(view.context, view.context.resources.getString(R.string.choose_action), arrayListOf(
                    Pair(view.context.resources.getString(R.string.tag_action_rename), {
                        TextDialog(view.context, view.context.resources.getString(R.string.tag_rename_dialog_title), initialValue = viewHolder.tag?.tag?.name ?: "") { result ->
                            viewHolder.tag?.tag?.apply {
                                name = result
                                save()
                            }
                            this@TagsListAdapter.notifyDataSetChanged()
                        }
                    }),
                    Pair(view.context.resources.getString(R.string.tag_action_delete), {
                        ConfirmDialog(view.context, view.context.resources.getString(R.string.tag_delete_confirm)) {
                            viewHolder.tag?.let { tag ->
                                if(tag.tag != null){
                                    TagTrackRelation.deleteByTagId(tag.tag.id)
                                    tag.tag.delete()
                                    tags.remove(tag)
                                    this@TagsListAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    })
            ))
            true
        }
        viewHolder.itemView.checkbox.setOnCheckedChangeListener { _, checked->
            viewHolder.tag?.status = if(checked) TagWrapper.Status.CHECKED else TagWrapper.Status.NONE
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
            itemView.text_tag_name.text = tag.tag!!.name
            itemView.text_count.text = tag.count.toString()
            itemView.checkbox.isChecked = tag.status == TagWrapper.Status.CHECKED
        }
    }
}