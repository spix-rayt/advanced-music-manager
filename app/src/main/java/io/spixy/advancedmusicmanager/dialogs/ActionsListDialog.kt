package io.spixy.advancedmusicmanager.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog

class ActionsListDialog(context: Context,title:String, actions: ArrayList<Pair<String, () -> Any>>) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setItems(actions.map { it.first }.toTypedArray()) { _, which ->
            actions[which].second()
        }
        builder.show()
    }
}