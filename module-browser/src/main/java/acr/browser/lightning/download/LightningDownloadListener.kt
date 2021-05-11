/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download

import acr.browser.lightning.BrowserApp.Companion.appComponent
import acr.browser.lightning.R
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.content.Context
import android.text.format.Formatter
import android.webkit.DownloadListener
import android.webkit.URLUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.timecat.module.browser.prepareShowInService
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
                    val downloadSize: String = if (contentLength > 0) {
                        Formatter.formatFileSize(context, contentLength)
                    } else {
                        context.getString(R.string.unknown_size)
                    }
                    val message = context.getString(R.string.dialog_download, downloadSize)
                    MaterialDialog(context).show {
                        prepareShowInService()
                        title(text = fileName)
                        message(text = message)
                        positiveButton(R.string.action_download) {
                            downloadHandler!!.onDownloadStart(context, userPreferences!!, url, userAgent, contentDisposition, mimetype, downloadSize)
                        }
                        negativeButton(R.string.action_cancel)
                    }
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