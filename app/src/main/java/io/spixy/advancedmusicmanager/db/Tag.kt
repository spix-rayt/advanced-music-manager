package io.spixy.advancedmusicmanager.db

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.activeandroid.query.Select

@Table(name = "Tag")
class Tag : Model() {
    @Column(name="Name")
    lateinit var name:String

    companion object {
        fun fetchAll(): MutableList<Tag> {
            return Select().from(Tag::class.java).execute<Tag>()
        }
    }
}