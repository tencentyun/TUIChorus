package com.tencent.liteav.tuichorus.ui.audio;

public interface IAudioEffectPanelDelegate {

    // 调整人声音量
    void onMicVolumChanged(int progress);

    // 调整音乐音量
    void onMusicVolumChanged(int progress);

    // 调整音乐升降调
    void onPitchLevelChanged(float pitch);

    // 变声效果
    void onChangeRV(int type);

    // 混响效果
    void onReverbRV(int type);
}
