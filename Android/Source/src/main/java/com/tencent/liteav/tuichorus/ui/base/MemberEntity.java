package com.tencent.liteav.tuichorus.ui.base;

import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDef;

public class MemberEntity extends TRTCChorusRoomDef.UserInfo {
    public static final int TYPE_IDEL       = 0;
    public static final int TYPE_IN_SEAT    = 1;
    public static final int TYPE_WAIT_AGREE = 2;

    public int type;
}
