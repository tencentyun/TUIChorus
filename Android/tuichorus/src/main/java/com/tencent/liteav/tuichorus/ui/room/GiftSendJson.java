package com.tencent.liteav.tuichorus.ui.room;

public class GiftSendJson {

    /**
     * sendUser : renee
     * sendUserHeadIcon : https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar3.png
     * giftId : 1
     */

    private String sendUser;
    private String sendUserHeadIcon;
    private String giftId;

    public String getSendUser() {
        return sendUser;
    }

    public void setSendUser(String sendUser) {
        this.sendUser = sendUser;
    }

    public String getSendUserHeadIcon() {
        return sendUserHeadIcon;
    }

    public void setSendUserHeadIcon(String sendUserHeadIcon) {
        this.sendUserHeadIcon = sendUserHeadIcon;
    }

    public String getGiftId() {
        return giftId;
    }

    public void setGiftId(String giftId) {
        this.giftId = giftId;
    }
}
