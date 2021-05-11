package acr.browser.lightning.browser.activity

import acr.browser.lightning.di.injector
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.ThemeUtils
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import com.timecat.page.base.base.theme.BaseThemeActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import javax.inject.Inject

abstract class ThemableBrowserActivity : BaseThemeActivity() {

    // TODO reduce protected visibility
    @Inject
    protected lateinit var userPreferences: UserPreferences

    private var themeId: Int = 0
    private var showTabsInDrawer: Boolean = false
    private var shouldRunOnResumeActions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        themeId = userPreferences.useTheme
        showTabsInDrawer = userPreferences.showTabsInDrawer
        super.onCreate(savedInstanceState)

        resetPreferences()
    }

    private fun resetPreferences() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (userPreferences.useBlackStatusBar) {
                window.statusBarColor = Color.BLACK
            } else {
                window.statusBarColor = ThemeUtils.getStatusBarColor(this)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && shouldRunOnResumeActions) {
            shouldRunOnResumeActions = false
            onWindowVisibleToUserAfterResume()
        }
    }

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    protected open fun onWindowVisibleToUserAfterResume() = Unit

    override fun onResume() {
        super.onResume()
        resetPreferences()
        shouldRunOnResumeActions = true
        val themePreference = userPreferences.useTheme
        val drawerTabs = userPreferences.showTabsInDrawer
        if (themeId != themePreference || showTabsInDrawer != drawerTabs) {
            restart()
        }
    }

    protected val isTablet: Boolean
        get() = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE

    protected fun restart() {
        finish()
        startActivity(Intent(this, javaClass))
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    class DefaultEvent

    @Subscribe
    open fun onEvent(e: DefaultEvent) {
    }
}
