package io.spixy.advancedmusicmanager.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import io.reactivex.Single
import io.spixy.advancedmusicmanager.MusicService
import io.spixy.advancedmusicmanager.R
import io.spixy.advancedmusicmanager.adapters.FilterByTagListAdapter
import io.spixy.advancedmusicmanager.adapters.TagWrapper
import io.spixy.advancedmusicmanager.dagger.BaseApplication
import io.spixy.advancedmusicmanager.db.Tag
import io.spixy.advancedmusicmanager.db.TagTrackRelation
import kotlinx.android.synthetic.main.activity_tags.*
import javax.inject.Inject

class FilterByTagsActivity : AppCompatActivity() {
    private val tagsListAdapter = FilterByTagListAdapter(arrayListOf())
    @Inject lateinit var musicService: Single<MusicService>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseApplication.component.inject(this)
        setContentView(R.layout.activity_filter_by_tags)

        tags_list.layoutManager = LinearLayoutManager(this)
        tags_list.adapter = tagsListAdapter
        loadTags()

        if(savedInstanceState != null){
            val filter = savedInstanceState.getSerializable("FILTER") as HashMap<Long?, TagWrapper.Status>
            tagsListAdapter.tags.forEach {
                it.status = filter[it.tag?.id] ?: TagWrapper.Status.NONE
            }
        }

        button_apply.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra(RESULT_FILTER, getFilterMap())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getFilterMap() = HashMap(tagsListAdapter.tags.map { it.tag?.id to it.status }.toMap())

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("FILTER", getFilterMap())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.filter_by_tag_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_include_all -> {
                tagsListAdapter.tags.filter { it.status == TagWrapper.Status.NONE }.forEach { it.status = TagWrapper.Status.INCLUDED }
                tagsListAdapter.notifyDataSetChanged()
                true
            }
            R.id.action_unselect_all -> {
                tagsListAdapter.tags.forEach { it.status = TagWrapper.Status.NONE }
                tagsListAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadTags(){
        val countTracksGroupByTag = TagTrackRelation.countTracksGroupByTag()
        val elements = Tag.fetchAll()
        tagsListAdapter.tags.clear()
        val tags = elements.map { TagWrapper(it, countTracksGroupByTag[it.id] ?: 0) }
                .sortedBy { it.count * -1 }
        val untagged = TagWrapper(null, countTracksGroupByTag[-1] ?: 0)
        tagsListAdapter.tags.addAll(tags + untagged)
        tagsListAdapter.notifyDataSetChanged()
    }

    companion object {
        val RESULT_FILTER = "FILTER"
    }
}
