package com.tencent.liteav.tuichorus.model;

import com.google.gson.annotations.SerializedName;

public class TRTCChorusSEIJsonData {

    /**
     * current_time : 0
     * music_id : 1002
     * total_time : 269.793
     */

    @SerializedName("current_time")
    private long currentTime;

    @SerializedName("music_id")
    private String musicId;

    @SerializedName("total_time")
    private long totalTime;

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
}
