package com.timecat.module.browser

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BuildConfig
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.device.BuildType
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.DeveloperPreferences
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.jess.arms.base.delegate.AppLifecycles
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import javax.inject.Inject

/**
 * @author dlink
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2019/4/6
 * @description null
 * @usage null
 */
class AppLifecyclesImpl : AppLifecycles {
    @Inject
    internal lateinit var developerPreferences: DeveloperPreferences
    @Inject
    internal lateinit var bookmarkModel: BookmarkRepository
    @Inject
    @field:DatabaseScheduler
    internal lateinit var databaseScheduler: Scheduler
    @Inject
    internal lateinit var logger: Logger
    @Inject
    internal lateinit var buildInfo: BuildInfo

    override fun attachBaseContext(base: Context) {

    }

    override fun onCreate(application: Application) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build())
//            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build())
//        }

//        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

//        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
//            if (BuildConfig.DEBUG) {
//                FileUtils.writeCrashToStorage(ex)
//            }
//
//            if (defaultHandler != null) {
//                defaultHandler.uncaughtException(thread, ex)
//            } else {
//                System.exit(2)
//            }
//        }

        RxJavaPlugins.setErrorHandler { throwable: Throwable? ->
            if (BuildConfig.DEBUG && throwable != null) {
                FileUtils.writeCrashToStorage(throwable)
                throw throwable
            }
        }

        BrowserApp.appComponent.inject(this)

        Single.fromCallable(bookmarkModel::count)
                .filter { it == 0L }
                .flatMapCompletable {
                    val assetsBookmarks = BookmarkExporter.importBookmarksFromAssets(application)
                    bookmarkModel.addBookmarkList(assetsBookmarks)
                }
                .subscribeOn(databaseScheduler)
                .subscribe()
        if (buildInfo.buildType == BuildType.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onTerminate(application: Application) {}

}
