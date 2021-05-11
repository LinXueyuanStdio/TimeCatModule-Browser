package com.timecat.module.browser

import android.content.Context
import com.same.lib.core.BasePage
import com.timecat.middle.block.service.BrowserPageService
import com.timecat.middle.block.service.GLOBAL_BrowserPageServiceImpl
import com.timecat.module.browser.page.BrowserPage
import com.timecat.module.browser.page.IncognitoPage
import com.xiaojinzi.component.anno.ServiceAnno

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/11
 * @description null
 * @usage null
 */
@ServiceAnno(BrowserPageService::class, name = [GLOBAL_BrowserPageServiceImpl])
class BrowserPageServiceImpl : BrowserPageService {
    override fun openPage(context: Context, url: String, isIncognito: Boolean): BasePage {
        if (isIncognito) {
            return IncognitoPage()
        } else {
            return BrowserPage()
        }
    }
}