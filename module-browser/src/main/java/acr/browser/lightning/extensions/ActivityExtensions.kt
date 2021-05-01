@file:JvmName("ActivityExtensions")

package acr.browser.lightning.extensions

import androidx.annotation.StringRes
import com.timecat.element.alert.ToastUtil

/**
 * Displays a snackbar to the user with a [StringRes] message.
 *
 * NOTE: If there is an accessibility manager enabled on
 * the device, such as LastPass, then the snackbar animations
 * will not work.
 *
 * @param resource the string resource to display to the user.
 */
fun snackbar(@StringRes resource: Int) {
    ToastUtil.i(resource)
}

/**
 * Display a snackbar to the user with a [String] message.
 *
 * @param message the message to display to the user.
 * @see snackbar
 */
fun snackbar(message: String) {
    ToastUtil.i(message)
}
