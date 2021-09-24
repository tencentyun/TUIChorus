package com.tencent.liteav.tuichorus.ui.room;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.basic.UserModel;
import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDef;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomManager;
import com.tencent.liteav.tuichorus.model.impl.base.TRTCLogger;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;
import com.tencent.liteav.tuichorus.ui.base.MemberEntity;
import com.tencent.liteav.tuichorus.ui.music.impl.ChorusMusicView;
import com.tencent.liteav.tuichorus.ui.widget.CommonBottomDialog;
import com.tencent.liteav.tuichorus.ui.widget.ConfirmDialogFragment;
import com.tencent.liteav.tuichorus.ui.widget.SelectMemberView;
import com.tencent.liteav.tuichorus.ui.widget.msg.MsgEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChorusRoomAnchorActivity extends ChorusRoomBaseActivity implements SelectMemberView.onSelectedCallback {
    public static final int ERROR_ROOM_ID_EXIT = -1301;

    // 用户消息的map
    private Map<String, String>         mTakeSeatInvitationMap;
    // 邀请人上麦的map
    private Map<String, SeatInvitation> mPickSeatInvitationMap;
    private boolean                     mIsEnterRoom;
    private String                      mPushUrl;
    private String                      mPlayUrl;

    private static final String KEY_CHORUS_PUSH_URL = "push_url";
    private static final String KEY_CHORUS_PLAY_URL = "play_url";

    /**
     * 创建房间
     */
    public static void createRoom(Context context, String roomName, String userId,
                                  String userName, String coverUrl, int audioQuality, boolean needRequest, String pushUrl, String playUrl) {
        Intent intent = new Intent(context, ChorusRoomAnchorActivity.class);
        intent.putExtra(CHORUSROOM_ROOM_NAME, roomName);
        intent.putExtra(CHORUSROOM_USER_ID, userId);
        intent.putExtra(CHORUSROOM_USER_NAME, userName);
        intent.putExtra(CHORUSROOM_AUDIO_QUALITY, audioQuality);
        intent.putExtra(CHORUSROOM_ROOM_COVER, coverUrl);
        intent.putExtra(CHORUSROOM_NEED_REQUEST, needRequest);
        intent.putExtra(KEY_CHORUS_PUSH_URL, pushUrl);
        intent.putExtra(KEY_CHORUS_PLAY_URL, playUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAnchor();
    }

    @Override
    public void onBackPressed() {
        if (mIsEnterRoom) {
            showExitRoom();
        } else {
            finish();
        }
    }

    private void showExitRoom() {
        if (mConfirmDialogFragment.isAdded()) {
            mConfirmDialogFragment.dismiss();
        }
        mConfirmDialogFragment.setMessage(getString(R.string.tuichorus_anchor_leave_room));
        mConfirmDialogFragment.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
            @Override
            public void onClick() {
                mConfirmDialogFragment.dismiss();
            }
        });
        mConfirmDialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                mConfirmDialogFragment.dismiss();
                destroyRoom();
                finish();
            }
        });
        mConfirmDialogFragment.show(getFragmentManager(), "confirm_fragment");
    }

    private void destroyRoom() {
        mTRTCChorusRoom.destroyRoom(new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    TRTCLogger.d(TAG, "IM destroy room success");
                } else {
                    TRTCLogger.d(TAG, "IM destroy room failed:" + msg);
                }
            }
        });

        TRTCChorusRoomManager.getInstance().destroyRoom(mRoomId, new TRTCChorusRoomManager.ActionCallback() {
            @Override
            public void onSuccess() {
                TRTCLogger.d(TAG, "destroy room success");
            }

            @Override
            public void onError(int errorCode, String message) {
                TRTCLogger.d(TAG, "destroy room failed:" + message);
            }
        });
        mTRTCChorusRoom.setDelegate(null);
    }

    /**
     * 主播的逻辑
     */
    private void initAnchor() {
        mChorusRoomInfoController.setRoomOwnerId(mSelfUserId);
        mTakeSeatInvitationMap = new HashMap<>();
        mPickSeatInvitationMap = new HashMap<>();
        mChorusRoomSeatAdapter.notifyDataSetChanged();
        mViewSelectMember.setList(mMemberEntityList);
        mViewSelectMember.setOnSelectedCallback(this);

        mPushUrl = getIntent().getStringExtra(KEY_CHORUS_PUSH_URL);
        mPlayUrl = getIntent().getStringExtra(KEY_CHORUS_PLAY_URL);
        mRoomId = getRoomId();
        setSignalVisibility(View.VISIBLE);
        //设置昵称、头像
        mTRTCChorusRoom.setSelfProfile(mUserName, mUserAvatar, null);
        PermissionUtils.permission(PermissionConstants.MICROPHONE).callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(List<String> permissionsGranted) {
                internalCreateRoom();
            }

            @Override
            public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                ToastUtils.showShort(R.string.tuichorus_tips_open_audio);
            }
        }).request();
    }

    private void internalCreateRoom() {
        final TRTCChorusRoomDef.RoomParam roomParam = new TRTCChorusRoomDef.RoomParam();
        roomParam.roomName = mRoomName;
        roomParam.needRequest = mNeedRequest;
        roomParam.seatCount = MAX_SEAT_SIZE;
        roomParam.coverUrl = mRoomCover;
        roomParam.mPushUrl = mPushUrl;
        roomParam.mPlayUrl = mPlayUrl;

        TRTCChorusRoomDef.RoomInfo roomInfo = new TRTCChorusRoomDef.RoomInfo();
        roomInfo.roomId = mRoomId;
        roomInfo.ownerId = mSelfUserId;
        roomInfo.roomName = mRoomName;
        mChorusMusicService.setRoomInfo(roomInfo);

        mTRTCChorusRoom.createRoom(mRoomId, roomParam, mVideoView, new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    onTRTCRoomCreateSuccess();
                }
            }
        });
        //房间创建完成后
        chorusMusicImplComplete();
        // 刷新界面
        refreshView();
        mChorusMusicView.setMsgListener(new ChorusMusicView.ChorusMusicMsgDelegate() {
            @Override
            public void sendOrderMsg(ChorusMusicModel model) {
                updateMsg(model);
            }
        });
    }

    private void onTRTCRoomCreateSuccess() {
        mIsEnterRoom = true;
        mTvRoomName.setText(mRoomName);
        mTvRoomId.setText(getString(R.string.tuichorus_room_id, mRoomId));
        mTRTCChorusRoom.setAudioQuality(mAudioQuality);
        //房主进房就占用一个座位,且不能下麦
        takeMainSeat();
        TRTCChorusRoomManager.getInstance().createRoom(mRoomId, new TRTCChorusRoomManager.ActionCallback() {
            @Override
            public void onSuccess() {
                TRTCLogger.d(TAG, "create room success");
            }

            @Override
            public void onError(int errorCode, String message) {
                if (errorCode == ERROR_ROOM_ID_EXIT) {
                    onSuccess();
                } else {
                    ToastUtils.showLong("create room failed[" + errorCode + "]:" + message);
                    finish();
                }
            }
        });
    }

    private void takeMainSeat() {
        // 开始创建房间
        mTRTCChorusRoom.enterSeat(0, new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    //成功上座位，可以展示UI了
                    ToastUtils.showLong(getString(R.string.tuichorus_toast_owner_succeeded_in_occupying_the_seat));
                } else {
                    ToastUtils.showLong(getString(R.string.tuichorus_toast_owner_failed_to_occupy_the_seat) + "[" + code + "]:" + msg);
                }
            }
        });
    }

    private int getRoomId() {
        // 这里我们用简单的 userId hashcode，然后取余
        // 您的room id应该是您后台生成的唯一值
        return (mSelfUserId + "_voice_room").hashCode() & 0x7FFFFFFF;
    }

    /**
     * 房主点击座位列表
     *
     * @param itemPos
     */
    @Override
    public void onItemClick(final int itemPos) {
        // 判断座位有没有人
        ChorusRoomSeatEntity entity = mRoomSeatEntityList.get(itemPos);
        if (entity.isUsed) {
            if (entity.userId.equals(mSelfUserId)) {
                return;
            }
            if (mIsChorusOn) {
                ToastUtils.showLong(R.string.tuichorus_toast_chorus_can_not_leave_seat);
                return;
            }
            // 其他主播,弹出踢人
            final CommonBottomDialog dialog = new CommonBottomDialog(this);
            dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                @Override
                public void onClick(int position, String text) {
                    dialog.dismiss();
                    mTRTCChorusRoom.kickSeat(changeSeatIndexToModelIndex(itemPos), null);
                }
            }, getString(R.string.tuichorus_leave_seat));
            dialog.show();
        } else {
            //座位没有人时弹出拉人上麦
            if (mViewSelectMember != null) {
                //设置一下邀请的座位号
                mViewSelectMember.setSeatIndex(itemPos);
                mViewSelectMember.updateCloseStatus(entity.isClose);
                mViewSelectMember.show();
            }
        }

    }

    @Override
    public void onAudienceEnter(TRTCChorusRoomDef.UserInfo userInfo) {
        super.onAudienceEnter(userInfo);
        if (userInfo.userId.equals(mSelfUserId)) {
            return;
        }
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.userId = userInfo.userId;
        memberEntity.userAvatar = userInfo.userAvatar;
        memberEntity.userName = userInfo.userName;
        memberEntity.type = MemberEntity.TYPE_IDEL;
        if (!mMemberEntityMap.containsKey(memberEntity.userId)) {
            mMemberEntityMap.put(memberEntity.userId, memberEntity);
            mMemberEntityList.add(memberEntity);
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAudienceExit(TRTCChorusRoomDef.UserInfo userInfo) {
        super.onAudienceExit(userInfo);
        MemberEntity entity = mMemberEntityMap.remove(userInfo.userId);
        if (entity != null) {
            mMemberEntityList.remove(entity);
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAnchorEnterSeat(int index, TRTCChorusRoomDef.UserInfo user) {
        super.onAnchorEnterSeat(index, user);
        MemberEntity entity = mMemberEntityMap.get(user.userId);
        if (entity != null) {
            entity.type = MemberEntity.TYPE_IN_SEAT;
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAnchorLeaveSeat(int index, TRTCChorusRoomDef.UserInfo user) {
        super.onAnchorLeaveSeat(index, user);
        MemberEntity entity = mMemberEntityMap.get(user.userId);
        if (entity != null) {
            entity.type = MemberEntity.TYPE_IDEL;
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAgreeClick(int position) {
        super.onAgreeClick(position);
        if (mMsgEntityList != null) {
            final MsgEntity entity   = mMsgEntityList.get(position);
            String          inviteId = entity.invitedId;
            if (inviteId == null) {
                ToastUtils.showLong(getString(R.string.tuichorus_request_expired));
                return;
            }
            mTRTCChorusRoom.acceptInvitation(inviteId, new TRTCChorusRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        entity.type = MsgEntity.TYPE_AGREED;
                        mMsgListAdapter.notifyDataSetChanged();
                    } else {
                        ToastUtils.showShort(getString(R.string.tuichorus_accept_failed) + code);
                    }
                }
            });
        }
    }

    @Override
    public void onReceiveNewInvitation(String id, String inviter, String cmd, String content) {
        super.onReceiveNewInvitation(id, inviter, cmd, content);
        if (cmd.equals(TCConstants.CMD_REQUEST_TAKE_SEAT)) {
            recvTakeSeat(id, inviter, content);
        }
    }

    private void recvTakeSeat(String inviteId, String inviter, String content) {
        //收到了听众的申请上麦消息，显示到通知栏
        MemberEntity memberEntity = mMemberEntityMap.get(inviter);
        MsgEntity    msgEntity    = new MsgEntity();
        msgEntity.userId = inviter;
        msgEntity.invitedId = inviteId;
        msgEntity.userName = (memberEntity != null ? memberEntity.userName : inviter);
        msgEntity.type = MsgEntity.TYPE_WAIT_AGREE;
        int seatIndex = Integer.parseInt(content);
        msgEntity.content = getString(R.string.tuichorus_msg_apply_for_chat, seatIndex + 1);
        if (memberEntity != null) {
            memberEntity.type = MemberEntity.TYPE_WAIT_AGREE;
        }
        mTakeSeatInvitationMap.put(inviter, inviteId);
        mViewSelectMember.notifyDataSetChanged();
        showImMsg(msgEntity);
    }

    private void updateMsg(ChorusMusicModel entity) {
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.invitedId = TCConstants.CMD_ORDER_SONG;
        msgEntity.type = MsgEntity.TYPE_ORDERED_SONG;

        int    seatIndex = 0;
        String userName  = null;
        for (int i = 0; i < MAX_SEAT_SIZE; i++) {
            if (entity.bookUser.equals(mRoomSeatEntityList.get(i).userId)) {
                seatIndex = i;
                userName = mRoomSeatEntityList.get(i).userName;
                break;
            }
        }
        msgEntity.userName = userName;
        msgEntity.content = getString(R.string.tuichorus_msg_order_song_seat, seatIndex + 1);
        msgEntity.linkUrl = getString(R.string.tuichorus_msg_order_song, entity.musicName);
        showImMsg(msgEntity);
    }

    @Override
    public void onOrderedManagerClick(int position) {
        super.onOrderedManagerClick(position);
        if (mMsgEntityList != null) {
            final MsgEntity entity   = mMsgEntityList.get(position);
            String          inviteId = entity.invitedId;
            if (inviteId == null) {
                ToastUtils.showLong(getString(R.string.tuichorus_request_expired));
                return;
            }
            //主播点歌后,房主在消息中拉起点歌/已点面板
            mChorusMusicView.showMusicDialog(true);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.NONE;
        if (mAudioManager != null) {
            mAudioManager.reset();
            mAudioManager.unInit();
            mAudioManager = null;
        }
        if (mChorusMusicService != null) {
            mChorusMusicService.onExitRoom();
            mChorusMusicService = null;
        }
    }

    /**
     * mViewSelectMember 的回调函数
     * 主播选择了听众进行邀请操作
     *
     * @param seatIndex
     * @param memberEntity
     */
    @Override
    public void onSelected(int seatIndex, final MemberEntity memberEntity) {
        // 座位号 seat index 上 选择了某个用户进行邀请
        ChorusRoomSeatEntity seatEntity = mRoomSeatEntityList.get(seatIndex);
        if (seatEntity.isUsed) {
            return;
        }
        if (memberEntity.type == MemberEntity.TYPE_WAIT_AGREE) {
            //这个用户已经发过申请了，那么进行同意操作，取最后一次收到消息的情况
            String inviteId = mTakeSeatInvitationMap.get(memberEntity.userId);
            if (inviteId == null) {
                ToastUtils.showLong(R.string.tuichorus_request_expired);
                memberEntity.type = MemberEntity.TYPE_IDEL;
                mViewSelectMember.notifyDataSetChanged();
                return;
            }
            mTRTCChorusRoom.acceptInvitation(inviteId, new TRTCChorusRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        for (MsgEntity msgEntity : mMsgEntityList) {
                            if (msgEntity.userId != null && msgEntity.userId.equals(memberEntity.userId)) {
                                msgEntity.type = MsgEntity.TYPE_AGREED;
                                break;
                            }
                        }
                        mMsgListAdapter.notifyDataSetChanged();
                    } else {
                        ToastUtils.showShort(getString(R.string.tuichorus_request_expired) + code);
                        memberEntity.type = MemberEntity.TYPE_IDEL;
                        mViewSelectMember.notifyDataSetChanged();
                    }
                }
            });
            // 这里也清空一下msg list里面对应的听众信息
            for (MsgEntity msgEntity : mMsgEntityList) {
                if (msgEntity.userId == null) {
                    continue;
                }
                if (msgEntity.userId.equals(memberEntity.userId)) {
                    msgEntity.type = MsgEntity.TYPE_AGREED;
                    mTakeSeatInvitationMap.remove(msgEntity.invitedId);
                }
            }
            mMsgListAdapter.notifyDataSetChanged();
            return;
        }

        SeatInvitation seatInvitation = new SeatInvitation();
        seatInvitation.inviteUserId = memberEntity.userId;
        seatInvitation.seatIndex = seatIndex;
        String inviteId = mTRTCChorusRoom.sendInvitation(TCConstants.CMD_PICK_UP_SEAT, seatInvitation.inviteUserId,
                String.valueOf(changeSeatIndexToModelIndex(seatIndex)), new TRTCChorusRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.d(TAG, "onCallback: code = " + code + " , msg = " + msg);
                    }
                });
        mPickSeatInvitationMap.put(inviteId, seatInvitation);
        mViewSelectMember.dismiss();
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onCloseButtonClick(int seatIndex) {
        onCloseSeatClick(seatIndex);
    }

    private void onCloseSeatClick(int itemPos) {
        ChorusRoomSeatEntity entity = mRoomSeatEntityList.get(itemPos);
        if (entity == null) {
            return;
        }
        final boolean isClose = entity.isClose;
        mTRTCChorusRoom.closeSeat(changeSeatIndexToModelIndex(itemPos), !isClose, new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    mViewSelectMember.updateCloseStatus(!isClose);
                }
            }
        });
    }

    private static class SeatInvitation {
        int    seatIndex;
        String inviteUserId;
    }

    @Override
    public void onInviteeAccepted(String id, final String invitee) {
        super.onInviteeAccepted(id, invitee);
        // 抱麦的用户同意了，先获取一下之前的消息
        SeatInvitation seatInvitation = mPickSeatInvitationMap.get(id);
        if (seatInvitation != null) {
            ChorusRoomSeatEntity entity = mRoomSeatEntityList.get(seatInvitation.seatIndex);
            if (entity.isUsed) {
                Log.e(TAG, "seat " + seatInvitation.seatIndex + " already used");
                return;
            }
            mTRTCChorusRoom.pickSeat(changeSeatIndexToModelIndex(seatInvitation.seatIndex), seatInvitation.inviteUserId, new TRTCChorusRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        //ToastUtils.showLong(getString(R.string.tuichorus_toast_invite_to_chat_successfully, invitee));
                    }
                }
            });
        } else {
            Log.e(TAG, "onInviteeAccepted: " + id + " user:" + invitee + " not this people");
        }
    }

    @Override
    public void onInviteeRejected(String id, String invitee) {
        super.onInviteeRejected(id, invitee);
        SeatInvitation seatInvitation = mPickSeatInvitationMap.remove(id);
        if (seatInvitation != null) {
            MemberEntity entity = mMemberEntityMap.get(seatInvitation.inviteUserId);
            if (entity != null) {
                ToastUtils.showShort(getString(R.string.tuichorus_toast_refuse_to_chat, entity.userName));
            }
        }
    }

    @Override
    public void onRoomDestroy(String roomId) {
        super.onRoomDestroy(roomId);
        finish();
    }
}
