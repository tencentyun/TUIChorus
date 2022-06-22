package com.tencent.liteav.tuichorus.ui.lrc.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.AttributeSet;
import android.view.View;

import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.lrc.LyricsReader;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsInfo;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsLineInfo;
import com.tencent.liteav.tuichorus.ui.lrc.utils.LyricsUtils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.TreeMap;

public abstract class AbstractLrcView extends View {
    /**
     * 初始
     */
    public static final int LRCSTATUS_INIT         = 0;
    /**
     * 加载中
     */
    public static final int LRCSTATUS_LOADING      = 1;
    /**
     * 绘画歌词
     */
    public static final int LRCSTATUS_LRC          = 4;
    /**
     * 绘画歌词出错
     */
    public static final int LRCSTATUS_ERROR        = 5;
    /**
     * 不支持格式
     */
    public static final int LRCSTATUS_NONSUPPORT   = 6;
    /**
     * 没有的额外的歌词
     */
    public static final int EXTRALRCTYPE_NOLRC     = 0;
    /**
     * 初始
     */
    public static final int LRCPLAYERSTATUS_INIT   = 0;
    /**
     * 播放
     */
    public static final int LRCPLAYERSTATUS_PLAY   = 1;
    /**
     * seekto
     */
    public static final int LRCPLAYERSTATUS_SEEKTO = 2;

    /**
     * 默认歌词画笔
     */
    private Paint mPaint;
    /**
     * 默认画笔颜色
     */
    private int[] mPaintColors   = new int[]{
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#FFFFFF")
    };
    /**
     * 高亮歌词画笔
     */
    private Paint mPaintHL;
    //高亮颜色
    private int[] mPaintHLColors = new int[]{
            Color.parseColor("#FF8607"),
            Color.parseColor("#FF8607")
    };
    /**
     * 轮廓画笔
     */
    private Paint mPaintOutline;

    /**
     * 额外歌词画笔
     */
    private Paint mExtraLrcPaint;
    /**
     * 额外歌词高亮画笔
     */
    private Paint mExtraLrcPaintHL;
    /**
     * 轮廓画笔
     */
    private Paint mExtraLrcPaintOutline;

    /**
     * 正在加载提示文本
     */
    private String mLoadingText;
    /**
     * 加载歌词出错
     */
    private String mLoadErrorText;
    /**
     * 不支持歌词格式文本
     */
    private String mNonsupportText;
    /**
     * 搜索提示文本
     */
    private String mGotoSearchText;

    /**
     * 搜索歌词区域
     */
    private RectF   mGotoSearchBtnRect;
    /**
     * 是否在去搜索歌词矩形区域内
     */
    private boolean mIsInGotoSearchBtnRect = false;

    /**
     * 绘画去搜索歌词文字矩形画笔
     */
    private Paint mGotoSearchRectPaint;

    /**
     * 绘画去搜索歌词文字画笔
     */
    private Paint                mGotoSearchTextPaint;
    /**
     * 歌词状态
     */
    private int                  mLrcStatus = LRCSTATUS_INIT;
    /**
     * 搜索歌词回调
     */
    private SearchLyricsListener mSearchLyricsListener;

    /**
     * 只显示默认歌词
     */
    public static final int EXTRALRCSTATUS_NOSHOWEXTRALRC = 2;

    /**
     * 空行高度
     */
    private float mSpaceLineHeight = 60;
    /**
     * 歌词字体大小
     */
    private float mFontSize        = 54;

    /**
     * 左右间隔距离
     */
    private float mPaddingLeftOrRight = 15;

    /**
     * 歌词的最大宽度
     */
    private float mTextMaxWidth         = 0;
    /**
     * 当前歌词的所在行数
     */
    private int   mLyricsLineNum        = 0;
    /**
     * 分割歌词的行索引
     */
    private int   mSplitLyricsLineNum   = 0;
    /**
     * 当前歌词的第几个字
     */
    private int   mLyricsWordIndex      = -1;
    /**
     * 分割歌词当前歌词的第几个字
     */
    private int   mSplitLyricsWordIndex = -1;

