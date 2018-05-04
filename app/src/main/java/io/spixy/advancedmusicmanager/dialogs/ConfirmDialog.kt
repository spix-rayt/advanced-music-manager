package io.spixy.advancedmusicmanager.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog

class ConfirmDialog(context: Context, title: String, confirmed: () -> Unit) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setPositiveButton(context.resources.getString(android.R.string.yes)) { _, _ ->
            confirmed()
        }
        builder.setNegativeButton(context.resources.getString(android.R.string.cancel)) { dialogInterface, _ -> dialogInterface.cancel() }
        builder.show()
    }
}