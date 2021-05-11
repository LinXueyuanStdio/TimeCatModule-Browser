package com.same.ui

import androidx.fragment.app.Fragment
import com.timecat.module.browser.fragment.BrowserFragment
import com.timecat.page.base.friend.compact.BaseFragmentActivity

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/26
 * @description null
 * @usage null
 */
class BrowserActivity : BaseFragmentActivity() {
    override fun createFragment(): Fragment {
        return BrowserFragment()
    }
}