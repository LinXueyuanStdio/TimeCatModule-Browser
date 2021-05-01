package acr.browser.lightning.di

import acr.browser.lightning.adblock.AssetsAdBlocker
import acr.browser.lightning.adblock.NoOpAdBlocker
import acr.browser.lightning.browser.SearchBoxModel
import acr.browser.lightning.browser.activity.BrowserActivity
import acr.browser.lightning.browser.activity.ThemableBrowserActivity
import acr.browser.lightning.browser.fragment.BookmarksFragment
import acr.browser.lightning.browser.fragment.TabsFragment
import com.timecat.module.browser.AppLifecyclesImpl
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.download.DownloadHandler
import acr.browser.lightning.download.LightningDownloadListener
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.*
import acr.browser.lightning.settings.SettingsActivity
import acr.browser.lightning.settings.fragment.*
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.view.LightningChromeClient
import acr.browser.lightning.view.LightningView
import acr.browser.lightning.view.LightningWebClient
import com.timecat.module.browser.fragment.AbsBrowserFragment
import com.timecat.module.browser.fragment.AbsThemeBrowserFragment
import com.timecat.module.browser.page.AbsThemeBrowserPage
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class), (AppBindsModule::class)])
interface AppComponent {

    fun inject(fragment: AdvancedSettingsActivity)
    fun inject(fragment: BookmarkSettingsActivity)
    fun inject(fragment: DisplaySettingsActivity)
    fun inject(fragment: GeneralSettingsActivity)
    fun inject(fragment: PrivacySettingsActivity)

    fun inject(page: AbsThemeBrowserPage)

    fun inject(fragment: AbsThemeBrowserFragment)

    fun inject(fragment: AbsBrowserFragment)

    fun inject(activity: BrowserActivity)

    fun inject(fragment: BookmarksFragment)

    fun inject(builder: LightningDialogBuilder)

    fun inject(fragment: TabsFragment)

    fun inject(lightningView: LightningView)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(app: AppLifecyclesImpl)

    fun inject(proxyUtils: ProxyUtils)

    fun inject(activity: ReadingActivity)

    fun inject(webClient: LightningWebClient)

    fun inject(activity: SettingsActivity)

    fun inject(listener: LightningDownloadListener)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(chromeClient: LightningChromeClient)

    fun inject(downloadHandler: DownloadHandler)

    fun inject(searchBoxModel: SearchBoxModel)

    fun provideAssetsAdBlocker(): AssetsAdBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

}
