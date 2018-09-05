package com.gcssloop.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.gcssloop.arcseekbar.R;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * 作用: 圆弧形 SeekBar
 * 作者: GcsSloop
 * 摘要: 该 SeekBar 实现的核心原理非常简单,其绘制内容实际上为一个圆弧,只用了一条 Path,
 * 尽管核心原理简单,但仍有以下细节需要进行注意:
 * 1. 画布旋转导致的坐标系问题.
 * -  为了让开口方向可控,并且实现起来简单,进行了画布旋转,因此实际显示的坐标系下的坐标和绘制坐标系坐标是不同的,尤其需要注意.
 * 2.当前进度确定问题
 * -  进度是由角度确定的, 先根据当前点击位置和中心点连线与水平线的夹角表示当前角度,
 * -  根据该角度和实际圆弧的角度相关信息推算出当前进度百分比.
 * -  根据百分比和最大数值确定当前实际的进度.
 * 3. 拖动与点击
 * -  为了防止误触, 只有手指按下位置在拖动按钮附近时才可以执行拖动.
 * -  但是用户单击时,可以可以直接跳转进度, 当然,只有点击在圆弧进度条的区域内才允许设置新的进度.
 * -  如何判断是否点击在圆弧区域内,使用了 Region 的相关方法,该 Region 区域是从 Paint 的 getFillPath 得到的.
 * 4. 进度回调去重
 * -  用户拖动时,判断是否和上次进度相同,如果相同,则不发送回调.
 * 5. 防止突变
 * -  由于进度条时圆弧形状的,因此进度可能会从 0.0 直接突变到 1.0 或者相反,因此在计算进度与当前进度差异过大时,禁止改变当前进度.
 */
public class ArcSeekBar extends View {
    private static final int DEFAULT_EDGE_LENGTH = 260;              // 默认宽高

    private static final float CIRCLE_ANGLE = 360;                  // 圆周角
    private static final int DEFAULT_ARC_WIDTH = 40;                // 默认宽度 dp
    private static final float DEFAULT_OPEN_ANGLE = 120;            // 开口角度
    private static final float DEFAULT_ROTATE_ANGLE = 90;           // 旋转角度
    private static final int DEFAULT_BORDER_WIDTH = 0;              // 默认描边宽度
    private static final int DEFAULT_BORDER_COLOR = 0xffffffff;     // 默认描边颜色

    private static final int DEFAULT_THUMB_COLOR = 0xffffffff;      // 拖动按钮颜色
    private static final int DEFAULT_THUMB_WIDTH = 2;               // 拖动按钮描边宽度 dp
    private static final int DEFAULT_THUMB_RADIUS = 15;             // 拖动按钮半径 dp

    private static final int DEFAULT_SHADOW_RADIUS = 0;             // 默认阴影半径 dp

    private static final int THUMB_MODE_STROKE = 0;                 // 拖动按钮模式 - 描边
    private static final int THUMB_MODE_FILL = 1;                   // 拖动按钮模式 - 填充
    private static final int THUMB_MODE_FILL_STROKE = 2;            // 拖动按钮模式 - 填充+描边

    private static final int DEFAULT_MAX_VALUE = 100;               // 默认最大数值

    private static final String KEY_PROGRESS_PRESENT = "PRESENT";   // 用于存储和获取当前百分比

    // 可配置数据
    private int[] mArcColors;       // Seek 颜色
    private float mArcWidth;        // Seek 宽度
    private float mOpenAngle;       // 开口的角度大小 0 - 360
    private float mRotateAngle;     // 旋转角度
    private int mBorderWidth;       // 描边宽度
    private int mBorderColor;       // 描边颜色

    private int mThumbColor;        // 拖动按钮颜色
    private float mThumbWidth;      // 拖动按钮宽度
    private float mThumbRadius;     // 拖动按钮半径
    private int mThumbMode;         // 拖动按钮模式

    private int mShadowRadius;      // 阴影半径

    private int mMaxValue;          // 最大数值

    private float mCenterX;         // 圆弧 SeekBar 中心点 X
    private float mCenterY;         // 圆弧 SeekBar 中心点 Y

    private float mThumbX;         // 拖动按钮 中心点 X
    private float mThumbY;         // 拖动按钮 中心点 Y

    private Path mSeekPath;
    private Path mBorderPath;
    private Paint mArcPaint;
    private Paint mThumbPaint;
    private Paint mBorderPaint;
    private Paint mShadowPaint;

