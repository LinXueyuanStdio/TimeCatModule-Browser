package com.same.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.timecat.fake.file.R
import com.timecat.module.browser.BrowserFragment
import com.timecat.page.base.friend.compact.BaseFragmentActivity

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/26
 * @description null
 * @usage null
 */
class BrowserActivity : BaseFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_LightTheme)
        super.onCreate(savedInstanceState)

    }
    override fun createFragment(): Fragment {
        return BrowserFragment()
    }
}