package com.tencent.liteav.tuichorus.ui.lrc.formats.vtt;

import android.util.Log;

import com.tencent.liteav.tuichorus.ui.lrc.formats.LyricsFileReader;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsInfo;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsLineInfo;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsTag;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VttLyricsFileReader extends LyricsFileReader {

    protected static final String TAG = "VttLyricsFileReader";

    /**
     * 歌曲名 字符串
     */
    private final static String LEGAL_SONGNAME_PREFIX   = "[ti:";
    /**
     * 歌手名 字符串
     */
    private final static String LEGAL_SINGERNAME_PREFIX = "[ar:";
    /**
     * 时间补偿值 字符串
     */
    private final static String LEGAL_OFFSET_PREFIX     = "[offset:";
    /**
     * 歌词上传者
     */
    private final static String LEGAL_BY_PREFIX         = "[by:";
    private final static String LEGAL_HASH_PREFIX       = "[hash:";
    /**
     * 专辑
     */
    private final static String LEGAL_AL_PREFIX         = "[al:";
    private final static String LEGAL_SIGN_PREFIX       = "[sign:";
    private final static String LEGAL_QQ_PREFIX         = "[qq:";
    private final static String LEGAL_TOTAL_PREFIX      = "[total:";
    private final static String LEGAL_LANGUAGE_PREFIX   = "[language:";

    @Override
    public LyricsInfo readInputStream(InputStream in) throws Exception {
        LyricsInfo lyricsIfno = new LyricsInfo();
        lyricsIfno.setLyricsFileExt(getSupportFileExt());
        if (in != null) {

            TreeMap<Integer, LyricsLineInfo> lyricsLineInfos = new TreeMap<>();
            Map<String, Object>              lyricsTags      = new HashMap<>();
            int                              index           = 0;
            String                           lyricsTextStr   = "";
            String                           lyricsLine      = "";

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((lyricsTextStr = reader.readLine()) != null) {
                // 匹配歌词行
                Pattern timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}).(\\d{3})");
                Matcher timeMatcher = timePattern.matcher(lyricsTextStr);
                if (timeMatcher.find()) {
                    lyricsLine = "[";
                    String[] temp = lyricsTextStr.split(" --> "); // 分割字符串
                    for (int i = 0; i < 2; i++) {
                        lyricsLine += dateToMilliseconds(temp[i]);
                        if (i == 0) {
                            lyricsLine += ",";
                        }
                    }

                    lyricsLine = lyricsLine + "]" + reader.readLine();
                    // 行读取，并解析每行歌词的内容
                    LyricsLineInfo lyricsLineInfo = parserLineInfos(lyricsTags, lyricsLine, lyricsIfno);
                    if (lyricsLineInfo != null) {
                        lyricsLineInfos.put(index, lyricsLineInfo);
                        index++;
                    }
                }
            }

            in.close();
            // 设置歌词的标签类
            lyricsIfno.setLyricsTags(lyricsTags);
            lyricsIfno.setLyricsLineInfoTreeMap(lyricsLineInfos);
        }
        return lyricsIfno;
    }

    private String dateToMilliseconds(String inputString) {
        long    milliseconds = -1;
        Pattern pattern      = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}).(\\d{3})");
        Matcher matcher      = pattern.matcher(inputString);
        if (matcher.matches()) {
            milliseconds = Long.parseLong(matcher.group(1)) * 3600000L
                    + Long.parseLong(matcher.group(2)) * 60000
                    + Long.parseLong(matcher.group(3)) * 1000
                    + Long.parseLong(matcher.group(4));
        } else {
            Log.e(TAG, " date to milliseconds error, inputString: " + inputString);
        }

        return Long.toString(milliseconds);
    }

    /**
     * 解析歌词
     *
     * @param lyricsTags
     * @param lineInfo
     * @param lyricsIfno
     * @return
     */
    private LyricsLineInfo parserLineInfos(Map<String, Object> lyricsTags, String lineInfo, LyricsInfo lyricsIfno) throws Exception {
        LyricsLineInfo lyricsLineInfo = null;
        if (lineInfo.startsWith(LEGAL_SONGNAME_PREFIX)) {
            int startIndex = LEGAL_SONGNAME_PREFIX.length();
            int endIndex   = lineInfo.lastIndexOf("]");
            lyricsTags.put(LyricsTag.TAG_TITLE, lineInfo.substring(startIndex, endIndex));
        } else {
            // 匹配歌词行
            Pattern pattern = Pattern.compile("\\[\\d+,\\d+\\]");
            Matcher matcher = pattern.matcher(lineInfo);
            if (matcher.find()) {
                lyricsLineInfo = new LyricsLineInfo();
                // [此行开始时刻距0时刻的毫秒数,此行持续的毫秒数]<0,此字持续的毫秒数,0>歌
                // 获取行的出现时间和结束时间
                int mStartIndex = matcher.start();
                int mEndIndex   = matcher.end();
                String lineTime[] = lineInfo.substring(mStartIndex + 1,
                        mEndIndex - 1).split(",");

                int startTime = Integer.parseInt(lineTime[0]);
                int endTime   = Integer.parseInt(lineTime[1]);
                lyricsLineInfo.setEndTime(endTime);
                lyricsLineInfo.setStartTime(startTime);
                // 获取歌词信息
                String lineContent = lineInfo.substring(mEndIndex, lineInfo.length());

                // 歌词匹配的正则表达式
                String  regex              = "\\<\\d+,\\d+,\\d+\\>";
                Pattern lyricsWordsPattern = Pattern.compile(regex);
                Matcher lyricsWordsMatcher = lyricsWordsPattern
                        .matcher(lineContent);

                // 歌词分隔
                String   lineLyricsTemp[] = lineContent.split(regex);
                String[] lyricsWords      = getLyricsWords(lineLyricsTemp);
                lyricsLineInfo.setLyricsWords(lyricsWords);

                // 获取每个歌词的时间
                int wordsDisInterval[] = new int[lyricsWords.length];
                int index              = 0;
                while (lyricsWordsMatcher.find()) {

                    //验证
                    if (index >= wordsDisInterval.length) {
                        throw new Exception("字标签个数与字时间标签个数不相符");
                    }

                    String wordsDisIntervalStr = lyricsWordsMatcher.group();
                    String wordsDisIntervalStrTemp = wordsDisIntervalStr
                            .substring(wordsDisIntervalStr.indexOf('<') + 1, wordsDisIntervalStr.lastIndexOf('>'));
                    String wordsDisIntervalTemp[] = wordsDisIntervalStrTemp
                            .split(",");
                    wordsDisInterval[index++] = Integer
                            .parseInt(wordsDisIntervalTemp[1]);
                }
                lyricsLineInfo.setWordsDisInterval(wordsDisInterval);

                // 获取当行歌词
                String lineLyrics = lyricsWordsMatcher.replaceAll("");
                lyricsLineInfo.setLineLyrics(lineLyrics);
            }
        }
        return lyricsLineInfo;
    }

    /**
     * 分隔每个歌词
     *
     * @param lineLyricsTemp
     * @return
     */
    private String[] getLyricsWords(String[] lineLyricsTemp) throws Exception {
        String temp[] = null;
        if (lineLyricsTemp.length < 2) {
            return new String[lineLyricsTemp.length];
        }
        //
        temp = new String[lineLyricsTemp.length - 1];
        for (int i = 1; i < lineLyricsTemp.length; i++) {
            temp[i - 1] = lineLyricsTemp[i];
        }
        return temp;
    }

    @Override
    public boolean isFileSupported(String ext) {
        return ext.equalsIgnoreCase("vtt");
    }

    @Override
    public String getSupportFileExt() {
        return "vtt";
    }

}
