package com.same.ui

import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.same.lib.base.AndroidUtilities
import com.same.lib.checkbox.CheckboxFont
import com.same.lib.font.FontManager
import com.same.lib.lottie.NativeLoader
import com.same.lib.same.theme.delegate.ColorDelegateLoader
import com.same.lib.same.view.PassCode
import com.same.lib.theme.Theme
import com.same.lib.util.*
import com.same.lib.util.ColorManager.ColorEngine
import com.same.lib.util.Lang.ILang
import com.same.ui.lang.MyLang
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.component.identity.Attr
import com.xiaojinzi.component.impl.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.DEBUG = true
        LogUtil.OPEN_LOG = true
        NativeLoader.initNativeLibs(application)
        MyLang.init(application)
        ColorDelegateLoader.init()
        AndroidUtilities.init(application)
        UIThread.init(application)
        ColorManager.install(ColorEngine { key ->
            when (key) {
                KeyHub.key_windowBackgroundWhite -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_windowBackgroundWhiteBlackText -> return@ColorEngine Attr.getPrimaryTextColor(application)
                KeyHub.key_listSelector -> {
                }
                KeyHub.key_actionBarDefault -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_actionBarDefaultSelector -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_actionBarButtonSelector -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_actionBarDefaultIcon -> return@ColorEngine Attr.getIconColor(application)
                KeyHub.key_actionBarActionModeDefault -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_actionBarActionModeDefaultTop -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_actionBarActionModeDefaultIcon -> return@ColorEngine Attr.getIconColor(application)
                KeyHub.key_actionBarActionModeDefaultSelector -> return@ColorEngine Attr.getPrimaryColor(application)
                KeyHub.key_actionBarDefaultTitle -> return@ColorEngine Attr.getPrimaryTextColor(application)
                KeyHub.key_actionBarDefaultSubtitle -> return@ColorEngine Attr.getSecondaryTextColor(application)
                KeyHub.key_actionBarDefaultSearch -> return@ColorEngine Attr.getPrimaryTextColor(application)
                KeyHub.key_actionBarDefaultSearchPlaceholder -> return@ColorEngine Attr.getSecondaryTextColor(application)
                KeyHub.key_actionBarDefaultSubmenuItem -> return@ColorEngine Attr.getPrimaryTextColor(application)
                KeyHub.key_actionBarDefaultSubmenuItemIcon -> return@ColorEngine Attr.getIconColor(application)
                KeyHub.key_actionBarDefaultSubmenuBackground -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_undo_background -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_undo_cancelColor -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_undo_infoColor -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_dialogTextBlack -> return@ColorEngine Attr.getPrimaryTextColor(application)
                KeyHub.key_dialogBackground -> return@ColorEngine Color.TRANSPARENT
                KeyHub.key_progressCircle -> return@ColorEngine Attr.getPrimaryColor(application)
            }
            Color.TRANSPARENT
        })
        Lang.install(object : ILang {
            override fun getString(key: String, string: Int): String {
                return MyLang.getString(key, string)
            }

            override fun getTranslitString(src: String): String {
                return MyLang.getInstance().getTranslitString(src)
            }

            override fun formatPluralString(key: String, plural: Int): String {
                return MyLang.formatPluralString(key, plural)
            }

            override fun formatString(key: String, res: Int, vararg args: Any): String {
                return MyLang.formatString(key, res, args)
            }
        })
        Font.install { context: Context?, assetPath: String? -> FontManager.getMediumTypeface(context) }
        CheckboxFont.install { context: Context?, assetPath: String? -> FontManager.getMediumTypeface(context) }
        PassCode.loadConfig(application)
        application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {}
            override fun onConfigurationChanged(newConfig: Configuration) {
                MyLang.onConfigurationChanged(newConfig)
                AndroidUtilities.checkDisplaySize(application, newConfig)
                Space.checkDisplaySize(application, newConfig)
                Theme.onConfigurationChanged(application, newConfig)
            }

            override fun onLowMemory() {}
        })
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(createButton("打开") {
            startActivity(Intent(this, BrowserActivity::class.java))
        })
        linearLayout.addView(createButton("PagerActivity") {
            startActivity(Intent(this, PagerActivity::class.java))
        })
        setContentView(linearLayout)
    }

    private fun createButton(name: String, path: String): Button {
        val button = createButton(name)
        button.setOnClickListener { go(path) }
        return button
    }

    private fun createButton(name: String, onClickListener: View.OnClickListener): Button {
        val button = createButton(name)
        button.setOnClickListener(onClickListener)
        return button
    }

    private fun createButton(name: String): Button {
        val button = Button(this)
        button.text = name
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        button.layoutParams = layoutParams
        button.gravity = Gravity.CENTER
        return button
    }

    private fun go(path: String) {
        Router.with().hostAndPath(path)
            .forward(object : Callback {
                override fun onSuccess(result: RouterResult) {}
                override fun onEvent(successResult: RouterResult?, errorResult: RouterErrorResult?) {}
                override fun onCancel(originalRequest: RouterRequest?) {}
                override fun onError(errorResult: RouterErrorResult) {
                    Log.e("ui", errorResult.error.toString())
                }
            })
    }
}