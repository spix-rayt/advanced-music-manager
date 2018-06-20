package io.spixy.advancedmusicmanager

import io.spixy.advancedmusicmanager.db.Tag
import io.spixy.advancedmusicmanager.db.TagTrackRelation
import java.io.File

class TrackFile(val file:File) {
    val lowerCaseName = file.name.toLowerCase()
    val name = file.name
    val date = file.lastModified()
    fun getTags(): List<Tag> {
        return TagTrackRelation.fetchWithTrackPath(this@TrackFile.name).map { it.tag }
    }
}