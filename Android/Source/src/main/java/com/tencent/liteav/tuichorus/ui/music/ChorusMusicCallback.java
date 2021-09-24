package com.tencent.liteav.tuichorus.ui.music;

import com.tencent.liteav.tuichorus.ui.base.ChorusMusicInfo;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;

import java.util.List;

public class ChorusMusicCallback {
    /**
     * 通用回调
     */
    public interface ActionCallback {
        void onCallback(int code, String msg);
    }

    /**
     * 歌曲信息回调
     */
    public interface MusicListCallback {
        void onCallback(int code, String msg, List<ChorusMusicInfo> list);
    }

    /**
     * 已选列表回调
     */
    public interface MusicSelectedListCallback {
        void onCallback(int code, String msg, List<ChorusMusicModel> list);
    }

    /**
     * 下载进度回调
     */
    public interface ProgressCallback {
        void onCallback(double progress);
    }
}
