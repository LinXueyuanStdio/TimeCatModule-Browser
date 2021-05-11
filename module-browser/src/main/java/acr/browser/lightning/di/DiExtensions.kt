package acr.browser.lightning.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BuildConfig
import acr.browser.lightning.device.BuildType
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.timecat.module.browser.page.AbsThemeBrowserPage

/**
 * The [AppComponent] attached to the application [Context].
 */
val Context.injector: AppComponent
    get() = BrowserApp.appComponent
/**
 * The [AppComponent] attached to the context, note that the fragment must be attached.
 */
val Fragment.injector: AppComponent
    get() = BrowserApp.appComponent
val AbsThemeBrowserPage.injector: AppComponent
    get() = BrowserApp.appComponent
val View.injector: AppComponent
    get() = BrowserApp.appComponent
/**
 * The [AppComponent] attached to the context, note that the fragment must be attached.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Consumers should switch to support.v4.app.Fragment")
val android.app.Fragment.injector: AppComponent
    get() = BrowserApp.appComponent

fun createBuildType() = when {
    BuildConfig.DEBUG -> BuildType.DEBUG
    else -> BuildType.RELEASE
}

