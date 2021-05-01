package com.timecat.module.browser.fragment

import acr.browser.lightning.R
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.timecat.identity.readonly.RouterHub
import com.xiaojinzi.component.anno.FragmentAnno
import io.reactivex.Completable

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/25
 * @description null
 * @usage null
 */
@FragmentAnno(RouterHub.MASTER_BrowserFragment)
class BrowserFragment : AbsBrowserFragment() {

    @Suppress("DEPRECATION")
    public override fun updateCookiePreference(): Completable = Completable.fromAction {
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(_mActivity)
        }
        cookieManager.setAcceptCookie(userPreferences.cookiesEnabled)
    }

    override fun menu(): Int = R.menu.main

    override fun onPause() {
        super.onPause()
        saveOpenTabs()
    }

    override fun updateHistory(title: String?, url: String) = addItemToHistory(title, url)

    override fun isIncognito() = false

    override fun closeActivity() = closeDrawers {
        performExitCleanUp()
    }
}