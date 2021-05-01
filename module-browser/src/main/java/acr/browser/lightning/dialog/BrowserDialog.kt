/*
 * Copyright 7/31/2016 Anthony Restaino
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package acr.browser.lightning.dialog

import acr.browser.lightning.R
import acr.browser.lightning.extensions.dimen
import acr.browser.lightning.extensions.inflater
import acr.browser.lightning.list.RecyclerViewDialogItemAdapter
import acr.browser.lightning.list.RecyclerViewStringAdapter
import acr.browser.lightning.utils.DeviceUtils
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

object BrowserDialog {

    @JvmStatic
    fun show(
        context: Context,
        @StringRes title: Int,
        vararg items: DialogItem
    ) = show(context, context.getString(title), *items)
    
    fun showWithIcons(context: Context, title: String?, vararg items: DialogItem) {
        val builder = AlertDialog.Builder(context)

        val layout = context.inflater.inflate(R.layout.browser_list_dialog, null)

        val titleView = layout.findViewById<TextView>(R.id.dialog_title)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.dialog_list)

        val itemList = items.filter(DialogItem::isConditionMet)

        val adapter = RecyclerViewDialogItemAdapter(itemList)

        if (title?.isNotEmpty() == true) {
            titleView.text = title
        }

        recyclerView.apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            this.adapter = adapter
            setHasFixedSize(true)
        }

        builder.setView(layout)

        val dialog = builder.show()

        setDialogSize(context, dialog)

        adapter.onItemClickListener = { item ->
            item.onClick()
            dialog.dismiss()
        }
    }

    @JvmStatic
    fun show(context: Context, title: String?, vararg items: DialogItem) {
        val builder = AlertDialog.Builder(context)

        val layout = context.inflater.inflate(R.layout.browser_list_dialog, null)

        val titleView = layout.findViewById<TextView>(R.id.dialog_title)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.dialog_list)

        val itemList = items.filter(DialogItem::isConditionMet)

        val adapter = RecyclerViewStringAdapter(itemList, convertToString = { context.getString(this.title) })

        if (title?.isNotEmpty() == true) {
            titleView.text = title
        }

        recyclerView.apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            this.adapter = adapter
            setHasFixedSize(true)
        }

        builder.setView(layout)

        val dialog = builder.show()

        setDialogSize(context, dialog)

        adapter.onItemClickListener = { item ->
            item.onClick()
            dialog.dismiss()
        }
    }

    @JvmStatic
    fun showPositiveNegativeDialog(
        context: Context,
        @StringRes title: Int,
        @StringRes message: Int,
        messageArguments: Array<Any>? = null,
        positiveButton: DialogItem,
        negativeButton: DialogItem,
        onCancel: () -> Unit
    ) {
        val messageValue = if (messageArguments != null) {
            context.getString(message, *messageArguments)
        } else {
            context.getString(message)
        }
        val dialog = AlertDialog.Builder(context).apply {
            setTitle(title)
            setMessage(messageValue)
            setOnCancelListener { onCancel() }
            setPositiveButton(positiveButton.title) { _, _ -> positiveButton.onClick() }
            setNegativeButton(negativeButton.title) { _, _ -> negativeButton.onClick() }
        }.show()

        setDialogSize(context, dialog)
    }

    @JvmStatic
    fun showEditText(
        context: Context,
        @StringRes title: Int,
        @StringRes hint: Int,
        @StringRes action: Int,
        textInputListener: (String) -> Unit
    ) = showEditText(context, title, hint, null, action, textInputListener)

    @JvmStatic
    fun showEditText(
        context: Context,
        @StringRes title: Int,
        @StringRes hint: Int,
        currentText: String?,
        @StringRes action: Int,
        textInputListener: (String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.browser_dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setHint(hint)
        if (currentText != null) {
            editText.setText(currentText)
        }

        val editorDialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(action
            ) { _, _ -> textInputListener(editText.text.toString()) }

        val dialog = editorDialog.show()
        setDialogSize(context, dialog)
    }

    @JvmStatic
    fun setDialogSize(context: Context, dialog: Dialog) {
        var maxWidth = context.dimen(R.dimen.dialog_max_size)
        val padding = context.dimen(R.dimen.dialog_padding)
        val screenSize = DeviceUtils.getScreenWidth(context)
        if (maxWidth > screenSize - 2 * padding) {
            maxWidth = screenSize - 2 * padding
        }
        val window = dialog.window
        window?.setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /**
     * Show the custom dialog with the custom builder arguments applied.
     */
    fun showCustomDialog(context: Context?, block: AlertDialog.Builder.(Context) -> Unit) {
        context?.let {
            AlertDialog.Builder(context).apply {
                block(it)
                val dialog = show()
                setDialogSize(it, dialog)
            }
        }
    }

}
