package com.timecat.module.browser.page

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.timecat.component.identity.Attr
import com.timecat.layout.ui.layout.*
import com.timecat.layout.ui.listener.OnDebouncedClickListener
import com.timecat.layout.ui.utils.IconLoader

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/17
 * @description null
 * @usage null
 */
class SquareSwitchText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var switchCompat: SwitchCompat
    var nameView: TextView

    init {
        orientation = VERTICAL
        layout_width = 48
        layout_height = wrap_content
        padding = 4
        margin = 4
        switchCompat = SwitchCompat(context).apply {
            layout_width = 36
            layout_height = 36
            margin = 4
            layout_gravity = gravity_center
            isClickable = false
        }
        nameView = TextView {
            layout_width = wrap_content
            layout_height = wrap_content
            margin = 4
            layout_gravity = gravity_center
            isClickable = false
            setTextColor(Attr.getSecondaryTextColor(context))
            setTextSize(12f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        setShakelessClickListener {
            switchCompat.isChecked = !switchCompat.isChecked
        }
    }

    var name: String
        get() = nameView.text.toString()
        set(value) {
            nameView.text = value
        }
    var isChecked: Boolean
        get() = switchCompat.isChecked
        set(value) {
            switchCompat.isChecked = value
        }
    var onCheckChange: (isChecked: Boolean) -> Unit = {}
    var initCheck: Boolean
        get() = switchCompat.isChecked
        set(value) {
            switchCompat.setOnCheckedChangeListener(null)
            switchCompat.isChecked = value
            switchCompat.setOnCheckedChangeListener { _, isChecked ->
                onCheckChange(isChecked)
            }
        }

    fun onClick(onClick: (item: SquareSwitchText) -> Unit) {
        setShakelessClickListener {
            onClick(this@SquareSwitchText)
        }
    }

    var uuid: String = ""

    var data: Data
        get() = Data(uuid, initCheck, name)
        set(value) {
            uuid = value.uuid
            initCheck = value.initCheck
            name = value.name
        }

    data class Data(var uuid: String, var initCheck: Boolean, var name: String)
}