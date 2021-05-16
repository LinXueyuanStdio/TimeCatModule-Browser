package acr.browser.lightning.view

import acr.browser.lightning.R
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.di.DiskScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.view.webrtc.WebRtcPermissionsModel
import acr.browser.lightning.view.webrtc.WebRtcPermissionsView
import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import com.afollestad.materialdialogs.MaterialDialog
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.timecat.module.browser.prepareShowInService
import io.reactivex.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class LightningChromeClient(
    private val context: Context,
    private val lightningView: LightningView,
    private val uiController: UIController
) : WebChromeClient(), WebRtcPermissionsView {

    private val geoLocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    @Inject
    internal lateinit var faviconModel: FaviconModel

    @Inject
    internal lateinit var userPreferences: UserPreferences

    @Inject
    internal lateinit var webRtcPermissionsModel: WebRtcPermissionsModel

    @Inject
    @field:DiskScheduler
    internal lateinit var diskScheduler: Scheduler

    init {
        context.injector.inject(this)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        if (lightningView.isShown) {
            uiController.updateProgress(newProgress)
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        lightningView.titleInfo.setFavicon(icon)
        uiController.tabChanged(lightningView)
        cacheFavicon(view.url, icon)
    }

    /**
     * Naive caching of the favicon according to the domain name of the URL
     *
     * @param icon the icon to cache
     */
    private fun cacheFavicon(url: String?, icon: Bitmap?) {
        if (icon == null || url == null) {
            return
        }

        faviconModel.cacheFaviconForUrl(icon, url)
            .subscribeOn(diskScheduler)
            .subscribe()
    }


    override fun onReceivedTitle(view: WebView?, title: String?) {
        if (title != null && !title.isEmpty()) {
            lightningView.titleInfo.setTitle(title)
        } else {
            lightningView.titleInfo.setTitle(context.getString(R.string.untitled))
        }
        uiController.tabChanged(lightningView)
        if (view != null && view.url != null) {
            uiController.updateHistory(title, view.url)
        }
    }

    override fun requestPermissions(permissions: Set<String>, onGrant: (Boolean) -> Unit) {
        val missingPermissions = permissions.filter { PermissionUtils.isGranted(it) }

        if (missingPermissions.isEmpty()) {
            onGrant(true)
        } else {
            PermissionUtils.permission(*missingPermissions.toTypedArray())
                .callback(object :PermissionUtils.SimpleCallback{
                    override fun onGranted() {
                        onGrant(true)
                    }

                    override fun onDenied() {
                        onGrant(false)
                    }

                })
                .request()
        }
    }

    override fun requestResources(source: String,
                                  resources: Array<String>,
                                  onGrant: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val resourcesString = resources.joinToString(separator = "\n")
            BrowserDialog.showPositiveNegativeDialog(
                context = context,
                title = R.string.title_permission_request,
                message = R.string.message_permission_request,
                messageArguments = arrayOf(source, resourcesString),
                positiveButton = DialogItem(title = R.string.action_allow) { onGrant(true) },
                negativeButton = DialogItem(title = R.string.action_dont_allow) { onGrant(false) },
                onCancel = { onGrant(false) }
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest) {
        if (userPreferences.webRtcEnabled) {
            webRtcPermissionsModel.requestPermission(request, this)
        } else {
            request.deny()
        }
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String,
                                                    callback: GeolocationPermissions.Callback) {
        PermissionUtils.permission(PermissionConstants.LOCATION)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    val remember = true
                    MaterialDialog(context).show {
                        prepareShowInService(context)
                        title(R.string.location)
                        val org = if (origin.length > 50) {
                            "${origin.subSequence(0, 50)}..."
                        } else {
                            origin
                        }
                        message(text = org + context.getString(R.string.message_location))
                        cancelable(true)
                        positiveButton(R.string.action_allow) {
                            callback.invoke(origin, true, remember)
                        }
                        negativeButton(R.string.action_dont_allow) {
                            callback.invoke(origin, false, remember)
                        }
                    }
                }

                override fun onDenied() {
                }
            })
            .request()
    }

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean,
                                resultMsg: Message): Boolean {
        uiController.onCreateWindow(resultMsg)
        return true
    }

    override fun onCloseWindow(window: WebView) = uiController.onCloseWindow(lightningView)

    @Suppress("unused", "UNUSED_PARAMETER")
    fun openFileChooser(uploadMsg: ValueCallback<Uri>) = uiController.openFileChooser(uploadMsg)

    @Suppress("unused", "UNUSED_PARAMETER")
    fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) =
        uiController.openFileChooser(uploadMsg)

    @Suppress("unused", "UNUSED_PARAMETER")
    fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) =
        uiController.openFileChooser(uploadMsg)

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        uiController.showFileChooser(filePathCallback)
        return true
    }

    /**
     * Obtain an image that is displayed as a placeholder on a video until the video has initialized
     * and can begin loading.
     *
     * @return a Bitmap that can be used as a place holder for videos.
     */
    override fun getDefaultVideoPoster(): Bitmap? {
        val resources = context.resources
        return BitmapFactory.decodeResource(resources, android.R.drawable.spinner_background)
    }

    /**
     * Inflate a view to send to a LightningView when it needs to display a video and has to
     * show a loading dialog. Inflates a progress view and returns it.
     *
     * @return A view that should be used to display the state
     * of a video's loading progress.
     */
    override fun getVideoLoadingProgressView(): View =
        LayoutInflater.from(context).inflate(R.layout.browser_video_loading_progress, null)

    override fun onHideCustomView() = uiController.onHideCustomView()

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) =
        uiController.onShowCustomView(view, callback)

    override fun onShowCustomView(view: View, requestedOrientation: Int,
                                  callback: WebChromeClient.CustomViewCallback) =
        uiController.onShowCustomView(view, callback, requestedOrientation)

}
