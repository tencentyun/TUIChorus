package com.tencent.liteav.tuichorus.ui.lrc.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.tencent.liteav.tuichorus.ui.lrc.LyricsReader;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsInfo;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsLineInfo;
import com.tencent.liteav.tuichorus.ui.lrc.utils.LyricsUtils;

import java.util.List;
import java.util.TreeMap;

public class FloatLyricsView extends AbstractLrcView {

    public static final int ORIENTATION_LEFT   = 0;      // 歌词靠左
    public static final int ORIENTATION_CENTER = 1;      // 歌词居中

    private int mOrientation = ORIENTATION_LEFT;

    public FloatLyricsView(Context context) {
        super(context);
        init(context);
    }

    public FloatLyricsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        WindowManager  wm             = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display        display        = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        int screensWidth = displayMetrics.widthPixels;

        //设置歌词的最大宽度
        int textMaxWidth = screensWidth / 3 * 2;
        setTextMaxWidth(textMaxWidth);
    }

    @Override
    protected void onDrawLrcView(Canvas canvas) {
        LyricsReader                     lyricsReader         = getLyricsReader();
        TreeMap<Integer, LyricsLineInfo> lrcLineInfos         = getLrcLineInfos();
        int                              lyricsLineNum        = getLyricsLineNum();
        int                              splitLyricsLineNum   = getSplitLyricsLineNum();
        int                              splitLyricsWordIndex = getSplitLyricsWordIndex();
        float                            lyricsWordHLTime     = getLyricsWordHLTime();
        Paint                            paint                = getPaint();
        Paint                            paintHL              = getPaintHL();
        Paint                            paintOutline         = getPaintOutline();

        int[] paintColors        = getPaintColors();
        int[] paintHLColors      = getPaintHLColors();
        float spaceLineHeight    = getSpaceLineHeight();
        float paddingLeftOrRight = getPaddingLeftOrRight();

        // 先设置当前歌词，之后再根据索引判断是否放在左边还是右边
        List<LyricsLineInfo> splitLyricsLineInfos = lrcLineInfos.get(lyricsLineNum).getSplitLyricsLineInfos();
        LyricsLineInfo       lyricsLineInfo       = splitLyricsLineInfos.get(splitLyricsLineNum);
        //获取行歌词高亮宽度
        float lineLyricsHLWidth = LyricsUtils.getLineLyricsHLWidth(lyricsReader.getLyricsType(), paint,
                lyricsLineInfo, splitLyricsWordIndex, lyricsWordHLTime);
        // 当行歌词
        String curLyrics       = lyricsLineInfo.getLineLyrics();
        float  curLrcTextWidth = LyricsUtils.getTextWidth(paint, curLyrics);
        // 当前歌词行的x坐标
        float textX = 0;
        // 当前歌词行的y坐标
        float textY                  = 0;
        int   splitLyricsRealLineNum = LyricsUtils.getSplitLyricsRealLineNum(lrcLineInfos, lyricsLineNum, splitLyricsLineNum);
        float topPadding             = (getHeight() - spaceLineHeight - 2 * LyricsUtils.getTextHeight(paint)) / 2;
        if (splitLyricsRealLineNum % 2 == 0) {
            textY = topPadding + LyricsUtils.getTextHeight(paint);
            float nextLrcTextY = textY + spaceLineHeight + LyricsUtils.getTextHeight(paint);
            if (lyricsLineNum + 1 <= lrcLineInfos.size()) {
                //画出当前右边歌词行
                textX = paddingLeftOrRight;
                LyricsUtils.drawOutline(canvas, paintOutline, curLyrics, textX, textY);
                LyricsUtils.drawDynamicText(canvas, paint, paintHL, paintColors, paintHLColors, curLyrics,
                        lineLyricsHLWidth, textX, textY);

                // 画下一句的歌词，该下一句不在该行分割歌词里面，需要从原始下一行的歌词里面找
                if ((lyricsLineNum + 1) == lrcLineInfos.size()) {
                    return;
                }
                List<LyricsLineInfo> nextSplitLyricsLineInfos = lrcLineInfos.get(lyricsLineNum + 1).getSplitLyricsLineInfos();
                String               lrcRightText             = nextSplitLyricsLineInfos.get(0).getLineLyrics();
                float                lrcRightTextWidth        = LyricsUtils.getTextWidth(paint, lrcRightText);
                float                textRightX               = 0;

                textRightX = getWidth() - lrcRightTextWidth - paddingLeftOrRight;

                //当最后一句歌词在右边时,不再画出左边;否则画出右边歌词
                LyricsUtils.drawOutline(canvas, paintOutline, lrcRightText, textRightX, nextLrcTextY);
                LyricsUtils.drawText(canvas, paint, paintColors, lrcRightText, textRightX, nextLrcTextY);
            }
        } else {
            float preLrcTextY = topPadding + LyricsUtils.getTextHeight(paint);
            textY = preLrcTextY + spaceLineHeight + LyricsUtils.getTextHeight(paint);
            if (lyricsLineNum + 1 <= lrcLineInfos.size()) {
                int tempNum = lyricsLineNum - 1;
                if (tempNum < 0) {
                    tempNum = 0;
                }
                // 画下一句的歌词，该下一句不在该行分割歌词里面，需要从原始下一行的歌词里面找
                List<LyricsLineInfo> nextSplitLyricsLineInfos = lrcLineInfos.get(tempNum).getSplitLyricsLineInfos();
                String               lrcLeftText              = nextSplitLyricsLineInfos.get(0).getLineLyrics();
                float                lrcLeftTextWidth         = LyricsUtils.getTextWidth(paint, lrcLeftText);

                float textLeftX = paddingLeftOrRight;

                LyricsUtils.drawOutline(canvas, paintOutline, lrcLeftText, textLeftX, preLrcTextY);
                LyricsUtils.drawText(canvas, paint, paintHLColors, lrcLeftText, textLeftX, preLrcTextY);

                //画歌词
                textX = getWidth() - curLrcTextWidth - paddingLeftOrRight;
                LyricsUtils.drawOutline(canvas, paintOutline, curLyrics, textX, textY);
                LyricsUtils.drawDynamicText(canvas, paint, paintHL, paintColors, paintHLColors, curLyrics,
                        lineLyricsHLWidth, textX, textY);
            }
        }
    }

    @Override
    public void updateView(long playProgress) {
        LyricsReader                     lyricsReader = getLyricsReader();
        TreeMap<Integer, LyricsLineInfo> lrcLineInfos = getLrcLineInfos();

        int lyricsLineNum = LyricsUtils.getLineNumber(lyricsReader.getLyricsType(),
                lrcLineInfos, playProgress, lyricsReader.getPlayOffset());
        setLyricsLineNum(lyricsLineNum);
        updateSplitData(playProgress);
    }

    /**
     * 设置默认颜色
     *
     * @param paintColor
     */
    public void setPaintColor(int[] paintColor) {
        setPaintColor(paintColor, false);
    }

    /**
     * 设置高亮颜色
     *
     * @param paintHLColor
     */
    public void setPaintHLColor(int[] paintHLColor) {
        setPaintHLColor(paintHLColor, false);
    }

    /**
     * 设置字体文件
     *
     * @param typeFace
     */
    public void setTypeFace(Typeface typeFace) {
        setTypeFace(typeFace, false);
    }

    /**
     * 设置空行高度
     *
     * @param spaceLineHeight
     */
    public void setSpaceLineHeight(float spaceLineHeight) {
        setSpaceLineHeight(spaceLineHeight, false);
    }

    /**
     * 设置歌词解析器
     *
     * @param lyricsReader
     */
    public void setLyricsReader(LyricsReader lyricsReader) {
        super.setLyricsReader(lyricsReader);
        if (lyricsReader != null && lyricsReader.getLyricsType() == LyricsInfo.DYNAMIC) {
        } else {
            setLrcStatus(AbstractLrcView.LRCSTATUS_NONSUPPORT);
        }
    }

    /**
     * 设置字体大小
     *
     * @param fontSize
     */
    public void setFontSize(float fontSize) {
        setFontSize(fontSize);
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

}
