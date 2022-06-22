package com.tencent.liteav.demo.chorusimpl;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.tencent.liteav.tuichorus.ui.base.ChorusMusicInfo;

import java.util.ArrayList;
import java.util.List;

public class MusicInfoController {
    private static final String TAG = "MusicInfoController";

    private       List<ChorusMusicInfo> mMusicLocalList;
    private final int                   MUSIC_NUMBER = 10;
    private       String             mPath;
    private       String             mDefaultUrl  =
            "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar2.png";

    public MusicInfoController(Context context) {
        mPath = ContextCompat.getExternalFilesDirs(context, null)[0].getAbsolutePath() + "/";
    }

    public ChorusMusicInfo getSongEntity(int id) {
        if (mPath == null) {
            return null;
        }

        String houlai_bz = mPath + "houlai_bz.mp3";
        String houlai_yc = mPath + "houlai_yc.mp3";

        String qfdy_yc = mPath + "qfdy_yc.mp3";
        String qfdy_bz = mPath + "qfdy_bz.mp3";


        String xq_bz       = mPath + "xq_bz.mp3";
        String xq_yc       = mPath + "xq_yc.mp3";
        String nuannuan_bz = mPath + "nuannuan_bz.mp3";
        String nuannuan_yc = mPath + "nuannuan_yc.mp3";

        String jda_yc = mPath + "jda.mp3";
        String jda_bz = mPath + "jda_bz.mp3";

        String houlai   = mPath + "houlai_lrc.vtt";
        String qfdy     = mPath + "qfdy_lrc.vtt";
        String xq       = mPath + "xq_lrc.vtt";
        String nuannuan = mPath + "nuannuan_lrc.vtt";
        String jda      = mPath + "jda_lrc.vtt";

        ChorusMusicInfo songEntity = new ChorusMusicInfo();
        if (id == 0) {
            songEntity.musicId = "1001"; //test
            songEntity.musicName = "后来_伴奏";
            songEntity.singer = "刘若英";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = houlai;
            songEntity.contentUrl = houlai_bz;
            return songEntity;
        } else if (id == 1) {
            songEntity.musicId = "1002"; //test
            songEntity.musicName = "后来_原唱";
            songEntity.singer = "刘若英";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = houlai;
            songEntity.contentUrl = houlai_yc;
            return songEntity;
        } else if (id == 2) {
            songEntity.musicId = "1003"; //test
            songEntity.musicName = "情非得已_伴奏";
            songEntity.singer = "庾澄庆";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = qfdy;
            songEntity.contentUrl = qfdy_bz;
            return songEntity;
        } else if (id == 3) {
            songEntity.musicId = "1004"; //test
            songEntity.musicName = "情非得已_原唱";
            songEntity.singer = "庾澄庆";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = qfdy;
            songEntity.contentUrl = qfdy_yc;
            return songEntity;
        } else if (id == 4) {
            songEntity.musicId = "1005"; //test
            songEntity.musicName = "星晴_伴奏";
            songEntity.singer = "周杰伦";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = xq;
            songEntity.contentUrl = xq_bz;
            return songEntity;
        } else if (id == 5) {
            songEntity.musicId = "1006"; //test
            songEntity.musicName = "星晴_原唱";
            songEntity.singer = "周杰伦";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = xq;
            songEntity.contentUrl = xq_yc;
            return songEntity;
        } else if (id == 6) {
            songEntity.musicId = "1007"; //test
            songEntity.musicName = "暖暖_伴奏";
            songEntity.singer = "梁静茹";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = nuannuan;
            songEntity.contentUrl = nuannuan_bz;
            return songEntity;
        } else if (id == 7) {
            songEntity.musicId = "1008"; //test
            songEntity.musicName = "暖暖_原唱";
            songEntity.singer = "梁静茹";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = nuannuan;
            songEntity.contentUrl = nuannuan_yc;
            return songEntity;
        } else if (id == 8) {
            songEntity.musicId = "1009"; //test
            songEntity.musicName = "简单爱_伴奏";
            songEntity.singer = "周杰伦";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = jda;
            songEntity.contentUrl = jda_bz;
            return songEntity;
        } else if (id == 9) {
            songEntity.musicId = "1010"; //test
            songEntity.musicName = "简单爱_原唱";
            songEntity.singer = "周杰伦";
            songEntity.coverUrl = mDefaultUrl;
            songEntity.lrcUrl = jda;
            songEntity.contentUrl = jda_yc;
            return songEntity;
        }
        return null;
    }


    public List<ChorusMusicInfo> getLibraryList() {
        if (mMusicLocalList != null) {
            mMusicLocalList.clear();
        } else {
            mMusicLocalList = new ArrayList<>();
        }
        for (int i = 0; i < MUSIC_NUMBER; i++) {
            mMusicLocalList.add(getSongEntity(i));
        }
        return mMusicLocalList;
    }
}
