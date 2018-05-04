package io.spixy.advancedmusicmanager.db

import android.util.Log
import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.activeandroid.query.Delete
import com.activeandroid.query.Select
import io.spixy.advancedmusicmanager.adapters.TagWrapper

@Table(name = "Track")
class Track:Model() {
    @Column(name="Path", index = true)
    lateinit var path: String


    companion object {
        fun fetchAll(): MutableList<Track> {
            return Select().from(Track::class.java).execute<Track>()
        }

        fun fetchWithPath(path: String): Track {
            return Select().from(Track::class.java).where("Path = ?", path).executeSingle<Track>()
        }

        fun delete(path: String){
            TagTrackRelation.delete(TagTrackRelation.fetchWithTrackPath(path).map { it.id })
            Delete().from(Track::class.java).where("Path = ?", path).execute<Track>()
        }

        fun filterByTag(filter: HashMap<Long, TagWrapper.Status>): MutableList<Track> {
            val tagsByStatus = filter.entries.groupBy({ it.value }, { it.key })
            val include = tagsByStatus[TagWrapper.Status.INCLUDED] ?: emptyList()
            val exclude = tagsByStatus[TagWrapper.Status.EXCLUDED] ?: emptyList()
            val none = tagsByStatus[TagWrapper.Status.NONE] ?: emptyList()

            if(include.isNotEmpty() && exclude.isNotEmpty() && none.isEmpty()){
                val relations = Select()
                        .from(TagTrackRelation::class.java)
                        .innerJoin(Tag::class.java)
                        .on("TagTrackRelation.Tag_id = Tag.Id")
                        .innerJoin(Track::class.java)
                        .on("TagTrackRelation.Track_id = Track.Id")
                        .where("Tag.Id IN (${include.joinToString(separator = ", ")})")
                        .execute<TagTrackRelation>()
                val tracks = relations.groupBy({ it.track }, {it.tag.id})
                return tracks.filter { it.value.containsAll(include) }.keys.toMutableList()
            }else{
                val query = Select().from(Track::class.java).leftJoin(TagTrackRelation::class.java).on("TagTrackRelation.Track_id = Track.Id").leftJoin(Tag::class.java).on("TagTrackRelation.Tag_id = Tag.Id")

                val tracks = if(include.isNotEmpty()){
                    query.where("Tag.Id IN (${include.joinToString(separator = ", ")})")
                }else{
                    if(filter.isNotEmpty() && filter.values.all { it == TagWrapper.Status.EXCLUDED }){
                        query.where("Tag.Id IS NULL")
                    }else{
                        query
                    }
                }.execute<Track>()

                val excludeTracks = if(exclude.isNotEmpty()){
                    Select().from(Track::class.java)
                            .leftJoin(TagTrackRelation::class.java)
                            .on("TagTrackRelation.Track_id = Track.Id")
                            .leftJoin(Tag::class.java).on("TagTrackRelation.Tag_id = Tag.Id")
                            .where("Tag.Id IN (${exclude.joinToString(separator = ", ")})")
                            .execute<Track>()
                }else{
                    emptyList()
                }

                tracks.removeAll(excludeTracks)
                return tracks
            }
        }
    }
}