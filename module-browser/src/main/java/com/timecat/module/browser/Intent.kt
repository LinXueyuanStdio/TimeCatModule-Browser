package com.timecat.module.browser

import android.content.Intent
import android.net.Uri

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/18
 * @description null
 * @usage null
 */
fun createBrowserIntent(url: String) = Intent().apply {
    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    data = Uri.parse(url)
}