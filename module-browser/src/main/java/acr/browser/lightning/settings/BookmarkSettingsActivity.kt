package acr.browser.lightning.settings

import acr.browser.lightning.R
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.extensions.toast
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.Utils
import android.app.Application
import android.os.Environment
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.timecat.layout.ui.business.form.Next
import com.timecat.middle.setting.BaseSettingActivity
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/1
 * @description null
 * @usage null
 */
class BookmarkSettingsActivity : BaseSettingActivity() {

    @Inject
    internal lateinit var bookmarkRepository: BookmarkRepository

    @Inject
    internal lateinit var application: Application

    @Inject
    @field:DatabaseScheduler
    internal lateinit var databaseScheduler: Scheduler

    @Inject
    @field:MainScheduler
    internal lateinit var mainScheduler: Scheduler

    @Inject
    internal lateinit var logger: Logger

    private var importSubscription: Disposable? = null
    private var exportSubscription: Disposable? = null

    @Inject
    internal lateinit var userPreferences: UserPreferences
    private val TAG = "BookmarkSettingsFrag"

    override fun title(): String = getString(R.string.bookmark_settings)
    override fun addSettingItems(container: ViewGroup) {
        injector.inject(this)
        PermissionUtils.permissionGroup(PermissionConstants.STORAGE).request()

        container.Next(
            title = getString(R.string.export_bookmarks),
        ) { item -> exportBookmarks() }

        container.Next(
            title = getString(R.string.import_backup),
        ) { item -> importBookmarks() }

        container.Next(
            title = getString(R.string.action_delete_all_bookmarks),
        ) { item -> deleteAllBookmarks() }
    }

    override fun onDestroy() {
        super.onDestroy()

        exportSubscription?.dispose()
        importSubscription?.dispose()
    }

    private fun exportBookmarks() {
        PermissionUtils.permissionGroup(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    bookmarkRepository.getAllBookmarksSorted()
                        .subscribeOn(databaseScheduler)
                        .subscribe { list ->
                            val exportFile = BookmarkExporter.createNewExportFile()
                            exportSubscription?.dispose()
                            exportSubscription = BookmarkExporter.exportBookmarksToFile(list, exportFile)
                                .subscribeOn(databaseScheduler)
                                .observeOn(mainScheduler)
                                .subscribeBy(
                                    onComplete = {
                                        snackbar("${getString(R.string.bookmark_export_path)} ${exportFile.path}")
                                    },
                                    onError = { throwable ->
                                        logger.log(TAG, "onError: exporting bookmarks", throwable)
                                        if (!isFinishing) {
                                            Utils.createInformativeDialog(this@BookmarkSettingsActivity, R.string.title_error, R.string.bookmark_export_failure)
                                        } else {
                                            application.toast(R.string.bookmark_export_failure)
                                        }
                                    }
                                )
                        }
                }

                override fun onDenied() {
                    if (!isFinishing) {
                        Utils.createInformativeDialog(this@BookmarkSettingsActivity, R.string.title_error, R.string.bookmark_export_failure)
                    } else {
                        application.toast(R.string.bookmark_export_failure)
                    }
                }
            })
            .request()
    }

    private fun importBookmarks() {
        PermissionUtils.permissionGroup(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    showImportBookmarkDialog(null)
                }

                override fun onDenied() {}
            })
            .request()
    }

    private fun deleteAllBookmarks() {
        showDeleteBookmarksDialog()
    }

    private fun showDeleteBookmarksDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            context = this,
            title = R.string.action_delete,
            message = R.string.action_delete_all_bookmarks,
            positiveButton = DialogItem(title = R.string.yes) {
                bookmarkRepository
                    .deleteAllBookmarks()
                    .subscribeOn(databaseScheduler)
                    .subscribe()
            },
            negativeButton = DialogItem(title = R.string.no) {},
            onCancel = {}
        )
    }

    private fun loadFileList(path: File?): Array<File> {
        val file: File = path ?: File(Environment.getExternalStorageDirectory().toString())

        try {
            file.mkdirs()
        } catch (e: SecurityException) {
            logger.log(TAG, "Unable to make directory", e)
        }

        return (if (file.exists()) {
            file.listFiles()
        } else {
            arrayOf()
        }).apply {
            sortWith(SortName())
        }
    }

    private class SortName : Comparator<File> {

        override fun compare(a: File, b: File): Int {
            return if (a.isDirectory && b.isDirectory) {
                a.name.compareTo(b.name)
            } else if (a.isDirectory) {
                -1
            } else if (b.isDirectory) {
                1
            } else if (a.isFile && b.isFile) {
                a.name.compareTo(b.name)
            } else {
                1
            }
        }
    }

    private fun showImportBookmarkDialog(path: File?) {
        val builder = AlertDialog.Builder(this)

        val title = getString(R.string.title_chooser)
        builder.setTitle(title + ": " + Environment.getExternalStorageDirectory())

        val fileList = loadFileList(path)
        val fileNames = fileList.map(File::getName).toTypedArray()

        builder.setItems(fileNames) { _, which ->
            if (fileList[which].isDirectory) {
                showImportBookmarkDialog(fileList[which])
            } else {
                importSubscription = BookmarkExporter
                    .importBookmarksFromFile(fileList[which])
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy(
                        onSuccess = { importList ->
                            bookmarkRepository.addBookmarkList(importList)
                                .subscribeOn(databaseScheduler)
                                .observeOn(mainScheduler)
                                .subscribe {
                                    snackbar("${importList.size} ${getString(R.string.message_import)}")
                                }
                        },
                        onError = { throwable ->
                            logger.log(TAG, "onError: importing bookmarks", throwable)
                            if (!isFinishing) {
                                Utils.createInformativeDialog(this, R.string.title_error, R.string.import_bookmark_error)
                            } else {
                                application.toast(R.string.import_bookmark_error)
                            }
                        }
                    )
            }
        }
        val dialog = builder.show()
        BrowserDialog.setDialogSize(this, dialog)
    }

}