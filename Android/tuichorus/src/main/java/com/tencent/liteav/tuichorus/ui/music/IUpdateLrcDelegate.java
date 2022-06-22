package com.tencent.liteav.tuichorus.ui.music;

public interface IUpdateLrcDelegate {
    // 设置歌词路径
    void setLrcPath(String path);

    // 音乐播放的时候调用该方法滚动歌词，高亮正在播放的那句歌词
    void seekLrcToTime(long time);
}
