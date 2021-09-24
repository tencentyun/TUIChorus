package com.tencent.liteav.tuichorus.model;

public class TRTCChorusSEIJsonData {

    /**
     * current_time : 0
     * music_id : 1002
     * total_time : 269.793
     */

    private long   current_time;
    private String music_id;
    private long   total_time;

    public long getCurrentTime() {
        return current_time;
    }

    public void setCurrentTime(long currentTime) {
        this.current_time = currentTime;
    }

    public String getMusicId() {
        return music_id;
    }

    public void setMusicId(String music_id) {
        this.music_id = music_id;
    }

    public long getTotalTime() {
        return total_time;
    }

    public void setTotalTime(long total_time) {
        this.total_time = total_time;
    }
}
