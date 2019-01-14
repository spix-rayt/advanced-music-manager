package io.spixy.advancedmusicmanager.db

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

        const val untaggedSql = "not exists (select * from TagTrackRelation where TagTrackRelation.Track_id = Track.Id)"

        fun filterByTag(filter: HashMap<Long?, TagWrapper.Status>): MutableList<Track> {
            val tagsByStatus = filter.filterKeys { it != null }.entries.groupBy({ it.value }, { it.key!! })
            val include = tagsByStatus[TagWrapper.Status.INCLUDED] ?: emptyList()
            val exclude = tagsByStatus[TagWrapper.Status.EXCLUDED] ?: emptyList()
            val require = tagsByStatus[TagWrapper.Status.REQUIRED] ?: emptyList()

            val untagged = filter[null]

            val where = mutableListOf<String>()

            if(untagged == TagWrapper.Status.REQUIRED){
                where.add(untaggedSql)
            }else{
                val includeOrList = mutableListOf<String>()
                if(include.isNotEmpty() || require.isNotEmpty()){
                    val includeStringList = (include + require).joinToString(separator = ",")
                    includeOrList.add("Id in (select Track_id from TagTrackRelation where Tag_id in ($includeStringList))")
                }
                if(untagged == TagWrapper.Status.INCLUDED){
                    includeOrList.add(untaggedSql)
                }
                if(includeOrList.isNotEmpty()){
                    where.add(includeOrList.joinToString(separator = " or "))
                }
                if(require.isNotEmpty()){
                    require.forEach {
                        where.add("exists (select * from TagTrackRelation where Track_id = Track.Id and Tag_id = $it)")
                    }
                }
                if(exclude.isNotEmpty()){
                    val excludeStringList = exclude.joinToString(separator = ",")
                    where.add("Id not in (select Track_id from TagTrackRelation where Tag_id in ($excludeStringList))")
                }
            }

            val tracks = if(where.isNotEmpty()){
                Select()
                        .from(Track::class.java)
                        .where(where.joinToString(separator = " and "))
                        .execute<Track>()
            }else{
                emptyList()
            }
            return tracks
        }
    }
}