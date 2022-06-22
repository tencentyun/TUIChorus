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
        return "RoomSeatEntity{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", userAvatar='" + userAvatar + '\'' +
                '}';
    }
}
