package io.spixy.advancedmusicmanager.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.TextView

class TextDialog(context: Context, title: String, initialValue: String = "", onResult: (result: String) -> Unit) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        val input = EditText(builder.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(initialValue, TextView.BufferType.NORMAL)
        builder.setView(input)

        builder.setPositiveButton(context.resources.getString(android.R.string.ok)) { _, _ ->
            onResult(input.text.toString())
        }
        builder.setNegativeButton(context.resources.getString(android.R.string.cancel)) { dialogInterface, _ -> dialogInterface.cancel() }
        builder.show()
    }
}