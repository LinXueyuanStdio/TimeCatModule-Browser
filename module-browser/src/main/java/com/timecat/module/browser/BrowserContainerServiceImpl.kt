package com.timecat.module.browser

import acr.browser.lightning.constant.FILE
import acr.browser.lightning.constant.FOLDER
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.bookmark.BookmarkDatabase
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.favicon.toValidUri
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.bookmark.BookmarkViewModel
import acr.browser.lightning.log.NoOpLogger
import android.content.Context
import android.net.Uri
import com.google.android.material.chip.Chip
import com.timecat.component.router.app.NAV
import com.timecat.data.room.record.RoomRecord
import com.timecat.extend.arms.BaseApplication
import com.timecat.identity.readonly.RouterHub
import com.timecat.layout.ui.business.breadcrumb.Path
import com.timecat.layout.ui.layout.setShakelessClickListener
import com.timecat.middle.block.service.*
import com.xiaojinzi.component.anno.ServiceAnno
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/24
 * @description null
 * @usage null
 */
@ServiceAnno(ContainerService::class, name = [RouterHub.GLOBAL_BrowserContainerService])
class BrowserContainerServiceImpl : ContainerService {

    val database = BookmarkDatabase(BaseApplication.getContext())
    val faviconModel = FaviconModel(BaseApplication.getContext(), NoOpLogger())
    private val defaultIconFile = File(BaseApplication.getContext().cacheDir, BookmarkPageFactory.DEFAULT_ICON)

    var dispose: Disposable? = null


    override fun loadForVirtualPath(
        context: Context,
        parentUuid: String,
        homeService: HomeService,
        callback: ContainerService.LoadCallback
    ) {
        if (parentUuid.startsWith(FOLDER)) {
            val path = parentUuid.substringAfter(FOLDER)
            loadForBookmarkFolder(context, path, homeService, callback)
        } else {
            loadForBookmarkFolder(context, "", homeService, callback)
        }
    }

    fun loadForBookmarkFolder(
        context: Context,
        parentUuid: String,
        homeService: HomeService,
        callback: ContainerService.LoadCallback
    ) {
        dispose = database.getAllBookmarksSorted()
            .flattenAsObservable { it }
            .groupBy<Bookmark.Folder, Bookmark>(Bookmark.Entry::folder) { it }
            .flatMapSingle { bookmarksInFolder ->
                val folder = bookmarksInFolder.key
                return@flatMapSingle bookmarksInFolder
                    .toList()
                    .concatWith(
                        if (folder == Bookmark.Folder.Root) {
                            database.getFoldersSorted().map { it.filterIsInstance<Bookmark.Folder.Entry>() }
                        } else {
                            Single.just(emptyList())
                        }
                    )
                    .toList()
                    .map { bookmarksAndFolders ->
                        Pair(folder, bookmarksAndFolders.flatten().map { it.asViewModel() })
                    }
            }
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe {

            }
    }

    private fun Bookmark.asViewModel(): BookmarkViewModel = when (this) {
        is Bookmark.Folder -> createViewModelForFolder(this)
        is Bookmark.Entry -> createViewModelForBookmark(this)
    }

    private fun createViewModelForFolder(folder: Bookmark.Folder): BookmarkViewModel {
        return BookmarkViewModel(
            title = folder.title,
            url = "${FOLDER}${folder.title}",
            iconUrl = "R.drawable.ic_folder"
        )
    }

    private fun createViewModelForBookmark(entry: Bookmark.Entry): BookmarkViewModel {
        val bookmarkUri = Uri.parse(entry.url).toValidUri()

        val iconUrl = if (bookmarkUri != null) {
            val faviconFile = FaviconModel.getFaviconCacheFile(BaseApplication.getContext(), bookmarkUri)
            if (!faviconFile.exists()) {
                val defaultFavicon = faviconModel.getDefaultBitmapForString(entry.title)
                faviconModel.cacheFaviconForUrl(defaultFavicon, entry.url)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }

            "$FILE$faviconFile"
        } else {
            "$FILE$defaultIconFile"
        }

        return BookmarkViewModel(
            title = entry.title,
            url = entry.url,
            iconUrl = iconUrl
        )
    }

    override fun loadMoreForVirtualPath(context: Context, parentUuid: String, offset: Int, homeService: HomeService, callback: ContainerService.LoadMoreCallback) {
        callback.onVirtualLoadSuccess(listOf())
    }

    override fun loadContext(path: Path, context: Context, parentUuid: String, record: RoomRecord?, homeService: HomeService) {
        homeService.loadMenu(EmptyMenuContext())
        homeService.loadHeader(listOf())
        homeService.loadChipType(listOf())
        homeService.loadPanel(EmptyPanelContext())
        homeService.loadChipButtons(listOf(Chip(context).apply {
            text = "浏览器"
            setShakelessClickListener {
                NAV.go(RouterHub.ARTIFACT_MainActivity)
            }
        }))
        homeService.loadCommand(EmptyCommandContext())
        homeService.loadInputSend(EmptyInputContext())
        homeService.reloadData()
    }

    override fun loadContextRecord(
        path: Path,
        context: Context,
        parentUuid: String,
        homeService: HomeService
    ) {
        homeService.loadContextRecord(null)
    }

}