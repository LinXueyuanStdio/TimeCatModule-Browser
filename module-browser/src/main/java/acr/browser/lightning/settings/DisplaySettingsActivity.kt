package acr.browser.lightning.settings

import acr.browser.lightning.R
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.preference.UserPreferences
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.timecat.layout.ui.business.form.Next
import com.timecat.layout.ui.business.form.Slide
import com.timecat.layout.ui.business.form.Switch
import com.timecat.middle.setting.BaseSettingActivity
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/1
 * @description null
 * @usage null
 */
class DisplaySettingsActivity : BaseSettingActivity() {

    private lateinit var themeOptions: Array<String>

    @Inject
    internal lateinit var userPreferences: UserPreferences
    override fun title(): String = getString(R.string.settings_display)
    override fun addSettingItems(container: ViewGroup) {
        injector.inject(this)
        themeOptions = this.resources.getStringArray(R.array.themes)

        container.Switch(
            title = getString(R.string.tabs_in_drawer),
            getInitialCheck = { userPreferences.showTabsInDrawer }
        ) { userPreferences.showTabsInDrawer = it }

        container.Switch(
            title = getString(R.string.swap_bookmarks_and_tabs),
            getInitialCheck = { userPreferences.bookmarksAndTabsSwapped }
        ) { userPreferences.bookmarksAndTabsSwapped = it }

        container.Switch(
            title = getString(R.string.fullScreenOption),
            getInitialCheck = { userPreferences.hideStatusBarEnabled }
        ) { userPreferences.hideStatusBarEnabled = it }

        container.Switch(
            title = getString(R.string.fullscreen),
            getInitialCheck = { userPreferences.fullScreenEnabled }
        ) { userPreferences.fullScreenEnabled = it }

        container.Switch(
            title = getString(R.string.settings_black_status_bar),
            getInitialCheck = { userPreferences.useBlackStatusBar }
        ) { userPreferences.useBlackStatusBar = it }

        container.Switch(
            title = getString(R.string.wideViewPort),
            hint = getString(R.string.recommended),
            getInitialCheck = { userPreferences.useWideViewportEnabled }
        ) { userPreferences.useWideViewportEnabled = it }

        container.Switch(
            title = getString(R.string.overViewMode),
            hint = getString(R.string.recommended),
            getInitialCheck = { userPreferences.overviewModeEnabled }
        ) { userPreferences.overviewModeEnabled = it }

        container.Switch(
            title = getString(R.string.reflow),
            getInitialCheck = { userPreferences.textReflowEnabled }
        ) { userPreferences.textReflowEnabled = it }

        // TODO 这个模块不能有自己的theme
        // TODO theme是关于全局的一种行为，该模块自己设置自己的，可能导致和整体行为不一致
//        container.Next(
//            title = getString(R.string.theme),
//            text = themeOptions[userPreferences.useTheme]
//        ) { item -> showThemePicker { item.hint = it } }

        container.Slide(
            title = getString(R.string.title_text_size),
            unit = "${getTextSize(userPreferences.textSize)}",
            from = 0f,
            to = 5f,
            value = userPreferences.textSize.toFloat()
        ) { it, item ->
            userPreferences.textSize = it.toInt()
            item.text = "${getTextSize(it.toInt())}"
        }
    }

    private fun showThemePicker(updateSummary: (String) -> Unit) {
        val currentTheme = userPreferences.useTheme

        val dialog = AlertDialog.Builder(this).apply {
            setTitle(resources.getString(R.string.theme))
            setSingleChoiceItems(themeOptions, currentTheme) { _, which ->
                userPreferences.useTheme = which
                if (which < themeOptions.size) {
                    updateSummary(themeOptions[which])
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok)) { _, _ ->
                if (currentTheme != userPreferences.useTheme) {
                    onBackPressed()
                }
            }
            setOnCancelListener {
                if (currentTheme != userPreferences.useTheme) {
                    onBackPressed()
                }
            }
        }.show()

        BrowserDialog.setDialogSize(this, dialog)
    }

    private class TextSeekBarListener(
        private val sampleText: TextView
    ) : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(view: SeekBar, size: Int, user: Boolean) {
            this.sampleText.textSize = getTextSize(size)
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {}

        override fun onStopTrackingTouch(arg0: SeekBar) {}

    }

    companion object {

        private const val SETTINGS_HIDESTATUSBAR = "fullScreenOption"
        private const val SETTINGS_FULLSCREEN = "fullscreen"
        private const val SETTINGS_VIEWPORT = "wideViewPort"
        private const val SETTINGS_OVERVIEWMODE = "overViewMode"
        private const val SETTINGS_REFLOW = "text_reflow"
        private const val SETTINGS_THEME = "app_theme"
        private const val SETTINGS_TEXTSIZE = "text_size"
        private const val SETTINGS_DRAWERTABS = "cb_drawertabs"
        private const val SETTINGS_SWAPTABS = "cb_swapdrawers"
        private const val SETTINGS_BLACK_STATUS = "black_status_bar"

        private const val XX_LARGE = 30.0f
        private const val X_LARGE = 26.0f
        private const val LARGE = 22.0f
        private const val MEDIUM = 18.0f
        private const val SMALL = 14.0f
        private const val X_SMALL = 10.0f

        private fun getTextSize(size: Int): Float = when (size) {
            0 -> X_SMALL
            1 -> SMALL
            2 -> MEDIUM
            3 -> LARGE
            4 -> X_LARGE
            5 -> XX_LARGE
            else -> MEDIUM
        }
    }
}