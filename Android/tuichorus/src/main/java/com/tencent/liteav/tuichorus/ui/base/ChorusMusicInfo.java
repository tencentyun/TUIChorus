package com.tencent.liteav.tuichorus.ui.base;

public class ChorusMusicInfo {
    public String musicId;     //歌曲Id
    public String musicName;   //歌曲名称
    public String singer;      //演唱者
    public String lrcUrl;      //歌词
    public String coverUrl;    //歌曲封面
    public String contentUrl;  //歌曲Url

    @Override
    public String toString() {
        return "MusicModel {" +
                "musicId='" + musicId +
                ", songName='" + musicName + '\'' +
                ", singer='" + singer + '\'' +
                ", lrcUrl='" + lrcUrl + '\'' +
                ", coverUrl=" + coverUrl + '\'' +
                ", contentUrl=" + contentUrl + '\'' +
                '}';
    }
}
