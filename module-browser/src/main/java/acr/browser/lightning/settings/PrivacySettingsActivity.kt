package acr.browser.lightning.settings

import acr.browser.lightning.R
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.ApiUtils
import acr.browser.lightning.utils.WebUtils
import acr.browser.lightning.view.LightningView
import android.view.ViewGroup
import android.webkit.WebView
import com.timecat.layout.ui.business.form.Next
import com.timecat.layout.ui.business.form.Switch
import com.timecat.middle.setting.BaseSettingActivity
import io.reactivex.Completable
import io.reactivex.Scheduler
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/1
 * @description null
 * @usage null
 */
class PrivacySettingsActivity : BaseSettingActivity() {

    @Inject
    internal lateinit var historyRepository: HistoryRepository
    @Inject
    internal lateinit var userPreferences: UserPreferences

    @Inject
    @field:DatabaseScheduler
    internal lateinit var databaseScheduler: Scheduler

    @Inject
    @field:MainScheduler
    internal lateinit var mainScheduler: Scheduler

    override fun title(): String = getString(R.string.settings_privacy)
    override fun addSettingItems(container: ViewGroup) {
        injector.inject(this)

        container.Switch(
            title = getString(R.string.location),
            getInitialCheck = { userPreferences.locationEnabled },
        ) { userPreferences.locationEnabled = it }

        container.Switch(
            title = getString(R.string.third_party),
            getInitialCheck = { userPreferences.blockThirdPartyCookiesEnabled },
            autoAdd = ApiUtils.doesSupportThirdPartyCookieBlocking(),
        ) { userPreferences.blockThirdPartyCookiesEnabled = it }

        container.Switch(
            title = getString(R.string.password),
            hint = getString(R.string.recommended),
            getInitialCheck = { userPreferences.savePasswordsEnabled },
        ) { userPreferences.savePasswordsEnabled = it }

        container.Switch(
            title = getString(R.string.clear_cookies_exit),
            getInitialCheck = { userPreferences.clearCacheExit },
        ) { userPreferences.clearCacheExit = it }

        container.Switch(
            title = getString(R.string.clear_history_exit),
            getInitialCheck = { userPreferences.clearHistoryExitEnabled },
        ) { userPreferences.clearHistoryExitEnabled = it }

        container.Switch(
            title = getString(R.string.cache),
            getInitialCheck = { userPreferences.clearCookiesExitEnabled },
        ) { userPreferences.clearCookiesExitEnabled = it }

        container.Switch(
            title = getString(R.string.clear_web_storage_exit),
            getInitialCheck = { userPreferences.clearWebStorageExitEnabled },
        ) { userPreferences.clearWebStorageExitEnabled = it }

        container.Switch(
            title = getString(R.string.do_not_track),
            getInitialCheck = { userPreferences.doNotTrackEnabled && ApiUtils.doesSupportWebViewHeaders() },
            autoAdd = ApiUtils.doesSupportWebViewHeaders(),
        ) { userPreferences.doNotTrackEnabled = it }

        container.Switch(
            title = getString(R.string.webrtc_support),
            getInitialCheck = { userPreferences.webRtcEnabled && ApiUtils.doesSupportWebRtc() },
            autoAdd = ApiUtils.doesSupportWebRtc(),
        ) { userPreferences.webRtcEnabled = it }

        container.Switch(
            title = getString(R.string.remove_identifying_headers),
            getInitialCheck = { userPreferences.removeIdentifyingHeadersEnabled && ApiUtils.doesSupportWebViewHeaders() },
            autoAdd = ApiUtils.doesSupportWebViewHeaders(),
            hint = "${LightningView.HEADER_REQUESTED_WITH}, ${LightningView.HEADER_WAP_PROFILE}",
        ) { userPreferences.removeIdentifyingHeadersEnabled = it }

        container.Next(title = getString(R.string.clear_cache)) { clearCache() }
        container.Next(title = getString(R.string.clear_history)) { clearHistoryDialog() }
        container.Next(title = getString(R.string.clear_cookies)) { clearCookiesDialog() }
        container.Next(title = getString(R.string.clear_web_storage)) { clearWebStorage() }
    }

    private fun clearHistoryDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            context = this,
            title = R.string.title_clear_history,
            message = R.string.dialog_history,
            positiveButton = DialogItem(title = R.string.action_yes) {
                clearHistory()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe {
                        snackbar(R.string.message_clear_history)
                    }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCookiesDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            context = this,
            title = R.string.title_clear_cookies,
            message = R.string.dialog_cookies,
            positiveButton = DialogItem(title = R.string.action_yes) {
                clearCookies()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe {
                        snackbar(R.string.message_cookies_cleared)
                    }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCache() {
        WebView(this).apply {
            clearCache(true)
            destroy()
        }
        snackbar(R.string.message_cache_cleared)
    }

    private fun clearHistory(): Completable = Completable.fromAction {
        WebUtils.clearHistory(this, historyRepository, databaseScheduler)
    }

    private fun clearCookies(): Completable = Completable.fromAction {
        WebUtils.clearCookies(this)
    }

    private fun clearWebStorage() {
        WebUtils.clearWebStorage()
        snackbar(R.string.message_web_storage_cleared)
    }

    companion object {
        private const val SETTINGS_LOCATION = "location"
        private const val SETTINGS_THIRDPCOOKIES = "third_party"
        private const val SETTINGS_SAVEPASSWORD = "password"
        private const val SETTINGS_CACHEEXIT = "clear_cache_exit"
        private const val SETTINGS_HISTORYEXIT = "clear_history_exit"
        private const val SETTINGS_COOKIEEXIT = "clear_cookies_exit"
        private const val SETTINGS_CLEARCACHE = "clear_cache"
        private const val SETTINGS_CLEARHISTORY = "clear_history"
        private const val SETTINGS_CLEARCOOKIES = "clear_cookies"
        private const val SETTINGS_CLEARWEBSTORAGE = "clear_webstorage"
        private const val SETTINGS_WEBSTORAGEEXIT = "clear_webstorage_exit"
        private const val SETTINGS_DONOTTRACK = "do_not_track"
        private const val SETTINGS_WEBRTC = "webrtc_support"
        private const val SETTINGS_IDENTIFYINGHEADERS = "remove_identifying_headers"
    }
}