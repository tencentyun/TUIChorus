package com.tencent.liteav.tuichorus.ui.music;

import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;

import java.util.List;

/**
 * 已点列表回调接口
 */
public interface ChorusMusicServiceDelegate {

    /**
     * 已点列表的更新
     *
     * @param musicInfoList 歌曲列表
     */
    void onMusicListChange(List<ChorusMusicModel> musicInfoList);

    /**
     * 歌词设置回调
     *
     * @param musicID 应播放的歌曲
     */
    void onShouldSetLyric(String musicID);

    /**
     * 歌曲播放回调
     *
     * @param model 应停止播放的歌曲
     */
    void onShouldPlay(ChorusMusicModel model);

    /**
     * 歌曲停止回调
     *
     * @param model 事件发生的歌曲
     */
    void onShouldStopPlay(ChorusMusicModel model);

    /**
     * 点歌信息展示回调
     *
     * @param model 事件发生的歌曲
     */
    void onShouldShowMessage(ChorusMusicModel model);
}