    /**
     * 当前歌词第几个字 已经播放的时间
     */
    private float mLyricsWordHLTime = 0;

    /**
     * 额外的歌词类型
     */
    private int                 mExtraLrcType            = EXTRALRCTYPE_NOLRC;
    /**
     * 额外歌词监听事件
     */
    private ExtraLyricsListener mExtraLyricsListener;
    /**
     * 额外歌词空行高度
     */
    private float               mExtraLrcSpaceLineHeight = 30;
    /**
     * 额外歌词字体大小
     */
    private float               mExtraLrcFontSize        = 30;

    /**
     * 当前额外分割歌词的所在行数
     */
    private             int mExtraSplitLyricsLineNum   = 0;
    /**
     * 当前额外歌词的第几个字
     */
    private             int mExtraLyricsWordIndex      = -1;
    /**
     * 当前额外分割歌词的第几个字
     */
    private             int mExtraSplitLyricsWordIndex = -1;
    /**
     * 绘画类型：lrc类型
     */
    public static final int TRANSLATE_DRAW_TYPE_LRC    = 0;

    /**
     * 翻译歌词绘画类型
     */
    private int mTranslateDrawType = TRANSLATE_DRAW_TYPE_LRC;

    /**
     * 翻译歌词的高亮宽度
     */
    private float mTranslateLyricsWordHLTime = 0;

    ////////////////////////////////////////////////////////////////////

    /**
     * 歌词处理类
     */
    private LyricsReader mLyricsReader;

    /**
     * 歌词列表
     */
    private TreeMap<Integer, LyricsLineInfo> mLrcLineInfos;
    /**
     * 翻译行歌词列表
     */
    private List<LyricsLineInfo>             mTranslateLrcLineInfos;
    /**
     * 音译歌词行
     */
    private List<LyricsLineInfo>             mTransliterationLrcLineInfos;

    ///////////////////////////////歌词绘画播放器//////////////////////////////////

    private byte[] mLock            = new byte[0];
    /**
     * 播放器类型
     */
    private int    mLrcPlayerStatus = LRCPLAYERSTATUS_INIT;

    /**
     * 播放器开始时间，用于计算歌曲播放的时长
     */
    private long mPlayerStartTime = 0;
    /**
     * 播放器开始后，所经历的播放时长
     */
    private long mPlayerSpendTime = 0;

    /**
     * 当前播放进度
     */
    private long mCurPlayingTime = 0;
    /**
     * 刷新延时时间
     */
    private long mRefreshTime    = 30;

