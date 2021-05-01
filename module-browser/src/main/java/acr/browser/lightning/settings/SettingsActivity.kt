package acr.browser.lightning.settings

import acr.browser.lightning.R
import android.content.Intent
import android.view.ViewGroup
import com.timecat.layout.ui.business.form.Next
import com.timecat.layout.ui.business.setting.NextItem
import com.timecat.middle.setting.BaseSettingActivity

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/1
 * @description null
 * @usage null
 */
class SettingsActivity : BaseSettingActivity() {
    override fun title(): String = "浏览器设置"
    override fun addSettingItems(container: ViewGroup) {
        container.PathNext(getString(R.string.settings_general), GeneralSettingsActivity::class.java)
        container.PathNext(getString(R.string.bookmark_settings), BookmarkSettingsActivity::class.java)
        container.PathNext(getString(R.string.settings_display), DisplaySettingsActivity::class.java)
        container.PathNext(getString(R.string.settings_privacy), PrivacySettingsActivity::class.java)
        container.PathNext(getString(R.string.settings_advanced), AdvancedSettingsActivity::class.java)
    }

    fun <T> ViewGroup.PathNext(
        title: String,
        path: Class<T>
    ): NextItem = Next(title) {
        startActivity(Intent(context, path))
    }
}