package com.tencent.liteav.tuichorus.ui.room;

/**
 * Module:   TCConstants
 * <p>
 * Function: 定义常量的类
 */
public class TCConstants {
    public static final String CMD_REQUEST_TAKE_SEAT = "1";
    public static final String CMD_PICK_UP_SEAT      = "2";
    public static final String CMD_ORDER_SONG        = "3";
    
    public static final int IMCMD_GIFT = 0; //礼物消息
    
    /**
     * 网络质量
     * <p>
     * TRTC 会每隔两秒对当前的网络质量进行评估，评估结果为六个等级(1-6)：Excellent(1) 表示最好，Down(6) 表示最差。
     */
    public static final int TRTCQUALITY_NONE      = 0;  //未定义或未上麦
    public static final int TRTCQUALITY_EXCELLENT = 1;  //当前网络非常好
    public static final int TRTCQUALITY_GOOD      = 2;  //当前网络比较好
    public static final int TRTCQUALITY_POOR      = 3;  //当前网络一般
    public static final int TRTCQUALITY_BAD       = 4;  //当前网络较差
    public static final int TRTCQUALITY_VBAD      = 5;  //当前网络很差
    public static final int TRTCQUALITY_DOWN      = 6;  //当前网络不满足 TRTC 的最低要求
    
}

