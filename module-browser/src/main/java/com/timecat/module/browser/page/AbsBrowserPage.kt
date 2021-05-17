package com.timecat.module.browser.page

import acr.browser.lightning.R
import acr.browser.lightning.browser.*
import acr.browser.lightning.browser.fragment.BookmarksFrameLayout
import acr.browser.lightning.browser.fragment.TabsFrameLayout
import acr.browser.lightning.constant.LOAD_READING_URL
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainHandler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.*
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.html.homepage.HomePageFactory
import acr.browser.lightning.log.Logger
import acr.browser.lightning.notifications.IncognitoNotification
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.SettingsActivity
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.utils.*
import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.ThemeUtils
import acr.browser.lightning.view.*
import acr.browser.lightning.view.find.FindResults
import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.ParagraphStyle
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.ViewGroup.LayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.FrameLayout
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.palette.graphics.Palette
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.anthonycr.progress.AnimatedProgressBar
import com.google.android.material.snackbar.Snackbar
import com.same.lib.core.ActionBar
import com.same.lib.core.ActionBarMenuItem
import com.same.lib.drawable.MenuDrawable
import com.same.lib.helper.LayoutHelper
import com.same.lib.util.Space
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.component.identity.Attr
import com.timecat.component.router.app.NAV
import com.timecat.component.setting.DEF
import com.timecat.data.bmob.dao.UserDao
import com.timecat.data.bmob.data.User
import com.timecat.data.room.RoomClient
import com.timecat.data.room.record.RoomRecord
import com.timecat.element.alert.ToastUtil
import com.timecat.identity.data.block.BLOCK_APP_WebApp
import com.timecat.identity.readonly.RouterHub
import com.timecat.layout.ui.layout.*
import com.timecat.layout.ui.utils.ScreenUtil
import com.timecat.module.browser.KeyEventView
import com.timecat.module.browser.prepareShowInService
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/29
 * @description null
 * @usage null
 */
