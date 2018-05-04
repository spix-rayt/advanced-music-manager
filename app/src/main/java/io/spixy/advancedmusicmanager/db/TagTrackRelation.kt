package io.spixy.advancedmusicmanager.db

import com.activeandroid.Cache
import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.activeandroid.query.Delete
import com.activeandroid.query.Select

@Table(name = "TagTrackRelation")
class TagTrackRelation: Model() {
    @Column(name = "Tag_id")
    lateinit var tag: Tag

    @Column(name="Track_id")
    lateinit var track: Track

    companion object {
        fun fetchWithTrackPath(path:String): MutableList<TagTrackRelation> {
            return Select()
                    .from(TagTrackRelation::class.java)
                    .innerJoin(Tag::class.java)
                    .on("TagTrackRelation.Tag_id = Tag.Id")
                    .innerJoin(Track::class.java)
                    .on("TagTrackRelation.Track_id = Track.Id")
                    .where("Track.Path = ?", path)
                    .execute<TagTrackRelation>()
        }

        fun delete(ids: List<Long>){
            Delete().from(TagTrackRelation::class.java).where("Id in (${ids.joinToString(separator = ", ")})").execute<TagTrackRelation>()
        }

        fun deleteByTrackId(id: Long){
            Delete().from(TagTrackRelation::class.java).where("Track_id = ?", id).execute<TagTrackRelation>()
        }

        fun deleteByTagId(id:Long){
            Delete().from(TagTrackRelation::class.java).where("Tag_id = ?", id).execute<TagTrackRelation>()
        }

        fun countTracksGroupByTag(): MutableMap<Long, Int> {
            val cursor = Cache.openDatabase().rawQuery(Select("Tag_id, COUNT(Tag_id)").from(TagTrackRelation::class.java).groupBy("Tag_id").toSql(), null)
            val result = mutableMapOf<Long, Int>()
            while (cursor.moveToNext()){
                val tagId = cursor.getLong(0)
                val count = cursor.getInt(1)
                result[tagId] = count
            }
            return result
        }
    }
}