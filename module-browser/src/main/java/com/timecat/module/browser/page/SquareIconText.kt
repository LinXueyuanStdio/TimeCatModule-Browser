package com.timecat.module.browser.page

import acr.browser.lightning.R
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.timecat.component.identity.Attr
import com.timecat.layout.ui.layout.*
import com.timecat.layout.ui.utils.IconLoader

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/17
 * @description null
 * @usage null
 */
class SquareIconText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var imageView: ImageView
    var nameView: TextView

    init {
        orientation = VERTICAL
        layout_width = 48
        layout_height = wrap_content
        padding = 4
        margin = 4
        imageView = ImageView {
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
    }

    var name: String
        get() = nameView.text.toString()
        set(value) {
            nameView.text = value
        }
    var icon: String = "R.drawable.ic_launcher"
        set(value) {
            IconLoader.loadIcon(context, imageView, value)
            field = value
        }

    fun onClick(onClick: (item: SquareIconText) -> Unit) {
        setShakelessClickListener {
            onClick(this@SquareIconText)
        }
    }

    var uuid: String = ""

    var data: Data
        get() = Data(uuid, icon, name)
        set(value) {
            uuid = value.uuid
            icon = value.icon
            name = value.name
        }

    data class Data(var uuid: String, var icon: String, var name: String)
}