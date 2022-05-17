package com.jack.edit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View.OnFocusChangeListener
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat

class ExtendedEditText(context: Context, attrs: AttributeSet? = null) :
    AppCompatEditText(context, attrs) {
    private val SPACE = " "

    /**
     * 默认是手机号模板
     */
    private val DEFAULT_PATTERN = intArrayOf(3, 4, 4)

    private var mTextChangeListener: OnTextChangeListener? = null

    private var mMarkerClickListener: OnMarkerClickListener? = null

    private var mTextWatcher: TextWatcher? = null

    private var preLength = 0

    private var currLength = 0

    private var mRightMarkerDrawable: Drawable? = null

    private var mLeftDrawable: Drawable? = null

    private var hasFocused = false

    /**
     * 模板
     */
    private lateinit var pattern: IntArray

    /**
     * 根据模板控制分隔符的插入位置
     */
    private lateinit var intervals: IntArray

    /**
     * 分割符，默认使用空格分割
     */
    private var separator: String? = null

    /**
     * 根据模板自动计算最大输入长度，超出输入无效。使用pattern时无需在xml中设置maxLength属性，若需要设置时应注意加上分隔符的数量
     */
    private var maxLength = 0

    /**
     * 设置为true时功能同EditText
     */
    private var hasNoSeparator = false

    /**
     * 自定义右侧Marker点击选项使能
     */
    private var customizeMarkerEnable = false

    /**
     * 自定义选项后选项显示的时间，默认输入后显示
     */
    private var mShowMarkerTime: ShowMarkerTime? = null

    private var mTextPaint: Paint? = null

    private var mRect: Rect? = null

    private var mTextRect: Rect? = null

    private var mBitmap: Bitmap? = null

    private var mBitPaint: Paint? = null

    /**
     * 仿iOS风格，目前需要结合shape.xml的方式设置外边框
     */
    private var iOSStyleEnable = false

    private var iOSFrameHide = false

    private var mHintCharSeq: CharSequence? = null

    private fun init() {
        // 如果设置 inputType="number" 的话是没法插入空格的，所以强行转为inputType="phone"
        if (inputType == InputType.TYPE_CLASS_NUMBER) {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        setPattern(DEFAULT_PATTERN)
        mTextWatcher = MyTextWatcher()
        this.addTextChangedListener(mTextWatcher)
        mRightMarkerDrawable = compoundDrawables[2]
        if (customizeMarkerEnable && mRightMarkerDrawable != null) { // 如果自定义Marker，暂时不显示rightDrawable
            setCompoundDrawables(
                compoundDrawables[0],
                compoundDrawables[1],
                null,
                compoundDrawables[3]
            )
        }
        if (mRightMarkerDrawable == null) { // 如未设置则采用默认
            mRightMarkerDrawable = ContextCompat.getDrawable(context, R.mipmap.icon_clear)
            if (mRightMarkerDrawable != null) {
                mRightMarkerDrawable!!.setBounds(
                    0,
                    0,
                    mRightMarkerDrawable!!.intrinsicWidth,
                    mRightMarkerDrawable!!.intrinsicHeight
                )
            }
        }
        onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            hasFocused = hasFocus
            markerFocusChangeLogic()
            iOSFocusChangeLogic()
        }
        if (iOSStyleEnable) {
            initiOSObjects()
        }
    }

    private fun initiOSObjects() {
        mLeftDrawable = compoundDrawables[0]
        if (mLeftDrawable != null) {
            if (mBitmap == null || mBitPaint == null) {
                val bd = mLeftDrawable as BitmapDrawable?
                mBitmap = bd!!.bitmap
                mBitPaint = Paint()
                mBitPaint!!.isAntiAlias = true
            }
            setCompoundDrawables(
                null, compoundDrawables[1],
                compoundDrawables[2], compoundDrawables[3]
            )
        }
        mHintCharSeq = hint
        if (mHintCharSeq != null) {
            hint = ""
            if (mRect == null || mTextRect == null || mTextPaint == null) {
                mRect = Rect(left, top, width, height)
                mTextRect = Rect()
                mTextPaint = Paint()
                mTextPaint!!.isAntiAlias = true
                mTextPaint!!.textSize = textSize
                mTextPaint!!.color = currentHintTextColor
                mTextPaint!!.textAlign = Paint.Align.CENTER
                mTextPaint!!.getTextBounds(
                    mHintCharSeq.toString(),
                    0,
                    mHintCharSeq!!.length,
                    mTextRect
                )
            }
        }
        iOSFrameHide = false
    }

    @SuppressLint("CanvasSize")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (iOSStyleEnable) {
            if (iOSFrameHide) {
                return
            }
            if (mHintCharSeq != null) {
                val fontMetrics = mTextPaint!!.fontMetricsInt
                val textCenterY =
                    (mRect!!.bottom + mRect!!.top - fontMetrics.bottom - fontMetrics.top) / 2
                canvas.drawText(
                    mHintCharSeq.toString(), canvas.width / 2f, canvas.height / 2f + textCenterY,
                    mTextPaint!!
                )
            }
            if (mBitmap != null) {
                canvas.drawBitmap(
                    mBitmap!!,
                    (canvas.width - mTextRect!!.width()) / 2f - mBitmap!!.width - compoundDrawablePadding,
                    (canvas.height - mBitmap!!.height) / 2f, mBitPaint
                )
            }
        }
    }

    /**
     * 监听右侧Marker图标点击事件
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (hasFocused && mRightMarkerDrawable != null && event.action == MotionEvent.ACTION_UP) {
            val rect = mRightMarkerDrawable!!.bounds
            val height = rect.height()
            val rectTopY: Int = (getHeight() - height) / 2
            val isAreaX = event.x >= width - totalPaddingRight &&
                    event.x <= width - paddingRight
            val isAreaY = event.y >= rectTopY && event.y <= rectTopY + height
            if (isAreaX && isAreaY) {
                if (customizeMarkerEnable) {
                    if (mMarkerClickListener != null) {
                        mMarkerClickListener!!.onMarkerClick(event.rawX, event.rawY)
                    }
                } else {
                    error = null
                    setText("")
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 自定义分隔符
     */
    fun setSeparator(separator: String) {
        this.separator = separator
    }

    /**
     * 自定义分割模板
     *
     * @param pattern 每一段的字符个数的数组
     */
    fun setPattern(pattern: IntArray) {
        this.pattern = pattern
        intervals = IntArray(pattern.size)
        var count = 0
        var sum = 0
        for (i in pattern.indices) {
            sum += pattern[i]
            intervals[i] = sum + count
            if (i < pattern.size - 1) {
                count++
            }
        }
        maxLength = intervals[intervals.size - 1]
    }

    /**
     * 自定义输入框最右边Marker图标
     */
    fun setRightMarkerDrawable(resId: Int) {
        mRightMarkerDrawable = ContextCompat.getDrawable(getContext(), resId)
    }

    /**
     * 输入待转换格式的字符串
     */
    fun setTextToSeparate(c: CharSequence?) {
        if (c == null || c.isEmpty()) {
            return
        }
        setText("")
        for (i in c.indices) {
            append(c.subSequence(i, i + 1))
        }
    }

    /**
     * 获得除去分割符的输入框内容
     */
    fun getNonSeparatorText(): String {
        return text.toString().replace(separator!!.toRegex(), "")
    }

    /**
     * 是否自定义Marker
     */
    fun setCustomizeMarkerEnable(customizeMarkerEnable: Boolean) {
        this.customizeMarkerEnable = customizeMarkerEnable
        if (customizeMarkerEnable && mRightMarkerDrawable != null) { // 如果自定义Marker，暂时不显示rightDrawable
            setCompoundDrawables(
                compoundDrawables[0], compoundDrawables[1],
                null, compoundDrawables.get(3)
            )
        }
    }

    /**
     * Marker在什么时间显示
     *
     * @param showMarkerTime BEFORE_INPUT：没有输入内容时显示；
     * AFTER_INPUT：有输入内容后显示；
     * ALWAYS：（获得焦点后）一直显示
     */
    fun setShowMarkerTime(showMarkerTime: ShowMarkerTime?) {
        mShowMarkerTime = showMarkerTime
    }

    /**
     * @return 是否有分割符
     */
    fun hasNoSeparator(): Boolean {
        return hasNoSeparator
    }

    /**
     * @param hasNoSeparator true设置无分隔符模式，功能同EditText
     */
    fun setHasNoSeparator(hasNoSeparator: Boolean) {
        this.hasNoSeparator = hasNoSeparator
        if (hasNoSeparator) {
            separator = ""
        }
    }

    /**
     * @param iOSStyleEnable true:开启仿iOS风格编辑框模式
     */
    fun setiOSStyleEnable(iOSStyleEnable: Boolean) {
        this.iOSStyleEnable = iOSStyleEnable
        initiOSObjects()
        invalidate()
    }

    /**
     * 设置OnTextChangeListener，同EditText.addOnTextChangeListener()
     */
    fun setOnTextChangeListener(listener: OnTextChangeListener?) {
        mTextChangeListener = listener
    }

    /**
     * 设置OnMarkerClickListener，Marker被点击的监听
     */
    fun setOnMarkerClickListener(markerClickListener: OnMarkerClickListener?) {
        mMarkerClickListener = markerClickListener
    }

    private inner class MyTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            preLength = s.length
            mTextChangeListener?.beforeTextChanged(s, start, count, after)
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            currLength = s.length
            if (hasNoSeparator) {
                maxLength = currLength
            }
            markerFocusChangeLogic()
            if (currLength > maxLength) {
                text?.delete(currLength - 1, currLength)
                return
            }
            for (i in pattern.indices) {
                if (currLength == intervals.get(i)) {
                    if (currLength > preLength) { // 正在输入
                        if (currLength < maxLength) {
                            removeTextChangedListener(mTextWatcher)
                            mTextWatcher = null
                            text?.insert(currLength, separator)
                        }
                    } else if (preLength <= maxLength) { // 正在删除
                        removeTextChangedListener(mTextWatcher)
                        mTextWatcher = null
                        text?.delete(currLength - 1, currLength)
                    }
                    if (mTextWatcher == null) {
                        mTextWatcher = MyTextWatcher()
                        addTextChangedListener(mTextWatcher)
                    }
                    break
                }
            }
            mTextChangeListener?.onTextChanged(s, start, before, count)
        }

        override fun afterTextChanged(s: Editable) {
            mTextChangeListener?.afterTextChanged(s)
        }
    }

    private fun markerFocusChangeLogic() {
        if (!hasFocused) {
            setCompoundDrawables(
                compoundDrawables[0],
                compoundDrawables[1],
                null,
                compoundDrawables[3]
            )
            return
        }
        var drawable: Drawable? = null
        when (mShowMarkerTime) {
            ShowMarkerTime.ALWAYS -> drawable = mRightMarkerDrawable
            ShowMarkerTime.BEFORE_INPUT -> if (currLength == 0) {
                drawable = mRightMarkerDrawable
            }
            ShowMarkerTime.AFTER_INPUT -> if (currLength > 0) {
                drawable = mRightMarkerDrawable
            }
            else -> {
            }
        }
        setCompoundDrawables(
            compoundDrawables[0],
            compoundDrawables[1],
            drawable,
            compoundDrawables[3]
        )
    }

    private fun iOSFocusChangeLogic() {
        if (!iOSStyleEnable) {
            return
        }
        if (hasFocused) {
            if (mLeftDrawable != null) {
                setCompoundDrawables(
                    mLeftDrawable, compoundDrawables[1],
                    compoundDrawables[2], compoundDrawables[3]
                )
            }
            if (mHintCharSeq != null) {
                hint = mHintCharSeq
            }
            iOSFrameHide = true
            invalidate()
        } else {
            if (currLength == 0) { // 编辑框无内容恢复居中状态
                initiOSObjects()
                invalidate()
            }
        }
    }

    interface OnTextChangeListener {
        fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int)

        fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)

        fun afterTextChanged(s: Editable?)
    }

    interface OnMarkerClickListener {
        /**
         * @param x 被点击点相对于屏幕的x坐标
         * @param y 被点击点相对于屏幕的y坐标
         */
        fun onMarkerClick(x: Float, y: Float)
    }

    enum class ShowMarkerTime {
        BEFORE_INPUT, AFTER_INPUT, ALWAYS
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ExtendedEditText, 0, 0)
        separator = a.getString(R.styleable.ExtendedEditText_separator)
        if (separator == null) {
            separator = SPACE
        }
        customizeMarkerEnable =
            a.getBoolean(R.styleable.ExtendedEditText_customizeMarkerEnable, false)
        when (a.getInt(R.styleable.ExtendedEditText_showMarkerTime, 0)) {
            0 -> mShowMarkerTime = ShowMarkerTime.AFTER_INPUT
            1 -> mShowMarkerTime = ShowMarkerTime.BEFORE_INPUT
            2 -> mShowMarkerTime = ShowMarkerTime.ALWAYS
        }
        iOSStyleEnable = a.getBoolean(R.styleable.ExtendedEditText_iOSStyleEnable, false)
        a.recycle()
        init()
    }
}