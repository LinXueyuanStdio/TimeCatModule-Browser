package acr.browser.lightning

import acr.browser.lightning.browser.activity.BrowserActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import io.reactivex.Completable
import org.greenrobot.eventbus.Subscribe

class IncognitoActivity : BrowserActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DarkTheme)
        super.onCreate(savedInstanceState)
    }

    @Suppress("DEPRECATION")
    public override fun updateCookiePreference(): Completable = Completable.fromAction {
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this@IncognitoActivity)
        }
        cookieManager.setAcceptCookie(userPreferences.incognitoCookiesEnabled)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.incognito, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @Suppress("RedundantOverride")
    override fun onNewIntent(intent: Intent) {
        handleNewIntent(intent)
        super.onNewIntent(intent)
    }

    @Suppress("RedundantOverride")
    override fun onPause() = super.onPause() // saveOpenTabs();

    override fun updateHistory(title: String?, url: String) = Unit // addItemToHistory(title, url)

    override fun isIncognito() = true

    override fun closeActivity() = closeDrawers(this::closeBrowser)

    @Subscribe
    override fun onEvent(e:DefaultEvent){}

    companion object {
        /**
         * Creates the intent with which to launch the activity. Adds the reorder to front flag.
         */
        fun createIntent(context: Context, uri: Uri? = null) = Intent(context, IncognitoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            data = uri
        }
    }
}
