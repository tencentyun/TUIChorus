package com.tencent.liteav.tuichorus.ui.room;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.debug.BuildConfig;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDef;
import com.tencent.liteav.tuichorus.model.impl.base.TRTCLogger;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;
import com.tencent.liteav.tuichorus.ui.floatwindow.FloatActivity;
import com.tencent.liteav.tuichorus.ui.floatwindow.FloatWindow;
import com.tencent.liteav.tuichorus.ui.floatwindow.PermissionListener;
import com.tencent.liteav.tuichorus.ui.utils.PermissionHelper;
import com.tencent.liteav.tuichorus.ui.widget.CommonBottomDialog;
import com.tencent.liteav.tuichorus.ui.widget.ConfirmDialogFragment;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUILoginListener;
import com.tencent.trtc.TRTCCloudDef;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 听众界面
 */
public class ChorusRoomAudienceActivity extends ChorusRoomBaseActivity {
    private static final int MSG_DISMISS_LOADING = 1001;

    private        Map<String, Integer>     mInvitationSeatMap;
    private        String                   mOwnerId;
    private        boolean                  mIsSeatInitSuccess;
    private        int                      mSelfSeatIndex;
    private        ConfirmDialogFragment    mAlertDialog;
    private static ChorusAudienceRoomEntity mCollectEntity;
    public static  ChorusAudienceRoomEntity mLastEntity;
    private        boolean                  mRoomDestroy;
    private        boolean                  mIsTakingSeat;        // 正在进行上麦
    private        boolean                  mFinEnterRoom = true; // 默认已完成进房流程

    public static void enterRoom(final Context context, final int roomId, final String userId, final int audioQuality) {
        //保存房间信息
        mCollectEntity = new ChorusAudienceRoomEntity();
        mCollectEntity.roomId = roomId;
        mCollectEntity.userId = userId;
        mCollectEntity.audioQuality = audioQuality;

        FloatWindow.getInstance().setRoomInfo(mCollectEntity);
        if (mLastEntity != null && mLastEntity.roomId == roomId) {
            FloatWindow.getInstance().hide();
        } else {
            if (FloatWindow.mIsShowing) {
                FloatWindow.getInstance().destroy();
            }
        }

        Intent starter = new Intent(context, ChorusRoomAudienceActivity.class);
        starter.putExtra(CHORUSROOM_ROOM_ID, roomId);
        starter.putExtra(CHORUSROOM_USER_ID, userId);
        starter.putExtra(CHORUSROOM_AUDIO_QUALITY, audioQuality);
        context.startActivity(starter);
    }

