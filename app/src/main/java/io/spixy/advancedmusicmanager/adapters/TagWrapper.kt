package io.spixy.advancedmusicmanager.adapters

import io.spixy.advancedmusicmanager.db.Tag

class TagWrapper(val tag: Tag?, val count: Int){
    var status = Status.NONE

    enum class Status {
        NONE, CHECKED, INCLUDED, EXCLUDED, REQUIRED
    }
}