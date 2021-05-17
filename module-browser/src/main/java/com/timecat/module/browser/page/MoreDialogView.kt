package com.timecat.module.browser.page

import acr.browser.lightning.R
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.timecat.component.identity.Attr
import com.timecat.data.bmob.data.User
import com.timecat.layout.ui.drawabe.roundRectSelector
import com.timecat.layout.ui.drawabe.selectableItemBackground
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
        fun openBookmark()
        fun openHistory()
        fun openDownload()
        fun onRefreshCurrentWeb()
        fun onCopyLink()
        fun onShare()
        fun openProperty()
        fun currentLinkIsInBookmarks(): Boolean
        fun collect()
        fun findInPage()
        fun addToHome()
        fun readingMode()
        fun forward()
        fun backward()
    }

    var listener: Listener? = null

    var avatarView: ImageView
    var usernameView: TextView
    var settingView: ImageView

    var newTabView: TextView
    var newIncognitoTabView: TextView

    var bookmarkSwitch: SquareSwitchText
    var bookmarkView: SquareIconText
    var historyView: SquareIconText
    var downloadView: SquareIconText
    var refreshView: SquareIconText
    var copyLinkView: SquareIconText
    var shareView: SquareIconText
    var toolsView: SquareIconText

    init {
        val iconColor = ColorStateList.valueOf(Attr.getIconColor(context))
        avatarView = ImageView {
            layout_id = "avatar"

            layout_width = 48
            layout_height = 48
            padding = 8
            margin_start = 10

            start_toStartOf = parent_id
            top_toTopOf = parent_id
            setImageResource(R.drawable.ic_incognito)
            imageTintList = iconColor
        }
        usernameView = TextView {
            layout_id = "username"
            layout_width = 0
            layout_height = 0

            start_toEndOf = "avatar"
            top_toTopOf = "avatar"
            bottom_toBottomOf = "avatar"
            end_toStartOf = "setting"

            text = "点击登录"
            background = selectableItemBackground(context)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            text_size = 16
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

            layout_width = 48
            layout_height = 48
            padding = 8
            margin_end = 10

            end_toEndOf = parent_id
            top_toTopOf = parent_id

            setImageResource(R.drawable.ic_settings)
            imageTintList = iconColor
        }

        newTabView = TextView {
            layout_id = "newTab"
            layout_width = 0
            layout_height = wrap_content
            start_toStartOf = parent_id
            top_toBottomOf = "avatar"
            end_toStartOf = "newIncognitoTab"

            background_drawable = roundRectSelector()
            drawable_start = R.drawable.ic_add
            TextViewCompat.setCompoundDrawableTintList(this, iconColor)
            padding = 20
            margin = 10

            text = "新标签页"
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            text_size = 20
            setShakelessClickListener {
                listener?.onNewTab()
            }
        }
        newIncognitoTabView = TextView {
            layout_id = "newIncognitoTab"
            layout_width = 0
            layout_height = wrap_content
            end_toEndOf = parent_id
            top_toBottomOf = "avatar"
            start_toEndOf = "newTab"

            background_drawable = roundRectSelector()
            drawable_start = R.drawable.ic_incognito
            TextViewCompat.setCompoundDrawableTintList(this, iconColor)
            padding = 20
            margin = 10

            text = "新无痕页"
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            text_size = 20
            setShakelessClickListener {
                listener?.onNewIncognitoTab()
            }
        }

        bookmarkSwitch = SquareSwitchText(context).apply {
            layout_id = "bookmarkSwitch"
            layout_width = 0
            layout_height = wrap_content
            start_toStartOf = parent_id
            top_toBottomOf = "newTab"

            data = SquareSwitchText.Data(
                "bookmark",
                listener?.currentLinkIsInBookmarks() == true,
                context.getString(R.string.action_add_bookmark)
            )
            onCheckChange = {
                listener?.onToggleBookmark(it)
            }
        }.also {
            addView(it)
        }
        bookmarkView = SquareIconTextView(
            "bookmarkView",
            "R.drawable.ic_bookmark",
            R.string.action_bookmarks
        ) {
            start_toEndOf = "bookmarkSwitch"
            top_toBottomOf = "newTab"
            end_toStartOf = "historyView"
            onClick {
                listener?.openBookmark()
            }
        }
        historyView = SquareIconTextView(
            "historyView",
            "R.drawable.ic_history",
            R.string.action_history
        ) {
            start_toEndOf = "downloadView"
            top_toBottomOf = "newTab"
            end_toStartOf = "bookmarkView"
            onClick {
                listener?.openHistory()
            }
        }
        downloadView = SquareIconTextView(
            "downloadView",
            "R.drawable.ic_bookmark",
            R.string.action_downloads
        ) {
            end_toEndOf = parent_id
            top_toBottomOf = "newTab"
            onClick {
                listener?.openDownload()
            }
        }
        refreshView = SquareIconTextView(
            "refreshView",
            "R.drawable.ic_action_refresh",
            R.string.actionbar_webview_refresh
        ) {
            start_toStartOf = parent_id
            top_toBottomOf = "bookmarkSwitch"
            onClick {
                listener?.onRefreshCurrentWeb()
            }
        }
        copyLinkView = SquareIconTextView(
            "copyLinkView",
            "R.drawable.ic_copy",
            R.string.action_copy
        ) {
            start_toEndOf = "bookmarkSwitch"
            top_toBottomOf = "bookmarkSwitch"
            end_toStartOf = "shareView"
            onClick {
                listener?.onCopyLink()
            }
        }
        shareView = SquareIconTextView(
            "shareView",
            "R.drawable.ic_share",
            R.string.action_share
        ) {
            start_toEndOf = "copyLinkView"
            top_toBottomOf = "bookmarkSwitch"
            end_toStartOf = "toolsView"
            onClick {
                listener?.onShare()
            }
        }
        toolsView = SquareIconTextView(
            "toolsView",
            "R.drawable.ic_bookmark",
            R.string.dialog_tools_title
        ) {
            end_toEndOf = parent_id
            top_toBottomOf = "bookmarkSwitch"
            onClick {
                PopupMenu(context, it).apply {
                    menu.add(R.string.actionbar_webview_collect).setIcon(R.drawable.ic_star).setOnMenuItemClickListener {
                        listener?.collect()
                        true
                    }
                    menu.add(R.string.action_find).setIcon(R.drawable.ic_search).setOnMenuItemClickListener {
                        listener?.findInPage()
                        true
                    }
                    menu.add(R.string.action_add_to_homescreen).setIcon(R.drawable.ic_home).setOnMenuItemClickListener {
                        listener?.addToHome()
                        true
                    }
                    menu.add(R.string.reading_mode).setIcon(R.drawable.ic_action_reading).setOnMenuItemClickListener {
                        listener?.readingMode()
                        true
                    }
                    menu.add(R.string.nav_info).setIcon(R.drawable.ic_info).setOnMenuItemClickListener {
                        listener?.openProperty()
                        true
                    }
                    menu.add(R.string.action_forward).setIcon(R.drawable.ic_action_forward).setOnMenuItemClickListener {
                        listener?.forward()
                        true
                    }
                    menu.add(R.string.action_back).setIcon(R.drawable.ic_back).setOnMenuItemClickListener {
                        listener?.backward()
                        true
                    }
                }
            }
        }
    }

    fun ViewGroup.SquareIconTextView(
        id: String,
        icon: String,
        name: String,
        autoAdd: Boolean = true,
        init: SquareIconText.() -> Unit
    ): SquareIconText {
        val imageView = SquareIconText(context)
        return imageView.apply {
            layout_id = id
            layout_width = 0
            layout_height = wrap_content

            data = SquareIconText.Data(id, icon, name)
        }.apply(init).also { if (autoAdd) addView(it) }
    }

    fun ViewGroup.SquareIconTextView(
        id: String,
        icon: String,
        nameRes: Int,
        autoAdd: Boolean = true,
        init: SquareIconText.() -> Unit
    ): SquareIconText {
        val imageView = SquareIconText(context)
        return imageView.apply {
            layout_id = id
            layout_width = 0
            layout_height = wrap_content

            data = SquareIconText.Data(id, icon, context.getString(nameRes))
        }.apply(init).also { if (autoAdd) addView(it) }
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