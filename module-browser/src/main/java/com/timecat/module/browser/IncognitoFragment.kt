package com.timecat.module.browser

import acr.browser.lightning.IncognitoActivity
import acr.browser.lightning.R
import acr.browser.lightning.browser.activity.BrowserActivity
import acr.browser.lightning.browser.activity.ThemableBrowserActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.timecat.identity.readonly.RouterHub
import com.xiaojinzi.component.anno.FragmentAnno
import io.reactivex.Completable
import org.greenrobot.eventbus.Subscribe

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/25
 * @description null
 * @usage null
 */
@FragmentAnno(RouterHub.MASTER_IncognitoFragment)
class IncognitoFragment : AbsBrowserFragment() {
    override fun theme():Int = R.style.Theme_DarkTheme
    override fun menu(): Int =R.menu.incognito
    @Suppress("DEPRECATION")
    public override fun updateCookiePreference(): Completable = Completable.fromAction {
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(_mActivity)
        }
        cookieManager.setAcceptCookie(userPreferences.incognitoCookiesEnabled)
    }

    @Suppress("RedundantOverride")
    override fun onPause() = super.onPause() // saveOpenTabs();

    override fun updateHistory(title: String?, url: String) = Unit // addItemToHistory(title, url)

    override fun isIncognito() = true

    override fun closeActivity() = closeDrawers(this::closeBrowser)

    companion object {
        /**
         * Creates the intent with which to launch the activity. Adds the reorder to front flag.
         */
        fun createIntent(context: Context, uri: Uri? = null) = Intent(context, IncognitoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            data = uri
        }
    }
}