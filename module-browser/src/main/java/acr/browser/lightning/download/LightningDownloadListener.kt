/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download

import acr.browser.lightning.BrowserApp.Companion.appComponent
import acr.browser.lightning.R
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.dialog.BrowserDialog.setDialogSize
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.format.Formatter
import android.webkit.DownloadListener
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import javax.inject.Inject

class LightningDownloadListener(context: Context) : DownloadListener {
    private val context: Context

    @JvmField
    @Inject
    var userPreferences: UserPreferences? = null

    @JvmField
    @Inject
    var downloadHandler: DownloadHandler? = null

    @JvmField
    @Inject
    var downloadsRepository: DownloadsRepository? = null

    @JvmField
    @Inject
    var logger: Logger? = null
    override fun onDownloadStart(url: String,
                                 userAgent: String,
                                 contentDisposition: String,
                                 mimetype: String,
                                 contentLength: Long
    ) {
        PermissionUtils.permissionGroup(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    val downloadSize: String
                    downloadSize = if (contentLength > 0) {
                        Formatter.formatFileSize(context, contentLength)
                    } else {
                        context.getString(R.string.unknown_size)
                    }
                    val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> downloadHandler!!.onDownloadStart(context, userPreferences!!, url, userAgent, contentDisposition, mimetype, downloadSize)
                            DialogInterface.BUTTON_NEGATIVE -> {
                            }
                        }
                    }
                    val builder = AlertDialog.Builder(context) // dialog
                    val message = context.getString(R.string.dialog_download, downloadSize)
                    val dialog: Dialog = builder.setTitle(fileName)
                        .setMessage(message)
                        .setPositiveButton(context.resources.getString(R.string.action_download),
                            dialogClickListener)
                        .setNegativeButton(context.resources.getString(R.string.action_cancel),
                            dialogClickListener).show()
                    setDialogSize(context, dialog)
                    logger!!.log(TAG, "Downloading: $fileName")
                }

                override fun onDenied() {
                }
            })
            .request()
    }

    companion object {
        private const val TAG = "LightningDownloader"
    }

    init {
        appComponent.inject(this)
        this.context = context
    }
}