package acr.browser.lightning.settings

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.constant.*
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.Suggestions
import acr.browser.lightning.search.engine.BaseSearchEngine
import acr.browser.lightning.search.engine.CustomSearch
import acr.browser.lightning.utils.FileUtils
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.utils.ThemeUtils
import android.content.Context
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
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
class GeneralSettingsActivity : BaseSettingActivity() {
    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun title(): String = getString(R.string.settings_general)
    override fun addSettingItems(container: ViewGroup) {
        injector.inject(this)
        proxyChoices = resources.getStringArray(R.array.proxy_choices_array)

        container.Switch(
            title = getString(R.string.block_ads),
            getInitialCheck = { BuildConfig.FULL_VERSION && userPreferences.adBlockEnabled }
        ) { userPreferences.adBlockEnabled = it }

        container.Switch(
            title = getString(R.string.block),
            getInitialCheck = { userPreferences.blockImagesEnabled }
        ) { userPreferences.blockImagesEnabled = it }

        container.Switch(
            title = getString(R.string.java),
            getInitialCheck = { userPreferences.javaScriptEnabled }
        ) { userPreferences.javaScriptEnabled = it }

        container.Switch(
            title = getString(R.string.color_mode),
            getInitialCheck = { userPreferences.colorModeEnabled }
        ) { userPreferences.colorModeEnabled = it }

        container.Next(
            title = getString(R.string.http_proxy),
            text = proxyChoiceToSummary(userPreferences.proxyChoice),
        ) { item -> showProxyPicker { item.text = it } }

        container.Next(
            title = getString(R.string.agent),
            text = choiceToUserAgent(userPreferences.userAgentChoice),
        ) { item -> showUserAgentChooserDialog { item.text = it } }

        container.Next(
            title = getString(R.string.download),
            text = userPreferences.downloadDirectory,
        ) { item -> showDownloadLocationDialog { item.text = it } }

        container.Next(
            title = getString(R.string.home),
            text = homePageUrlToDisplayTitle(userPreferences.homepage),
        ) { item -> showHomePageDialog { item.text = it } }

        container.Next(
            title = getString(R.string.search),
            text = getSearchEngineSummary(searchEngineProvider.provideSearchEngine()),
        ) { item -> showSearchProviderDialog { item.text = it } }

        container.Next(
            title = getString(R.string.search_suggestions),
            text = searchSuggestionChoiceToTitle(Suggestions.from(userPreferences.searchSuggestionChoice)),
        ) { item -> showSearchSuggestionsDialog { item.text = it } }
    }

    private fun proxyChoiceToSummary(choice: Int) = when (choice) {
        PROXY_MANUAL -> "${userPreferences.proxyHost}:${userPreferences.proxyPort}"
        NO_PROXY,
        PROXY_ORBOT,
        PROXY_I2P -> proxyChoices[choice]
        else -> proxyChoices[NO_PROXY]
    }

