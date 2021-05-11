package acr.browser.lightning.settings

import acr.browser.lightning.R
import acr.browser.lightning.constant.TEXT_ENCODINGS
import acr.browser.lightning.di.injector
import acr.browser.lightning.preference.UserPreferences
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.timecat.layout.ui.business.form.Next
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
class AdvancedSettingsActivity : BaseSettingActivity() {
    @Inject
    internal lateinit var userPreferences: UserPreferences
    override fun title(): String = getString(R.string.settings_advanced)
    override fun addSettingItems(container: ViewGroup) {
        injector.inject(this)
        container.Switch(
            title = getString(R.string.window),
            hint = getString(R.string.recommended),
            getInitialCheck = { userPreferences.popupsEnabled }
        ) { userPreferences.popupsEnabled = it }

        container.Switch(
            title = getString(R.string.cookies),
            hint = getString(R.string.recommended),
            getInitialCheck = { userPreferences.cookiesEnabled }
        ) { userPreferences.cookiesEnabled = it }

        container.Switch(
            title = getString(R.string.incognito_cookies),
            getInitialCheck = { userPreferences.incognitoCookiesEnabled }
        ) { userPreferences.incognitoCookiesEnabled = it }

        container.Switch(
            title = getString(R.string.restore_when_start),
            getInitialCheck = { userPreferences.restoreLostTabsEnabled }
        ) { userPreferences.restoreLostTabsEnabled = it }

        container.Next(
            title = getString(R.string.text_encoding),
            hint = userPreferences.textEncoding
        ) { item -> showTextEncodingDialogPicker { item.hint = it } }

        container.Next(
            title = getString(R.string.rendering_mode),
            hint = getString(renderingModePreferenceToString(userPreferences.renderingMode))
        ) { item -> showRenderingDialogPicker { item.hint = it } }

        container.Next(
            title = getString(R.string.url_contents),
            hint = urlBoxPreferenceToString(userPreferences.urlBoxContentChoice)
        ) { item -> showUrlBoxDialogPicker { item.hint = it } }
    }


    /**
     * Shows the dialog which allows the user to choose the browser's rendering method.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showRenderingDialogPicker(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.rendering_mode)

            val choices = arrayOf(
                getString(R.string.name_normal),
                getString(R.string.name_inverted),
                getString(R.string.name_grayscale),
                getString(R.string.name_inverted_grayscale),
                getString(R.string.name_increase_contrast)
            )

            listItemsSingleChoice(items = choices.asList(), initialSelection = userPreferences.renderingMode) { _, which, _ ->
                userPreferences.renderingMode = which
                updateSummary(getString(renderingModePreferenceToString(which)))
            }
            positiveButton(R.string.action_ok)
        }
    }

    /**
     * Shows the dialog which allows the user to choose the browser's text encoding.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showTextEncodingDialogPicker(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.text_encoding)
            val currentChoice = TEXT_ENCODINGS.indexOf(userPreferences.textEncoding)

            listItemsSingleChoice(items = TEXT_ENCODINGS.asList(), initialSelection = currentChoice) { _, which, _ ->
                userPreferences.textEncoding = TEXT_ENCODINGS[which]
                updateSummary(TEXT_ENCODINGS[which])
            }
            positiveButton(R.string.action_ok)
        }
    }

    /**
     * Shows the dialog which allows the user to choose the browser's URL box display options.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showUrlBoxDialogPicker(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {

            title(R.string.url_contents)
            listItemsSingleChoice(R.array.url_content_array, initialSelection = userPreferences.urlBoxContentChoice) { _, which, _ ->
                userPreferences.urlBoxContentChoice = which
                updateSummary(urlBoxPreferenceToString(which))
            }
            positiveButton(R.string.action_ok)
        }
    }

    /**
     * Convert an integer to the [StringRes] representation which can be displayed to the user for
     * the rendering mode preference.
     */
    @StringRes
    private fun renderingModePreferenceToString(preference: Int): Int = when (preference) {
        0 -> R.string.name_normal
        1 -> R.string.name_inverted
        2 -> R.string.name_grayscale
        3 -> R.string.name_inverted_grayscale
        4 -> R.string.name_increase_contrast
        else -> throw IllegalArgumentException("Unknown rendering mode preference $preference")
    }

    /**
     * Convert an integer to the [String] representation which can be displayed to the user for the
     * URL box preference.
     */
    private fun urlBoxPreferenceToString(preference: Int): String {
        val stringArray = resources.getStringArray(R.array.url_content_array)

        return stringArray[preference]
    }
}