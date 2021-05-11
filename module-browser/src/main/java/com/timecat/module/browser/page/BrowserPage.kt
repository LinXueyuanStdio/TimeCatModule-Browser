package com.timecat.module.browser.page

import acr.browser.lightning.R
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import io.reactivex.Completable

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/1
 * @description null
 * @usage null
 */
class BrowserPage(
    intent: Intent? = null
) : AbsBrowserPage(intent) {

    @Suppress("DEPRECATION")
    public override fun updateCookiePreference(): Completable = Completable.fromAction {
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context())
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
        finishFragment()
    }
}