    private float[] mTempPos;
    private float[] mTempTan;
    private PathMeasure mSeekPathMeasure;

    private float mProgressPresent = 0;         // 当前进度百分比
    private boolean mCanDrag = false;           // 是否允许拖动
    private boolean mAllowTouchSkip = false;    // 是否允许越过边界
    private GestureDetector mDetector;
    private Matrix mInvertMatrix;               // 逆向 Matrix, 用于计算触摸坐标和绘制坐标的转换
    private Region mArcRegion;                  // ArcPath的实际区域大小,用于判定单击事件


    public ArcSeekBar(Context context) {
        this(context, null);
    }

    public ArcSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSaveEnabled(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        initAttrs(context, attrs);
        initData();
        initPaint();
    }

    //--- 初始化 -----------------------------------------------------------------------------------

    // 初始化各种属性
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ArcSeekBar);
        mArcColors = getArcColors(context, ta);
        mArcWidth = ta.getDimensionPixelSize(R.styleable.ArcSeekBar_arc_width, dp2px(DEFAULT_ARC_WIDTH));
        mOpenAngle = ta.getFloat(R.styleable.ArcSeekBar_arc_open_angle, DEFAULT_OPEN_ANGLE);
        mRotateAngle = ta.getFloat(R.styleable.ArcSeekBar_arc_rotate_angle, DEFAULT_ROTATE_ANGLE);
        mMaxValue = ta.getInt(R.styleable.ArcSeekBar_arc_max, DEFAULT_MAX_VALUE);
        int progress = ta.getInt(R.styleable.ArcSeekBar_arc_progress, 0);
        setProgress(progress);
        mBorderWidth = ta.getDimensionPixelSize(R.styleable.ArcSeekBar_arc_border_width, dp2px(DEFAULT_BORDER_WIDTH));
        mBorderColor = ta.getColor(R.styleable.ArcSeekBar_arc_border_color, DEFAULT_BORDER_COLOR);

        mThumbColor = ta.getColor(R.styleable.ArcSeekBar_arc_thumb_color, DEFAULT_THUMB_COLOR);
        mThumbRadius = ta.getDimensionPixelSize(R.styleable.ArcSeekBar_arc_thumb_radius, dp2px(DEFAULT_THUMB_RADIUS));
        mThumbWidth = ta.getDimensionPixelSize(R.styleable.ArcSeekBar_arc_thumb_width, dp2px(DEFAULT_THUMB_WIDTH));
        mThumbMode = ta.getInt(R.styleable.ArcSeekBar_arc_thumb_mode, THUMB_MODE_STROKE);

        mShadowRadius = ta.getDimensionPixelSize(R.styleable.ArcSeekBar_arc_shadow_radius, dp2px(DEFAULT_SHADOW_RADIUS));
        ta.recycle();
    }

    // 获取 Arc 颜色数组
    private int[] getArcColors(Context context, TypedArray ta) {
        int[] ret;
        int resId = ta.getResourceId(R.styleable.ArcSeekBar_arc_colors, 0);
        if (0 == resId) {
            resId = R.array.arc_colors_default;
        }
        TypedArray colorArray = context.getResources().obtainTypedArray(resId);
        ret = new int[colorArray.length()];
        for (int i = 0; i < colorArray.length(); i++) {
            ret[i] = colorArray.getColor(i, 0);
        }
        return ret;
    }

    // 初始化数据
    private void initData() {
        mSeekPath = new Path();
        mBorderPath = new Path();
        mSeekPathMeasure = new PathMeasure();
        mTempPos = new float[2];
        mTempTan = new float[2];

        mDetector = new GestureDetector(getContext(), new OnClickListener());
        mInvertMatrix = new Matrix();
        mArcRegion = new Region();
    }

    // 初始化画笔
    private void initPaint() {
        initArcPaint();
        initThumbPaint();
        initBorderPaint();
        initShadowPaint();
    }

    // 初始化圆弧画笔
    private void initArcPaint() {
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    // 初始化拖动按钮画笔
    private void initThumbPaint() {
        mThumbPaint = new Paint();
        mThumbPaint.setAntiAlias(true);
        mThumbPaint.setColor(mThumbColor);
        mThumbPaint.setStrokeWidth(mThumbWidth);
        mThumbPaint.setStrokeCap(Paint.Cap.ROUND);
        if (mThumbMode == THUMB_MODE_FILL) {
            mThumbPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else if (mThumbMode == THUMB_MODE_FILL_STROKE) {
            mThumbPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            mThumbPaint.setStyle(Paint.Style.STROKE);
        }
        mThumbPaint.setTextSize(56);
    }

    // 初始化拖动按钮画笔
    private void initBorderPaint() {
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setStyle(Paint.Style.STROKE);
    }

    // 初始化阴影画笔
    private void initShadowPaint() {
        mShadowPaint = new Paint();
        mShadowPaint.setAntiAlias(true);
        mShadowPaint.setStrokeWidth(mBorderWidth);
        mShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    //--- 初始化结束 -------------------------------------------------------------------------------

    //--- 状态存储 ---------------------------------------------------------------------------------

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putFloat(KEY_PROGRESS_PRESENT, mProgressPresent);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            this.mProgressPresent = bundle.getFloat(KEY_PROGRESS_PRESENT);
            state = bundle.getParcelable("superState");
        }
        if (null != mOnProgressChangeListener) {
            mOnProgressChangeListener.onProgressChanged(this, getProgress(), false);
        }
        super.onRestoreInstanceState(state);
    }

    //--- 状态存储结束 -----------------------------------------------------------------------------

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ws = MeasureSpec.getSize(widthMeasureSpec);     //取出宽度的确切数值
        int wm = MeasureSpec.getMode(widthMeasureSpec);     //取出宽度的测量模式
        int hs = MeasureSpec.getSize(heightMeasureSpec);    //取出高度的确切数值
        int hm = MeasureSpec.getMode(heightMeasureSpec);    //取出高度的测量模

        if (wm == MeasureSpec.UNSPECIFIED) {
            wm = MeasureSpec.EXACTLY;
            ws = dp2px(DEFAULT_EDGE_LENGTH);
        } else if (wm == MeasureSpec.AT_MOST) {
            wm = MeasureSpec.EXACTLY;
            ws = Math.min(dp2px(DEFAULT_EDGE_LENGTH), ws);
        }
        if (hm == MeasureSpec.UNSPECIFIED) {
            hm = MeasureSpec.EXACTLY;
            hs = dp2px(DEFAULT_EDGE_LENGTH);
        } else if (hm == MeasureSpec.AT_MOST) {
            hm = MeasureSpec.EXACTLY;
            hs = Math.min(dp2px(DEFAULT_EDGE_LENGTH), hs);
        }
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(ws, wm), MeasureSpec.makeMeasureSpec(hs, hm));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 计算在当前大小下,内容应该显示的大小和起始位置
        int safeW = w - getPaddingLeft() - getPaddingRight();
        int safeH = h - getPaddingTop() - getPaddingBottom();
        float edgeLength, startX, startY;
        float fix = mArcWidth / 2 + mBorderWidth + mShadowRadius * 2;  // 修正距离,画笔宽度的修正
        if (safeW < safeH) {
            // 宽度小于高度,以宽度为准
            edgeLength = safeW - fix;
            startX = getPaddingLeft();
            startY = (safeH - safeW) / 2.0f + getPaddingTop();
        } else {
            // 宽度大于高度,以高度为准
            edgeLength = safeH - fix;
            startX = (safeW - safeH) / 2.0f + getPaddingLeft();
            startY = getPaddingTop();
        }

        // 得到显示区域和中心的
        RectF content = new RectF(startX + fix, startY + fix, startX + edgeLength, startY + edgeLength);
        mCenterX = content.centerX();
        mCenterY = content.centerY();

        // 得到路径
        mSeekPath.reset();
        mSeekPath.addArc(content, mOpenAngle / 2, CIRCLE_ANGLE - mOpenAngle);
        mSeekPathMeasure.setPath(mSeekPath, false);
        computeThumbPos(mProgressPresent);

        // 计算渐变数组
        float startPos = (mOpenAngle / 2) / CIRCLE_ANGLE;
        float stopPos = (CIRCLE_ANGLE - (mOpenAngle / 2)) / CIRCLE_ANGLE;
        int len = mArcColors.length - 1;
        float distance = (stopPos - startPos) / len;
        float pos[] = new float[mArcColors.length];
        for (int i = 0; i < mArcColors.length; i++) {
            pos[i] = startPos + (distance * i);
        }
        SweepGradient gradient = new SweepGradient(content.centerX(), content.centerY(), mArcColors, pos);
        mArcPaint.setShader(gradient);

        mInvertMatrix.reset();
        mInvertMatrix.preRotate(-mRotateAngle, mCenterX, mCenterY);

        mArcPaint.getFillPath(mSeekPath, mBorderPath);
        mBorderPath.close();
        mArcRegion.setPath(mBorderPath, new Region(0, 0, w, h));
    }

    // 具体绘制
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.rotate(mRotateAngle, mCenterX, mCenterY);
        mShadowPaint.setShadowLayer(mShadowRadius * 2, 0, 0, getColor());
        canvas.drawPath(mBorderPath, mShadowPaint);
        canvas.drawPath(mSeekPath, mArcPaint);
        if (mBorderWidth > 0) {
            canvas.drawPath(mBorderPath, mBorderPaint);
        }
        canvas.drawCircle(mThumbX, mThumbY, mThumbRadius, mThumbPaint);
        canvas.restore();
    }

    private boolean moved = false;
    private int lastProgress = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getActionMasked();
        switch (action) {
            case ACTION_DOWN:
                moved = false;
                judgeCanDrag(event);
                if (null != mOnProgressChangeListener) {
                    mOnProgressChangeListener.onStartTrackingTouch(this);
                }
                break;
            case ACTION_MOVE:
                if (!mCanDrag) {
                    break;
                }
                float tempProgressPresent = getCurrentProgress(event.getX(), event.getY());
                if (!mAllowTouchSkip) {
                    // 不允许突变
                    if (Math.abs(tempProgressPresent - mProgressPresent) > 0.5f) {
                        break;
                    }
                }
                // 允许突变 或者非突变
                mProgressPresent = tempProgressPresent;
                computeThumbPos(mProgressPresent);
                // 事件回调
                if (null != mOnProgressChangeListener && getProgress() != lastProgress) {
                    mOnProgressChangeListener.onProgressChanged(this, getProgress(), true);
                    lastProgress = getProgress();
                }
                moved = true;
                break;
            case ACTION_UP:
            case ACTION_CANCEL:
                if (null != mOnProgressChangeListener && moved) {
                    mOnProgressChangeListener.onStopTrackingTouch(this);
                }
                break;
        }
        mDetector.onTouchEvent(event);
        invalidate();
        return true;
    }

    // 判断是否允许拖动
    private void judgeCanDrag(MotionEvent event) {
        float[] pos = {event.getX(), event.getY()};
        mInvertMatrix.mapPoints(pos);
        if (getDistance(pos[0], pos[1]) <= mThumbRadius * 1.5) {
            mCanDrag = true;
        } else {
            mCanDrag = false;
        }
    }

    private class OnClickListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // 判断是否点击在了进度区域
            if (!isInArcProgress(e.getX(), e.getY())) return false;
            // 点击允许突变
            mProgressPresent = getCurrentProgress(e.getX(), e.getY());
            computeThumbPos(mProgressPresent);
            // 事件回调
            if (null != mOnProgressChangeListener) {
                mOnProgressChangeListener.onProgressChanged(ArcSeekBar.this, getProgress(), true);
                mOnProgressChangeListener.onStopTrackingTouch(ArcSeekBar.this);
            }
            return true;
        }
    }

    // 判断该点是否在进度条上面
    private boolean isInArcProgress(float px, float py) {
        float[] pos = {px, py};
        mInvertMatrix.mapPoints(pos);
        return mArcRegion.contains((int) pos[0], (int) pos[1]);
    }

    // 获取当前进度理论进度数值
    private float getCurrentProgress(float px, float py) {
        float diffAngle = getDiffAngle(px, py);
        float progress = diffAngle / (CIRCLE_ANGLE - mOpenAngle);
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        return progress;
    }

    // 获得当前点击位置所成角度与开始角度之间的数值差
    private float getDiffAngle(float px, float py) {
        float angle = getAngle(px, py);
        float diffAngle;
        diffAngle = angle - mRotateAngle;
        if (diffAngle < 0) {
            diffAngle = (diffAngle + CIRCLE_ANGLE) % CIRCLE_ANGLE;
        }
        diffAngle = diffAngle - mOpenAngle / 2;
        return diffAngle;
    }

    // 计算指定位置与内容区域中心点的夹角
    private float getAngle(float px, float py) {
        float angle = (float) ((Math.atan2(py - mCenterY, px - mCenterX)) * 180 / 3.14f);
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    // 计算指定位置与上次位置的距离
    private float getDistance(float px, float py) {
        return (float) Math.sqrt((px - mThumbX) * (px - mThumbX) + (py - mThumbY) * (py - mThumbY));
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    // 计算拖动快应该显示的位置
    private void computeThumbPos(float present) {
        if (present < 0) present = 0;
        if (present > 1) present = 1;
        float distance = mSeekPathMeasure.getLength() * present;
        mSeekPathMeasure.getPosTan(distance, mTempPos, mTempTan);
        mThumbX = mTempPos[0];
        mThumbY = mTempPos[1];
    }

    //--- 线性取色 ---------------------------------------------------------------------------------

    /**
     * 获取当前进度的具体颜色
     *
     * @return 当前进度在渐变中的颜色
     */
    public int getColor() {
        return getColor(mProgressPresent);
    }

    /**
     * 获取某个百分比位置的颜色
     *
     * @param radio 取值[0,1]
     * @return 最终颜色
     */
    private int getColor(float radio) {
        float diatance = 1.0f / (mArcColors.length - 1);
        int startColor;
        int endColor;
        if (radio >= 1) {
            return mArcColors[mArcColors.length - 1];
        }
        for (int i = 0; i < mArcColors.length; i++) {
            if (radio <= i * diatance) {
                if (i == 0) {
                    return mArcColors[0];
                }
                startColor = mArcColors[i - 1];
                endColor = mArcColors[i];
                float areaRadio = getAreaRadio(radio, diatance * (i - 1), diatance * i);
                return getColorFrom(startColor, endColor, areaRadio);
            }
        }
        return -1;
    }

    /**
     * 计算当前比例在子区间的比例
     *
     * @param radio         总比例
     * @param startPosition 子区间开始位置
     * @param endPosition   子区间结束位置
     * @return 自区间比例[0, 1]
     */
    private float getAreaRadio(float radio, float startPosition, float endPosition) {
        return (radio - startPosition) / (endPosition - startPosition);
    }

    /**
     * 取两个颜色间的渐变区间 中的某一点的颜色
     *
     * @param startColor 开始的颜色
     * @param endColor   结束的颜色
     * @param radio      比例 [0, 1]
     * @return 选中点的颜色
     */
    private int getColorFrom(int startColor, int endColor, float radio) {
        int redStart = Color.red(startColor);
        int blueStart = Color.blue(startColor);
        int greenStart = Color.green(startColor);
        int redEnd = Color.red(endColor);
        int blueEnd = Color.blue(endColor);
        int greenEnd = Color.green(endColor);

        int red = (int) (redStart + ((redEnd - redStart) * radio + 0.5));
        int greed = (int) (greenStart + ((greenEnd - greenStart) * radio + 0.5));
        int blue = (int) (blueStart + ((blueEnd - blueStart) * radio + 0.5));
        return Color.argb(255, red, greed, blue);
    }


    //--- 对外接口 ---------------------------------------------------------------------------------

    /**
     * 设置进度
     *
     * @param progress 进度值
     */
    public void setProgress(int progress) {
        System.out.println("setProgress = " + progress);
        if (progress < 0) progress = 0;
        if (progress > mMaxValue) progress = mMaxValue;
        mProgressPresent = progress * 1.0f / mMaxValue;
        System.out.println("setProgress = " + mProgressPresent);
        if (null != mOnProgressChangeListener) {
            mOnProgressChangeListener.onProgressChanged(this, progress, false);
        }
        postInvalidate();
    }

    /**
     * 获取当前进度数值
     *
     * @return 当前进度数值
     */
    public int getProgress() {
        return (int) (mProgressPresent * mMaxValue);
    }

    OnProgressChangeListener mOnProgressChangeListener;

    public void setOnProgressChangeListener(OnProgressChangeListener onProgressChangeListener) {
        mOnProgressChangeListener = onProgressChangeListener;
    }

    public interface OnProgressChangeListener {
        /**
         * 进度发生变化
         *
         * @param seekBar  拖动条
         * @param progress 当前进度数值
         * @param isUser   是否是用户操作, true 表示用户拖动, false 表示通过代码设置
         */
        void onProgressChanged(ArcSeekBar seekBar, int progress, boolean isUser);

        /**
         * 用户开始拖动
         *
         * @param seekBar 拖动条
         */
        void onStartTrackingTouch(ArcSeekBar seekBar);

        /**
         * 用户结束拖动
         *
         * @param seekBar 拖动条
         */
        void onStopTrackingTouch(ArcSeekBar seekBar);
    }
}
