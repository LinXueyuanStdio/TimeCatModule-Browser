package com.same.ui

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.same.lib.base.AndroidUtilities
import com.same.lib.core.BasePage
import com.same.lib.core.ContainerLayout
import com.same.lib.same.ContainerCreator
import com.same.lib.theme.KeyHub
import com.same.lib.theme.Theme
import com.same.lib.util.Space
import com.timecat.fake.file.R
import com.timecat.module.browser.page.BrowserPage
import java.util.*

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/6/21
 * @description null
 * @usage null
 */
class PagerActivity : FragmentActivity(), ContainerCreator.ContextDelegate {
    lateinit var creator: ContainerCreator
    private var onGlobalLayoutListener: OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        creator = ContainerCreator(this, this)
        creator.onPreCreate()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setTheme(R.style.ThemeLight)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                setTaskDescription(
                    TaskDescription(
                        null,
                        null,
                        Theme.getColor(KeyHub.key_actionBarDefault) or -0x1000000
                    )
                )
            } catch (ignore: java.lang.Exception) {
            }
            try {
                window.navigationBarColor = -0x1000000
            } catch (ignore: java.lang.Exception) {
            }
        }
        window.setBackgroundDrawableResource(R.drawable.transparent)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 24) {
            //适配分屏
            AndroidUtilities.isInMultiwindow = isInMultiWindowMode
        }
        val container = FrameLayout(this)
        creator.onCreateView(container, BrowserPage(intent))
        setContentView(container)
        if (Space.isTablet()) {
            //适配平板
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        checkSystemBarColors()

        //适配各种国产 ROM
        try {
            var os1 = Build.DISPLAY
            var os2 = Build.USER
            os1 = os1?.toLowerCase(Locale.getDefault()) ?: ""
            os2 = if (os2 != null) {
                os1.toLowerCase(Locale.getDefault())
            } else {
                ""
            }
            if (os1.contains("flyme") || os2.contains("flyme")) {
                AndroidUtilities.incorrectDisplaySizeFix = true
                val view = window.decorView.rootView
                view.viewTreeObserver.addOnGlobalLayoutListener(OnGlobalLayoutListener {
                    var height = view.measuredHeight
                    if (Build.VERSION.SDK_INT >= 21) {
                        height -= AndroidUtilities.statusBarHeight
                    }
                    if (height > Space.dp(100f) && height < AndroidUtilities.displaySize.y && height + Space.dp(
                            100f
                        ) > AndroidUtilities.displaySize.y
                    ) {
                        AndroidUtilities.displaySize.y = height
                    }
                }.also { onGlobalLayoutListener = it })
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun checkSystemBarColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var color = Theme.getColor(KeyHub.key_actionBarDefault, null, true)
            AndroidUtilities.setLightStatusBar(window, color == Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val window = window
                color = Theme.getColor(KeyHub.key_windowBackgroundGray, null, true)
                if (window.navigationBarColor != color) {
                    window.navigationBarColor = color
                    val brightness = AndroidUtilities.computePerceivedBrightness(color)
                    AndroidUtilities.setLightNavigationBar(getWindow(), brightness >= 0.721f)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        creator.onPause()
    }

    override fun onResume() {
        super.onResume()
        creator.onResume()
    }

    override fun onDestroy() {
        try {
            if (onGlobalLayoutListener != null) {
                val view = window.decorView.rootView
                view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        creator.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        creator.onPreActivityResult()
        super.onActivityResult(requestCode, resultCode, data)
        creator.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        creator.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        creator.onPreConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
        creator.onPostConfigurationChanged(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        AndroidUtilities.isInMultiwindow = isInMultiWindowMode
        creator.checkLayout()
    }

    override fun getContainerLayout(): ContainerLayout? {
        return creator.containerLayout
    }

    override fun getRightActionBarLayout(): ContainerLayout? {
        return creator.rightActionBarLayout
    }

    override fun getLayersActionBarLayout(): ContainerLayout? {
        return creator.layersActionBarLayout
    }

    private fun didSetNewTheme(nightTheme: Boolean) {
        creator.didSetNewTheme(nightTheme)
        if (!nightTheme) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    setTaskDescription(
                        TaskDescription(
                            null,
                            null,
                            Theme.getColor(KeyHub.key_actionBarDefault) or -0x1000000
                        )
                    )
                } catch (ignore: Exception) {
                }
            }
        }
        checkSystemBarColors()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        try {
            super.onSaveInstanceState(outState)
            creator.onSaveInstanceState(outState)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        creator.onBackPressed()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        creator.onLowMemory()
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        creator.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        creator.onActionModeFinished(mode)
    }

    fun onPreIme(): Boolean {
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        creator.onKeyUp(keyCode, event)
        return super.onKeyUp(keyCode, event)
    }

    fun needPresentFragment(
        fragment: BasePage?,
        removeLast: Boolean,
        forceWithoutAnimation: Boolean,
        layout: ContainerLayout?
    ): Boolean {
        return creator.needPresentFragment(fragment, removeLast, forceWithoutAnimation, layout)
    }

    fun needAddFragmentToStack(fragment: BasePage?, layout: ContainerLayout?): Boolean {
        return creator.needAddFragmentToStack(fragment, layout)
    }

    fun needCloseLastFragment(layout: ContainerLayout?): Boolean {
        return creator.needCloseLastFragment(layout)
    }

    override fun rebuildAllFragments(last: Boolean) {
        creator.rebuildAllFragments(last)
    }

    fun onRebuildAllFragments(layout: ContainerLayout?, last: Boolean) {
        creator.onRebuildAllFragments(layout, last)
    }

    override fun getConfiguration(): Configuration? {
        return resources.configuration
    }

    override fun close() {
        finish()
    }

    override fun getWindowLayoutParams(): WindowManager.LayoutParams {
        return window.attributes
    }

    override fun getWindowDecorView(): View {
        return window.decorView
    }
}