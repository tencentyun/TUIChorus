package com.tencent.liteav.tuichorus.ui.base;

public class ChorusRoomSeatEntity {
    public int     index;
    public String  userId;
    public String  userName;
    public String  userAvatar;
    public boolean isUsed;
    public boolean isClose;
    public boolean isSeatMute;
    public boolean isUserMute = true;
    public boolean isTalk;

    @Override
    public String toString() {
        return "ChorusRoomSeatEntity{"
                + "index=" + index
                + ", userId='" + userId + '\''
                + ", userName='" + userName + '\''
                + ", userAvatar='" + userAvatar + '\''
                + ", isUsed=" + isUsed
                + ", isClose=" + isClose
                + ", isSeatMute=" + isSeatMute
                + ", isUserMute=" + isUserMute
                + ", isTalk=" + isTalk
                + '}';
    }
}
