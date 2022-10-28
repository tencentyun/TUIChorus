package com.tencent.liteav.tuichorus.ui.lrc;

import android.util.Base64;

import com.tencent.liteav.tuichorus.ui.lrc.formats.LyricsFileReader;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsInfo;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsLineInfo;
import com.tencent.liteav.tuichorus.ui.lrc.model.LyricsTag;
import com.tencent.liteav.tuichorus.ui.lrc.utils.LyricsIOUtils;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;


public class LyricsReader {

    /**
     * 时间补偿值,其单位是毫秒，正值表示整体提前，负值相反。这是用于总体调整显示快慢的。
     */
    private long mDefOffset = 0;
    /**
     * 增量
     */
    private long mOffset    = 0;

    /**
     * 歌词类型
     */
    private int mLyricsType = LyricsInfo.DYNAMIC;

    /**
     * 歌词文件路径
     */
    private String mLrcFilePath;

    /**
     * 文件hash
     */
    private String mHash;

    /**
     * 原始歌词列表
     */
    private TreeMap<Integer, LyricsLineInfo> mLrcLineInfos;

    private LyricsInfo mLyricsInfo;

    /**
     * 加载歌词数据
     *
     * @param lyricsFile
     */
    public void loadLrc(File lyricsFile) throws Exception {
        if (!lyricsFile.exists() || lyricsFile.length() == 0) {
            return;
        }

        this.mLrcFilePath = lyricsFile.getPath();
        LyricsFileReader lyricsFileReader = LyricsIOUtils.getLyricsFileReader(lyricsFile);
        LyricsInfo lyricsInfo = lyricsFileReader.readFile(lyricsFile);
        parser(lyricsInfo);
    }

    /**
     * @param base64FileContentString 歌词base64文件
     * @param saveLrcFile             要保存的的lrc文件
     * @param fileName                含后缀名的文件名称
     */
    public void loadLrc(String base64FileContentString, File saveLrcFile, String fileName) throws Exception {
        loadLrc(Base64.decode(base64FileContentString, Base64.NO_WRAP), saveLrcFile, fileName);
    }

    /**
     * @param base64ByteArray 歌词base64数组
     * @param saveLrcFile
     * @param fileName
     */
    public void loadLrc(byte[] base64ByteArray, File saveLrcFile, String fileName) throws Exception {
        if (saveLrcFile != null) {
            mLrcFilePath = saveLrcFile.getPath();
        }
        LyricsFileReader lyricsFileReader = LyricsIOUtils.getLyricsFileReader(fileName);
        LyricsInfo lyricsInfo = lyricsFileReader.readLrcText(base64ByteArray, saveLrcFile);
        parser(lyricsInfo);

    }

    /**
     * 解析
     *
     * @param lyricsInfo
     */
    private void parser(LyricsInfo lyricsInfo) {
        mLyricsInfo = lyricsInfo;
        mLyricsType = lyricsInfo.getLyricsType();
        Map<String, Object> tags = lyricsInfo.getLyricsTags();
        if (tags.containsKey(LyricsTag.TAG_OFFSET)) {
            mDefOffset = 0;
            try {
                mDefOffset = Long.parseLong((String) tags.get(LyricsTag.TAG_OFFSET));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mDefOffset = 0;
        }
        //默认歌词行
        mLrcLineInfos = lyricsInfo.getLyricsLineInfoTreeMap();
    }

    public int getLyricsType() {
        return mLyricsType;
    }

    public TreeMap<Integer, LyricsLineInfo> getLrcLineInfos() {
        return mLrcLineInfos;
    }

    public String getHash() {
        return mHash;
    }

    public void setHash(String mHash) {
        this.mHash = mHash;
    }

    public String getLrcFilePath() {
        return mLrcFilePath;
    }

    public void setLrcFilePath(String mLrcFilePath) {
        this.mLrcFilePath = mLrcFilePath;
    }

    public long getOffset() {
        return mOffset;
    }

    public void setOffset(long offset) {
        this.mOffset = offset;
    }

    public LyricsInfo getLyricsInfo() {
        return mLyricsInfo;
    }

    public void setLyricsType(int mLyricsType) {
        this.mLyricsType = mLyricsType;
    }

    public void setLrcLineInfos(TreeMap<Integer, LyricsLineInfo> mLrcLineInfos) {
        this.mLrcLineInfos = mLrcLineInfos;
    }

    public void setLyricsInfo(LyricsInfo lyricsInfo) {
        /**
         * 重新解析歌词
         */
        parser(lyricsInfo);
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * 播放的时间补偿值
     *
     * @return
     */
    public long getPlayOffset() {
        return mDefOffset + mOffset;
    }
}
