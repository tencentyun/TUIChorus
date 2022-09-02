package com.tencent.liteav.tuichorus.ui.base;

public class ChorusMusicModel extends ChorusMusicInfo {

    public String  bookUser;    //点歌用户
    public Boolean isSelected;  //歌曲是否已点,点歌的时候将该值置为true ,切歌或删除时置为false

    @Override
    public String toString() {
        return "ChorusMusicModel{"
                + "bookUser='" + bookUser + '\''
                + ", isSelected=" + isSelected
                + '}';
    }
}
