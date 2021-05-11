package com.timecat.module.browser

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.LinearLayout
import com.timecat.module.browser.page.IncognitoPage

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/26
 * @description null
 * @usage null
 */
class KeyEventView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface KeyEventListener {
        fun ctrl_F()
        fun ctrl_T()
        fun ctrl_W()
        fun ctrl_Q()
        fun ctrl_R()
        fun ctrl_tab()
        fun ctrl_shift_tab()
        fun ctrl_shift_P()
        fun search()
        fun alt_tab(number: Int)
    }

    var listener: KeyEventListener? = null
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Keyboard shortcuts
        if (event.action == KeyEvent.ACTION_DOWN) {
            when {
                event.isCtrlPressed -> when (event.keyCode) {
                    KeyEvent.KEYCODE_F -> {
                        listener?.ctrl_F()
                        return true
                    }
                    KeyEvent.KEYCODE_T -> {
                        listener?.ctrl_T()
                        return true
                    }
                    KeyEvent.KEYCODE_W -> {
                        listener?.ctrl_W()
                        return true
                    }
                    KeyEvent.KEYCODE_Q -> {
                        listener?.ctrl_Q()
                        return true
                    }
                    KeyEvent.KEYCODE_R -> {
                        listener?.ctrl_R()
                        return true
                    }
                    KeyEvent.KEYCODE_TAB -> {
                        if (event.isShiftPressed) {
                            listener?.ctrl_shift_tab()
                        } else {
                            listener?.ctrl_tab()
                        }

                        return true
                    }
                    KeyEvent.KEYCODE_P ->{
                        // Open a new private window
                        if (event.isShiftPressed) {
                            listener?.ctrl_shift_P()
                            return true
                        }
                    }
                }
                event.keyCode == KeyEvent.KEYCODE_SEARCH -> {
                    listener?.search()
                    return true
                }
                event.isAltPressed -> {
                    // Alt + tab number
                    if (KeyEvent.KEYCODE_0 <= event.keyCode && event.keyCode <= KeyEvent.KEYCODE_9) {
                        listener?.alt_tab(event.keyCode)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}