package acr.browser.lightning.view

import acr.browser.lightning.R
import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.timecat.layout.ui.layout.hint_text_res

open class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    interface PreFocusListener {
        fun onPreFocus()
    }

    var onPreFocusListener: PreFocusListener? = null
    var onRightDrawableClickListener: ((SearchView) -> Unit)? = null
    private var isBeingClicked: Boolean = false
    private var timePressed: Long = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                timePressed = System.currentTimeMillis()
                isBeingClicked = true
            }
            MotionEvent.ACTION_CANCEL -> isBeingClicked = false
            MotionEvent.ACTION_UP -> if (isBeingClicked && !isLongPress(timePressed)) {
                onPreFocusListener?.onPreFocus()
            }
        }

        compoundDrawables[2]
            ?.takeIf { event.x > (width - paddingRight - it.intrinsicWidth) }
            ?.let {
                if (event.action == MotionEvent.ACTION_UP) {
                    onRightDrawableClickListener?.invoke(this@SearchView)
                }
                return true
            }


        return super.onTouchEvent(event)
    }

    private fun isLongPress(actionDownTime: Long): Boolean =
        System.currentTimeMillis() - actionDownTime >= ViewConfiguration.getLongPressTimeout()


    init {
        hint_text_res = R.string.search_hint
        imeOptions = EditorInfo.IME_ACTION_GO
        inputType = InputType.TYPE_TEXT_VARIATION_URI
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
        }

    }
}
