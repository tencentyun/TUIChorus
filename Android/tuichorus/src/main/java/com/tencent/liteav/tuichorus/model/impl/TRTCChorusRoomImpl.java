package com.tencent.liteav.tuichorus.model.impl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.tencent.liteav.audio.TXAudioEffectManager;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDef;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDelegate;
import com.tencent.liteav.tuichorus.model.impl.base.TRTCLogger;
import com.tencent.liteav.tuichorus.model.impl.base.TXCallback;
import com.tencent.liteav.tuichorus.model.impl.base.TXRoomInfo;
import com.tencent.liteav.tuichorus.model.impl.base.TXSeatInfo;
import com.tencent.liteav.tuichorus.model.impl.base.TXUserInfo;
import com.tencent.liteav.tuichorus.model.impl.base.TXUserListCallback;
import com.tencent.liteav.tuichorus.model.impl.room.ITXRoomServiceDelegate;
import com.tencent.liteav.tuichorus.model.impl.room.impl.TXRoomService;
import com.tencent.liteav.tuichorus.model.impl.trtc.TRTCChorusRoomService;
import com.tencent.liteav.tuichorus.model.impl.trtc.TRTCChorusRoomServiceDelegate;
import com.tencent.rtmp.TXLiveBase;
import com.tencent.rtmp.TXLiveBaseListener;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloudDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TRTCChorusRoomImpl extends TRTCChorusRoom implements ITXRoomServiceDelegate,
        TRTCChorusRoomServiceDelegate {
    private static final String TAG = "TRTCChorusRoomImpl";

    private static final int TRTC_ROLE_OWNER = 19;
    private static final int MAX_SEAT_SIZE   = 2;

    private static TRTCChorusRoomImpl sInstance;
    private final  Context            mContext;

    private TRTCChorusRoomDelegate mDelegate;
    // 所有调用都切到主线程使用，保证内部多线程安全问题
    private Handler                mMainHandler;
    // 外部可指定的回调线程
    private Handler                mDelegateHandler;
    private int                    mSdkAppId;
    private String                 mUserId;
    private String                 mUserSig;
    private int                    mRoomId;

    // 主播列表
    private Set<String>                           mAnchorList;
    // 已抛出的听众列表
    private Set<String>                           mAudienceList;
    private List<TRTCChorusRoomDef.SeatInfo>      mSeatInfoList;
    private TRTCChorusRoomCallback.ActionCallback mEnterSeatCallback;
    private TRTCChorusRoomCallback.ActionCallback mLeaveSeatCallback;
    private TRTCChorusRoomCallback.ActionCallback mPickSeatCallback;
    private TRTCChorusRoomCallback.ActionCallback mKickSeatCallback;
    private int                                   mTakeSeatIndex;
    private int                                   mRole = TRTCCloudDef.TRTCRoleAudience;

    //////////////////////////////////////////////////////////
    //                 合唱
    /////////////////////////////////////////////////////////
    private TRTCChorusManager mTRTCChorusManager;
    private String            mPushUrl;
    private String            mPlayUrl;
    private TXCloudVideoView  mVideoView;
    private boolean           mIsInTRTCRoom;

    public static synchronized TRTCChorusRoom sharedInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TRTCChorusRoomImpl(context.getApplicationContext());
        }
        return sInstance;
    }

    public static synchronized void destroySharedInstance() {
        if (sInstance != null) {
            sInstance.destroy();
            sInstance = null;
        }
    }

    private void destroy() {
        TXRoomService.getInstance().destroy();
    }

    private TRTCChorusRoomImpl(Context context) {
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());
        mDelegateHandler = new Handler(Looper.getMainLooper());
        mSeatInfoList = new ArrayList<>();
        mAnchorList = new HashSet<>();
        mAudienceList = new HashSet<>();
        mTakeSeatIndex = -1;
        TRTCChorusRoomService.getInstance().init(context);
        TRTCChorusRoomService.getInstance().setDelegate(this);
        TXRoomService.getInstance().init(context);
        TXRoomService.getInstance().setDelegate(this);
        mTRTCChorusManager = new TRTCChorusManager(mContext, com.tencent.trtc.TRTCCloud.sharedInstance(context));
        mTRTCChorusManager.setListener(new ChorusManagerListener());

        //开启校时
        updateNtpTime();
    }

    private void clearList() {
        mSeatInfoList.clear();
        mAnchorList.clear();
        mAudienceList.clear();
    }

    private void runOnMainThread(Runnable runnable) {
        Handler handler = mMainHandler;
        if (handler != null) {
            if (handler.getLooper() == Looper.myLooper()) {
                runnable.run();
            } else {
                handler.post(runnable);
            }
        } else {
            runnable.run();
        }
    }

    private void runOnDelegateThread(Runnable runnable) {
        Handler handler = mDelegateHandler;
        if (handler != null) {
            if (handler.getLooper() == Looper.myLooper()) {
                runnable.run();
            } else {
                handler.post(runnable);
            }
        } else {
            runnable.run();
        }
    }

    @Override
    public void setDelegate(TRTCChorusRoomDelegate delegate) {
        mDelegate = delegate;
    }

    @Override
    public void setDelegateHandler(Handler handler) {
        mDelegateHandler = handler;
    }

    @Override
    public void login(final int sdkAppId, final String userId, final String userSig,
                      final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "start login, sdkAppId:" + sdkAppId + " userId:" + userId
                        + " sign is empty:" + TextUtils.isEmpty(userSig));
                if (sdkAppId == 0 || TextUtils.isEmpty(userId) || TextUtils.isEmpty(userSig)) {
                    TRTCLogger.e(TAG, "start login fail. params invalid.");
                    if (callback != null) {
                        callback.onCallback(-1, "登录失败，参数有误");
                    }
                    return;
                }
                mSdkAppId = sdkAppId;
                mUserId = userId;
                mUserSig = userSig;
                TRTCLogger.i(TAG, "start login room service");
                TXRoomService.getInstance().login(sdkAppId, userId, userSig, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void logout(final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "start logout");
                mSdkAppId = 0;
                mUserId = "";
                mUserSig = "";
                TRTCLogger.i(TAG, "start logout room service");
                TXRoomService.getInstance().logout(new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        TRTCLogger.i(TAG, "logout room service finish, code:" + code + " msg:" + msg);
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void setSelfProfile(final String userName, final String avatarURL,
                               final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "set profile, user name:" + userName + " avatar url:" + avatarURL);
                TXRoomService.getInstance().setSelfProfile(userName, avatarURL, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        TRTCLogger.i(TAG, "set profile finish, code:" + code + " msg:" + msg);
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void createRoom(final int roomId, final TRTCChorusRoomDef.RoomParam roomParam, TXCloudVideoView videoView,
                           final TRTCChorusRoomCallback.ActionCallback callback) {
        this.mVideoView = videoView;
        enableRealtimeChorus(1);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "create room, roomId:" + roomId + " info:" + roomParam);
                if (roomId == 0) {
                    TRTCLogger.e(TAG, "create room fail. params invalid");
                    return;
                }

                final String strRoomId = String.valueOf(roomId);

                mRoomId = roomId;
                mRole = TRTC_ROLE_OWNER;
                clearList();

                final String roomName = (roomParam == null ? "" : roomParam.roomName);
                final String roomCover = (roomParam == null ? "" : roomParam.coverUrl);
                final boolean isNeedRequest = (roomParam != null && roomParam.needRequest);
                final int seatCount = (roomParam == null ? MAX_SEAT_SIZE : roomParam.seatCount);
                final List<TXSeatInfo> txSeatInfoList = new ArrayList<>();
                if (roomParam != null && roomParam.seatInfoList != null) {
                    for (TRTCChorusRoomDef.SeatInfo seatInfo : roomParam.seatInfoList) {
                        TXSeatInfo item = new TXSeatInfo();
                        item.status = seatInfo.status;
                        item.mute = seatInfo.mute;
                        item.user = seatInfo.userId;
                        txSeatInfoList.add(item);
                        mSeatInfoList.add(seatInfo);
                    }
                } else {
                    for (int i = 0; i < seatCount; i++) {
                        txSeatInfoList.add(new TXSeatInfo());
                        mSeatInfoList.add(new TRTCChorusRoomDef.SeatInfo());
                    }
                }
                mPushUrl = roomParam.mPushUrl;
                mPlayUrl = roomParam.mPlayUrl;
                // 创建房间
                TXRoomService.getInstance().createRoom(strRoomId, roomName, roomCover, isNeedRequest, txSeatInfoList,
                        new TXCallback() {
                            @Override
                            public void onCallback(final int code, final String msg) {
                                TRTCLogger.i(TAG, "create room in IM, code:" + code + " , msg: " + msg);
                                if (code == 0) {
                                    enterTRTCRoomInner(roomId, mUserId, mUserSig, mRole, callback);
                                    return;
                                } else {
                                    runOnDelegateThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mDelegate != null) {
                                                mDelegate.onError(code, msg);
                                            }
                                        }
                                    });
                                }
                                runOnDelegateThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (callback != null) {
                                            callback.onCallback(code, msg);
                                        }
                                    }
                                });
                            }
                        }, roomParam.mPlayUrl);
            }
        });
    }

    @Override
    public void destroyRoom(final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "start destroy room.");
                // TRTC 房间退房结果不关心
                TRTCChorusRoomService.getInstance().exitRoom(new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        TRTCLogger.i(TAG, "exit trtc room finish, code:" + code + " msg:" + msg);
                        if (code != 0) {
                            runOnDelegateThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDelegate != null) {
                                        mDelegate.onError(code, msg);
                                    }
                                }
                            });
                        }
                    }
                });

                TXRoomService.getInstance().destroyRoom(new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        TRTCLogger.i(TAG, "destroy room finish, code:" + code + " msg:" + msg);
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });

                // 恢复设定
                clearList();
                //停止推流
                mTRTCChorusManager.stopCdnPush();
                enableRealtimeChorus(0);
            }
        });
    }

    @Override
    public void enterRoom(final int roomId, final TXCloudVideoView videoView,
                          final TRTCChorusRoomCallback.ActionCallback callback) {
        this.mVideoView = videoView;
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                // 恢复设定
                clearList();
                mRoomId = roomId;
                TRTCLogger.i(TAG, "start enter IM room, roomId:" + roomId);
                TXRoomService.getInstance().enterRoom(String.valueOf(roomId), new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        TRTCLogger.i(TAG, "enter IM room service finish, roomId:" + roomId + " code:" + code);
                        runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (code != 0) {
                                    runOnDelegateThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mDelegate != null) {
                                                mDelegate.onError(code, msg);
                                            }
                                        }
                                    });
                                    if (null != callback) {
                                        callback.onCallback(code, msg);
                                    }
                                } else {
                                    //进入房间时，获取IM的attr来获取播放地址
                                    mPlayUrl = msg;
                                    //开始拉流
                                    if (!mTRTCChorusManager.isCdnPlaying()) {
                                        mTRTCChorusManager.startCdnPlay(mPlayUrl, videoView);
                                    }
                                    if (null != callback) {
                                        callback.onCallback(0, "enter room success");
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void exitRoom(final TRTCChorusRoomCallback.ActionCallback callback) {
        enableRealtimeChorus(0);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "start exit IM room.");
                // 退房的时候需要判断主播是否在座位，如果是麦上主播，需要先清空座位列表
                if (isOnSeat(mUserId)) {
                    leaveSeat(new TRTCChorusRoomCallback.ActionCallback() {
                        @Override
                        public void onCallback(int code, String msg) {
                            exitRoomInternal(callback);
                        }
                    });
                } else {
                    exitRoomInternal(callback);
                }
            }
        });
    }

    //退出房间（IM的房间）
    private void exitRoomInternal(final TRTCChorusRoomCallback.ActionCallback callback) {
        mTRTCChorusManager.stopCdnPlay();
        TXRoomService.getInstance().exitRoom(new TXCallback() {
            @Override
            public void onCallback(final int code, final String msg) {
                TRTCLogger.i(TAG, "exit room finish, code:" + code + " msg:" + msg);
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onCallback(code, msg);
                        }
                    }
                });
            }
        });
        clearList();
    }

    //进入合唱（TRTC房间），即主播上麦
    private void enterTRTCRoomInner(final int roomId, final String userId, final String userSig, final int role,
                                    final TRTCChorusRoomCallback.ActionCallback callback) {
        // 进入 TRTC 房间
        enableRealtimeChorus(1);
        TRTCLogger.i(TAG, "enter trtc room.");
        TRTCChorusRoomService.getInstance().enterRoom(mSdkAppId, roomId, userId, userSig, role, new TXCallback() {
            @Override
            public void onCallback(final int code, final String msg) {
                TRTCLogger.i(TAG, "enter trtc room finish, code:" + code + " msg:" + msg);
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        //上麦后停止拉流
                        mTRTCChorusManager.stopCdnPlay();
                        if (code == 0 && mRole == TRTC_ROLE_OWNER) {
                            //房主开始推流
                            mTRTCChorusManager.startCdnPush(mPushUrl);
                        }
                        //房主进房身份变为主播
                        if (mUserId != null && mUserId.equals(userId)) {
                            mRole = TRTCCloudDef.TRTCRoleAnchor;
                        }
                        //在TRTC房间中
                        mIsInTRTCRoom = true;
                        if (callback != null) {
                            callback.onCallback(code, msg);
                        }
                    }
                });
            }
        });
    }

    //退出合唱（TRTC房间），即主播下麦并拉流
    private void exitTRTCRoom() {
        enableRealtimeChorus(0);
        TRTCLogger.i(TAG, "exit trtc chorus Room.");
        TRTCChorusRoomService.getInstance().exitRoom(new TXCallback() {
            @Override
            public void onCallback(final int code, final String msg) {
                if (code != 0) {
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mDelegate != null) {
                                mDelegate.onError(code, msg);
                            }
                        }
                    });
                } else {
                    //结束合唱
                    mTRTCChorusManager.stopChorus();
                    //开始拉流
                    mTRTCChorusManager.startCdnPlay(mPlayUrl, mVideoView);
                    //身份变为听众
                    mRole = TRTCCloudDef.TRTCRoleAudience;
                    mIsInTRTCRoom = false;
                }
            }
        });
        //听众无法实时获取网络质量（仅主播通过TRTCChorusRoomService创建或进入房间可实时获取）
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onNetworkQuality(null, new ArrayList<TRTCCloudDef.TRTCQuality>());
                }
            }
        });
    }

    private boolean isOnSeat(String userId) {
        // 判断某个userId 是不是在座位上
        if (mSeatInfoList == null) {
            return false;
        }
        for (TRTCChorusRoomDef.SeatInfo seatInfo : mSeatInfoList) {
            if (userId != null && userId.equals(seatInfo.userId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getUserInfoList(final List<String> userIdList, final TRTCChorusRoomCallback.UserListCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (userIdList == null) {
                    getAudienceList(callback);
                    return;
                }
                TXRoomService.getInstance().getUserInfo(userIdList, new TXUserListCallback() {
                    @Override
                    public void onCallback(final int code, final String msg, final List<TXUserInfo> list) {
                        TRTCLogger.i(TAG, "getUserInfoList finish, code:" + code + " msg:" + msg
                                + " list:" + (list != null ? list.size() : 0));
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    List<TRTCChorusRoomDef.UserInfo> userList = new ArrayList<>();
                                    if (list != null) {
                                        for (TXUserInfo info : list) {
                                            TRTCChorusRoomDef.UserInfo trtcUserInfo = new TRTCChorusRoomDef.UserInfo();
                                            trtcUserInfo.userId = info.userId;
                                            trtcUserInfo.userAvatar = info.avatarURL;
                                            trtcUserInfo.userName = info.userName;
                                            userList.add(trtcUserInfo);
                                            TRTCLogger.i(TAG, "info:" + info);
                                        }
                                    }
                                    callback.onCallback(code, msg, userList);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void getAudienceList(final TRTCChorusRoomCallback.UserListCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TXRoomService.getInstance().getAudienceList(new TXUserListCallback() {
                    @Override
                    public void onCallback(final int code, final String msg, final List<TXUserInfo> list) {
                        TRTCLogger.i(TAG, "getAudienceList, code:" + code + " msg:" + msg
                                + " list:" + (list != null ? list.size() : 0));
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    List<TRTCChorusRoomDef.UserInfo> userList = new ArrayList<>();
                                    if (list != null) {
                                        for (TXUserInfo info : list) {
                                            TRTCChorusRoomDef.UserInfo trtcUserInfo = new TRTCChorusRoomDef.UserInfo();
                                            trtcUserInfo.userId = info.userId;
                                            trtcUserInfo.userAvatar = info.avatarURL;
                                            trtcUserInfo.userName = info.userName;
                                            userList.add(trtcUserInfo);
                                            TRTCLogger.i(TAG, "info:" + info);
                                        }
                                    }
                                    callback.onCallback(code, msg, userList);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void enterSeat(final int seatIndex, final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "enterSeat seatIndex = " + seatIndex);
                if (isOnSeat(mUserId)) {
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onCallback(-1, "you are already in the seat");
                            }
                        }
                    });
                    return;
                }
                mEnterSeatCallback = callback;
                TXRoomService.getInstance().takeSeat(seatIndex, new TXCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code != 0) {
                            //出错了，恢复callback
                            mEnterSeatCallback = null;
                            mTakeSeatIndex = -1;
                            if (callback != null) {
                                callback.onCallback(code, msg);
                            }
                        } else {
                            //成功上麦后,更新本地麦位表
                            TRTCChorusRoomDef.SeatInfo seatInfo = new TRTCChorusRoomDef.SeatInfo();
                            seatInfo.userId = mUserId;
                            mSeatInfoList.add(seatInfo);
                            TRTCLogger.i(TAG, "take seat callback success, and wait attrs changed.");
                        }
                    }
                });

                //进入TRTC房间进行合唱
                //房主创建房间的时候已经进入了TRTC房间,上麦的时候不需要再次进入
                if (!mIsInTRTCRoom) {
                    enterTRTCRoomInner(mRoomId, mUserId, mUserSig, mRole, callback);
                }
            }
        });
    }

    @Override
    public void leaveSeat(final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "leaveSeat seatIndex = " + mTakeSeatIndex);
                if (mTakeSeatIndex == -1) {
                    //已经不再座位上了
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onCallback(-1, "you are not in the seat");
                            }
                        }
                    });
                    return;
                }
                mLeaveSeatCallback = callback;
                TXRoomService.getInstance().leaveSeat(mTakeSeatIndex, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        if (code != 0) {
                            //出错了，恢复callback
                            mLeaveSeatCallback = null;
                            runOnDelegateThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCallback(code, msg);
                                    }
                                }
                            });
                        }
                        //离开麦位，身份变为听众
                        exitTRTCRoom();
                    }
                });
            }
        });
    }

    @Override
    public void pickSeat(final int seatIndex, final String userId,
                         final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                //判断该用户是否已经在麦上
                TRTCLogger.i(TAG, "pickSeat " + seatIndex);
                if (isOnSeat(userId)) {
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onCallback(-1, "该用户已经是麦上主播了");
                            }
                        }
                    });
                    return;
                }
                mPickSeatCallback = callback;
                TXRoomService.getInstance().pickSeat(seatIndex, userId, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        if (code != 0) {
                            //出错了，恢复callback
                            mPickSeatCallback = null;
                            runOnDelegateThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCallback(code, msg);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public void kickSeat(final int index, final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "kickSeat " + index);
                mKickSeatCallback = callback;
                TXRoomService.getInstance().kickSeat(index, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        if (code != 0) {
                            //出错了，恢复callback
                            mKickSeatCallback = null;
                            runOnDelegateThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCallback(code, msg);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public void muteSeat(final int seatIndex, final boolean isMute,
                         final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "muteSeat " + seatIndex + " " + isMute);
                TXRoomService.getInstance().muteSeat(seatIndex, isMute, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void closeSeat(final int seatIndex, final boolean isClose,
                          final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "closeSeat " + seatIndex + " " + isClose);
                TXRoomService.getInstance().closeSeat(seatIndex, isClose, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void startMicrophone() {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().startMicrophone();
            }
        });
    }

    @Override
    public void stopMicrophone() {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().stopMicrophone();
            }
        });
    }

    @Override
    public void setAudioQuality(final int quality) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().setAudioQuality(quality);
            }
        });
    }

    @Override
    public void setVoiceEarMonitorEnable(final boolean enable) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().enableAudioEarMonitoring(enable);
            }
        });
    }

    /**
     * 静音本地音频
     *
     * @param mute 是否静音
     */
    @Override
    public void muteLocalAudio(final boolean mute) {
        TRTCLogger.i(TAG, "mute local audio, mute:" + mute);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().muteLocalAudio(mute);
            }
        });
    }

    @Override
    public void setSpeaker(final boolean useSpeaker) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().setSpeaker(useSpeaker);
            }
        });
    }

    @Override
    public void setAudioCaptureVolume(final int volume) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().setAudioCaptureVolume(volume);
            }
        });
    }

    @Override
    public void setAudioPlayoutVolume(final int volume) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomService.getInstance().setAudioPlayoutVolume(volume);
            }
        });
    }

    /**
     * 静音远端音频
     *
     * @param userId 用户ID
     * @param mute   是否静音
     */
    @Override
    public void muteRemoteAudio(final String userId, final boolean mute) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "mute trtc audio, user id:" + userId);
                TRTCChorusRoomService.getInstance().muteRemoteAudio(userId, mute);
            }
        });
    }

    /**
     * 静音所有远端音频
     *
     * @param mute 是否静音
     */
    @Override
    public void muteAllRemoteAudio(final boolean mute) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "mute all trtc remote audio success, mute:" + mute);
                TRTCChorusRoomService.getInstance().muteAllRemoteAudio(mute);
            }
        });
    }

    @Override
    public TXAudioEffectManager getAudioEffectManager() {
        return TRTCChorusRoomService.getInstance().getAudioEffectManager();
    }

    @Override
    public void sendRoomTextMsg(final String message, final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "sendRoomTextMsg");
                TXRoomService.getInstance().sendRoomTextMsg(message, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void sendRoomCustomMsg(final String cmd, final String message,
                                  final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "sendRoomCustomMsg");
                TXRoomService.getInstance().sendRoomCustomMsg(cmd, message, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public String sendInvitation(final String cmd, final String userId, final String content,
                                 final TRTCChorusRoomCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "sendInvitation to " + userId + " cmd:" + cmd + " content:" + content);
        return TXRoomService.getInstance().sendInvitation(cmd, userId, content, new TXCallback() {
            @Override
            public void onCallback(final int code, final String msg) {
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onCallback(code, msg);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void acceptInvitation(final String id, final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "acceptInvitation " + id);
                TXRoomService.getInstance().acceptInvitation(id, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void rejectInvitation(final String id, final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "rejectInvitation " + id);
                TXRoomService.getInstance().rejectInvitation(id, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void cancelInvitation(final String id, final TRTCChorusRoomCallback.ActionCallback callback) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                TRTCLogger.i(TAG, "cancelInvitation " + id);
                TXRoomService.getInstance().cancelInvitation(id, new TXCallback() {
                    @Override
                    public void onCallback(final int code, final String msg) {
                        runOnDelegateThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCallback(code, msg);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onRoomDestroy(final String roomId) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                exitRoom(null);
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mDelegate != null) {
                            mDelegate.onRoomDestroy(roomId);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onRoomRecvRoomTextMsg(final String roomId, final String message, final TXUserInfo userInfo) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    TRTCChorusRoomDef.UserInfo throwUser = new TRTCChorusRoomDef.UserInfo();
                    throwUser.userId = userInfo.userId;
                    throwUser.userName = userInfo.userName;
                    throwUser.userAvatar = userInfo.avatarURL;
                    mDelegate.onRecvRoomTextMsg(message, throwUser);
                }
            }
        });
    }

    @Override
    public void onRoomRecvRoomCustomMsg(String roomId, final String cmd, final String message,
                                        final TXUserInfo userInfo) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    TRTCChorusRoomDef.UserInfo throwUser = new TRTCChorusRoomDef.UserInfo();
                    throwUser.userId = userInfo.userId;
                    throwUser.userName = userInfo.userName;
                    throwUser.userAvatar = userInfo.avatarURL;
                    mDelegate.onRecvRoomCustomMsg(cmd, message, throwUser);
                }
            }
        });
    }

    @Override
    public void onRoomInfoChange(final TXRoomInfo tXRoomInfo) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                TRTCChorusRoomDef.RoomInfo roomInfo = new TRTCChorusRoomDef.RoomInfo();
                roomInfo.roomName = tXRoomInfo.roomName;
                int translateRoomId = Integer.valueOf(mRoomId);
                try {
                    translateRoomId = Integer.parseInt(tXRoomInfo.roomId);
                } catch (NumberFormatException e) {
                    TRTCLogger.e(TAG, e.getMessage());
                }
                roomInfo.roomId = translateRoomId;
                roomInfo.ownerId = tXRoomInfo.ownerId;
                roomInfo.ownerName = tXRoomInfo.ownerName;
                roomInfo.coverUrl = tXRoomInfo.cover;
                roomInfo.memberCount = tXRoomInfo.memberCount;
                roomInfo.needRequest = (tXRoomInfo.needRequest == 1);
                if (mDelegate != null) {
                    mDelegate.onRoomInfoChange(roomInfo);
                }
                mPlayUrl = tXRoomInfo.playUrl;
                //主播不需要去播放
                if (mRole != TRTCCloudDef.TRTCRoleAudience) {
                    return;
                }
                if (mTRTCChorusManager.isCdnPlaying()) {
                    mTRTCChorusManager.stopCdnPlay();
                }
                mTRTCChorusManager.startCdnPlay(mPlayUrl, mVideoView);
            }
        });
    }

    @Override
    public void onSeatInfoListChange(final List<TXSeatInfo> tXSeatInfoList) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                List<TRTCChorusRoomDef.SeatInfo> seatInfoList = new ArrayList<>();
                for (TXSeatInfo seatInfo : tXSeatInfoList) {
                    TRTCChorusRoomDef.SeatInfo info = new TRTCChorusRoomDef.SeatInfo();
                    info.userId = seatInfo.user;
                    info.mute = seatInfo.mute;
                    info.status = seatInfo.status;
                    seatInfoList.add(info);
                }
                mSeatInfoList = seatInfoList;
                if (mDelegate != null) {
                    mDelegate.onSeatListChange(seatInfoList);
                }
            }
        });
    }

    @Override
    public void onRoomAudienceEnter(final TXUserInfo userInfo) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    TRTCChorusRoomDef.UserInfo throwUser = new TRTCChorusRoomDef.UserInfo();
                    throwUser.userId = userInfo.userId;
                    throwUser.userName = userInfo.userName;
                    throwUser.userAvatar = userInfo.avatarURL;
                    mDelegate.onAudienceEnter(throwUser);
                }
            }
        });
    }

    @Override
    public void onRoomAudienceLeave(final TXUserInfo userInfo) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    TRTCChorusRoomDef.UserInfo throwUser = new TRTCChorusRoomDef.UserInfo();
                    throwUser.userId = userInfo.userId;
                    throwUser.userName = userInfo.userName;
                    throwUser.userAvatar = userInfo.avatarURL;
                    mDelegate.onAudienceExit(throwUser);
                }
            }
        });
    }

    @Override
    public void onSeatTake(final int index, final TXUserInfo userInfo) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (userInfo.userId.equals(mUserId)) {
                    //是自己上线了, 切换角色
                    mTakeSeatIndex = index;
                    TRTCChorusRoomService.getInstance().switchToAnchor();
                    boolean mute = mSeatInfoList.get(index).mute;
                    TRTCChorusRoomService.getInstance().muteLocalAudio(mute);
                    if (!mute) {
                        mDelegate.onUserMicrophoneMute(userInfo.userId, false);
                    }
                }
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mDelegate != null) {
                            TRTCChorusRoomDef.UserInfo info = new TRTCChorusRoomDef.UserInfo();
                            info.userId = userInfo.userId;
                            info.userAvatar = userInfo.avatarURL;
                            info.userName = userInfo.userName;
                            mDelegate.onAnchorEnterSeat(index, info);
                        }
                        if (mPickSeatCallback != null) {
                            mPickSeatCallback.onCallback(0, "pick seat success");
                            mPickSeatCallback = null;
                        }
                    }
                });
                if (userInfo.userId.equals(mUserId)) {
                    //在回调出去
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mEnterSeatCallback != null) {
                                mEnterSeatCallback.onCallback(0, "enter seat success");
                                mEnterSeatCallback = null;
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onSeatClose(final int index, final boolean isClose) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mTakeSeatIndex == index && isClose) {
                    TRTCChorusRoomService.getInstance().switchToAudience();
                    mTakeSeatIndex = -1;
                }
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mDelegate != null) {
                            mDelegate.onSeatClose(index, isClose);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onSeatLeave(final int index, final TXUserInfo userInfo) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (userInfo.userId.equals(mUserId)) {
                    //自己下线了~
                    mTakeSeatIndex = -1;
                    TRTCChorusRoomService.getInstance().switchToAudience();
                }
                runOnDelegateThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mDelegate != null) {
                            TRTCChorusRoomDef.UserInfo info = new TRTCChorusRoomDef.UserInfo();
                            info.userId = userInfo.userId;
                            info.userAvatar = userInfo.avatarURL;
                            info.userName = userInfo.userName;
                            mDelegate.onAnchorLeaveSeat(index, info);
                        }
                        if (mKickSeatCallback != null) {
                            mKickSeatCallback.onCallback(0, "kick seat success");
                            mKickSeatCallback = null;
                        }
                    }
                });
                if (userInfo.userId.equals(mUserId)) {
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mLeaveSeatCallback != null) {
                                mLeaveSeatCallback.onCallback(0, "enter seat success");
                                mLeaveSeatCallback = null;
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onSeatMute(final int index, final boolean mute) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onSeatMute(index, mute);
                }
            }
        });
    }

    @Override
    public void onReceiveNewInvitation(final String id, final String inviter, final String cmd, final String content) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onReceiveNewInvitation(id, inviter, cmd, content);
                }
            }
        });
    }

    @Override
    public void onInviteeAccepted(final String id, final String invitee) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onInviteeAccepted(id, invitee);
                }
            }
        });
    }

    @Override
    public void onInviteeRejected(final String id, final String invitee) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onInviteeRejected(id, invitee);
                }
            }
        });
    }

    @Override
    public void onInvitationCancelled(final String id, final String inviter) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onInvitationCancelled(id, inviter);
                }
            }
        });
    }

    @Override
    public void onTRTCAnchorEnter(String userId) {
        mAnchorList.add(userId);
    }

    @Override
    public void onTRTCAnchorExit(String userId) {
        if (TXRoomService.getInstance().isOwner()) {
            // 主播是房主
            if (mSeatInfoList != null) {
                int kickSeatIndex = -1;
                for (int i = 0; i < mSeatInfoList.size(); i++) {
                    if (userId.equals(mSeatInfoList.get(i).userId)) {
                        kickSeatIndex = i;
                        break;
                    }
                }
                if (kickSeatIndex != -1) {
                    kickSeat(kickSeatIndex, null);
                }
            }
        }
        mAnchorList.remove(userId);
    }

    @Override
    public void onTRTCVideoAvailable(final String userId, final boolean available) {
        TRTCLogger.d(TAG, "onTRTCVideoAvailable userId = " + userId + " , available = " + available);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (available) {
                    startRemoteView(userId, null);
                } else {
                    stopRemoteView(userId);
                }
            }
        });
    }

    @Override
    public void onTRTCAudioAvailable(final String userId, final boolean available) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onUserMicrophoneMute(userId, !available);
                }
            }
        });
    }

    @Override
    public void onError(final int errorCode, final String errorMsg) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onError(errorCode, errorMsg);
                }
            }
        });
    }

    @Override
    public void onNetworkQuality(final TRTCCloudDef.TRTCQuality trtcQuality,
                                 final ArrayList<TRTCCloudDef.TRTCQuality> arrayList) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null) {
                    mDelegate.onNetworkQuality(trtcQuality, arrayList);
                }
            }
        });
    }

    @Override
    public void onUserVoiceVolume(final ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, final int totalVolume) {
        runOnDelegateThread(new Runnable() {
            @Override
            public void run() {
                if (mDelegate != null && userVolumes != null) {
                    mDelegate.onUserVolumeUpdate(userVolumes, totalVolume);
                }
            }
        });
    }

    @Override
    public void startPlayMusic(int id, String url) {
        mTRTCChorusManager.startChorus(id, url);
    }

    @Override
    public void stopPlayMusic() {
        mTRTCChorusManager.stopChorus();
    }

    @Override
    public void pausePlayMusic() {

    }

    @Override
    public void resumePlayMusic() {

    }

    public void startRemoteView(final String userId, final TXCloudVideoView view) {
        TRTCChorusRoomService.getInstance().startRemoteView(userId, view);
    }

    public void stopRemoteView(final String userId) {
        TRTCChorusRoomService.getInstance().stopRemoteView(userId);
    }

    public void enableRealtimeChorus(int status) {
        TRTCLogger.d(TAG, "enableRealtimeChorus : " + status);
        TRTCChorusRoomService.getInstance().enableRealtimeChorus(status);
    }

    @Override
    public void onRecvCustomCmdMsg(String userId, final int cmdID, int seq, final byte[] message) {
        //onRecvCustomCmdMsg监听目前只在TRTCChorusRoomService中实现，回调至此并传给TRTCChorusManager进行合唱校准
        mTRTCChorusManager.onRecvCustomCmdMsg(userId, cmdID, seq, message);
    }

    //校准时间
    private void updateNtpTime() {
        //调用TXLivebase的updateNetworkTime启动一次ntp校时；
        //启动校时成功后，正常情况下回调errCode为0，说明ntp校时准确；如果多次校时后仍不准确，errCode为1；
        TXLiveBase.setListener(new TXLiveBaseListener() {
            @Override
            public void onUpdateNetworkTime(int errCode, String errMsg) {
                if (errCode != 0) {
                    //重新校验
                    TXLiveBase.updateNetworkTime();
                    TRTCLogger.d(TAG, "onUpdateNetworkTime: ntp time update failed = " + errCode);
                }
            }
        });
        TXLiveBase.updateNetworkTime();
    }

    /**
     * TRTCChorusManager合唱相关回调
     */
    private class ChorusManagerListener implements TRTCChorusManager.TRTCChorusListener {
        /**
         * 合唱开始回调
         *
         * @param reason 合唱开始原因，参考ChorusStartReason
         */
        @Override
        public void onChorusStart(TRTCChorusManager.ChorusStartReason reason) {

        }

        @Override
        public void onChorusStop(TRTCChorusManager.ChorusStopReason reason) {

        }

        @Override
        public void onCdnPushStatusUpdate(TRTCChorusManager.CdnPushStatus status) {

        }

        @Override
        public void onCdnPlayStatusUpdate(TRTCChorusManager.CdnPlayStatus status) {

        }

        @Override
        public void onMusicPrepareToPlay(final int musicID) {
            TRTCLogger.d(TAG, "onMusicPrepareToPlay musicID = " + musicID);
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mDelegate != null) {
                        mDelegate.onMusicPrepareToPlay(musicID);
                    }

                }
            });
        }

        @Override
        public void onChorusProgress(final int musicID, final long curPtsMS, final long durationMS) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mDelegate != null) {
                                mDelegate.onMusicProgressUpdate(musicID, curPtsMS, durationMS);
                            }
                        }

                    });
                }
            });
        }

        @Override
        public void onMusicCompletePlaying(final int musicID) {
            TRTCLogger.d(TAG, "onMusicCompletePlaying musicID = " + musicID);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    runOnDelegateThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mDelegate != null) {
                                mDelegate.onMusicCompletePlaying(musicID);
                            }
                        }
                    });
                }
            });
        }

        @Override
        public void onReceiveAnchorSendChorusMsg(final String musicID, final long startDelay) {
            runOnDelegateThread(new Runnable() {
                @Override
                public void run() {
                    if (mDelegate != null) {
                        //回调给上层查询音乐路径
                        mDelegate.onReceiveAnchorSendChorusMsg(String.valueOf(musicID), startDelay);
                        TRTCLogger.d(TAG, "onReceiveAnchorSendChorusMsg:" + musicID);
                    }
                }
            });
        }
    }
}
