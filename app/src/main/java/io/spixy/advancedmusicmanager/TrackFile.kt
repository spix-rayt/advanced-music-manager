package io.spixy.advancedmusicmanager

import io.spixy.advancedmusicmanager.db.Tag
import io.spixy.advancedmusicmanager.db.TagTrackRelation
import java.io.File

private val regex = Regex("\\d+")

class TrackFile(val file:File) {
    val lowerCaseName = file.name.toLowerCase()
    val lowerCaseNameForSort = lowerCaseName.replace(regex) { matchResult -> matchResult.value.padStart(7, '0') }
    val name = file.name
    val date = file.lastModified()
    fun getTags(): List<Tag> {
        return TagTrackRelation.fetchWithTrackPath(this@TrackFile.name).map { it.tag }
    }
}