    /**
     * 子线程用于执行耗时任务
     */
    private Handler       mWorkerHandler;
    //创建异步HandlerThread
    private HandlerThread mHandlerThread;
    /**
     * 处理ui任务
     */
    private Handler       mUIHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Context context = mActivityWR.get();
            if (context != null) {
                synchronized (mLock) {
                    if (mLrcPlayerStatus == LRCPLAYERSTATUS_PLAY && mLyricsReader != null) {
                        invalidateView();
                        long endTime    = System.currentTimeMillis();
                        long updateTime = (endTime - mPlayerStartTime) - mPlayerSpendTime;
                        mPlayerSpendTime = (endTime - mPlayerStartTime);
                        long delayMs = mRefreshTime - updateTime;
                        mWorkerHandler.sendEmptyMessageDelayed(0, Math.max(0, delayMs));
                    }
                }
            }
        }
    };

    private WeakReference<Context> mActivityWR;

    public AbstractLrcView(Context context) {
        super(context);
        init(context);
    }

    public AbstractLrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

        //初始默认数据
        mLoadingText = context.getString(R.string.tuichorus_room_id);
        mLoadErrorText = context.getString(R.string.tuichorus_room_id);
        mNonsupportText = context.getString(R.string.tuichorus_room_id);
        mGotoSearchText = context.getString(R.string.tuichorus_room_id);

        //默认画笔
        mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mFontSize);

        //高亮画笔
        mPaintHL = new Paint();
        mPaintHL.setDither(true);
        mPaintHL.setAntiAlias(true);
        mPaintHL.setTextSize(mFontSize);

        //轮廓画笔
        mPaintOutline = new Paint();
        mPaintOutline.setDither(true);
        mPaintOutline.setAntiAlias(true);
        mPaintOutline.setColor(Color.BLACK);
        mPaintOutline.setTextSize(mFontSize);

        //额外歌词画笔
        mExtraLrcPaint = new Paint();
        mExtraLrcPaint.setDither(true);
        mExtraLrcPaint.setAntiAlias(true);
        mExtraLrcPaint.setTextSize(mExtraLrcFontSize);

        //额外高亮歌词画笔
        mExtraLrcPaintHL = new Paint();
        mExtraLrcPaintHL.setDither(true);
        mExtraLrcPaintHL.setAntiAlias(true);
        mExtraLrcPaintHL.setTextSize(mExtraLrcFontSize);

        //额外画笔轮廓
        mExtraLrcPaintOutline = new Paint();
        mExtraLrcPaintOutline.setDither(true);
        mExtraLrcPaintOutline.setAntiAlias(true);
        mExtraLrcPaintOutline.setColor(Color.BLACK);
        mExtraLrcPaintOutline.setTextSize(mExtraLrcFontSize);

        //绘画去搜索歌词画笔
        mGotoSearchTextPaint = new Paint();
        mGotoSearchTextPaint.setDither(true);
        mGotoSearchTextPaint.setAntiAlias(true);
        mGotoSearchTextPaint.setTextSize(mFontSize);

        //绘画去搜索歌词矩形画笔
        mGotoSearchRectPaint = new Paint();
        mGotoSearchRectPaint.setDither(true);
        mGotoSearchRectPaint.setAntiAlias(true);
        mGotoSearchRectPaint.setStrokeWidth(2);
        mGotoSearchRectPaint.setTextSize(mFontSize);

        //
        mActivityWR = new WeakReference<Context>(context);
        //创建异步HandlerThread
        mHandlerThread = new HandlerThread("updateLrcData", Process.THREAD_PRIORITY_BACKGROUND);
        //必须先开启线程
        mHandlerThread.start();
        //子线程Handler
        mWorkerHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Context context = mActivityWR.get();
                if (context != null) {
                    synchronized (mLock) {
                        if (mLyricsReader != null) {
                            updateView(mCurPlayingTime + mPlayerSpendTime);
                            if (mLrcPlayerStatus == LRCPLAYERSTATUS_PLAY) {
                                mUIHandler.sendEmptyMessage(0);
                            } else if (mLrcPlayerStatus == LRCPLAYERSTATUS_SEEKTO) {
                                invalidateView();
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        onDrawView(canvas);
    }

    /**
     * 绘画视图
     *
     * @param canvas
     */
    private void onDrawView(Canvas canvas) {
        synchronized (mLock) {
            mPaint.setAlpha(255);
            mPaintHL.setAlpha(255);
            mExtraLrcPaint.setAlpha(255);
            mExtraLrcPaintHL.setAlpha(255);
            if (mLrcStatus == LRCSTATUS_LOADING || mLrcStatus == LRCSTATUS_ERROR || mLrcStatus == LRCSTATUS_NONSUPPORT) {
                //绘画加载中文本
//                String text = "";
//                if (mLrcStatus == LRCSTATUS_LOADING) {
//                    text = getLoadingText();
//                } else if (mLrcStatus == LRCSTATUS_ERROR) {
//                    text = getLoadErrorText();
//                } else if (mLrcStatus == LRCSTATUS_NONSUPPORT) {
//                    text = getNonsupportText();
//                }
//                float textWidth  = LyricsUtils.getTextWidth(mPaint, text);
//                int   textHeight = LyricsUtils.getTextHeight(mPaint);
//                float x          = (getWidth() - textWidth) / 2;
//                float y          = (getHeight() + textHeight) / 2;
//                LyricsUtils.drawOutline(canvas, mPaintOutline, text, x, y);
//                LyricsUtils.drawText(canvas, mPaint, mPaintColors, text, x, y);
            } else if (mLrcStatus == LRCSTATUS_LRC) {
                onDrawLrcView(canvas);
            }
        }
    }

    public synchronized void invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            //  当前线程是主UI线程，直接刷新。
            invalidate();
        } else {
            //  当前线程是非UI线程，post刷新。
            postInvalidate();
        }
    }

    /**
     * view的draw歌词调用方法
     *
     * @param canvas
     * @return
     */
    protected abstract void onDrawLrcView(Canvas canvas);

    /**
     * 更新视图
     *
     * @param playProgress
     */
    protected abstract void updateView(long playProgress);

    /**
     * 获取歌词状态
     *
     * @return
     */
    public int getLrcStatus() {
        return mLrcStatus;
    }

    /**
     * 初始歌词数据
     */
    public void initLrcData() {
        synchronized (mLock) {
            mLyricsReader = null;
            mLrcStatus = LRCSTATUS_INIT;
            resetData();
            invalidateView();
        }
    }

    /**
     * 设置歌词状态
     *
     * @param lrcStatus
     */
    public void setLrcStatus(int lrcStatus) {
        this.mLrcStatus = lrcStatus;
        invalidateView();
    }

    /**
     * 设置默认颜色
     *
     * @param paintColor       至少两种颜色
     * @param isInvalidateView 是否更新视图
     */
    public void setPaintColor(int[] paintColor, boolean isInvalidateView) {
        this.mPaintColors = paintColor;
        if (isInvalidateView) {
            invalidateView();
        }
    }

    /**
     * 设置高亮颜色
     *
     * @param paintHLColor     至少两种颜色
     * @param isInvalidateView 是否更新视图
     */
    public void setPaintHLColor(int[] paintHLColor, boolean isInvalidateView) {
        this.mPaintHLColors = paintHLColor;
        if (isInvalidateView) {
            invalidateView();
        }
    }

    /**
     * 设置字体文件
     *
     * @param typeFace
     * @param isInvalidateView 是否更新视图
     */
    public void setTypeFace(Typeface typeFace, boolean isInvalidateView) {

        if (typeFace != null) {
            mPaint.setTypeface(typeFace);
            mPaintHL.setTypeface(typeFace);
            mPaintOutline.setTypeface(typeFace);
            mExtraLrcPaint.setTypeface(typeFace);
            mExtraLrcPaintHL.setTypeface(typeFace);
            mExtraLrcPaintOutline.setTypeface(typeFace);
        }

        if (isInvalidateView) {
            invalidateView();
        }
    }

    /**
     * 获取分隔歌词的开始时间
     *
     * @param playProgress
     * @return
     */
    public int getSplitLineLrcStartTime(int playProgress) {
        if (mLyricsReader == null || mLrcLineInfos == null || mLrcLineInfos.size() == 0)
            return -1;
        return LyricsUtils.getSplitLineLrcStartTime(mLyricsReader.getLyricsType(), mLrcLineInfos, playProgress, mLyricsReader.getPlayOffset());

    }

    /**
     * 获取行歌词的开始时间
     *
     * @param playProgress
     * @return
     */
    public int getLineLrcStartTime(int playProgress) {
        if (mLyricsReader == null || mLrcLineInfos == null || mLrcLineInfos.size() == 0)
            return -1;
        return LyricsUtils.getLineLrcStartTime(mLyricsReader.getLyricsType(), mLrcLineInfos, playProgress, mLyricsReader.getPlayOffset());
    }

    /**
     * 获取分隔行的歌词内容
     *
     * @param playProgress
     * @return
     */
    public String getSplitLineLrc(int playProgress) {
        if (mLyricsReader == null || mLrcLineInfos == null || mLrcLineInfos.size() == 0)
            return null;
        return LyricsUtils.getSplitLineLrc(mLyricsReader.getLyricsType(), mLrcLineInfos, playProgress, mLyricsReader.getPlayOffset());
    }

    /**
     * 获取行的歌词内容
     *
     * @param playProgress
     * @return
     */
    public String getLineLrc(int playProgress) {
        if (mLyricsReader == null || mLrcLineInfos == null || mLrcLineInfos.size() == 0)
            return null;
        return LyricsUtils.getLineLrc(mLyricsReader.getLyricsType(), mLrcLineInfos, playProgress, mLyricsReader.getPlayOffset());

    }

    /**
     * 设置空行高度
     *
     * @param spaceLineHeight
     * @param isInvalidateView 是否更新视图
     */
    public void setSpaceLineHeight(float spaceLineHeight, boolean isInvalidateView) {
        this.mSpaceLineHeight = spaceLineHeight;
        if (isInvalidateView) {
            invalidateView();
        }
    }

    /**
     * 歌词播放
     *
     * @param playProgress
     */
    public void play(long playProgress) {
        synchronized (mLock) {
            if (playProgress - 400 > mCurPlayingTime) {
                seek(playProgress);
                return;
            }

            if (mLrcPlayerStatus == LRCPLAYERSTATUS_PLAY) {
                removeCallbacksAndMessages();
            }
            mLrcPlayerStatus = LRCPLAYERSTATUS_PLAY;
            this.mCurPlayingTime = playProgress;
            mPlayerStartTime = System.currentTimeMillis();
            mPlayerSpendTime = 0;
            mWorkerHandler.sendEmptyMessageDelayed(0, 0);
        }
    }

    /**
     * 歌词暂停
     */
    public void pause() {
        synchronized (mLock) {
            if (mLrcPlayerStatus == LRCPLAYERSTATUS_PLAY) {
                mLrcPlayerStatus = LRCPLAYERSTATUS_INIT;
                removeCallbacksAndMessages();
            }
            mCurPlayingTime += mPlayerSpendTime;
            mPlayerSpendTime = 0;
        }
    }

    /**
     * 快进
     *
     * @param playProgress
     */
    public void seek(long playProgress) {
        synchronized (mLock) {
            mLrcPlayerStatus = LRCPLAYERSTATUS_SEEKTO;

            this.mCurPlayingTime = playProgress;
            mPlayerStartTime = System.currentTimeMillis();
            mPlayerSpendTime = 0;
            mWorkerHandler.sendEmptyMessageDelayed(0, 0);
        }
    }

    /**
     * 唤醒
     */
    public void resume() {
        synchronized (mLock) {
            mLrcPlayerStatus = LRCPLAYERSTATUS_PLAY;
            mPlayerStartTime = System.currentTimeMillis();
            mPlayerSpendTime = 0;
            mWorkerHandler.sendEmptyMessageDelayed(0, 0);
        }
    }

    /**
     * 获取歌词播放器状态
     *
     * @return
     */
    public int getLrcPlayerStatus() {
        return mLrcPlayerStatus;
    }

    /**
     * 设置歌词解析器
     *
     * @param lyricsReader
     */
    public void setLyricsReader(LyricsReader lyricsReader) {
        synchronized (mLock) {
            this.mLyricsReader = lyricsReader;
            resetData();
            if (hasLrcLineInfos()) {
                //是否有歌词数据
                mLrcStatus = LRCSTATUS_LRC;

                updateView(mCurPlayingTime);
            }
            invalidateView();
        }
    }

    /**
     * 是否有歌词数据
     *
     * @return
     */
    public boolean hasLrcLineInfos() {
        if (mLyricsReader != null && mLyricsReader.getLrcLineInfos() != null && mLyricsReader.getLrcLineInfos().size() > 0) {
            //获取分割歌词集合
            if (mLyricsReader.getLyricsType() == LyricsInfo.DYNAMIC) {
                //默认歌词
                mLrcLineInfos = LyricsUtils.getSplitDynamicLyrics(mLyricsReader.getLrcLineInfos(), mTextMaxWidth, mPaint);
            }

            return true;
        }

        return false;
    }

    /**
     * 重置数据
     */
    private void resetData() {
        //
        mLrcPlayerStatus = LRCPLAYERSTATUS_INIT;
        removeCallbacksAndMessages();

        //player
        mCurPlayingTime = 0;
        mPlayerStartTime = 0;
        mPlayerSpendTime = 0;
        mLyricsLineNum = 0;
        mSplitLyricsLineNum = 0;
        mLyricsWordIndex = -1;
        mSplitLyricsWordIndex = -1;
        mLyricsWordHLTime = 0;

        //
        mLrcLineInfos = null;
        mTranslateLrcLineInfos = null;
        mTransliterationLrcLineInfos = null;
        mExtraSplitLyricsLineNum = 0;
        mExtraLyricsWordIndex = -1;
        mExtraSplitLyricsWordIndex = -1;
        mTranslateLyricsWordHLTime = 0;

        //无额外歌词回调
        if (mExtraLyricsListener != null) {
            mExtraLyricsListener.extraLrcCallback();
        }
    }

    /**
     * 设置字体大小
     *
     * @param fontSize
     */
    public void setFontSize(float fontSize) {
        synchronized (mLock) {
            this.mFontSize = fontSize;
            mPaint.setTextSize(mFontSize);
            mPaintHL.setTextSize(mFontSize);
            mPaintOutline.setTextSize(mFontSize);

            if (mSearchLyricsListener != null) {
                mGotoSearchRectPaint.setTextSize(mFontSize);
                mGotoSearchTextPaint.setTextSize(mFontSize);
                mGotoSearchBtnRect = null;
            }
        }
    }

    /**
     * 更新分隔后的行号，字索引，高亮时间
     *
     * @param playProgress
     */
    public void updateSplitData(long playProgress) {
        //动感歌词
        if (mLyricsReader.getLyricsType() == LyricsInfo.DYNAMIC) {
            //获取分割后的索引
            mSplitLyricsLineNum = LyricsUtils.getSplitDynamicLyricsLineNum(mLrcLineInfos, mLyricsLineNum, playProgress, mLyricsReader.getPlayOffset());
            //获取原始的歌词字索引
            mLyricsWordIndex = LyricsUtils.getLyricsWordIndex(mLrcLineInfos, mLyricsLineNum, playProgress, mLyricsReader.getPlayOffset());
            //获取分割后的歌词字索引
            mSplitLyricsWordIndex = LyricsUtils.getSplitLyricsWordIndex(mLrcLineInfos, mLyricsLineNum, playProgress, mLyricsReader.getPlayOffset());
            mLyricsWordHLTime = LyricsUtils.getDisWordsIndexLenTime(mLrcLineInfos, mLyricsLineNum, playProgress, mLyricsReader.getPlayOffset());
        } else {
            //lrc歌词
            //获取分割后的索引
            mSplitLyricsLineNum = LyricsUtils.getSplitLrcLyricsLineNum(mLrcLineInfos, mLyricsLineNum, playProgress, mLyricsReader.getPlayOffset());
        }
    }

    /**
     * 释放
     */
    public void release() {
        removeCallbacksAndMessages();
        //关闭线程
        if (mHandlerThread != null)
            mHandlerThread.quit();
    }

    /**
     *
     */
    private void removeCallbacksAndMessages() {
        //移除队列任务
        if (mUIHandler != null) {
            mUIHandler.removeCallbacksAndMessages(null);
        }

        //移除队列任务
        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
        }

    }

    /**
     * 搜索歌词接口
     */
    public interface SearchLyricsListener {
        /**
         * 搜索歌词回调
         */
        void goToSearchLrc();
    }

    /**
     * 额外歌词事件
     */
    public interface ExtraLyricsListener {
        /**
         * 额外歌词回调
         */
        void extraLrcCallback();
    }

    ///////////////////////////////////////////////

    public void setRefreshTime(long refreshTime) {
        this.mRefreshTime = refreshTime;
    }

    public void setTextMaxWidth(float mTextMaxWidth) {
        this.mTextMaxWidth = mTextMaxWidth;
    }

    public String getLoadingText() {
        return mLoadingText;
    }

    public void setLoadingText(String mLoadingText) {
        this.mLoadingText = mLoadingText;
    }

    public String getLoadErrorText() {
        return mLoadErrorText;
    }

    public void setLoadErrorText(String mLoadErrorText) {
        this.mLoadErrorText = mLoadErrorText;
    }

    public String getNonsupportText() {
        return mNonsupportText;
    }

    public void setNonsupportText(String mNonsupportText) {
        this.mNonsupportText = mNonsupportText;
    }

    public String getGotoSearchText() {
        return mGotoSearchText;
    }

    public void setGotoSearchText(String mGotoSearchText) {
        this.mGotoSearchText = mGotoSearchText;
    }

    public void setLyricsLineNum(int mLyricsLineNum) {
        this.mLyricsLineNum = mLyricsLineNum;
    }

    public int getLyricsLineNum() {
        return mLyricsLineNum;
    }

    public int getSplitLyricsLineNum() {
        return mSplitLyricsLineNum;
    }

    public int getSplitLyricsWordIndex() {
        return mSplitLyricsWordIndex;
    }

    public int getLyricsWordIndex() {
        return mLyricsWordIndex;
    }

    public float getLyricsWordHLTime() {
        return mLyricsWordHLTime;
    }

    public int getExtraSplitLyricsLineNum() {
        return mExtraSplitLyricsLineNum;
    }

    public int getExtraSplitLyricsWordIndex() {
        return mExtraSplitLyricsWordIndex;
    }

    public int getExtraLyricsWordIndex() {
        return mExtraLyricsWordIndex;
    }

    public float getTranslateLyricsWordHLTime() {
        return mTranslateLyricsWordHLTime;
    }

    public float getSpaceLineHeight() {
        return mSpaceLineHeight;
    }

    public float getPaddingLeftOrRight() {
        return mPaddingLeftOrRight;
    }

    public int getTranslateDrawType() {
        return mTranslateDrawType;
    }

    public LyricsReader getLyricsReader() {
        return mLyricsReader;
    }

    public TreeMap<Integer, LyricsLineInfo> getLrcLineInfos() {
        return mLrcLineInfos;
    }

    public List<LyricsLineInfo> getTranslateLrcLineInfos() {
        return mTranslateLrcLineInfos;
    }

    public List<LyricsLineInfo> getTransliterationLrcLineInfos() {
        return mTransliterationLrcLineInfos;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public int[] getPaintColors() {
        return mPaintColors;
    }

    public Paint getPaintHL() {
        return mPaintHL;
    }

    public int[] getPaintHLColors() {
        return mPaintHLColors;
    }

    public Paint getPaintOutline() {
        return mPaintOutline;
    }

    public Paint getExtraLrcPaint() {
        return mExtraLrcPaint;
    }

    public Paint getExtraLrcPaintHL() {
        return mExtraLrcPaintHL;
    }

    public Paint getExtraLrcPaintOutline() {
        return mExtraLrcPaintOutline;
    }
}