abstract class AbsBrowserPage(
    var intent: Intent?
) : AbsThemeBrowserPage(), BrowserView, UIController, OnClickListener {

    //region field
    // Toolbar Views
    private var searchBackground: View? = null
    private var searchView: acr.browser.lightning.view.SearchView? = null

    // Current tab view being displayed
    private var currentTabView: View? = null

    // Full Screen Video Views
    private var fullscreenContainerView: FrameLayout? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Adapter
    private var suggestionsAdapter: SuggestionsAdapter? = null

    // Callback
    private var customViewCallback: CustomViewCallback? = null
    private var uploadMessageCallback: ValueCallback<Uri>? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Primitives
    private var isFullScreen: Boolean = false
    private var hideStatusBar: Boolean = false
    private var isDarkTheme: Boolean = false
    private var isImmersiveMode = false
    private var swapBookmarksAndTabs: Boolean = false

    private var backgroundColor: Int = 0
    private var iconColor: Int = 0
    private var disabledIconColor: Int = 0
    private var currentUiColor = Color.BLACK
    private var keyDownStartTime: Long = 0
    private var searchText: String? = null
    private var cameraPhotoPath: String? = null

    private var findResult: FindResults? = null

    // The singleton BookmarkManager
    @Inject
    lateinit var bookmarkManager: BookmarkRepository

    @Inject
    lateinit var historyModel: HistoryRepository

    @Inject
    lateinit var searchBoxModel: SearchBoxModel

    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var inputMethodManager: InputMethodManager

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    @field:DatabaseScheduler
    lateinit var databaseScheduler: Scheduler

    @Inject
    @field:MainScheduler
    lateinit var mainScheduler: Scheduler

    @Inject
    lateinit var tabsManager: TabsManager

    @Inject
    lateinit var homePageFactory: HomePageFactory

    @Inject
    lateinit var bookmarkPageFactory: BookmarkPageFactory

    @Inject
    lateinit var historyPageFactory: HistoryPageFactory

    @Inject
    lateinit var historyPageInitializer: HistoryPageInitializer

    @Inject
    lateinit var downloadPageInitializer: DownloadPageInitializer

    @Inject
    lateinit var homePageInitializer: HomePageInitializer

    @Inject
    @field:MainHandler
    lateinit var mainHandler: Handler

    @Inject
    lateinit var proxyUtils: ProxyUtils

    @Inject
    lateinit var logger: Logger

    // Image
    private var webPageBitmap: Bitmap? = null
    private var deleteIconDrawable: Drawable? = null
    private var refreshIconDrawable: Drawable? = null
    private var clearIconDrawable: Drawable? = null
    private var iconDrawable: Drawable? = null
    private var sslDrawable: Drawable? = null

    protected var presenter: BrowserPresenter? = null
    private var tabsView: TabsView? = null
    private lateinit var tabsFrameLayout: TabsFrameLayout
    private var bookmarksView: BookmarksView? = null

    // Menu
    private var backMenuItem: MenuItem? = null
    private var forwardMenuItem: MenuItem? = null

    private val longPressBackRunnable = Runnable {
        showCloseDialog(tabsManager.positionOf(tabsManager.currentTab))
    }
    //endregion

    //region config
    open fun theme(): Int = R.style.ThemeLight

    abstract fun menu(): Int

    /**
     * Determines if the current browser instance is in incognito mode or not.
     */
    protected abstract fun isIncognito(): Boolean

    /**
     * Choose the behavior when the controller closes the view.
     */
    abstract override fun closeActivity()

    /**
     * Choose what to do when the browser visits a website.
     *
     * @param title the title of the site visited.
     * @param url the url of the site visited.
     */
    abstract override fun updateHistory(title: String?, url: String)

    /**
     * An observable which asynchronously updates the user's cookie preferences.
     */
    protected abstract fun updateCookiePreference(): Completable
    //endregion

    override fun createActionBar(context: Context): ActionBar {
        val actionBar = buildActionBar(context)
        val menuDrawable = MenuDrawable()
        menuDrawable.setRotateToBack(true)
        actionBar.setBackButtonDrawable(menuDrawable)

        val iconColor = Attr.getIconColor(context)
        val menu = actionBar.createMenu()
        tabsFrameLayout = TabsFrameLayout.createTabsView(context, isIncognito(), this).apply {
            bindSearch(actionBar)
            listener = object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                override fun onSearchExpand() {
                    LogUtil.se("onSearchExpand")
                }

                override fun onSearchCollapse() {
                    LogUtil.se("onSearchCollapse")
                    val currentTab = tabsManager.currentTab ?: return
                    currentTab.requestFocus()
                }

                override fun onTextChanged(editText: EditText) {
                    LogUtil.se("onTextChanged ${editText.text}")
                }
            }
            id = R.id.toolbar_layout
        }
        tabsView = tabsFrameLayout
        actionBar.addView(tabsFrameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM))
        tabsFrameLayout.apply {
            (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                topMargin = Space.statusBarHeight
                leftMargin = 48.dp
                rightMargin = 96.dp
            }
        }
        progress_view = LayoutInflater.from(context).inflate(R.layout.browser_progress_view, null) as AnimatedProgressBar
        actionBar.addView(progress_view, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 4, Gravity.BOTTOM))
        // initialize background ColorDrawable
        val primaryColor = ThemeUtils.getPrimaryColor(context)
        mainHandler.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getBookmarkDrawer()) }
        val iconBounds = Utils.dpToPx(24f)
        backgroundColor = primaryColor
        deleteIconDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_delete_24dp, isDarkTheme).apply {
            setBounds(0, 0, iconBounds, iconBounds)
        }
        refreshIconDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_refresh_24dp, isDarkTheme).apply {
            setBounds(0, 0, iconBounds, iconBounds)
        }
        clearIconDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_clear_24dp, isDarkTheme).apply {
            setBounds(0, 0, iconBounds, iconBounds)
        }

        // create the search EditText in the ToolBar
        searchView = tabsFrameLayout.searchField.apply {
            iconDrawable = refreshIconDrawable
            compoundDrawablePadding = Utils.dpToPx(3f)
            setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, refreshIconDrawable, null)

            val searchListener = SearchListenerClass()
            setOnKeyListener(searchListener)
            onFocusChangeListener = searchListener
            setOnEditorActionListener(searchListener)
            onPreFocusListener = searchListener
            addTextChangedListener(searchListener)
            onRightDrawableClickListener = {
                if (it.hasFocus()) {
                    it.setText("")
                } else {
                    refreshOrStop()
                }
            }

            initializeSearchSuggestions(context, this)
        }

        searchBackground = tabsFrameLayout.searchContainer.apply {
            // initialize search background color
            setBackgroundColor(getSearchBarColor(primaryColor, primaryColor))
//            background.setColorFilter(, PorterDuff.Mode.SRC_IN)
        }

        val addItemId = 1
        val moreItemId = 2
        menu.addItem(addItemId, R.drawable.ic_add).apply {
            setIconColor(iconColor)
        }
        menu.addItem(moreItemId, R.drawable.ic_more_vert_black_24dp).apply {
            setIconColor(iconColor)
        }
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                when (id) {
                    -1 -> {
                        finishFragment(true)
                    }
                    addItemId -> {
                        presenter?.newTab(homePageInitializer, true)
                    }
                    moreItemId -> {
                        MaterialDialog(context, BottomSheet()).show {
                            prepareShowInService(context)
                            val view = MoreDialogView(context)
                            val currentView = tabsManager.currentTab
                            val currentUrl = currentView?.url
                            view.user = UserDao.getCurrentUser()
                            view.listener = object : MoreDialogView.Listener {
                                override fun onToggleBookmark(check: Boolean) {
                                    if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                                        if (check) {
                                            addBookmark(currentView.title, currentUrl)
                                        } else{
                                            deleteBookmark(currentView.title, currentUrl)
                                        }
                                    }
                                }

                                override fun onLogin() {
                                    NAV.go(RouterHub.LOGIN_LoginActivity)
                                }

                                override fun onClickUser(user: User) {
                                    NAV.go(RouterHub.USER_UserDetailActivity, "userId", user.uuid)
                                }

                                override fun onSetting() {
                                    context().startActivity(Intent(context(), SettingsActivity::class.java))
                                }

                                override fun onNewTab() {
                                    presenter?.newTab(homePageInitializer, true)
                                }

                                override fun onNewIncognitoTab() {
                                    presentFragment(IncognitoPage())
                                }

                                override fun openBookmark() {
                                    this@AbsBrowserPage.openBookmarks()
                                }

                                override fun openHistory() {
                                   this@AbsBrowserPage.openHistory()
                                }

                                override fun openDownload() {
                                    openDownloads()
                                }

                                override fun onRefreshCurrentWeb() {
                                    refreshOrStop()
                                }

                                override fun onCopyLink() {
                                    if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("label", currentUrl))
                                        showSnackbar(R.string.message_link_copied)
                                    }
                                }

                                override fun onShare() {
                                    IntentUtils(context).shareUrl(currentUrl, currentView?.title)
                                }

                                override fun openProperty() {
                                    snackbar("openProperty")
                                }

                                override fun currentLinkIsInBookmarks(): Boolean {
                                    if (currentUrl==null) return false
                                    return bookmarkManager.isBookmark(currentUrl)
                                        .subscribeOn(databaseScheduler)
                                        .observeOn(mainScheduler)
                                        .blockingGet()
                                }

                                override fun collect() {
                                    if (currentView != null
                                        && currentView.url.isNotBlank()
                                        && !UrlUtils.isSpecialUrl(currentView.url)
                                    ) {
                                        if (UserDao.getCurrentUser() != null) {
                                            if (DEF.config().getBoolean(IS_FIRST_COLLECTURL, true)) {
                                                MaterialDialog(context()).show {
                                                    prepareShowInService(context())
                                                    message(text = "网址不同于文章，相同网址可多次进行收藏，且不会显示收藏状态。")
                                                    positiveButton(text = "知道了") {
                                                        DEF.config().save(IS_FIRST_COLLECTURL, false)
                                                        collectUrl(currentView.title, currentView.url)
                                                    }
                                                }
                                            } else {
                                                collectUrl(currentView.title, currentView.url)
                                            }
                                        } else {
                                            toast("请先登录")
                                        }
                                    } else {
                                        toast("暂不支持当前页面")
                                    }
                                }

                                override fun findInPage() {
                                    this@AbsBrowserPage.findInPage()
                                }

                                override fun addToHome() {
                                    if (currentView != null
                                        && currentView.url.isNotBlank()
                                        && !UrlUtils.isSpecialUrl(currentView.url)
                                    ) {
                                        HistoryEntry(currentView.url, currentView.title).also {
                                            Utils.createShortcut(context(), it, currentView.favicon)
                                            logger.log(TAG, "Creating shortcut: ${it.title} ${it.url}")
                                        }
                                    }
                                }

                                override fun readingMode() {
                                    if (currentUrl != null) {
                                        val read = Intent(context(), ReadingActivity::class.java)
                                        read.putExtra(LOAD_READING_URL, currentUrl)
                                        context().startActivity(read)
                                    }
                                }

                                override fun forward() {
                                    if (currentView?.canGoForward() == true) {
                                        currentView.goForward()
                                    }
                                }

                                override fun backward() {
                                    if (currentView?.canGoBack() == true) {
                                        currentView.goBack()
                                    }
                                }
                            }
                            customView(view = view)
                        }
                    }
                }
            }
        })
        return actionBar
    }

    lateinit var v: View

    private lateinit var progress_view: AnimatedProgressBar

    private lateinit var coordinator_layout: CoordinatorLayout
    private lateinit var drawer_layout: DrawerLayout
    private lateinit var ui_layout: KeyEventView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var content_frame: FrameLayout
    private lateinit var search_bar: RelativeLayout
    private lateinit var search_query: TextView
    private lateinit var button_back: ImageButton
    private lateinit var button_next: ImageButton
    private lateinit var button_quit: ImageButton
    private lateinit var left_drawer: FrameLayout
    private lateinit var right_drawer: FrameLayout

    override fun createView(context: Context): View {
        val origin = context
        val contextThemeWrapper: Context = ContextThemeWrapper(origin, theme())
        val themeAwareInflater = LayoutInflater.from(origin).cloneInContext(contextThemeWrapper)
        v = themeAwareInflater.inflate(layout(), null, false)
        fragmentView = v
        bindView(v)
        lazyInit(context)
        return v
    }

    @LayoutRes
    protected fun layout(): Int = R.layout.browser_page_main

    protected open fun bindView(view: View) {
        coordinator_layout = view.findViewById(R.id.coordinator_layout)
        drawer_layout = view.findViewById(R.id.drawer_layout)
        ui_layout = view.findViewById(R.id.ui_layout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        content_frame = view.findViewById(R.id.content_frame)
        search_bar = view.findViewById(R.id.search_bar)
        search_query = view.findViewById(R.id.search_query)
        button_back = view.findViewById(R.id.button_back)
        button_next = view.findViewById(R.id.button_next)
        button_quit = view.findViewById(R.id.button_quit)
        left_drawer = view.findViewById(R.id.left_drawer)
        right_drawer = view.findViewById(R.id.right_drawer)
    }

    fun lazyInit(context: Context) {
        val incognitoNotification = IncognitoNotification(context, notificationManager)
        tabsManager.addTabNumberChangedListener {
            if (isIncognito()) {
                if (it == 0) {
                    incognitoNotification.hide()
                } else {
                    incognitoNotification.show(it)
                }
            }
        }

        presenter = BrowserPresenter(
            context,
            this,
            this,
            isIncognito(),
            userPreferences,
            tabsManager,
            mainScheduler,
            homePageFactory,
            bookmarkPageFactory,
            RecentTabModel(),
            logger
        )

        registerKeyEvent()
        isDarkTheme = userPreferences.useTheme != 0 || isIncognito()
        iconColor = ThemeUtils.getIconThemeColor(context, isDarkTheme)
        disabledIconColor = if (isDarkTheme) {
            ContextCompat.getColor(context, R.color.icon_dark_theme_disabled)
        } else {
            ContextCompat.getColor(context, R.color.icon_light_theme_disabled)
        }
        swapBookmarksAndTabs = userPreferences.bookmarksAndTabsSwapped

        swipeRefreshLayout.setOnRefreshListener {
            tabsManager.currentTab?.reload()
        }

        // Drawer stutters otherwise
        left_drawer.setLayerType(View.LAYER_TYPE_NONE, null)
        right_drawer.setLayerType(View.LAYER_TYPE_NONE, null)

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    left_drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    right_drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                } else if (newState == DrawerLayout.STATE_IDLE) {
                    left_drawer.setLayerType(View.LAYER_TYPE_NONE, null)
                    right_drawer.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            }
        })

        setNavigationDrawerWidth(context)
        drawer_layout.addDrawerListener(DrawerLocker())

        webPageBitmap = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, isDarkTheme)

        val bookmarksFrameLayout = BookmarksFrameLayout.createBookmarksView(context, isIncognito(), this)
        bookmarksView = bookmarksFrameLayout

