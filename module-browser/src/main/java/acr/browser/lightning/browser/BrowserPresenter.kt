package acr.browser.lightning.browser

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.constant.INTENT_ORIGIN
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.homepage.HomePageFactory
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.view.BundleInitializer
import acr.browser.lightning.view.LightningView
import acr.browser.lightning.view.TabInitializer
import acr.browser.lightning.view.UrlInitializer
import acr.browser.lightning.view.find.FindResults
import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
import com.timecat.component.commonsdk.utils.override.LogUtil
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy

/**
 * Presenter in charge of keeping track of the current tab and setting the current tab of the
 * browser.
 */
class BrowserPresenter(
    private val context: Context,
    private val uiController: UIController,
    private val view: BrowserView,
    private val isIncognito: Boolean,
    private val userPreferences: UserPreferences,
    private val tabsModel: TabsManager,
    @MainScheduler private val mainScheduler: Scheduler,
    private val homePageFactory: HomePageFactory,
    private val bookmarkPageFactory: BookmarkPageFactory,
    private val recentTabModel: RecentTabModel,
    private val logger: Logger
) {

    private var currentTab: LightningView? = null
    private var shouldClose: Boolean = false
    private var sslStateSubscription: Disposable? = null

    init {
        tabsModel.addTabNumberChangedListener(view::updateTabNumber)
    }

    /**
     * Initializes the tab manager with the new intent that is handed in by the BrowserActivity.
     *
     * @param intent the intent to handle, may be null.
     */
    fun setupTabs(intent: Intent?) {
        tabsModel.initializeTabs(context, intent, isIncognito, uiController)
            .subscribeBy(
                onSuccess = {
                    // At this point we always have at least a tab in the tab manager
                    view.notifyTabViewInitialized()
                    view.updateTabNumber(tabsModel.size())
                    tabChanged(tabsModel.positionOf(it))
                }
            )
    }

    /**
     * Notify the presenter that a change occurred to the current tab. Currently doesn't do anything
     * other than tell the view to notify the adapter about the change.
     *
     * @param tab the tab that changed, may be null.
     */
    fun tabChangeOccurred(tab: LightningView?) = tab?.let {
        view.notifyTabViewChanged(tabsModel.indexOfTab(it))
    }

    private fun onTabChanged(newTab: LightningView?) {
        logger.log(TAG, "On tab changed")
        view.updateSslState(newTab?.currentSslState() ?: SSLState.None)

        sslStateSubscription?.dispose()
        sslStateSubscription = newTab
            ?.sslStateObservable()
            ?.observeOn(mainScheduler)
            ?.subscribe(view::updateSslState)

        val webView = newTab?.webView

        if (newTab == null) {
            view.removeTabView()
            currentTab?.let {
                it.pauseTimers()
                it.onDestroy()
            }
        } else {
            if (webView == null) {
                view.removeTabView()
                currentTab?.let {
                    it.pauseTimers()
                    it.onDestroy()
                }
            } else {
                currentTab.let {
                    // TODO: Restore this when Google fixes the bug where the WebView is
                    // blank after calling onPause followed by onResume.
                    // currentTab.onPause();
                    it?.isForegroundTab = false
                }

                newTab.resumeTimers()
                newTab.onResume()
                newTab.isForegroundTab = true

                view.updateProgress(newTab.progress)
                view.setBackButtonEnabled(newTab.canGoBack())
                view.setForwardButtonEnabled(newTab.canGoForward())
                view.updateUrl(newTab.url, false)
                view.setTabView(webView)
                val index = tabsModel.indexOfTab(newTab)
                if (index >= 0) {
                    view.notifyTabViewChanged(tabsModel.indexOfTab(newTab))
                }
            }
        }

        currentTab = newTab
    }

    /**
     * Closes all tabs but the current tab.
     */
    fun closeAllOtherTabs() {

        while (tabsModel.last() != tabsModel.indexOfCurrentTab()) {
            deleteTab(tabsModel.last())
        }

        while (0 != tabsModel.indexOfCurrentTab()) {
            deleteTab(0)
        }

    }

    private fun mapHomepageToCurrentUrl(): String {
        val homepage = userPreferences.homepage

        return when (homepage) {
            SCHEME_HOMEPAGE -> "$FILE${homePageFactory.createHomePage()}"
            SCHEME_BOOKMARKS -> "$FILE${bookmarkPageFactory.createBookmarkPage(null)}"
            else -> homepage
        }
    }

    /**
     * Deletes the tab at the specified position.
     * true:delete success, false: no need to delete
     * @param position the position at which to delete the tab.
     */
    fun deleteTab(position: Int): Boolean {
        logger.log(TAG, "deleting tab...")
        val tabToDelete = tabsModel.getTabAtPosition(position) ?: return false

        recentTabModel.addClosedTab(tabToDelete.saveState())

        val isShown = tabToDelete.isShown
        val shouldClose = shouldClose && isShown && tabToDelete.isNewTab
        val currentTab = tabsModel.currentTab
        if (tabsModel.size() == 1
            && currentTab != null
            && URLUtil.isFileUrl(currentTab.url)
            && currentTab.url == mapHomepageToCurrentUrl()) {
                LogUtil.se("closeActivity")
            view.closeActivity()
            return true
        } else {
            if (isShown) {
                view.removeTabView()
            }
            val currentDeleted = tabsModel.deleteTab(position)
            if (currentDeleted) {
                tabChanged(tabsModel.indexOfCurrentTab())
            }
        }

        val afterTab = tabsModel.currentTab
        view.notifyTabViewRemoved(position)
        LogUtil.se("notifyTabViewRemoved")

        if (afterTab == null) {
            view.closeBrowser()
            LogUtil.se("closeBrowser")
            return true
        } else if (afterTab !== currentTab) {
            LogUtil.se("closeActivity")
            view.notifyTabViewChanged(tabsModel.indexOfCurrentTab())
        }

        LogUtil.se("closeActivity")
        if (shouldClose && !isIncognito) {
            this.shouldClose = false
            LogUtil.se("closeActivity")
            view.closeActivity()
        }

        view.updateTabNumber(tabsModel.size())
        LogUtil.se("closeActivity")

        logger.log(TAG, "...deleted tab")
        return false
    }

    /**
     * Handle a new intent from the the main BrowserActivity.
     *
     * @param intent the intent to handle, may be null.
     */
    fun onNewIntent(uiController: UIController, intent: Intent?) = tabsModel.doAfterInitialization {
        val url = if (intent?.action == Intent.ACTION_WEB_SEARCH) {
            tabsModel.extractSearchFromIntent(intent)
        } else {
            intent?.dataString
        }

        val tabHashCode = intent?.extras?.getInt(INTENT_ORIGIN, 0) ?: 0

        if (tabHashCode != 0 && url != null) {
            tabsModel.getTabForHashCode(tabHashCode)?.loadUrl(url)
        } else if (url != null) {
            if (URLUtil.isFileUrl(url)) {
                view.showBlockedLocalFileDialog {
                    newTab(UrlInitializer(url), true)
                    shouldClose = true
                    tabsModel.lastTab()?.isNewTab = true
                }
            } else {
                newTab(UrlInitializer(url), true)
                shouldClose = true
                tabsModel.lastTab()?.isNewTab = true
            }
        }
    }

    /**
     * Call when the user long presses the new tab button.
     */
    fun onNewTabLongClicked() {
        recentTabModel.lastClosed()?.let {
            newTab(BundleInitializer(it), true)
            view.showSnackbar(R.string.reopening_recent_tab)
        }
    }

    /**
     * Loads a URL in the current tab.
     *
     * @param url the URL to load, must not be null.
     */
    fun loadUrlInCurrentView(url: String) {
        tabsModel.currentTab?.loadUrl(url)
    }

    /**
     * Notifies the presenter that it should shut down. This should be called when the
     * BrowserActivity is destroyed so that we don't leak any memory.
     */
    fun shutdown() {
        onTabChanged(null)
        tabsModel.cancelPendingWork()
        sslStateSubscription?.dispose()
    }

    /**
     * Notifies the presenter that we wish to switch to a different tab at the specified position.
     * If the position is not in the model, this method will do nothing.
     *
     * @param position the position of the tab to switch to.
     */
    fun tabChanged(position: Int) {
        if (position < 0 || position >= tabsModel.size()) {
            logger.log(TAG, "tabChanged invalid position: $position")
            return
        }

        logger.log(TAG, "tabChanged: $position")
        onTabChanged(tabsModel.switchToTab(position))
    }

    /**
     * Open a new tab with the specified URL. You can choose to show the tab or load it in the
     * background.
     *
     * @param tabInitializer the tab initializer to run after the tab as been created.
     * @param show whether or not to switch to this tab after opening it.
     * @return true if we successfully created the tab, false if we have hit max tabs.
     */
    fun newTab(tabInitializer: TabInitializer, show: Boolean): Boolean {
        // Limit number of tabs for limited version of app
        if (!BuildConfig.FULL_VERSION && tabsModel.size() >= 10) {
            view.showSnackbar(R.string.max_tabs)
            return false
        }

        logger.log(TAG, "New tab, show: $show")

        val startingTab = tabsModel.newTab(context, tabInitializer, isIncognito, uiController)
        if (tabsModel.size() == 1) {
            startingTab.resumeTimers()
        }

        view.notifyTabViewAdded()
        view.updateTabNumber(tabsModel.size())

        if (show) {
            onTabChanged(tabsModel.switchToTab(tabsModel.last()))
        }

        return true
    }

    fun onAutoCompleteItemPressed() {
        tabsModel.currentTab?.requestFocus()
    }

    fun findInPage(query: String): FindResults? {
        return tabsModel.currentTab?.find(query)
    }

    companion object {
        private const val TAG = "BrowserPresenter"
    }

}