    private fun showProxyPicker(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.http_proxy)
            positiveButton(R.string.action_ok)
            listItemsSingleChoice(items = proxyChoices.asList(), initialSelection = userPreferences.proxyChoice) { _, idx, _ ->
                updateProxyChoice(idx, context, updateSummary)
            }
        }
    }

    private fun updateProxyChoice(@Proxy choice: Int, context: Context, updateSummary: (String) -> Unit) {
        val sanitizedChoice = ProxyUtils.sanitizeProxyChoice(choice, context)
        when (sanitizedChoice) {
            PROXY_ORBOT,
            PROXY_I2P,
            NO_PROXY -> Unit
            PROXY_MANUAL -> showManualProxyPicker(context, updateSummary)
        }

        userPreferences.proxyChoice = sanitizedChoice
        if (sanitizedChoice < proxyChoices.size) {
            updateSummary(proxyChoices[sanitizedChoice])
        }
    }

    private fun showManualProxyPicker(context: Context, updateSummary: (String) -> Unit) {
        val v = LayoutInflater.from(context).inflate(R.layout.browser_dialog_manual_proxy, null)
        val eProxyHost = v.findViewById<TextView>(R.id.proxyHost)
        val eProxyPort = v.findViewById<TextView>(R.id.proxyPort)

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.toString(Integer.MAX_VALUE).length
        eProxyPort.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxCharacters - 1))

        eProxyHost.text = userPreferences.proxyHost
        eProxyPort.text = Integer.toString(userPreferences.proxyPort)

        MaterialDialog(this).show {
            title(R.string.manual_proxy)
            positiveButton(R.string.action_ok) {
                val proxyHost = eProxyHost.text.toString()
                val proxyPort = try {
                    // Try/Catch in case the user types an empty string or a number
                    // larger than max integer
                    Integer.parseInt(eProxyPort.text.toString())
                } catch (ignored: NumberFormatException) {
                    userPreferences.proxyPort
                }

                userPreferences.proxyHost = proxyHost
                userPreferences.proxyPort = proxyPort
                updateSummary("$proxyHost:$proxyPort")
            }
            customView(view = v)
        }
    }

    private fun choiceToUserAgent(index: Int) = when (index) {
        1 -> resources.getString(R.string.agent_default)
        2 -> resources.getString(R.string.agent_desktop)
        3 -> resources.getString(R.string.agent_mobile)
        4 -> resources.getString(R.string.agent_custom)
        else -> resources.getString(R.string.agent_default)
    }

    private fun showUserAgentChooserDialog(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.title_user_agent)
            positiveButton(R.string.action_ok)
            listItemsSingleChoice(R.array.user_agent, initialSelection = userPreferences.userAgentChoice - 1) { _, which, _ ->
                userPreferences.userAgentChoice = which + 1
                updateSummary(choiceToUserAgent(userPreferences.userAgentChoice))
                when (which) {
                    in 0..2 -> Unit
                    3 -> {
                        updateSummary(resources.getString(R.string.agent_custom))
                        showCustomUserAgentPicker(updateSummary)
                    }
                }
            }
        }
    }

    private fun showCustomUserAgentPicker(updateSummary: (String) -> Unit) {
        BrowserDialog.showEditText(this,
            R.string.title_user_agent,
            R.string.title_user_agent,
            userPreferences.userAgentString,
            R.string.action_ok) { s ->
            userPreferences.userAgentString = s
            updateSummary(getString(R.string.agent_custom))
        }
    }

    private fun showDownloadLocationDialog(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.title_download_location)
            positiveButton(R.string.action_ok)
            val n: Int = if (userPreferences.downloadDirectory.contains(Environment.DIRECTORY_DOWNLOADS)) {
                0
            } else {
                1
            }
            listItemsSingleChoice(R.array.download_folder, initialSelection = n) { _, which, _ ->
                when (which) {
                    0 -> {
                        userPreferences.downloadDirectory = FileUtils.DEFAULT_DOWNLOAD_PATH
                        updateSummary(FileUtils.DEFAULT_DOWNLOAD_PATH)
                    }
                    1 -> {
                        showCustomDownloadLocationPicker(updateSummary)
                    }
                }
            }
        }
    }


    private fun showCustomDownloadLocationPicker(updateSummary: (String) -> Unit) {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.browser_dialog_edit_text, null)
        val getDownload = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        val errorColor = ContextCompat.getColor(this, R.color.error_red)
        val regularColor = ThemeUtils.getTextColor(this)
        getDownload.setTextColor(regularColor)
        getDownload.addTextChangedListener(DownloadLocationTextWatcher(getDownload, errorColor, regularColor))
        getDownload.setText(userPreferences.downloadDirectory)

        MaterialDialog(this).show {
            title(R.string.title_download_location)
            customView(view = dialogView)
            positiveButton(R.string.action_ok) {
                var text = getDownload.text.toString()
                text = FileUtils.addNecessarySlashes(text)
                userPreferences.downloadDirectory = text
                updateSummary(text)
            }
        }
    }

    private class DownloadLocationTextWatcher(
        private val getDownload: EditText,
        private val errorColor: Int,
        private val regularColor: Int
    ) : TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (!FileUtils.isWriteAccessAvailable(s.toString())) {
                this.getDownload.setTextColor(this.errorColor)
            } else {
                this.getDownload.setTextColor(this.regularColor)
            }
        }
    }

    private fun homePageUrlToDisplayTitle(url: String): String = when (url) {
        SCHEME_HOMEPAGE -> resources.getString(R.string.action_homepage)
        SCHEME_BLANK -> resources.getString(R.string.action_blank)
        SCHEME_BOOKMARKS -> resources.getString(R.string.action_bookmarks)
        else -> url
    }

    private fun showHomePageDialog(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.home)
            positiveButton(R.string.action_ok)
            val n = when (userPreferences.homepage) {
                SCHEME_HOMEPAGE -> 0
                SCHEME_BLANK -> 1
                SCHEME_BOOKMARKS -> 2
                else -> 3
            }
            listItemsSingleChoice(R.array.homepage, initialSelection = n) { _, which, _ ->
                when (which) {
                    0 -> {
                        userPreferences.homepage = SCHEME_HOMEPAGE
                        updateSummary(resources.getString(R.string.action_homepage))
                    }
                    1 -> {
                        userPreferences.homepage = SCHEME_BLANK
                        updateSummary(resources.getString(R.string.action_blank))
                    }
                    2 -> {
                        userPreferences.homepage = SCHEME_BOOKMARKS
                        updateSummary(resources.getString(R.string.action_bookmarks))
                    }
                    3 -> {
                        showCustomHomePagePicker(updateSummary)
                    }
                }
            }
        }
    }

    private fun showCustomHomePagePicker(updateSummary: (String) -> Unit) {
        val currentHomepage: String = if (!URLUtil.isAboutUrl(userPreferences.homepage)) {
            userPreferences.homepage
        } else {
            "https://www.google.com"
        }

        BrowserDialog.showEditText(this,
            R.string.title_custom_homepage,
            R.string.title_custom_homepage,
            currentHomepage,
            R.string.action_ok) { url ->
            userPreferences.homepage = url
            updateSummary(url)
        }
    }

    private fun getSearchEngineSummary(baseSearchEngine: BaseSearchEngine): String {
        return if (baseSearchEngine is CustomSearch) {
            baseSearchEngine.queryUrl
        } else {
            getString(baseSearchEngine.titleRes)
        }
    }

    private fun convertSearchEngineToString(searchEngines: List<BaseSearchEngine>): Array<CharSequence> =
        searchEngines.map { getString(it.titleRes) }.toTypedArray()

    private fun showSearchProviderDialog(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.title_search_engine)
            positiveButton(R.string.action_ok)

            val searchEngineList = searchEngineProvider.provideAllSearchEngines()

            val chars = convertSearchEngineToString(searchEngineList)

            val n = userPreferences.searchChoice

            listItemsSingleChoice(items = chars.asList(), initialSelection = n) { _, which, _ ->
                val searchEngine = searchEngineList[which]

                // Store the search engine preference
                val preferencesIndex = searchEngineProvider.mapSearchEngineToPreferenceIndex(searchEngine)
                userPreferences.searchChoice = preferencesIndex

                if (searchEngine is CustomSearch) {
                    // Show the URL picker
                    showCustomSearchDialog(searchEngine, updateSummary)
                } else {
                    // Set the new search engine summary
                    updateSummary(getSearchEngineSummary(searchEngine))
                }
            }
        }
    }

    private fun showCustomSearchDialog(customSearch: CustomSearch, updateSummary: (String) -> Unit) {
        BrowserDialog.showEditText(
            this,
            R.string.search_engine_custom,
            R.string.search_engine_custom,
            userPreferences.searchUrl,
            R.string.action_ok
        ) { searchUrl ->
            userPreferences.searchUrl = searchUrl
            updateSummary(getSearchEngineSummary(customSearch))
        }
    }

    private fun searchSuggestionChoiceToTitle(choice: Suggestions): String =
        when (choice) {
            Suggestions.NONE -> getString(R.string.search_suggestions_off)
            Suggestions.GOOGLE -> getString(R.string.powered_by_google)
            Suggestions.DUCK -> getString(R.string.powered_by_duck)
            Suggestions.BAIDU -> getString(R.string.powered_by_baidu)
            Suggestions.NAVER -> getString(R.string.powered_by_naver)
        }

    private fun showSearchSuggestionsDialog(updateSummary: (String) -> Unit) {
        MaterialDialog(this).show {
            title(R.string.search_suggestions)
            positiveButton(R.string.action_ok)

            val currentChoice = when (Suggestions.from(userPreferences.searchSuggestionChoice)) {
                Suggestions.GOOGLE -> 0
                Suggestions.DUCK -> 1
                Suggestions.BAIDU -> 2
                Suggestions.NAVER -> 3
                Suggestions.NONE -> 3
            }
            listItemsSingleChoice(R.array.suggestions, initialSelection = currentChoice) { _, which, _ ->
                val suggestionsProvider = when (which) {
                    0 -> Suggestions.GOOGLE
                    1 -> Suggestions.DUCK
                    2 -> Suggestions.BAIDU
                    3 -> Suggestions.NAVER
                    4 -> Suggestions.NONE
                    else -> Suggestions.GOOGLE
                }
                userPreferences.searchSuggestionChoice = suggestionsProvider.index
                updateSummary(searchSuggestionChoiceToTitle(suggestionsProvider))
            }
        }
    }
}