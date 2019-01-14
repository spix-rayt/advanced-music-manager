package io.spixy.advancedmusicmanager.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.adapters.TagWrapper
import io.spixy.advancedmusicmanager.adapters.TagsListAdapter
import io.spixy.advancedmusicmanager.db.Tag
import io.spixy.advancedmusicmanager.db.TagTrackRelation
import io.spixy.advancedmusicmanager.db.Track
import io.spixy.advancedmusicmanager.dialogs.TextDialog
import kotlinx.android.synthetic.main.activity_tags.*

class TagsActivity : AppCompatActivity() {
    private val tagsListAdapter = TagsListAdapter(arrayListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        tags_list.layoutManager = LinearLayoutManager(this)
        tags_list.adapter = tagsListAdapter
        loadTags()

        savedInstanceState?.getLongArray("CHECKED")?.let { checkedIds ->
            tagsListAdapter.tags.forEach {
                if(checkedIds.contains(it.tag!!.id)){
                    it.status = TagWrapper.Status.CHECKED
                }else{
                    it.status = TagWrapper.Status.NONE
                }
            }
            tagsListAdapter.notifyDataSetChanged()
        }

        button_new.setOnClickListener {
            TextDialog(this, resources.getString(R.string.tag_create_dialog_title)) { result ->
                val tag = Tag().apply {
                    name = result
                    save()
                }
                tagsListAdapter.tags.add(TagWrapper(tag, 0))
            }
        }

        button_apply.setOnClickListener {
            val track = Track.fetchWithPath(intent.getStringExtra(INTENT_DATA_NAME_PATH))
            TagTrackRelation.deleteByTrackId(track.id)
            tagsListAdapter.tags.filter { it.status== TagWrapper.Status.CHECKED }.forEach {
                TagTrackRelation().apply {
                    tag = it.tag!!
                    this.track = track
                    save()
                }
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray("CHECKED", tagsListAdapter.tags.filter { it.status == TagWrapper.Status.CHECKED }.map { it.tag!!.id }.toLongArray())
    }

    private fun loadTags(){
        val countTracksGroupByTag = TagTrackRelation.countTracksGroupByTag()
        val relations = TagTrackRelation.fetchWithTrackPath(intent.getStringExtra(INTENT_DATA_NAME_PATH))
        val tags = Tag.fetchAll().map { TagWrapper(it, countTracksGroupByTag[it.id]?:0) }.sortedBy { it.count*-1 }
        tags.forEach { if(relations.map { it.tag.id }.contains(it.tag!!.id)) it.status = TagWrapper.Status.CHECKED }
        tagsListAdapter.tags.clear()
        tagsListAdapter.tags.addAll(tags)
        tagsListAdapter.notifyDataSetChanged()
    }

    companion object {
        val INTENT_DATA_NAME_PATH = "PATH"
    }
}
