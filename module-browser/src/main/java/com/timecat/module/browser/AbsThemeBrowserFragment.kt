package com.timecat.module.browser

import acr.browser.lightning.di.injector
import acr.browser.lightning.preference.UserPreferences
import android.content.res.Configuration
import android.os.Bundle
import com.timecat.page.base.base.lazyload.BaseLazyLoadSupportFragment
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/25
 * @description null
 * @usage null
 */
abstract class AbsThemeBrowserFragment: BaseLazyLoadSupportFragment() {

    // TODO reduce protected visibility
    @Inject
    protected lateinit var userPreferences: UserPreferences

    private var themeId: Int = 0
    private var showTabsInDrawer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
        themeId = userPreferences.useTheme
        showTabsInDrawer = userPreferences.showTabsInDrawer
    }

    protected val isTablet: Boolean
        get() = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE

}