//        val tabsFrameLayout = TabsFrameLayout.createTabsView(context, isIncognito(), this)
//        tabsView = tabsFrameLayout
//        val tabDrawer = getTabDrawer()
//        tabDrawer.removeAllViews()
//        tabDrawer.addView(tabsFrameLayout)

        val bookmarkDrawer = getBookmarkDrawer()
        bookmarkDrawer.removeAllViews()
        bookmarkDrawer.addView(bookmarksFrameLayout)

        drawer_layout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END)
        drawer_layout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START)

        val launchedFromHistory = intent != null && intent!!.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

        proxyUtils.onStart(context)
        if (intent?.action == INTENT_PANIC_TRIGGER) {
            panicClean()
        } else {
            if (launchedFromHistory) {
                intent = null
            }
            presenter?.setupTabs(intent)
            proxyUtils.checkForProxy(context)
        }
    }
    //endregion

    fun context(): Context = fragmentView.context

    private fun registerKeyEvent() {
        ui_layout.listener = object : KeyEventView.KeyEventListener {
            override fun ctrl_F() {
                // Search in page
                findInPage()
            }

            override fun ctrl_T() {
                // Open new tab
                presenter?.newTab(homePageInitializer, true)
            }

            override fun ctrl_W() {
                // Close current tab
                tabsManager.let { presenter?.deleteTab(it.indexOfCurrentTab()) }
            }

            override fun ctrl_Q() {
                // Close browser
                closeBrowser()
            }

            override fun ctrl_R() {
                // Refresh current tab
                tabsManager.currentTab?.reload()
            }

            override fun ctrl_tab() {
                tabsManager.let {
                    // Go forward one tab
                    val nextIndex = if (it.indexOfCurrentTab() < it.last()) {
                        it.indexOfCurrentTab() + 1
                    } else {
                        0
                    }

                    presenter?.tabChanged(nextIndex)
                }
            }

            override fun ctrl_shift_tab() {
                tabsManager.let {
                    // Go back one tab
                    val nextIndex = if (it.indexOfCurrentTab() > 0) {
                        it.indexOfCurrentTab() - 1
                    } else {
                        it.last()
                    }

                    presenter?.tabChanged(nextIndex)
                }
            }

            override fun ctrl_shift_P() {
                presentFragment(IncognitoPage())
            }

            override fun search() {
                // Highlight search field
                onSearchMode()
                searchView?.requestFocus()
                searchView?.selectAll()
            }

            override fun alt_tab(number: Int) {
                tabsManager.let {
                    val nextIndex =
                        if (number > it.last() + KeyEvent.KEYCODE_1 || number == KeyEvent.KEYCODE_0) {
                            it.last()
                        } else {
                            number - KeyEvent.KEYCODE_1
                        }
                    presenter?.tabChanged(nextIndex)
                }
            }
        }
        v.setOnKeyListener { v, keyCode, event ->
            if (event?.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (searchView?.hasFocus() == true) {
                        searchView?.let { searchTheWeb(it.text.toString()) }
                    }
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    keyDownStartTime = System.currentTimeMillis()
                    mainHandler.postDelayed(longPressBackRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                }
            } else if (event?.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mainHandler.removeCallbacks(longPressBackRunnable)
                    if (System.currentTimeMillis() - keyDownStartTime > ViewConfiguration.getLongPressTimeout()) {
                        return@setOnKeyListener true
                    }
                }
            }
            return@setOnKeyListener false
        }

    }

    private fun getBookmarkDrawer(): ViewGroup = if (swapBookmarksAndTabs) {
        left_drawer
    } else {
        right_drawer
    }

    private fun getTabDrawer(): ViewGroup = if (swapBookmarksAndTabs) {
        right_drawer
    } else {
        left_drawer
    }

    protected fun panicClean() {
        logger.log(TAG, "Closing browser")
        tabsManager.newTab(context(), NoOpInitializer(), false, this)
        tabsManager.switchToTab(0)
        tabsManager.clearSavedState()

        historyPageFactory.deleteHistoryPage().subscribe()
        closeBrowser()
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        System.exit(1)
    }

    private inner class SearchListenerClass : OnKeyListener,
        OnEditorActionListener,
        OnFocusChangeListener,
        acr.browser.lightning.view.SearchView.PreFocusListener,
        TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

        override fun afterTextChanged(e: Editable) {
            e.getSpans(0, e.length, CharacterStyle::class.java).forEach(e::removeSpan)
            e.getSpans(0, e.length, ParagraphStyle::class.java).forEach(e::removeSpan)
        }

        override fun onKey(view: View, keyCode: Int, keyEvent: KeyEvent): Boolean {

            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    searchView?.let {
                        inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                        searchTheWeb(it.text.toString())
                    }

                    tabsManager.currentTab?.requestFocus()
                    return true
                }
                else -> {
                }
            }
            return false
        }

        override fun onEditorAction(arg0: TextView, actionId: Int, arg2: KeyEvent?): Boolean {
            // hide the keyboard and search the web when the enter key
            // button is pressed
            if (actionId == EditorInfo.IME_ACTION_GO
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_SEARCH
                || arg2?.action == KeyEvent.KEYCODE_ENTER
            ) {
                searchView?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                    searchTheWeb(it.text.toString())
                }

                tabsManager.currentTab?.requestFocus()
                return true
            }
            return false
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            val currentView = tabsManager.currentTab
            if (!hasFocus && currentView != null) {
                setIsLoading(currentView.progress < 100)
                updateUrl(currentView.url, false)
            } else if (hasFocus && currentView != null) {

                // Hack to make sure the text gets selected
                (v as acr.browser.lightning.view.SearchView).selectAll()
                iconDrawable = clearIconDrawable
                searchView?.setCompoundDrawablesWithIntrinsicBounds(null, null, clearIconDrawable, null)
            }

            if (!hasFocus) {
                searchView?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
        }

        override fun onPreFocus() {
            val currentView = tabsManager.currentTab ?: return
            val url = currentView.url
            if (!UrlUtils.isSpecialUrl(url)) {
                if (searchView?.hasFocus() == false) {
                    searchView?.setText(url)
                }
            }
        }
    }

    private inner class DrawerLocker : DrawerLayout.DrawerListener {

        override fun onDrawerClosed(v: View) {
            val tabsDrawer = getTabDrawer()
            val bookmarksDrawer = getBookmarkDrawer()

            if (v === tabsDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, bookmarksDrawer)
            }
        }

        override fun onDrawerOpened(v: View) {
            val tabsDrawer = getTabDrawer()
            val bookmarksDrawer = getBookmarkDrawer()

            if (v === tabsDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, bookmarksDrawer)
            } else {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, tabsDrawer)
            }
        }

        override fun onDrawerSlide(v: View, arg: Float) = Unit

        override fun onDrawerStateChanged(arg: Int) = Unit

    }

    private fun setNavigationDrawerWidth(context: Context) {
        val width = context.resources.displayMetrics.widthPixels - Utils.dpToPx(56f)
        val maxWidth = if (context.isTablet) {
            Utils.dpToPx(320f)
        } else {
            Utils.dpToPx(300f)
        }
        if (width > maxWidth) {
            val params = left_drawer.layoutParams as DrawerLayout.LayoutParams
            params.width = maxWidth
            left_drawer.layoutParams = params
            left_drawer.requestLayout()
            val paramsRight = right_drawer.layoutParams as DrawerLayout.LayoutParams
            paramsRight.width = maxWidth
            right_drawer.layoutParams = paramsRight
            right_drawer.requestLayout()
        } else {
            val params = left_drawer.layoutParams as DrawerLayout.LayoutParams
            params.width = width
            left_drawer.layoutParams = params
            left_drawer.requestLayout()
            val paramsRight = right_drawer.layoutParams as DrawerLayout.LayoutParams
            paramsRight.width = width
            right_drawer.layoutParams = paramsRight
            right_drawer.requestLayout()
        }
    }

    private fun initializePreferences() {
        val currentView = tabsManager.currentTab
        isFullScreen = userPreferences.fullScreenEnabled
        val colorMode = userPreferences.colorModeEnabled && !isDarkTheme

        webPageBitmap?.let { webBitmap ->
            if (!isIncognito() && !colorMode && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            } else if (!isIncognito() && currentView != null && !isDarkTheme) {
                changeToolbarBackground(currentView.favicon, null)
            } else if (!isIncognito() && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            }
        }

        tabsView?.reinitializePreferences()
        bookmarksView?.reinitializePreferences()

        // TODO layout transition causing memory leak
        //        content_frame.setLayoutTransition(new LayoutTransition());

        setFullscreen(userPreferences.hideStatusBarEnabled, false)

        val currentSearchEngine = searchEngineProvider.provideSearchEngine()
        searchText = currentSearchEngine.queryUrl

        updateCookiePreference().subscribeOn(Schedulers.computation()).subscribe()
        proxyUtils.updateProxySettings(context())
    }

    public fun onWindowVisibleToUserAfterResume() {
    }

    public fun openUrl(mUrl: String) {
        if (!TextUtils.isEmpty(mUrl)) {
            val myIntent = Intent()
            myIntent.data = Uri.parse(mUrl)
            presenter?.onNewIntent(this, myIntent)
        }
    }

    private fun toast(msg: String) {
        ToastUtil.i_long(msg)
    }

    private fun toast(@StringRes msgRes: Int) {
        ToastUtil.i_long(msgRes)
    }

    // 是否第一次收藏网址
    var IS_FIRST_COLLECTURL = "isFirstCollectUrl"

    private fun collectUrl(title: String, url: String) {
        // 收藏
        val record = RoomRecord.forName(title)
        record.content = url
        record.type = BLOCK_APP_WebApp
        RoomClient.recordDao().insert(record)
        toast("收藏网址成功")
    }

    // By using a manager, adds a bookmark and notifies third parties about that
    private fun addBookmark(title: String, url: String) {
        bookmarkManager.addBookmarkIfNotExists(Bookmark.Entry(url, title, 0, Bookmark.Folder.Root))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { boolean ->
                if (boolean) {
                    suggestionsAdapter?.refreshBookmarks()
                    bookmarksView?.handleUpdatedUrl(url)
                    toast(R.string.message_bookmark_added)
                }
            }
    }

    private fun deleteBookmark(title: String, url: String) {
        bookmarkManager.deleteBookmark(Bookmark.Entry(url, title, 0, Bookmark.Folder.Root))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { boolean ->
                if (boolean) {
                    suggestionsAdapter?.refreshBookmarks()
                    bookmarksView?.handleUpdatedUrl(url)
                }
            }
    }

    private fun putToolbarInRoot() {
    }

    private fun overlayToolbarOnWebView() {
    }

    private fun setWebViewTranslation(translation: Float) =
        if (isFullScreen) {
            currentTabView?.translationY = translation
        } else {
            currentTabView?.translationY = 0f
        }

    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    private fun findInPage() = BrowserDialog.showEditText(
        context(),
        R.string.action_find,
        R.string.search_hint,
        R.string.search_hint
    ) { text ->
        if (text.isNotEmpty()) {
            findResult = presenter?.findInPage(text)
            showFindInPageControls(text)
        }
    }

    private fun showFindInPageControls(text: String) {
        search_bar.visibility = View.VISIBLE

        v.findViewById<TextView>(R.id.search_query).apply { this.text = "'$text'" }
        v.findViewById<ImageButton>(R.id.button_next)?.setOnClickListener(this)
        v.findViewById<ImageButton>(R.id.button_back)?.setOnClickListener(this)
        v.findViewById<ImageButton>(R.id.button_quit)?.setOnClickListener(this)
    }

    override fun getTabModel(): TabsManager = tabsManager

    override fun showCloseDialog(position: Int) {
        if (position < 0) {
            return
        }
        BrowserDialog.show(
            context(),
            R.string.dialog_title_close_browser,
            DialogItem(title = R.string.close_tab) {
                presenter?.deleteTab(position)
            },
            DialogItem(title = R.string.close_other_tabs) {
                presenter?.closeAllOtherTabs()
            },
            DialogItem(title = R.string.close_all_tabs, onClick = this::closeBrowser)
        )
    }

    override fun notifyTabViewRemoved(position: Int) {
        logger.log(TAG, "Notify Tab Removed: $position")
        tabsView?.tabRemoved(position)
    }

    override fun notifyTabViewAdded() {
        logger.log(TAG, "Notify Tab Added")
        tabsView?.tabAdded()
    }

    override fun notifyTabViewChanged(position: Int) {
        logger.log(TAG, "Notify Tab Changed: $position")
        tabsView?.tabChanged(position)
    }

    override fun notifyTabViewInitialized() {
        logger.log(TAG, "Notify Tabs Initialized")
        tabsView?.tabsInitialized()
    }

    override fun updateSslState(sslState: SSLState) {
        sslDrawable = when (sslState) {
            is SSLState.None -> null
            is SSLState.Valid -> {
                val bitmap =
                    DrawableUtils.getImageInsetInRoundedSquare(context(), R.drawable.ic_secured, R.color.ssl_secured)
                val securedDrawable = BitmapDrawable(context().resources, bitmap)
                securedDrawable
            }
            is SSLState.Invalid -> {
                val bitmap =
                    DrawableUtils.getImageInsetInRoundedSquare(context(), R.drawable.ic_unsecured, R.color.ssl_unsecured)
                val unsecuredDrawable = BitmapDrawable(context().resources, bitmap)
                unsecuredDrawable
            }
        }

        searchView?.setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, iconDrawable, null)
    }

    override fun tabChanged(tab: LightningView) {
        presenter?.tabChangeOccurred(tab)
    }

    override fun removeTabView() {

        logger.log(TAG, "Remove the tab view")

        // Set the background color so the color mode color doesn't show through
        content_frame.setBackgroundColor(backgroundColor)

        currentTabView.removeFromParent()

        currentTabView = null

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        mainHandler.postDelayed(drawer_layout::closeDrawers, 200)

    }

    override fun setTabView(view: View) {
        if (currentTabView == view) {
            return
        }

        logger.log(TAG, "Setting the tab view")

        // Set the background color so the color mode color doesn't show through
        content_frame.setBackgroundColor(backgroundColor)

        view.removeFromParent()
        currentTabView.removeFromParent()

        content_frame.addView(view, 0, MATCH_PARENT)

        view.requestFocus()

        currentTabView = view

        showActionBar()

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        mainHandler.postDelayed(drawer_layout::closeDrawers, 200)

        // mainHandler.postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        // Remove browser frame background to reduce overdraw
        //TODO evaluate performance
        //         content_frame.setBackgroundColor(Color.TRANSPARENT);
        //     }
        // }, 300);
    }

    override fun showBlockedLocalFileDialog(onPositiveClick: () -> Unit) {
        MaterialDialog(context()).show {
            prepareShowInService(context())
            title(R.string.title_warning)
            message(R.string.message_blocked_local)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.action_open) { onPositiveClick() }
        }
    }

    override fun showSnackbar(@StringRes resource: Int) = Snackbar.make(v, resource, Snackbar.LENGTH_SHORT).show()

    override fun tabCloseClicked(position: Int) {
        presenter?.deleteTab(position)
    }

    override fun tabClicked(position: Int) {
        LogUtil.se("pos=${position}, cur=${tabsManager.indexOfCurrentTab()}")
        if (tabsManager.indexOfCurrentTab() == position) {
            onSearchMode()
        } else {
            showTab(position)
        }
    }

    fun onSearchMode() {
        val toggle = !actionBar.isSearchFieldVisible
        val text = tabsManager.currentTab?.url ?: ""
        val animated = true
        if (toggle) {
            tabsFrameLayout.openSearch(true)
        }
        tabsFrameLayout.setSearchFieldText(text, animated)
        tabsFrameLayout.searchField.setSelection(text.length)
    }

    override fun newTabButtonClicked() {
        presenter?.newTab(homePageInitializer, true)
    }

    override fun newTabButtonLongClicked() {
        presenter?.onNewTabLongClicked()
    }

    override fun bookmarkButtonClicked() {
        val currentTab = tabsManager.currentTab
        val url = currentTab?.url
        val title = currentTab?.title
        if (url == null || title == null) {
            return
        }

        if (!UrlUtils.isSpecialUrl(url)) {
            bookmarkManager.isBookmark(url)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { boolean ->
                    if (boolean) {
                        deleteBookmark(title, url)
                    } else {
                        addBookmark(title, url)
                    }
                }
        }
    }

    override fun bookmarkItemClicked(entry: Bookmark.Entry) {
        presenter?.loadUrlInCurrentView(entry.url)
        // keep any jank from happening when the drawer is closed after the URL starts to load
        mainHandler.postDelayed({ closeDrawers(null) }, 150)
    }

    override fun handleHistoryChange() {
        historyPageFactory
            .buildPage()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(onSuccess = { tabsManager.currentTab?.reload() })
    }

    /**
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     *
     * @param position the position of the tab to display
     */
    private fun showTab(position: Int) {
        presenter?.tabChanged(position)
    }

    protected fun handleNewIntent(intent: Intent) {
        presenter?.onNewIntent(this, intent)
    }

    protected fun performExitCleanUp() {
        val currentTab = tabsManager.currentTab
        if (userPreferences.clearCacheExit && currentTab != null && !isIncognito()) {
            WebUtils.clearCache(currentTab.webView)
            logger.log(TAG, "Cache Cleared")
        }
        if (userPreferences.clearHistoryExitEnabled && !isIncognito()) {
            WebUtils.clearHistory(context(), historyModel, databaseScheduler)
            logger.log(TAG, "History Cleared")
        }
        if (userPreferences.clearCookiesExitEnabled && !isIncognito()) {
            WebUtils.clearCookies(context())
            logger.log(TAG, "Cookies Cleared")
        }
        if (userPreferences.clearWebStorageExitEnabled && !isIncognito()) {
            WebUtils.clearWebStorage()
            logger.log(TAG, "WebStorage Cleared")
        } else if (isIncognito()) {
            WebUtils.clearWebStorage()     // We want to make sure incognito mode is secure
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        logger.log(TAG, "onConfigurationChanged")

        if (isFullScreen) {
            showActionBar()
        }
    }

    override fun closeBrowser() {
        content_frame.setBackgroundColor(backgroundColor)
        currentTabView.removeFromParent()
        performExitCleanUp()
        val size = tabsManager.size()
        tabsManager.shutdown()
        currentTabView = null
        for (n in 0 until size) {
            tabsView?.tabRemoved(0)
        }
    }

    override fun onBackPressed(): Boolean {
        logger.log(TAG, "onBackPressed")
        Log.e(TAG, "onBackPressed")
        val currentTab = tabsManager.currentTab
        if (drawer_layout.isDrawerOpen(getTabDrawer())) {
            drawer_layout.closeDrawer(getTabDrawer())
            return true
        } else if (drawer_layout.isDrawerOpen(getBookmarkDrawer())) {
            bookmarksView?.navigateBack()
            return true
        } else {
            if (currentTab != null) {
                logger.log(TAG, "currentTab != null")
                if (searchView?.hasFocus() == true) {
                    logger.log(TAG, "searchView?.hasFocus() == true")
                    currentTab.requestFocus()
                    return true
                } else if (currentTab.canGoBack()) {
                    logger.log(TAG, "currentTab.canGoBack()")
                    if (!currentTab.isShown) {
                        onHideCustomView()
                    } else {
                        currentTab.goBack()
                    }
                    return true
                } else {
                    logger.log(TAG, "else")
                    if (customView != null || customViewCallback != null) {
                        onHideCustomView()
                    } else {
                        return presenter?.deleteTab(tabsManager.positionOf(currentTab)) == true
                    }
                    return true
                }
            } else {
                logger.log(TAG, "This shouldn't happen ever")
                return false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        logger.log(TAG, "onPause")
        tabsManager.pauseAll()
    }

    protected fun saveOpenTabs() {
        if (userPreferences.restoreLostTabsEnabled) {
            tabsManager.saveState()
        }
    }

    override fun onFragmentCreate(): Boolean {
        return super.onFragmentCreate()
    }

    override fun onFragmentDestroy() {
        proxyUtils.onStop()
        logger.log(TAG, "onDestroy")

        mainHandler.removeCallbacksAndMessages(null)

        presenter?.shutdown()

        super.onFragmentDestroy()
    }

    override fun onResume() {
        super.onResume()
        logger.log(TAG, "onResume")

        suggestionsAdapter?.let {
            it.refreshPreferences()
            it.refreshBookmarks()
        }
        tabsManager.resumeAll()
        initializePreferences()
        tabsView?.onResume()
        bookmarksView?.onResume()

        if (isFullScreen) {
            overlayToolbarOnWebView()
        } else {
            putToolbarInRoot()
        }
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private fun searchTheWeb(query: String) {
        val currentTab = tabsManager.currentTab
        if (query.isEmpty()) {
            return
        }
        val searchUrl = "$searchText${UrlUtils.QUERY_PLACE_HOLDER}"
        if (currentTab != null) {
            currentTab.stopLoading()
            presenter?.loadUrlInCurrentView(UrlUtils.smartUrlFilter(query.trim(), true, searchUrl))
        }
    }

    protected fun loadUrl(url: String) {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            currentTab.stopLoading()
            presenter?.loadUrlInCurrentView(url)
        } else {
            handleNewTab(LightningDialogBuilder.NewTab.FOREGROUND, url)
        }
    }

    /**
     * Animates the color of the toolbar from one color to another. Optionally animates
     * the color of the tab background, for use when the tabs are displayed on the top
     * of the screen.
     *
     * @param favicon the Bitmap to extract the color from
     * @param tabBackground the optional LinearLayout to color
     */
    override fun changeToolbarBackground(favicon: Bitmap, tabBackground: Drawable?) {
        val defaultColor = ContextCompat.getColor(context(), R.color.primary_color)
        if (currentUiColor == Color.BLACK) {
            currentUiColor = defaultColor
        }
        Palette.from(favicon).generate { palette ->
            // OR with opaque black to remove transparency glitches
            val color = Color.BLACK or (palette?.getVibrantColor(defaultColor) ?: defaultColor)

            // Lighten up the dark color if it is too dark
            val finalColor = if (Utils.isColorTooDark(color)) {
                Utils.mixTwoColors(defaultColor, color, 0.25f)
            } else {
                color
            }

            v.setBackground(ColorDrawable(Color.BLACK))

            val startSearchColor = getSearchBarColor(currentUiColor, defaultColor)
            val finalSearchColor = getSearchBarColor(finalColor, defaultColor)

            val animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    val animatedColor = DrawableUtils.mixColor(interpolatedTime, currentUiColor, finalColor)
                    tabBackground?.setColorFilter(animatedColor, PorterDuff.Mode.SRC_IN)
                    currentUiColor = animatedColor
                    actionBar.setBackgroundColor(animatedColor)
                    searchBackground?.background?.setColorFilter(
                        DrawableUtils.mixColor(
                            interpolatedTime,
                            startSearchColor, finalSearchColor
                        ), PorterDuff.Mode.SRC_IN
                    )
                }
            }
            animation.duration = 300
            actionBar.startAnimation(animation)
        }
    }

    private fun getSearchBarColor(requestedColor: Int, defaultColor: Int): Int =
        if (requestedColor == defaultColor) {
            if (isDarkTheme) DrawableUtils.mixColor(0.25f, defaultColor, Color.WHITE) else Color.WHITE
        } else {
            DrawableUtils.mixColor(0.25f, requestedColor, Color.WHITE)
        }

    override fun getUseDarkTheme(): Boolean = isDarkTheme

    @ColorInt
    override fun getUiColor(): Int = currentUiColor

    override fun updateUrl(url: String?, isLoading: Boolean) {
        if (url == null || searchView?.hasFocus() != false) {
            return
        }
        val currentTab = tabsManager.currentTab
        bookmarksView?.handleUpdatedUrl(url)

        val currentTitle = currentTab?.title

        searchView?.setText(searchBoxModel.getDisplayContent(url, currentTitle, isLoading))
    }

    override fun updateTabNumber(number: Int) {
//        if (shouldShowTabsInDrawer) {
//            if (isIncognito()) {
//                arrowImageView?.setImageDrawable(ThemeUtils.getThemedDrawable(context(), R.drawable.incognito_mode, true))
//            } else {
//                arrowImageView?.setImageBitmap(
//                    DrawableUtils.getRoundedNumberImage(
//                        number, Utils.dpToPx(24f),
//                        Utils.dpToPx(24f), ThemeUtils.getIconThemeColor(context(), isDarkTheme), Utils.dpToPx(2.5f)
//                    )
//                )
//            }
//        }
    }

    override fun updateProgress(progress: Int) {
        setIsLoading(progress < 100)
        progress_view.progress = progress
    }

    protected fun addItemToHistory(title: String?, url: String) {
        if (UrlUtils.isSpecialUrl(url)) {
            return
        }

        historyModel.visitHistoryEntry(url, title)
            .subscribeOn(databaseScheduler)
            .subscribe()
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private fun initializeSearchSuggestions(context: Context, getUrl: AutoCompleteTextView) {

        suggestionsAdapter = SuggestionsAdapter(context, isDarkTheme, isIncognito())

        getUrl.threshold = 1
        getUrl.dropDownWidth = -1
        getUrl.dropDownAnchor = R.id.toolbar_layout
        getUrl.onItemClickListener = OnItemClickListener { _, view, _, _ ->
            var url: String? = null
            val urlString = (view.findViewById<View>(R.id.url) as TextView).text
            if (urlString != null) {
                url = urlString.toString()
            }
            if (url == null || url.startsWith(getUrl.resources.getString(R.string.suggestion))) {
                val searchString = (view.findViewById<View>(R.id.title) as TextView).text
                if (searchString != null) {
                    url = searchString.toString()
                }
            }
            if (url == null) {
                return@OnItemClickListener
            }
            getUrl.setText(url)
            searchTheWeb(url)
            inputMethodManager.hideSoftInputFromWindow(getUrl.windowToken, 0)
            presenter?.onAutoCompleteItemPressed()
        }

        getUrl.setSelectAllOnFocus(true)
        getUrl.setAdapter<SuggestionsAdapter>(suggestionsAdapter)
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private fun openHistory() {
        presenter?.newTab(historyPageInitializer, true)
    }

    private fun openDownloads() {
        presenter?.newTab(downloadPageInitializer, true)
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private fun openBookmarks() {
        if (drawer_layout.isDrawerOpen(getTabDrawer())) {
            drawer_layout.closeDrawers()
        }
        drawer_layout.openDrawer(getBookmarkDrawer())
    }

    /**
     * This method closes any open drawer and executes the runnable after the drawers are closed.
     *
     * @param runnable an optional runnable to run after the drawers are closed.
     */
    protected fun closeDrawers(runnable: (() -> Unit)?) {
        if (!drawer_layout.isDrawerOpen(left_drawer) && !drawer_layout.isDrawerOpen(right_drawer)) {
            if (runnable != null) {
                runnable()
                return
            }
        }
        drawer_layout.closeDrawers()

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                runnable?.invoke()
                drawer_layout.removeDrawerListener(this)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit
        })
    }

    override fun setForwardButtonEnabled(enabled: Boolean) {
        val colorFilter = if (enabled) {
            iconColor
        } else {
            disabledIconColor
        }
        forwardMenuItem?.icon?.setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN)
        forwardMenuItem?.icon = forwardMenuItem?.icon
    }

    override fun setBackButtonEnabled(enabled: Boolean) {
        val colorFilter = if (enabled) {
            iconColor
        } else {
            disabledIconColor
        }
        backMenuItem?.icon?.setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN)
        backMenuItem?.icon = backMenuItem?.icon
    }

    fun onCreateOptionsMenu(menu: Menu) {
        backMenuItem = menu.findItem(R.id.action_back)?.apply {
            icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        }
        forwardMenuItem = menu.findItem(R.id.action_forward)?.apply {
            icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        }
    }

    /**
     * opens a file chooser
     * param ValueCallback is the message from the WebView indicating a file chooser
     * should be opened
     */
    override fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
        uploadMessageCallback = uploadMsg
        startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }, context().getString(R.string.title_file_chooser)), FILE_CHOOSER_REQUEST_CODE)
    }

    /**
     * used to allow uploading into the browser
     */
    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) {
                    null
                } else {
                    intent.data
                }

                uploadMessageCallback?.onReceiveValue(result)
                uploadMessageCallback = null
            } else {
                val results: Array<Uri>? = if (resultCode == Activity.RESULT_OK) {
                    if (intent == null) {
                        // If there is not data, then we may have taken a photo
                        cameraPhotoPath?.let { arrayOf(Uri.parse(it)) }
                    } else {
                        intent.dataString?.let { arrayOf(Uri.parse(it)) }
                    }
                } else {
                    null
                }

                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            }
        } else {
            super.onActivityResultFragment(requestCode, resultCode, intent)
        }
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        // Create the File where the photo should go
        val intentArray: Array<Intent> = try {
            arrayOf(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra("PhotoPath", cameraPhotoPath)
                putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(Utils.createImageFile().also { file ->
                        cameraPhotoPath = "file:${file.absolutePath}"
                    })
                )
            })
        } catch (ex: IOException) {
            // Error occurred while creating the File
            logger.log(TAG, "Unable to create Image File", ex)
            emptyArray()
        }

        startActivityForResult(Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            })
            putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        }, FILE_CHOOSER_REQUEST_CODE)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrientation: Int) {
        onShowCustomView(view, callback)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val currentTab = tabsManager.currentTab
        if (customView != null) {
            try {
                callback.onCustomViewHidden()
            } catch (e: Exception) {
                logger.log(TAG, "Error hiding custom view", e)
            }

            return
        }

        try {
            view.keepScreenOn = true
        } catch (e: SecurityException) {
            logger.log(TAG, "WebView is not allowed to keep the screen on")
        }

        customViewCallback = callback
        customView = view

        val decorView = v as ViewGroup

        fullscreenContainerView = FrameLayout(context())
        fullscreenContainerView?.setBackgroundColor(ContextCompat.getColor(context(), R.color.black))
        if (view is FrameLayout) {
            val child = view.focusedChild
            if (child is VideoView) {
                videoView = child
                child.setOnErrorListener(VideoCompletionListener())
                child.setOnCompletionListener(VideoCompletionListener())
            }
        } else if (view is VideoView) {
            videoView = view
            view.setOnErrorListener(VideoCompletionListener())
            view.setOnCompletionListener(VideoCompletionListener())
        }
        decorView.addView(fullscreenContainerView, COVER_SCREEN_PARAMS)
        fullscreenContainerView?.addView(customView, COVER_SCREEN_PARAMS)
        decorView.requestLayout()
        setFullscreen(true, true)
        currentTab?.setVisibility(View.INVISIBLE)
    }

    override fun onHideCustomView() {
        val currentTab = tabsManager.currentTab
        if (customView == null || customViewCallback == null || currentTab == null) {
            if (customViewCallback != null) {
                try {
                    customViewCallback?.onCustomViewHidden()
                } catch (e: Exception) {
                    logger.log(TAG, "Error hiding custom view", e)
                }

                customViewCallback = null
            }
            return
        }
        logger.log(TAG, "onHideCustomView")
        currentTab.setVisibility(View.VISIBLE)
        try {
            customView?.keepScreenOn = false
        } catch (e: SecurityException) {
            logger.log(TAG, "WebView is not allowed to keep the screen on")
        }

        setFullscreen(userPreferences.hideStatusBarEnabled, false)
        if (fullscreenContainerView != null) {
            val parent = fullscreenContainerView?.parent as ViewGroup
            parent.removeView(fullscreenContainerView)
            fullscreenContainerView?.removeAllViews()
        }

        fullscreenContainerView = null
        customView = null

        logger.log(TAG, "VideoView is being stopped")
        videoView?.stopPlayback()
        videoView?.setOnErrorListener(null)
        videoView?.setOnCompletionListener(null)
        videoView = null

        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {
            logger.log(TAG, "Error hiding custom view", e)
        }

        customViewCallback = null
    }

    private inner class VideoCompletionListener : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

        override fun onCompletion(mp: MediaPlayer) = onHideCustomView()

    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        logger.log(TAG, "onWindowFocusChanged")
        if (hasFocus) {
            setFullscreen(hideStatusBar, isImmersiveMode)
        }
    }

    override fun onBackButtonPressed() {
        if (drawer_layout.closeDrawerIfOpen(getTabDrawer())) {
            val currentTab = tabsManager.currentTab
            if (currentTab?.canGoBack() == true) {
                currentTab.goBack()
            } else if (currentTab != null) {
                tabsManager.let { presenter?.deleteTab(it.positionOf(currentTab)) }
            }
        } else if (drawer_layout.closeDrawerIfOpen(getBookmarkDrawer())) {
            // Don't do anything other than close the bookmarks drawer when the activity is being
            // delegated to.
        }
    }

    override fun onForwardButtonPressed() {
        val currentTab = tabsManager.currentTab
        if (currentTab?.canGoForward() == true) {
            currentTab.goForward()
            closeDrawers(null)
        }
    }

    override fun onHomeButtonPressed() {
        tabsManager.currentTab?.loadHomePage()
        closeDrawers(null)
    }

    /**
     * This method sets whether or not the activity will display
     * in full-screen mode (i.e. the ActionBar will be hidden) and
     * whether or not immersive mode should be set. This is used to
     * set both parameters correctly as during a full-screen video,
     * both need to be set, but other-wise we leave it up to user
     * preference.
     *
     * @param enabled   true to enable full-screen, false otherwise
     * @param immersive true to enable immersive mode, false otherwise
     */
    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        hideStatusBar = enabled
        isImmersiveMode = immersive
    }

    /**
     * This method handles the JavaScript callback to create a new tab.
     * Basically this handles the event that JavaScript needs to create
     * a popup.
     *
     * @param resultMsg the transport message used to send the URL to
     * the newly created WebView.
     */
    override fun onCreateWindow(resultMsg: Message) {
        presenter?.newTab(ResultMessageInitializer(resultMsg), true)
    }

    /**
     * Closes the specified [LightningView]. This implements
     * the JavaScript callback that asks the tab to close itself and
     * is especially helpful when a page creates a redirect and does
     * not need the tab to stay open any longer.
     *
     * @param tab the LightningView to close, delete it.
     */
    override fun onCloseWindow(tab: LightningView) {
        presenter?.deleteTab(tabsManager.positionOf(tab))
    }

    /**
     * Hide the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    override fun hideActionBar() {
        if (isFullScreen) {
//            val height = toolbar_layout.height
//            if (toolbar_layout.translationY > -0.01f) {
//                val hideAnimation = object : Animation() {
//                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
//                        val trans = interpolatedTime * height
//                        toolbar_layout.translationY = -trans
//                        actionBar.translationY = -trans
//                        setWebViewTranslation(height - trans)
//                    }
//                }
//                hideAnimation.duration = 250
//                hideAnimation.interpolator = BezierDecelerateInterpolator()
//                content_frame.startAnimation(hideAnimation)
//            }
        }
    }

    /**
     * Display the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    override fun showActionBar() {
        if (isFullScreen) {
//            logger.log(TAG, "showActionBar")
//            var height = toolbar_layout.height
//            if (height == 0) {
//                toolbar_layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
//                height = toolbar_layout.measuredHeight
//            }
//
//            val totalHeight = height
//            if (toolbar_layout.translationY < -(height - 0.01f)) {
//                val show = object : Animation() {
//                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
//                        val trans = interpolatedTime * totalHeight
//                        toolbar_layout.translationY = trans - totalHeight
//                        actionBar.translationY = trans - totalHeight
//                        setWebViewTranslation(trans)
//                    }
//                }
//                show.duration = 250
//                show.interpolator = BezierDecelerateInterpolator()
//                content_frame.startAnimation(show)
//            }
        }
    }

    override fun handleBookmarksChange() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && UrlUtils.isBookmarkUrl(currentTab.url)) {
            currentTab.loadBookmarkPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleDownloadDeleted() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && UrlUtils.isDownloadsUrl(currentTab.url)) {
            currentTab.loadDownloadsPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) {
        bookmarksView?.handleBookmarkDeleted(bookmark)
        handleBookmarksChange()
    }

    override fun handleNewTab(newTabType: LightningDialogBuilder.NewTab, url: String) {
        val urlInitializer = UrlInitializer(url)
        when (newTabType) {
            LightningDialogBuilder.NewTab.FOREGROUND -> presenter?.newTab(urlInitializer, true)
            LightningDialogBuilder.NewTab.BACKGROUND -> presenter?.newTab(urlInitializer, false)
            LightningDialogBuilder.NewTab.INCOGNITO -> {
                drawer_layout.closeDrawers()
                presentFragment(IncognitoPage(Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    data = Uri.parse(url)
                }))
            }
        }
    }

    /**
     * This method lets the search bar know that the page is currently loading
     * and that it should display the stop icon to indicate to the user that
     * pressing it stops the page from loading
     */
    private fun setIsLoading(isLoading: Boolean) {
        if (searchView?.hasFocus() == false) {
            iconDrawable = if (isLoading) deleteIconDrawable else refreshIconDrawable
            searchView?.setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, iconDrawable, null)
        }
        swipeRefreshLayout.isRefreshing = isLoading
    }

    /**
     * handle presses on the refresh icon in the search bar, if the page is
     * loading, stop the page, if it is done loading refresh the page.
     * See setIsFinishedLoading and setIsLoading for displaying the correct icon
     */
    private fun refreshOrStop() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            if (currentTab.progress < 100) {
                currentTab.stopLoading()
            } else {
                currentTab.reload()
            }
        }
    }

    /**
     * Handle the click event for the views that are using
     * this class as a click listener. This method should
     * distinguish between the various views using their IDs.
     *
     * @param v the view that the user has clicked
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_next -> findResult?.nextResult()
            R.id.button_back -> findResult?.previousResult()
            R.id.button_quit -> {
                findResult?.clearResults()
                findResult = null
                search_bar.visibility = View.GONE
            }
        }
    }

    /**
     * If the [drawer] is open, close it and return true. Return false otherwise.
     */
    private fun DrawerLayout.closeDrawerIfOpen(drawer: View): Boolean =
        if (isDrawerOpen(drawer)) {
            closeDrawer(drawer)
            true
        } else {
            false
        }

    companion object {

        private const val TAG = "AbsBrowserPage"

        const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"

        private const val FILE_CHOOSER_REQUEST_CODE = 1111

        // Constant
        private val MATCH_PARENT = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    }

    init {
        injector.inject(this)
    }
}