    private void enterRoom() {
        mIsSeatInitSuccess = false;
        mSelfSeatIndex = -1;
        mCurrentRole = TRTCCloudDef.TRTCRoleAudience;
        mTRTCChorusRoom.setSelfProfile(mUserName, mUserAvatar, null);
        mFinEnterRoom = false;
        mTRTCChorusRoom.enterRoom(mRoomId, mVideoView, new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                mFinEnterRoom = true;
                if (code == 0) {
                    //进房成功
                    ToastUtils.showShort(R.string.tuichorus_toast_enter_the_room_successfully);
                    mTRTCChorusRoom.setAudioQuality(mAudioQuality);
                } else {
                    ToastUtils.showShort(getString(R.string.tuichorus_toast_enter_the_room_failure, code, msg));
                    finish();
                }
            }
        });
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_DISMISS_LOADING) {
                mHandler.removeMessages(MSG_DISMISS_LOADING);
                mProgressBar.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAudience();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TUILogin.removeLoginListener(mTUILoginListener);
    }

    private void showTakingSeatLoading(boolean isShow) {
        mIsTakingSeat = isShow;
        mProgressBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
        if (isShow) {
            mHandler.sendEmptyMessageDelayed(MSG_DISMISS_LOADING, 10000);
        } else {
            mHandler.removeMessages(MSG_DISMISS_LOADING);
        }
    }

    private void showFloatWindow() {
        if (mRoomDestroy) {
            mLastEntity = null;
            FloatWindow.getInstance().destroy();
        } else {
            if (mLastEntity != null && mLastEntity.roomId == mCollectEntity.roomId
                    && !FloatWindow.mIsDestroyByself) {
                FloatWindow.getInstance().show();
            } else {
                FloatWindow.mIsDestroyByself = false;
                FloatWindow.getInstance().init(getApplicationContext(), mChorusRoomInfoController);
                FloatWindow.getInstance().createView();
            }
            mLastEntity = mCollectEntity;
        }
    }

    @Override
    public void onBackPressed() {
        if (!mFinEnterRoom) {
            TRTCLogger.d(TAG, "you have not finish enter room,please wait a bit");
            return;
        }
        if (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor) {
            leaveSeatAndQuit();
        } else {
            super.onBackPressed();
            requestFloatWindow();
        }
    }

    //观众端:在返回或退出的时候显示悬浮窗
    private void requestFloatWindow() {
        //申请悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            FloatActivity.request(this, new PermissionListener() {
                @Override
                public void onSuccess() {
                    showFloatWindow();
                }

                @Override
                public void onFail() {
                    //没有悬浮窗权限就直接退房,回到房间列表
                    if (mTRTCChorusRoom != null) {
                        mTRTCChorusRoom.exitRoom(new TRTCChorusRoomCallback.ActionCallback() {
                            @Override
                            public void onCallback(int code, String msg) {
                                ToastUtils.showShort(R.string.tuichorus_toast_exit_the_room_successfully);
                            }
                        });
                    }

                    if (mChorusRoomInfoController != null && mChorusRoomInfoController.getMusicServiceImpl() != null) {
                        mChorusRoomInfoController.getMusicServiceImpl().onExitRoom();
                    }
                }
            });
        } else {
            showFloatWindow();
        }
    }

    private void initAudience() {
        mInvitationSeatMap = new HashMap<>();
        mChorusRoomSeatAdapter.notifyDataSetChanged();
        // 开始进房
        enterRoom();
        TUILogin.addLoginListener(mTUILoginListener);
        mBtnReport.setVisibility(BuildConfig.RTCube_APPSTORE ? View.VISIBLE : View.GONE);
        mBtnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReportDialog();
            }
        });
    }

    //下麦
    public void leaveSeatAndQuit() {
        if (mAlertDialog == null) {
            mAlertDialog = new ConfirmDialogFragment();
        }
        if (mAlertDialog.isAdded()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog.setMessage(getString(R.string.tuichorus_leave_seat_and_exit_ask));
        mAlertDialog.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                mTRTCChorusRoom.leaveSeat(new TRTCChorusRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code == 0) {
                            ToastUtils.showShort(R.string.tuichorus_toast_offline_successfully);
                        } else {
                            ToastUtils.showShort(getString(R.string.tuichorus_toast_offline_failure, msg));
                        }
                    }
                });
                mAlertDialog.dismiss();
                requestFloatWindow();
                finish();
            }
        });
        mAlertDialog.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
            @Override
            public void onClick() {
                mAlertDialog.dismiss();
            }
        });
        mAlertDialog.show(this.getFragmentManager(), "confirm_leave_seat");
    }

    @Override
    public void onSeatListChange(List<TRTCChorusRoomDef.SeatInfo> seatInfoList) {
        super.onSeatListChange(seatInfoList);
        mIsSeatInitSuccess = true;
    }

    /**
     * 点击麦位列表听众端的操作
     *
     * @param itemPos
     */
    @Override
    public void onItemClick(final int itemPos) {
        if (!mIsSeatInitSuccess) {
            ToastUtils.showLong(R.string.tuichorus_toast_list_has_not_been_initialized);
            return;
        }
        // 判断座位有没有人
        ChorusRoomSeatEntity entity = mRoomSeatEntityList.get(itemPos);
        if (entity.isClose) {
            ToastUtils.showShort(R.string.tuichorus_toast_position_is_locked_cannot_enter_seat);
        } else if (!entity.isUsed) {
            if (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor) {
                ToastUtils.showShort(R.string.tuichorus_toast_you_are_already_an_anchor);
                return;
            }
            final CommonBottomDialog dialog = new CommonBottomDialog(this);
            dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                @Override
                public void onClick(int position, String text) {
                    if (position == 0) {
                        // 发送请求之前再次判断一下这个座位有没有人
                        ChorusRoomSeatEntity seatEntity = mRoomSeatEntityList.get(itemPos);
                        if (seatEntity.isUsed) {
                            ToastUtils.showShort(R.string.tuichorus_toast_position_is_already_occupied);
                            return;
                        }
                        if (seatEntity.isClose) {
                            ToastUtils.showShort(getString(R.string.tuichorus_seat_closed));
                            return;
                        }
                        PermissionHelper.requestPermission(ChorusRoomAudienceActivity.this,
                                PermissionHelper.PERMISSION_MICROPHONE, new PermissionHelper.PermissionCallback() {
                                    @Override
                                    public void onGranted() {
                                        startTakeSeat(itemPos);
                                    }

                                    @Override
                                    public void onDenied() {

                                    }

                                    @Override
                                    public void onDialogApproved() {

                                    }

                                    @Override
                                    public void onDialogRefused() {
                                        finish();
                                    }
                                });
                    }
                    dialog.dismiss();
                }
            }, getString(R.string.tuichorus_tv_apply_for_chat));
            dialog.show();
        } else {
            //主播点击自己的头像主动下麦
            if (entity.userId.equals(mSelfUserId)) {
                leaveSeat();
            } else {
                ToastUtils.showShort(R.string.tuichorus_toast_position_is_already_occupied);
            }
        }
    }

    //上麦
    public void startTakeSeat(final int itemPos) {
        if (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor) {
            ToastUtils.showShort(R.string.tuichorus_toast_you_are_already_an_anchor);
            return;
        }

        if (mNeedRequest) {
            //需要申请上麦
            if (mOwnerId == null) {
                ToastUtils.showShort(R.string.tuichorus_toast_the_room_is_not_ready);
                return;
            }
            String inviteId = mTRTCChorusRoom.sendInvitation(TCConstants.CMD_REQUEST_TAKE_SEAT, mOwnerId,
                    String.valueOf(changeSeatIndexToModelIndex(itemPos)), new TRTCChorusRoomCallback.ActionCallback() {
                        @Override
                        public void onCallback(int code, String msg) {
                            if (code == 0) {
                                ToastUtils.showShort(R.string
                                        .tuichorus_toast_application_has_been_sent_please_wait_for_processing);
                            } else {
                                ToastUtils.showShort(getString(R.string.tuichorus_toast_failed_to_send_application,
                                        msg));
                            }
                        }
                    });
            mInvitationSeatMap.put(inviteId, itemPos);
        } else {
            //听众自动上麦
            if (mAlertDialog == null) {
                mAlertDialog = new ConfirmDialogFragment();
            }
            if (mAlertDialog.isAdded()) {
                mAlertDialog.dismiss();
            }
            mAlertDialog.setMessage(getString(R.string.tuichorus_apply_seat_ask));
            mAlertDialog.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
                @Override
                public void onClick() {
                    mAlertDialog.dismiss();
                    if (mIsTakingSeat) {
                        return;
                    }
                    showTakingSeatLoading(true);
                    mTRTCChorusRoom.enterSeat(changeSeatIndexToModelIndex(itemPos),
                            new TRTCChorusRoomCallback.ActionCallback() {
                                @Override
                                public void onCallback(int code, String msg) {
                                    if (code == 0) {
                                        //成功上座位，可以展示UI了
                                        ToastUtils.showLong(getString(R
                                                .string.tuichorus_toast_owner_succeeded_in_occupying_the_seat));
                                        setSignalVisibility(View.VISIBLE);
                                    } else {
                                        showTakingSeatLoading(false);
                                        ToastUtils.showLong(getString(R.string
                                                .tuichorus_toast_owner_failed_to_occupy_the_seat), code, msg);
                                    }
                                }
                            });
                }
            });
            mAlertDialog.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
                @Override
                public void onClick() {
                    mAlertDialog.dismiss();
                }
            });
            mAlertDialog.show(this.getFragmentManager(), "confirm_apply_seat");
        }

    }

    @Override
    public void onRoomInfoChange(TRTCChorusRoomDef.RoomInfo roomInfo) {
        super.onRoomInfoChange(roomInfo);
        mOwnerId = roomInfo.ownerId;
        mChorusRoomInfoController.setRoomOwnerId(roomInfo.ownerId);
        //进入房间后,将roominfo先传递给ChorusMusic实现类,再传递实现类给布局
        mChorusMusicService.setRoomInfo(roomInfo);
        chorusMusicImplComplete();
        // 刷新界面
        refreshView();
    }

    @Override
    public void onReceiveNewInvitation(final String id, String inviter, String cmd, final String content) {
        super.onReceiveNewInvitation(id, inviter, cmd, content);
        if (cmd.equals(TCConstants.CMD_PICK_UP_SEAT)) {
            recvPickSeat(id, cmd, content);
        }
    }

    private void recvPickSeat(final String id, String cmd, final String content) {
        //这里收到了主播抱麦的邀请
        if (mConfirmDialogFragment != null && mConfirmDialogFragment.isAdded()) {
            mConfirmDialogFragment.dismiss();
        }
        mConfirmDialogFragment = new ConfirmDialogFragment();
        final int seatIndex = Integer.parseInt(content);
        mConfirmDialogFragment.setMessage(getString(R.string.tuichorus_msg_invite_you_to_chat, seatIndex + 1));
        mConfirmDialogFragment.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
            @Override
            public void onClick() {
                mTRTCChorusRoom.rejectInvitation(id, new TRTCChorusRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.d(TAG, "rejectInvitation callback:" + code);
                        ToastUtils.showShort(R.string.tuichorus_msg_you_refuse_to_chat);
                    }
                });
                mConfirmDialogFragment.dismiss();
            }
        });
        mConfirmDialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                mInvitationSeatMap.put(id, seatIndex);
                //同意上麦，回复接受
                mTRTCChorusRoom.acceptInvitation(id, new TRTCChorusRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code != 0) {
                            ToastUtils.showShort(getString(R.string.tuichorus_toast_accept_request_failure, code));
                        }
                        Log.d(TAG, "acceptInvitation callback:" + code);
                    }
                });
                mConfirmDialogFragment.dismiss();
            }
        });
        mConfirmDialogFragment.show(getFragmentManager(), "confirm_fragment" + seatIndex);
    }

    @Override
    public void onInviteeAccepted(String id, String invitee) {
        super.onInviteeAccepted(id, invitee);
        Integer seatIndex = mInvitationSeatMap.remove(id);
        if (seatIndex != null) {
            ChorusRoomSeatEntity entity = mRoomSeatEntityList.get(seatIndex);
            if (!entity.isUsed) {
                if (mIsTakingSeat) {
                    return;
                }
                showTakingSeatLoading(true);
                mTRTCChorusRoom.enterSeat(changeSeatIndexToModelIndex(seatIndex),
                        new TRTCChorusRoomCallback.ActionCallback() {
                            @Override
                            public void onCallback(int code, String msg) {
                                if (code == 0) {
                                    TRTCLogger.d(TAG, " enter seat succeed");
                                    setSignalVisibility(View.VISIBLE);
                                } else {
                                    showTakingSeatLoading(false);
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onAnchorEnterSeat(int index, TRTCChorusRoomDef.UserInfo user) {
        super.onAnchorEnterSeat(index, user);
        if (user.userId.equals(mSelfUserId)) {
            mCurrentRole = TRTCCloudDef.TRTCRoleAnchor;
            mSelfSeatIndex = index;
            showTakingSeatLoading(false);
            refreshView();
            ToastUtils.showShort(R.string.tuichorus_put_on_your_headphones);
        }
    }

    @Override
    public void onAnchorLeaveSeat(int index, TRTCChorusRoomDef.UserInfo user) {
        super.onAnchorLeaveSeat(index, user);
        if (user.userId.equals(mSelfUserId)) {
            mCurrentRole = TRTCCloudDef.TRTCRoleAudience;
            setSignalVisibility(View.GONE);
            mSelfSeatIndex = -1;
            refreshView();
        }
    }

    @Override
    public void onRoomDestroy(String roomId) {
        super.onRoomDestroy(roomId);
        //在房间内,房主解散销毁界面,则退出房间界面,不显示悬浮窗;如果在房间外房主销毁解散房间,直接销毁悬浮窗
        if (FloatWindow.mIsShowing) {
            FloatWindow.getInstance().destroy();
            mLastEntity = null;
        } else {
            mRoomDestroy = true;
            mTRTCChorusRoom.exitRoom(new TRTCChorusRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    TRTCLogger.d(TAG, "onRoomDestroy errorCode : " + code + " , errorMsg : " + msg);
                }
            });
        }
        finish();
    }

    private void showReportDialog() {
        try {
            Class clz = Class.forName("com.tencent.liteav.demo.report.ReportDialog");
            Method method = clz.getDeclaredMethod("showReportDialog", Context.class, String.class, String.class);
            method.invoke(null, this, String.valueOf(mRoomId), mOwnerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TUILoginListener mTUILoginListener = new TUILoginListener() {
        @Override
        public void onKickedOffline() {
            Log.e(TAG, "onKickedOffline");
            mTRTCChorusRoom.exitRoom(null);
            finish();
        }
    };
}
