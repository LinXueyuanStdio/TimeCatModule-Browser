package acr.browser.lightning.browser.fragment

import acr.browser.lightning.R
import acr.browser.lightning.browser.TabsManager
import acr.browser.lightning.browser.TabsView
import acr.browser.lightning.browser.fragment.anim.HorizontalItemAnimator
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.di.injector
import acr.browser.lightning.extensions.desaturate
import acr.browser.lightning.extensions.drawTrapezoid
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.ThemeUtils
import acr.browser.lightning.utils.Utils
import acr.browser.lightning.view.LightningView
import acr.browser.lightning.view.SearchView
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.same.lib.core.ActionBar
import com.same.lib.core.ActionBarMenuItem
import com.same.lib.drawable.CloseProgressDrawable2
import com.same.lib.helper.LayoutHelper
import com.same.lib.util.*
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.component.identity.Attr
import com.timecat.layout.ui.layout.*
import java.util.*
import javax.inject.Inject

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/5/2
 * @description null
 * @usage null
 */
class TabsFrameLayout @JvmOverloads constructor(
    context: Context,
    var uiController: UIController,
    private var isIncognito: Boolean = false,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener, View.OnLongClickListener, TabsView {
    var listener: ActionBarMenuItem.ActionBarMenuItemSearchListener? = null
    private var darkTheme: Boolean = false
    private var iconColor: Int = 0
    private var colorMode = true

    private var tabsAdapter: LightningViewAdapter? = null

    @Inject
    internal lateinit var userPreferences: UserPreferences
    private var tabs_list: RecyclerView

    init {
        injector.inject(this)
        darkTheme = userPreferences.useTheme != 0 || isIncognito
        colorMode = userPreferences.colorModeEnabled
        colorMode = colorMode and !darkTheme
        iconColor = ThemeUtils.getIconThemeColor(context, darkTheme)

        tabs_list = RecyclerView(context).apply {
            overScrollMode = OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
        }
        addView(tabs_list, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        tabsAdapter = LightningViewAdapter()
        tabs_list.apply {
            val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            val animator = HorizontalItemAnimator().apply {
                supportsChangeAnimations = false
                addDuration = 200
                changeDuration = 0
                removeDuration = 200
                moveDuration = 200
            }
            setLayerType(View.LAYER_TYPE_NONE, null)
            itemAnimator = animator
            this.layoutManager = layoutManager
            adapter = tabsAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDetachedFromWindow() {
        tabsAdapter = null
        super.onDetachedFromWindow()
    }

    private fun getTabsManager(): TabsManager = uiController.getTabModel()

    override fun onResume() {
        tabsAdapter?.notifyDataSetChanged()
    }

    override fun tabsInitialized() {
        tabsAdapter?.notifyDataSetChanged()
    }

    override fun reinitializePreferences() {
        darkTheme = userPreferences.useTheme != 0 || isIncognito
        colorMode = userPreferences.colorModeEnabled
        colorMode = colorMode and !darkTheme
        iconColor = ThemeUtils.getIconThemeColor(context, darkTheme)
        tabsAdapter?.notifyDataSetChanged()
    }

    override fun onClick(v: View) = when (v.id) {
        R.id.tab_header_button -> uiController.showCloseDialog(getTabsManager().indexOfCurrentTab())
        R.id.new_tab_button -> uiController.newTabButtonClicked()
        R.id.action_back -> uiController.onBackButtonPressed()
        R.id.action_forward -> uiController.onForwardButtonPressed()
        R.id.action_home -> uiController.onHomeButtonPressed()
        else -> {
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (v.id) {
            R.id.new_tab_button -> uiController.newTabButtonLongClicked()
            else -> {
            }
        }
        return true
    }

    override fun tabAdded() {
        tabsAdapter?.let {
            it.showTabs(toViewModels(getTabsManager().allTabs))
            tabs_list.postDelayed({ tabs_list.smoothScrollToPosition(it.itemCount - 1) }, 500)
        }
    }

    override fun tabRemoved(position: Int) {
        tabsAdapter?.showTabs(toViewModels(getTabsManager().allTabs))
    }

    override fun tabChanged(position: Int) {
        tabsAdapter?.showTabs(toViewModels(getTabsManager().allTabs))
    }

    private fun toViewModels(tabs: List<LightningView>) = tabs.map(::TabViewState)

    private inner class LightningViewAdapter : RecyclerView.Adapter<LightningViewAdapter.LightningViewHolder>() {

        private val layoutResourceId: Int = R.layout.browser_tab_list_item_horizontal_chip
        private val backgroundTabDrawable: Drawable?
        private val foregroundTabBitmap: Bitmap?
        private var tabList: List<TabViewState> = ArrayList()

        init {
            val context = requireNotNull(context) { "Adapter cannot be initialized when fragment is detached" }

            val backgroundColor = Utils.mixTwoColors(ThemeUtils.getPrimaryColor(context), Color.BLACK, 0.75f)
            val backgroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175f), Utils.dpToPx(30f), Bitmap.Config.ARGB_8888).also {
                Canvas(it).drawTrapezoid(backgroundColor, true)
            }
            backgroundTabDrawable = BitmapDrawable(resources, backgroundTabBitmap)

            val foregroundColor = ThemeUtils.getPrimaryColor(context)
            foregroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175f), Utils.dpToPx(30f), Bitmap.Config.ARGB_8888).also {
                Canvas(it).drawTrapezoid(foregroundColor, false)
            }
        }

        fun showTabs(tabs: List<TabViewState>) {
            val oldList = tabList
            tabList = ArrayList(tabs)

            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = tabList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition] == tabList[newItemPosition]

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldTab = oldList[oldItemPosition]
                    val newTab = tabList[newItemPosition]

                    return (oldTab.title == newTab.title
                        && oldTab.favicon == newTab.favicon
                        && oldTab.isForegroundTab == newTab.isForegroundTab
                        && oldTab == newTab)
                }
            })

            result.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LightningViewHolder {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(layoutResourceId, viewGroup, false)
            return LightningViewHolder(view)
        }

        override fun onBindViewHolder(holder: LightningViewHolder, position: Int) {
            holder.chip.tag = position
            holder.chip.jumpDrawablesToCurrentState()

            val web = tabList[position]

            updateViewHolderTitle(holder, web.title)
            updateViewHolderAppearance(holder, web.favicon, web.isForegroundTab)
            updateViewHolderFavicon(holder, web.favicon, web.isForegroundTab)
            updateViewHolderBackground(holder, web.isForegroundTab)
        }

        private fun updateViewHolderTitle(viewHolder: LightningViewHolder, title: String) {
            viewHolder.chip.text = title
        }

        private fun updateViewHolderFavicon(viewHolder: LightningViewHolder, favicon: Bitmap, isForeground: Boolean) {
            if (isForeground) {
                viewHolder.chip.apply {
                    chipIcon = BitmapDrawable(context.getResources(), favicon)
                }
            } else {
                viewHolder.chip.apply {
                    chipIcon = BitmapDrawable(context.getResources(), favicon.desaturate())
                }
            }
        }

        private fun updateViewHolderBackground(viewHolder: LightningViewHolder, isForeground: Boolean) {
            viewHolder.chip.isCloseIconVisible = isForeground
        }

        private fun updateViewHolderAppearance(viewHolder: LightningViewHolder, favicon: Bitmap, isForeground: Boolean) {
            if (isForeground) {
                viewHolder.chip.textStyle = Typeface.BOLD
                val foregroundDrawable = BitmapDrawable(resources, foregroundTabBitmap)
                if (!isIncognito && colorMode) {
                    foregroundDrawable.setColorFilter(uiController.getUiColor(), PorterDuff.Mode.SRC_IN)
                }
//                TextViewCompat.setTextAppearance(viewHolder.chip, R.style.boldText)
//                DrawableUtils.setBackground(viewHolder.chip, foregroundDrawable)
                if (!isIncognito && colorMode) {
                    uiController.changeToolbarBackground(favicon, foregroundDrawable)
                }
            } else {
                viewHolder.chip.textStyle = Typeface.NORMAL
//                TextViewCompat.setTextAppearance(viewHolder.chip, R.style.normalText)
//                DrawableUtils.setBackground(viewHolder.chip, backgroundTabDrawable)
            }
        }

        override fun getItemCount() = tabList.size

        inner class LightningViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val chip: Chip = view.findViewById(R.id.chip)

            init {
                chip.chipIconTint = ColorStateList.valueOf(iconColor)
                chip.setOnCloseIconClickListener {
                    uiController.tabCloseClicked(adapterPosition)
                }
                chip.setOnClickListener {
                    val curActiveIdx = getTabsManager().indexOfCurrentTab()
                    LogUtil.se("chip on click, pos=${adapterPosition}, curActive=${curActiveIdx}")
                    uiController.tabClicked(adapterPosition)
                }
                chip.setOnLongClickListener {
                    LogUtil.se("chip on long click, pos=${adapterPosition}")
                    uiController.showCloseDialog(adapterPosition)
                    true
                }
            }
        }

    }

    //region search field
    lateinit var searchContainer: FrameLayout
    lateinit var searchField: SearchView
    lateinit var searchFieldCaption: TextView
    lateinit var clearButton: ImageView
    lateinit var parentActionBar: ActionBar
    private var progressDrawable: CloseProgressDrawable2? = null
    private var ignoreOnTextChange = false
    private var animateClear = true
    private var clearsTextOnSearchCollapse = true

    fun isSearchFieldVisible(): Boolean {
        return searchContainer.visibility == VISIBLE
    }

    fun toggleSearch(openKeyboard: Boolean): Boolean {
        if (listener != null) {
            val animator = listener?.customToggleTransition
            if (animator != null) {
                searchField.setText("")
                animator.start()
                return true
            }
        }
        return if (searchContainer.visibility == VISIBLE) {
            searchContainer.visibility = GONE
            searchField.clearFocus()
            visibility = VISIBLE
            listener?.onSearchCollapse()
            if (openKeyboard) {
                Keyboard.hideKeyboard(searchField)
            }
            if (clearsTextOnSearchCollapse) {
                searchField.setText("")
            }
            false
        } else {
            listener?.onSearchExpand()
            searchContainer.visibility = VISIBLE
            visibility = GONE
            searchField.setText("")
            searchField.requestFocus()
            if (openKeyboard) {
                Keyboard.showKeyboard(searchField)
            }
            true
        }
    }

    fun bindSearch(parentActionBar: ActionBar) {
        this.parentActionBar = parentActionBar
        searchContainer = object : FrameLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                measureChildWithMargins(clearButton, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val width: Int = if (searchFieldCaption.getVisibility() == VISIBLE) {
                    measureChildWithMargins(searchFieldCaption, widthMeasureSpec, MeasureSpec.getSize(widthMeasureSpec) / 2, heightMeasureSpec, 0)
                    searchFieldCaption.getMeasuredWidth() + Space.dp(4f)
                } else {
                    0
                }
                measureChildWithMargins(searchField, widthMeasureSpec, width, heightMeasureSpec, 0)
                val w = MeasureSpec.getSize(widthMeasureSpec)
                val h = MeasureSpec.getSize(heightMeasureSpec)
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
            }

            override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                super.onLayout(changed, left, top, right, bottom)
                val x: Int = if (Store.isRTL) {
                    0
                } else {
                    if (searchFieldCaption.getVisibility() == VISIBLE) {
                        searchFieldCaption.getMeasuredWidth() + Space.dp(4f)
                    } else {
                        0
                    }
                }
                searchField.layout(x, searchField.getTop(), x + searchField.getMeasuredWidth(), searchField.getBottom())
            }
        }
        val linear = LinearLayout(context).apply {
            addView(searchContainer, 0, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 6, 0, 0, 0))
        }
        parentActionBar.addView(linear)
        linear.apply{
            (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                topMargin = Space.statusBarHeight
                leftMargin = 48.dp
                rightMargin = 0
            }
        }
        searchContainer.setVisibility(GONE)

        searchFieldCaption = TextView(context)
        searchFieldCaption.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        searchFieldCaption.setTextColor(ColorManager.getColor(KeyHub.key_actionBarDefaultSearch))
        searchFieldCaption.setSingleLine(true)
        searchFieldCaption.setEllipsize(TextUtils.TruncateAt.END)
        searchFieldCaption.setVisibility(GONE)
        searchFieldCaption.setGravity(if (Store.isRTL) Gravity.RIGHT else Gravity.LEFT)

        searchField = object : SearchView(context) {
            override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
                if (keyCode == KeyEvent.KEYCODE_DEL && searchField.length() == 0 && searchFieldCaption.getVisibility() == VISIBLE && searchFieldCaption.length() > 0) {
                    clearButton.callOnClick()
                    return true
                }
                return super.onKeyDown(keyCode, event)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (!Keyboard.showKeyboard(this)) {
                        clearFocus()
                        requestFocus()
                    }
                }
                return super.onTouchEvent(event)
            }
        }
        searchField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        searchField.setHintTextColor(ColorManager.getColor(KeyHub.key_actionBarDefaultSearchPlaceholder))
        searchField.setTextColor(ColorManager.getColor(KeyHub.key_actionBarDefaultSearch))
        searchField.setSingleLine(true)
        searchField.setBackground(null)
        searchField.setPadding(0, 0, 0, 0)
        val inputType: Int = searchField.getInputType() or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        searchField.setInputType(inputType)
        if (Build.VERSION.SDK_INT < 23) {
            searchField.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode) {}
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    return false
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    return false
                }
            })
        }
        searchField.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (event != null && (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_SEARCH
                    || event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                Keyboard.hideKeyboard(searchField)
                listener?.onSearchPressed(searchField)
            }
            false
        }
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (ignoreOnTextChange) {
                    ignoreOnTextChange = false
                    return
                }
                listener?.onTextChanged(searchField)
                if (TextUtils.isEmpty(s) &&
                    (listener == null || !listener!!.forceShowClear()) &&
                    (searchFieldCaption.getVisibility() != VISIBLE)) {
                    if (clearButton.getTag() != null) {
                        clearButton.setTag(null)
                        clearButton.clearAnimation()
                        if (animateClear) {
                            clearButton.animate()
                                .setInterpolator(DecelerateInterpolator())
                                .alpha(0.0f)
                                .setDuration(180)
                                .scaleY(0.0f)
                                .scaleX(0.0f)
                                .rotation(45f)
                                .withEndAction {
                                    clearButton.setVisibility(INVISIBLE)
                                }.start()
                        } else {
                            clearButton.setAlpha(0.0f)
                            clearButton.setRotation(45f)
                            clearButton.setScaleX(0.0f)
                            clearButton.setScaleY(0.0f)
                            clearButton.setVisibility(INVISIBLE)
                            animateClear = true
                        }
                    }
                } else {
                    if (clearButton.getTag() == null) {
                        clearButton.setTag(1)
                        clearButton.clearAnimation()
                        clearButton.setVisibility(VISIBLE)
                        if (animateClear) {
                            clearButton.animate()
                                .setInterpolator(DecelerateInterpolator())
                                .alpha(1.0f)
                                .setDuration(180)
                                .scaleY(1.0f)
                                .scaleX(1.0f)
                                .rotation(0f)
                                .start()
                        } else {
                            clearButton.setAlpha(1.0f)
                            clearButton.setRotation(0f)
                            clearButton.setScaleX(1.0f)
                            clearButton.setScaleY(1.0f)
                            animateClear = true
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        searchField.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_SEARCH)
        searchField.setTextIsSelectable(false)
        if (!Store.isRTL) {
            searchContainer.addView(searchFieldCaption, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36f, Gravity.CENTER_VERTICAL or Gravity.LEFT, 0f, 5.5f, 0f, 0f))
            searchContainer.addView(searchField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36f, Gravity.CENTER_VERTICAL, 0f, 0f, 48f, 0f))
        } else {
            searchContainer.addView(searchField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36f, Gravity.CENTER_VERTICAL, 0f, 0f, 48f, 0f))
            searchContainer.addView(searchFieldCaption, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36f, Gravity.CENTER_VERTICAL or Gravity.RIGHT, 0f, 5.5f, 48f, 0f))
        }

        clearButton = object : AppCompatImageView(context) {
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                clearAnimation()
                if (tag == null) {
                    clearButton.setVisibility(INVISIBLE)
                    clearButton.setAlpha(0.0f)
                    clearButton.setRotation(45f)
                    clearButton.setScaleX(0.0f)
                    clearButton.setScaleY(0.0f)
                } else {
                    clearButton.setAlpha(1.0f)
                    clearButton.setRotation(0f)
                    clearButton.setScaleX(1.0f)
                    clearButton.setScaleY(1.0f)
                }
            }
        }
        clearButton.setImageDrawable(CloseProgressDrawable2().also { progressDrawable = it })
        clearButton.setColorFilter(PorterDuffColorFilter(Attr.getIconColor(context), PorterDuff.Mode.MULTIPLY))
        clearButton.setScaleType(ImageView.ScaleType.CENTER)
        clearButton.setAlpha(0.0f)
        clearButton.setRotation(45f)
        clearButton.setScaleX(0.0f)
        clearButton.setScaleY(0.0f)
        clearButton.setOnClickListener {
            if (searchField.length() != 0) {
                searchField.setText("")
            } else if (searchFieldCaption.getVisibility() == VISIBLE) {
                searchFieldCaption.setVisibility(GONE)
                listener?.onCaptionCleared()
            }
            searchField.requestFocus()
            Keyboard.showKeyboard(searchField)
        }
        clearButton.setContentDescription(Lang.getString(context, "ClearButton", R.string.ClearButton))
        searchContainer.addView(clearButton, LayoutHelper.createFrame(48, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL or Gravity.RIGHT))
    }

    fun openSearch(openKeyboard: Boolean) {
        if (searchContainer.visibility == VISIBLE) {
            return
        }
        parentActionBar.onSearchFieldVisibilityChanged(toggleSearch(openKeyboard))
    }

    fun setSearchFieldHint(hint: CharSequence?) {
        searchField.hint = hint
        contentDescription = hint
    }

    fun setSearchFieldText(text: CharSequence, animated: Boolean) {
        animateClear = animated
        searchField.setText(text)
        if (!TextUtils.isEmpty(text)) {
            searchField.setSelection(text.length)
        }
    }

    fun clearSearchText() {
        searchField.setText("")
    }

    fun setShowSearchProgress(show: Boolean) {
        if (progressDrawable == null) {
            return
        }
        if (show) {
            progressDrawable!!.startAnimation()
        } else {
            progressDrawable!!.stopAnimation()
        }
    }
    fun setSearchFieldCaption(caption: CharSequence?) {
        if (TextUtils.isEmpty(caption)) {
            searchFieldCaption.visibility = GONE
        } else {
            searchFieldCaption.visibility = VISIBLE
            searchFieldCaption.text = caption
        }
    }
    //endregion
    companion object {

        @JvmStatic
        fun createTabsView(
            context: Context,
            isIncognito: Boolean,
            controller: UIController
        ): TabsFrameLayout {
            return TabsFrameLayout(context, controller, isIncognito)
        }
    }
}