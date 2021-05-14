package com.timecat.module.browser.page

import acr.browser.lightning.R
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.timecat.component.identity.Attr
import com.timecat.data.bmob.data.User
import com.timecat.layout.ui.drawabe.roundRectSelector
import com.timecat.layout.ui.layout.*
import com.timecat.layout.ui.utils.IconLoader

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/14
 * @description null
 * @usage null
 */
class MoreDialogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    interface Listener {
        fun onToggleBookmark(check: Boolean)
        fun onLogin()
        fun onClickUser(user: User)
        fun onSetting()
        fun onNewTab()
        fun onNewIncognitoTab()
        fun openBookmarkOrHistory()
        fun openDownload()
        fun onRefreshCurrentWeb()
        fun onCopyLink()
        fun onShare()
        fun openProperty()
    }

    var listener: Listener? = null

    var avatarView: ImageView
    var usernameView: TextView
    var settingView: ImageView

    var newTabView: TextView
    var newIncognitoTabView: TextView

    init {
        val iconColor = ColorStateList.valueOf(Attr.getIconColor(context))
        avatarView = ImageView {
            layout_id = "avatar"

            layout_width = 40
            layout_height = 40
            padding = 8

            start_toStartOf = parent_id
            top_toTopOf = parent_id
        }
        usernameView = TextView {
            layout_id = "username"

            start_toEndOf = "avatar"
            top_toTopOf = "avatar"
            bottom_toBottomOf = "avatar"
            end_toStartOf = "setting"

            text = "点击登录"
            setShakelessClickListener {
                if (user == null) {
                    listener?.onLogin()
                } else {
                    listener?.onClickUser(user!!)
                }
            }
        }
        settingView = ImageView {
            layout_id = "setting"

            layout_width = 40
            layout_height = 40
            padding = 8

            end_toEndOf = parent_id
            top_toTopOf = parent_id
        }

        newTabView = TextView {
            layout_id = "newTab"

            start_toStartOf = parent_id
            top_toBottomOf = "avatar"
            end_toStartOf = "newIncognitoTab"

            background_drawable = roundRectSelector()
            drawable_start = R.drawable.ic_add
            TextViewCompat.setCompoundDrawableTintList(this, iconColor)

            text = "新标签页"
            setShakelessClickListener {
                listener?.onNewTab()
            }
        }

        newIncognitoTabView = TextView {
            layout_id = "newIncognitoTab"

            end_toEndOf = parent_id
            top_toBottomOf = "avatar"
            start_toEndOf = "newTab"

            background_drawable = roundRectSelector()
            drawable_start = R.drawable.ic_incognito
            TextViewCompat.setCompoundDrawableTintList(this, iconColor)

            text = "新无痕页"
            setShakelessClickListener {
                listener?.onNewIncognitoTab()
            }
        }
    }

    var user: User? = null
        set(value) {
            if (value == null) {
                avatarView.setImageResource(R.drawable.ic_launcher)
                usernameView.setText("点击登录")
            } else {
                IconLoader.loadIcon(context, avatarView, value.avatar)
                usernameView.setText(value.username)
            }
            field = value
        }
}