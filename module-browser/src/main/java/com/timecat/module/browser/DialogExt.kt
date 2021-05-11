package com.timecat.module.browser

import acr.browser.lightning.R
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/11
 * @description null
 * @usage null
 */
fun MaterialDialog.prepareShowInService() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
    }
}
fun createInformativeDialog(activity: Activity, @StringRes title: Int, @StringRes message: Int) {
    MaterialDialog(activity).show {
        prepareShowInService()
        title(title)
        message(message)
        cancelable(true)
        positiveButton(R.string.action_ok)
    }
}

fun createInformativeDialog(context: Context, title: Int, msg: String, iconRes: Int) {
    MaterialDialog(context).show {
        prepareShowInService()
        title(title)
        message(text=msg)
        icon(iconRes)
        positiveButton(R.string.action_ok)
    }
}
