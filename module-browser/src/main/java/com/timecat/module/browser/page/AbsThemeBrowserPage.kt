package com.timecat.module.browser.page

import acr.browser.lightning.di.injector
import acr.browser.lightning.preference.UserPreferences
import android.content.Context
import android.content.res.Configuration
import com.same.lib.core.BasePage
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/1
 * @description null
 * @usage null
 */
abstract class AbsThemeBrowserPage : BasePage() {

    @Inject
    protected lateinit var userPreferences: UserPreferences

    private var themeId: Int = 0
    private var showTabsInDrawer: Boolean = false

    init {
        injector.inject(this)
        themeId = userPreferences.useTheme
        showTabsInDrawer = userPreferences.showTabsInDrawer
    }

    protected val Context.isTablet: Boolean
        get() = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE

}