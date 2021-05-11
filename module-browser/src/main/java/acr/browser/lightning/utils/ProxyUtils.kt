package acr.browser.lightning.utils

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BrowserApp.Companion.appComponent
import acr.browser.lightning.R
import acr.browser.lightning.constant.*
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.preference.DeveloperPreferences
import acr.browser.lightning.preference.UserPreferences
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.timecat.extend.arms.BaseApplication
import com.timecat.module.browser.prepareShowInService
import info.guardianproject.netcipher.proxy.OrbotHelper
import info.guardianproject.netcipher.webkit.WebkitProxy
import net.i2p.android.ui.I2PAndroidHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyUtils @Inject constructor() {
    @JvmField
    @Inject
    var mUserPreferences: UserPreferences? = null

    @JvmField
    @Inject
    var mDeveloperPreferences: DeveloperPreferences? = null

    @JvmField
    @Inject
    var mI2PHelper: I2PAndroidHelper? = null

    /*
     * If Orbot/Tor or I2P is installed, prompt the user if they want to enable
     * proxying for this session
     */
    fun checkForProxy(context: Context) {
        val proxyChoice = mUserPreferences!!.proxyChoice
        val orbotInstalled = OrbotHelper.isOrbotInstalled(context)
        val orbotChecked = mDeveloperPreferences!!.checkedForTor
        val orbot = orbotInstalled && !orbotChecked
        val i2pInstalled = mI2PHelper!!.isI2PAndroidInstalled
        val i2pChecked = mDeveloperPreferences!!.checkedForI2P
        val i2p = i2pInstalled && !i2pChecked

        // TODO Is the idea to show this per-session, or only once?
        if (proxyChoice != NO_PROXY && (orbot || i2p)) {
            if (orbot) {
                mDeveloperPreferences!!.checkedForTor = true
            }
            if (i2p) {
                mDeveloperPreferences!!.checkedForI2P = true
            }
            if (orbotInstalled && i2pInstalled) {
                val proxyChoices = context.resources.getStringArray(R.array.proxy_choices_array)
                MaterialDialog(context).show {
                    title(R.string.http_proxy)
                    listItemsSingleChoice(items = proxyChoices.asList(), initialSelection = mUserPreferences!!.proxyChoice) { _, which, _ ->
                        mUserPreferences!!.proxyChoice = which
                        if (which != NO_PROXY) {
                            initializeProxy(context)
                        }
                    }
                    positiveButton(R.string.action_ok)
                }
            } else {
                MaterialDialog(context).show {
                    message(if (orbotInstalled) R.string.use_tor_prompt else R.string.use_i2p_prompt)
                    positiveButton(R.string.yes) {
                        mUserPreferences!!.proxyChoice = if (orbotInstalled) PROXY_ORBOT else PROXY_I2P
                        initializeProxy(context)
                    }
                    negativeButton(R.string.no) {
                        mUserPreferences!!.proxyChoice = NO_PROXY
                    }
                }
            }
        }
    }

    /*
     * Initialize WebKit Proxying
     */
    private fun initializeProxy(context: Context) {
        val host: String
        val port: Int
        when (mUserPreferences!!.proxyChoice) {
            NO_PROXY ->                 // We shouldn't be here
                return
            PROXY_ORBOT -> {
                if (!OrbotHelper.isOrbotRunning(context)) {
                    OrbotHelper.requestStartTor(context)
                }
                host = "localhost"
                port = 8118
            }
            PROXY_I2P -> {
                sI2PProxyInitialized = true
                if (sI2PHelperBound && !mI2PHelper!!.isI2PAndroidRunning) {
                    MaterialDialog(context).show {
                        prepareShowInService()
                        title(R.string.start_i2p_android)
                        message(R.string.would_you_like_to_start_i2p_android)
                        positiveButton(R.string.yes) {
                            val i = Intent("net.i2p.android.router.START_I2P")
                            context.startActivity(i)
                        }
                        negativeButton(R.string.no)
                    }
                }
                host = "localhost"
                port = 4444
            }
            PROXY_MANUAL -> {
                host = mUserPreferences!!.proxyHost
                port = mUserPreferences!!.proxyPort
            }
            else -> {
                host = mUserPreferences!!.proxyHost
                port = mUserPreferences!!.proxyPort
            }
        }
        try {
            WebkitProxy.setProxy(BrowserApp::class.java.name, context.applicationContext, null, host, port)
        } catch (e: Exception) {
            Log.d(TAG, "error enabling web proxying", e)
        }
    }

    fun isProxyReady(): Boolean {
        if (mUserPreferences!!.proxyChoice == PROXY_I2P) {
            if (!mI2PHelper!!.isI2PAndroidRunning) {
                snackbar(R.string.i2p_not_running)
                return false
            } else if (!mI2PHelper!!.areTunnelsActive()) {
                snackbar(R.string.i2p_tunnels_not_ready)
                return false
            }
        }
        return true
    }

    fun updateProxySettings(context: Context) {
        if (mUserPreferences!!.proxyChoice != NO_PROXY) {
            initializeProxy(context)
        } else {
            try {
                WebkitProxy.resetProxy(BaseApplication::class.java.name, context.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to reset proxy", e)
            }
            sI2PProxyInitialized = false
        }
    }

    fun onStop() {
        mI2PHelper!!.unbind()
        sI2PHelperBound = false
    }

    fun onStart(context: Context) {
        if (mUserPreferences!!.proxyChoice == PROXY_I2P) {
            // Try to bind to I2P Android
            mI2PHelper!!.bind {
                sI2PHelperBound = true
                if (sI2PProxyInitialized && !mI2PHelper!!.isI2PAndroidRunning) {
                    MaterialDialog(context).show {
                        prepareShowInService()
                        title(R.string.start_i2p_android)
                        message(R.string.would_you_like_to_start_i2p_android)
                        positiveButton(R.string.yes) {
                            val i = Intent("net.i2p.android.router.START_I2P")
                            context.startActivity(i)
                        }
                        negativeButton(R.string.no)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ProxyUtils"

        // Helper
        private var sI2PHelperBound = false
        private var sI2PProxyInitialized = false

        @Proxy
        fun sanitizeProxyChoice(choice: Int, context: Context): Int {
            var choice = choice
            when (choice) {
                PROXY_ORBOT -> if (!OrbotHelper.isOrbotInstalled(context)) {
                    choice = NO_PROXY
                    snackbar(R.string.install_orbot)
                }
                PROXY_I2P -> {
                    val ih = I2PAndroidHelper(context)
                    if (!ih.isI2PAndroidInstalled) {
                        choice = NO_PROXY
                        MaterialDialog(context).show {
                            prepareShowInService()
                            title(R.string.start_i2p_android)
                            message(R.string.you_must_have_i2p_android)
                            positiveButton(R.string.yes) {
                                val uriMarket: String = context.getString(R.string.market_i2p_android)
                                val uri = Uri.parse(uriMarket)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                            negativeButton(R.string.no)
                        }
                    }
                }
                PROXY_MANUAL -> {
                }
            }
            return choice
        }
    }

    init {
        appComponent.inject(this)
    }
}