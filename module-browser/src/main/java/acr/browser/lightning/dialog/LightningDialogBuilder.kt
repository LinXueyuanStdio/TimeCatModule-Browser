package acr.browser.lightning.dialog

import acr.browser.lightning.MainActivity
import acr.browser.lightning.R
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.download.DownloadHandler
import acr.browser.lightning.extensions.copyToClipboard
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.IntentUtils
import acr.browser.lightning.utils.UrlUtils
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import io.reactivex.Scheduler
import javax.inject.Inject

/**
 * A builder of various dialogs.
 */
class LightningDialogBuilder @Inject constructor(
    private val bookmarkManager: BookmarkRepository,
    private val downloadsModel: DownloadsRepository,
    private val historyModel: HistoryRepository,
    private val userPreferences: UserPreferences,
    private val downloadHandler: DownloadHandler,
    private val clipboardManager: ClipboardManager,
    @DatabaseScheduler private val databaseScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler
) {

    enum class NewTab {
        FOREGROUND,
        BACKGROUND,
        INCOGNITO
    }

    /**
     * Show the appropriated dialog for the long pressed link. It means that we try to understand
     * if the link is relative to a bookmark or is just a folder.
     *
     * @param context used to show the dialog
     * @param url      the long pressed url
     */
    fun showLongPressedDialogForBookmarkUrl(
        context: Context,
        uiController: UIController,
        url: String
    ) {
        if (UrlUtils.isBookmarkUrl(url)) {
            // TODO hacky, make a better bookmark mechanism in the future
            val uri = Uri.parse(url)
            val filename = requireNotNull(uri.lastPathSegment) { "Last segment should always exist for bookmark file" }
            val folderTitle = filename.substring(0, filename.length - BookmarkPageFactory.FILENAME.length - 1)
            showBookmarkFolderLongPressedDialog(context, uiController, folderTitle.asFolder())
        } else {
            bookmarkManager.findBookmarkForUrl(url)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { historyItem ->
                    // TODO: 6/14/17 figure out solution to case where slashes get appended to root urls causing the item to not exist
                    showLongPressedDialogForBookmarkUrl(context, uiController, historyItem)
                }
        }
    }

    fun showLongPressedDialogForBookmarkUrl(
        context: Context,
        uiController: UIController,
        entry: Bookmark.Entry
    ) = BrowserDialog.show(context, R.string.action_bookmarks,
        DialogItem(title = R.string.dialog_open_new_tab) {
            uiController.handleNewTab(NewTab.FOREGROUND, entry.url)
        },
        DialogItem(title = R.string.dialog_open_background_tab) {
            uiController.handleNewTab(NewTab.BACKGROUND, entry.url)
        },
        DialogItem(
            title = R.string.dialog_open_incognito_tab,
            isConditionMet = context is MainActivity
        ) {
            uiController.handleNewTab(NewTab.INCOGNITO, entry.url)
        },
        DialogItem(title = R.string.action_share) {
            IntentUtils(context).shareUrl(entry.url, entry.title)
        },
        DialogItem(title = R.string.dialog_copy_link) {
            clipboardManager.copyToClipboard(entry.url)
        },
        DialogItem(title = R.string.dialog_remove_bookmark) {
            bookmarkManager.deleteBookmark(entry)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { success ->
                    if (success) {
                        uiController.handleBookmarkDeleted(entry)
                    }
                }
        },
        DialogItem(title = R.string.dialog_edit_bookmark) {
            showEditBookmarkDialog(context, uiController, entry)
        })

    /**
     * Show the appropriated dialog for the long pressed link.
     *
     * @param context used to show the dialog
     * @param url      the long pressed url
     */
    // TODO allow individual downloads to be deleted.
    fun showLongPressedDialogForDownloadUrl(
        context: Context,
        uiController: UIController,
        url: String
    ) = BrowserDialog.show(context, R.string.action_downloads,
        DialogItem(title = R.string.dialog_delete_all_downloads) {
            downloadsModel.deleteAllDownloads()
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe(uiController::handleDownloadDeleted)
        })

    private fun showEditBookmarkDialog(
        context: Context,
        uiController: UIController,
        entry: Bookmark.Entry
    ) {
        val dialogLayout = View.inflate(context, R.layout.browser_dialog_edit_bookmark, null)
        val getTitle = dialogLayout.findViewById<EditText>(R.id.bookmark_title)
        getTitle.setText(entry.title)
        val getUrl = dialogLayout.findViewById<EditText>(R.id.bookmark_url)
        getUrl.setText(entry.url)
        val getFolder = dialogLayout.findViewById<AutoCompleteTextView>(R.id.bookmark_folder)
        getFolder.setHint(R.string.folder)
        getFolder.setText(entry.folder.title)

        bookmarkManager.getFolderNames()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { folders ->
                val suggestionsAdapter = ArrayAdapter(context,
                    android.R.layout.simple_dropdown_item_1line, folders)
                getFolder.threshold = 1
                getFolder.setAdapter(suggestionsAdapter)

                MaterialDialog(context).show {
                    title(R.string.title_edit_bookmark)
                    customView(view = dialogLayout)
                    positiveButton(R.string.action_ok) {
                        val editedItem = Bookmark.Entry(
                            title = getTitle.text.toString(),
                            url = getUrl.text.toString(),
                            folder = getFolder.text.toString().asFolder(),
                            position = entry.position
                        )
                        bookmarkManager.editBookmark(entry, editedItem)
                            .subscribeOn(databaseScheduler)
                            .observeOn(mainScheduler)
                            .subscribe(uiController::handleBookmarksChange)
                    }
                }
            }
    }

    fun showBookmarkFolderLongPressedDialog(
        context: Context,
        uiController: UIController,
        folder: Bookmark.Folder
    ) = BrowserDialog.show(context, R.string.action_folder,
        DialogItem(title = R.string.dialog_rename_folder) {
            showRenameFolderDialog(context, uiController, folder)
        },
        DialogItem(title = R.string.dialog_remove_folder) {
            bookmarkManager.deleteFolder(folder.title)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe {
                    uiController.handleBookmarkDeleted(folder)
                }
        })

    private fun showRenameFolderDialog(
        context: Context,
        uiController: UIController,
        folder: Bookmark.Folder
    ) = BrowserDialog.showEditText(context,
        R.string.title_rename_folder,
        R.string.hint_title,
        folder.title,
        R.string.action_ok) { text ->
        if (!TextUtils.isEmpty(text)) {
            val oldTitle = folder.title
            bookmarkManager.renameFolder(oldTitle, text)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe(uiController::handleBookmarksChange)
        }
    }

    fun showLongPressedHistoryLinkDialog(
        context: Context,
        uiController: UIController,
        url: String
    ) = BrowserDialog.show(context, R.string.action_history,
        DialogItem(title = R.string.dialog_open_new_tab) {
            uiController.handleNewTab(NewTab.FOREGROUND, url)
        },
        DialogItem(title = R.string.dialog_open_background_tab) {
            uiController.handleNewTab(NewTab.BACKGROUND, url)
        },
        DialogItem(
            title = R.string.dialog_open_incognito_tab,
            isConditionMet = context is MainActivity
        ) {
            uiController.handleNewTab(NewTab.INCOGNITO, url)
        },
        DialogItem(title = R.string.action_share) {
            IntentUtils(context).shareUrl(url, null)
        },
        DialogItem(title = R.string.dialog_copy_link) {
            clipboardManager.copyToClipboard(url)
        },
        DialogItem(title = R.string.dialog_remove_from_history) {
            historyModel.deleteHistoryEntry(url)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe(uiController::handleHistoryChange)
        })

    // TODO There should be a way in which we do not need an activity reference to dowload a file
    fun showLongPressImageDialog(
        context: Context,
        uiController: UIController,
        url: String,
        userAgent: String
    ) = BrowserDialog.show(context, url.replace(HTTP, ""),
        DialogItem(title = R.string.dialog_open_new_tab) {
            uiController.handleNewTab(NewTab.FOREGROUND, url)
        },
        DialogItem(title = R.string.dialog_open_background_tab) {
            uiController.handleNewTab(NewTab.BACKGROUND, url)
        },
        DialogItem(
            title = R.string.dialog_open_incognito_tab,
            isConditionMet = context is MainActivity
        ) {
            uiController.handleNewTab(NewTab.INCOGNITO, url)
        },
        DialogItem(title = R.string.action_share) {
            IntentUtils(context).shareUrl(url, null)
        },
        DialogItem(title = R.string.dialog_copy_link) {
            clipboardManager.copyToClipboard(url)
        },
        DialogItem(title = R.string.dialog_download_image) {
            downloadHandler.onDownloadStart(context, userPreferences, url, userAgent, "attachment", null, "")
        })

    fun showLongPressLinkDialog(
        context: Context,
        uiController: UIController,
        url: String
    ) = BrowserDialog.show(context, url,
        DialogItem(title = R.string.dialog_open_new_tab) {
            uiController.handleNewTab(NewTab.FOREGROUND, url)
        },
        DialogItem(title = R.string.dialog_open_background_tab) {
            uiController.handleNewTab(NewTab.BACKGROUND, url)
        },
        DialogItem(
            title = R.string.dialog_open_incognito_tab,
            isConditionMet = context is MainActivity
        ) {
            uiController.handleNewTab(NewTab.INCOGNITO, url)
        },
        DialogItem(title = R.string.action_share) {
            IntentUtils(context).shareUrl(url, null)
        },
        DialogItem(title = R.string.dialog_copy_link) {
            clipboardManager.copyToClipboard(url)
        })

}
