package com.tencent.liteav.tuichorus.ui.room;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;
import com.tencent.liteav.basic.ImageLoader;
import com.tencent.liteav.basic.UserModel;
import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;

import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDef;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDelegate;
import com.tencent.liteav.tuichorus.model.impl.base.TXSeatInfo;
import com.tencent.liteav.tuichorus.ui.audio.impl.TUIChorusAudioManager;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicInfo;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;
import com.tencent.liteav.tuichorus.ui.lrc.LyricsReader;
import com.tencent.liteav.tuichorus.ui.lrc.widget.AbstractLrcView;
import com.tencent.liteav.tuichorus.ui.lrc.widget.FloatLyricsView;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicService;
import com.tencent.liteav.tuichorus.ui.music.IUpdateLrcDelegate;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicCallback;
import com.tencent.liteav.tuichorus.ui.music.impl.ChorusMusicView;
import com.tencent.liteav.tuichorus.ui.base.MemberEntity;
import com.tencent.liteav.tuichorus.ui.gift.GiftAdapter;
import com.tencent.liteav.tuichorus.ui.gift.GiftPanelDelegate;
import com.tencent.liteav.tuichorus.ui.gift.IGiftPanelView;
import com.tencent.liteav.tuichorus.ui.gift.imp.DefaultGiftAdapterImp;
import com.tencent.liteav.tuichorus.ui.gift.imp.GiftAnimatorLayout;
import com.tencent.liteav.tuichorus.ui.gift.imp.GiftInfo;
import com.tencent.liteav.tuichorus.ui.gift.imp.GiftInfoDataHandler;
import com.tencent.liteav.tuichorus.ui.gift.imp.GiftPanelViewImp;
import com.tencent.liteav.tuichorus.ui.widget.ConfirmDialogFragment;
import com.tencent.liteav.tuichorus.ui.widget.InputTextMsgDialog;
import com.tencent.liteav.tuichorus.ui.widget.SelectMemberView;
import com.tencent.liteav.tuichorus.ui.widget.msg.AudienceEntity;
import com.tencent.liteav.tuichorus.ui.widget.msg.MsgEntity;
import com.tencent.liteav.tuichorus.ui.widget.msg.MsgListAdapter;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChorusRoomBaseActivity extends AppCompatActivity implements ChorusRoomSeatAdapter.OnItemClickListener,
        TRTCChorusRoomDelegate,
        InputTextMsgDialog.OnTextSendListener,
        MsgListAdapter.OnItemClickListener,
        IUpdateLrcDelegate {
    protected static final String TAG = ChorusRoomBaseActivity.class.getName();

    protected static final int    MAX_SEAT_SIZE            = 2;
    protected static final String CHORUSROOM_ROOM_ID       = "room_id";
    protected static final String CHORUSROOM_ROOM_NAME     = "room_name";
    protected static final String CHORUSROOM_USER_NAME     = "user_name";
    protected static final String CHORUSROOM_USER_ID       = "user_id";
    protected static final String CHORUSROOM_NEED_REQUEST  = "need_request";
    protected static final String CHORUSROOM_AUDIO_QUALITY = "audio_quality";
    protected static final String CHORUSROOM_USER_AVATAR   = "user_avatar";
    protected static final String CHORUSROOM_ROOM_COVER    = "room_cover";

    private static final int MESSAGE_USERNAME_COLOR_ARR[] = {
            R.color.tuichorus_color_msg_1,
            R.color.tuichorus_color_msg_2,
            R.color.tuichorus_color_msg_3,
            R.color.tuichorus_color_msg_4,
            R.color.tuichorus_color_msg_5,
            R.color.tuichorus_color_msg_6,
            R.color.tuichorus_color_msg_7,
    };

    private static final String CHORUS_MUSIC_SERVICE_IMPL_URI = "com.tencent.liteav.demo.chorusimpl.ChorusMusicServiceImpl";

    protected String                     mSelfUserId;     //进房用户ID
    protected int                        mCurrentRole;    //用户当前角色:主播/听众
    public    TRTCChorusRoom             mTRTCChorusRoom;
    private   boolean                    isInitSeat;
    protected List<ChorusRoomSeatEntity> mRoomSeatEntityList;
    protected Map<String, Boolean>       mSeatUserMuteMap;
    protected ChorusRoomSeatAdapter      mChorusRoomSeatAdapter;
    protected SelectMemberView           mViewSelectMember;
    protected AudienceListAdapter        mAudienceListAdapter;
    protected TextView                   mTvRoomName;
    protected TextView                   mTvRoomId;
    protected RecyclerView               mRvSeat;
    protected RecyclerView               mRvAudience;
    protected RecyclerView               mRvImMsg;
    protected ChorusMusicView            mChorusMusicView;
    protected AppCompatImageButton       mBtnExitRoom;
    protected AppCompatImageButton       mBtnMsg;
    protected AppCompatImageButton       mBtnGift;
    protected ImageView                  mIvAudienceMove;

    protected TUIChorusAudioManager      mAudioManager;
    protected InputTextMsgDialog         mInputTextMsgDialog;
    protected int                        mRoomId;
    protected String                     mRoomName;
    protected String                     mUserName;
    protected String                     mRoomCover;
    protected String                     mUserAvatar;
    protected CircleImageView            mImgRoom;
    protected boolean                    mNeedRequest;
    protected int                        mAudioQuality;
    protected List<MsgEntity>            mMsgEntityList;
    protected LinkedList<AudienceEntity> mAudienceEntityList;
    protected MsgListAdapter             mMsgListAdapter;
    protected ConfirmDialogFragment      mConfirmDialogFragment;
    protected List<MemberEntity>         mMemberEntityList;
    protected Map<String, MemberEntity>  mMemberEntityMap;
    protected TXCloudVideoView           mVideoView;
    protected boolean                    mIsChorusOn;

    private Context mContext;
    private String  mLastMsgUserId = null;
    private int     mMessageColorIndex;
    private int     mRvAudienceScrollPosition;
    private int     mSelfSeatIndex = -1;
    private boolean mIsRecord;

    //礼物
    private GiftInfoDataHandler mGiftInfoDataHandler;
    private GiftAnimatorLayout  mGiftAnimatorLayout;

    private ConfirmDialogFragment    mAlertDialog;
    private FloatLyricsView          mLrcView;
    public  ChorusMusicService       mChorusMusicService;
    private String                   mRoomDefaultCover =
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover1.png";
    private boolean                  mIsDestroyed;
    public  ChorusRoomInfoController mChorusRoomInfoController;
    private ImageView                mImageLocalNetwork;
    private TextView                 mTextLocalNetwork;
    private ImageView                mImageRemoteNetwork;
    private TextView                 mTextRemoteNetwork;
    private ConstraintLayout         mLocalNetWorkSignalLayout;
    private ConstraintLayout         mRemoteNetWorkSignalLayout;
    private Button                   mBtnRecord;
    private ImageView                mIvStartChorus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tuichorus_activity_main);
        initStatusBar();
        createChorusMusicImpl();
        initView();
        initData();
        initListener();
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_WELCOME;
        msgEntity.content = getString(R.string.tuichorus_welcome_visit);
        msgEntity.linkUrl = getString(R.string.tuichorus_welcome_visit_link);
        showImMsg(msgEntity);
    }

    // 通过反射创建歌曲管理实现类的实例
    public void createChorusMusicImpl() {
        try {
            Class       clz         = Class.forName(CHORUS_MUSIC_SERVICE_IMPL_URI);
            Constructor constructor = clz.getConstructor(Context.class);
            mChorusMusicService = (ChorusMusicService) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    protected void initView() {
        mImgRoom = (CircleImageView) findViewById(R.id.iv_anchor_head);
        mTvRoomName = (TextView) findViewById(R.id.tv_room_name);
        mTvRoomId = (TextView) findViewById(R.id.tv_room_id);
        mRvAudience = (RecyclerView) findViewById(R.id.rv_audience);
        mIvAudienceMove = (ImageView) findViewById(R.id.iv_audience_move);
        mBtnExitRoom = (AppCompatImageButton) findViewById(R.id.exit_room);
        mVideoView = findViewById(R.id.cloud_view);
        mRvSeat = (RecyclerView) findViewById(R.id.rv_seat);
        mRvImMsg = (RecyclerView) findViewById(R.id.rv_im_msg);
        mBtnRecord = findViewById(R.id.btn_record);
        mBtnMsg = (AppCompatImageButton) findViewById(R.id.btn_msg);
        mBtnGift = (AppCompatImageButton) findViewById(R.id.btn_more_gift);
        mIvStartChorus = findViewById(R.id.img_start_chorus);

        mConfirmDialogFragment = new ConfirmDialogFragment();
        mInputTextMsgDialog = new InputTextMsgDialog(this, R.style.TUIChorusInputDialog);
        mInputTextMsgDialog.setmOnTextSendListener(this);
        mMsgEntityList = new ArrayList<>();
        mMemberEntityList = new ArrayList<>();
        mMemberEntityMap = new HashMap<>();
        mBtnExitRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mMsgListAdapter = new MsgListAdapter(this, mMsgEntityList, this);
        mRvImMsg.setLayoutManager(new LinearLayoutManager(this));
        mRvImMsg.setAdapter(mMsgListAdapter);
        mSeatUserMuteMap = new HashMap<>();
        mRoomSeatEntityList = new ArrayList<>();
        for (int i = 0; i < MAX_SEAT_SIZE; i++) {
            ChorusRoomSeatEntity seatEntity = new ChorusRoomSeatEntity();
            seatEntity.index = i;
            mRoomSeatEntityList.add(seatEntity);
        }
        //听众框
        mViewSelectMember = new SelectMemberView(this);
        //座位表
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        mChorusRoomSeatAdapter = new ChorusRoomSeatAdapter(this, mRoomSeatEntityList, this);
        mRvSeat.setLayoutManager(gridLayoutManager);
        mRvSeat.setAdapter(mChorusRoomSeatAdapter);

        //听众表
        mAudienceEntityList = new LinkedList<>();
        mAudienceListAdapter = new AudienceListAdapter(this, mAudienceEntityList);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRvAudience.setLayoutManager(lm);
        mRvAudience.setAdapter(mAudienceListAdapter);

        //礼物消息显示
        mGiftAnimatorLayout = findViewById(R.id.gift_animator_layout);

        //歌曲管理控件
        mChorusMusicView = (ChorusMusicView) findViewById(R.id.fl_songtable_container);

        //歌词显示控件
        mLrcView = findViewById(R.id.lrc_view);

        //网络状态
        mImageLocalNetwork = findViewById(R.id.iv_local_network);
        mTextLocalNetwork = findViewById(R.id.tv_local_network);
        mImageRemoteNetwork = findViewById(R.id.iv_remote_network);
        mTextRemoteNetwork = findViewById(R.id.tv_remote_network);
        mLocalNetWorkSignalLayout = findViewById(R.id.ll_local_network);
        mRemoteNetWorkSignalLayout = findViewById(R.id.ll_remote_network);
        setSignalVisibility(View.GONE);
    }

    public void chorusMusicImplComplete() {
        mChorusMusicView.setLrcDelegate(this);
        mChorusRoomInfoController.setMusicImpl(mChorusMusicService);
        mChorusMusicView.init(mChorusRoomInfoController);
    }

    protected void initData() {
        Intent intent = getIntent();
        mRoomId = intent.getIntExtra(CHORUSROOM_ROOM_ID, 0);
        mRoomName = intent.getStringExtra(CHORUSROOM_ROOM_NAME);
        mUserName = intent.getStringExtra(CHORUSROOM_USER_NAME);
        mSelfUserId = intent.getStringExtra(CHORUSROOM_USER_ID);
        mNeedRequest = intent.getBooleanExtra(CHORUSROOM_NEED_REQUEST, false);
        mUserAvatar = intent.getStringExtra(CHORUSROOM_USER_AVATAR);
        mAudioQuality = intent.getIntExtra(CHORUSROOM_AUDIO_QUALITY, TRTCCloudDef.TRTC_AUDIO_QUALITY_MUSIC);
        mRoomCover = intent.getStringExtra(CHORUSROOM_ROOM_COVER);
        if (mRoomCover == null) {
            ImageLoader.loadImage(this, mImgRoom, mRoomDefaultCover);
        } else {
            ImageLoader.loadImage(this, mImgRoom, mRoomCover, R.drawable.tuichorus_ic_cover);
        }

        if (mIvStartChorus != null) {
            ImageLoader.loadGifImage(this, mIvStartChorus, R.drawable.tuichorus_bg_start_chorus);
        }

        mTRTCChorusRoom = TRTCChorusRoom.sharedInstance(this);
        mTRTCChorusRoom.setDelegate(this);

        mAudioManager = TUIChorusAudioManager.getInstance();
        mAudioManager.setTUIChorus(mTRTCChorusRoom);

        // 礼物
        GiftAdapter giftAdapter = new DefaultGiftAdapterImp();
        mGiftInfoDataHandler = new GiftInfoDataHandler();
        mGiftInfoDataHandler.setGiftAdapter(giftAdapter);

        mChorusRoomInfoController = new ChorusRoomInfoController();
    }

    protected void initListener() {
        mBtnMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputMsgDialog();
            }
        });
        mBtnGift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGiftPanel();
            }
        });

        mRvAudience.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mRvAudienceScrollPosition = dx;
            }
        });
        mIvAudienceMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRvAudienceScrollPosition < 0) {
                    mRvAudienceScrollPosition = 0;
                }
                int position = mRvAudienceScrollPosition + dp2px(mContext, 32);
                mRvAudience.smoothScrollBy(position, 0);
            }
        });
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsRecord = !mIsRecord;
                if (mIsRecord) {
                    startRecordLocal();
                    mBtnRecord.setText(getResources().getString(R.string.tuichorus_btn_stop_record));
                } else {
                    stopRecordlocal();
                    mBtnRecord.setText(getResources().getString(R.string.tuichorus_btn_start_record));
                }
            }
        });
    }

    public int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    private boolean isSeatMute(int seatIndex) {
        ChorusRoomSeatEntity seatEntity = findSeatEntityFromUserId(seatIndex);
        if (seatEntity != null) {
            return seatEntity.isSeatMute;
        }
        return false;
    }

    /**
     * 判断是否为主播，有操作按钮的权限
     *
     * @return 是否有权限
     */
    protected boolean checkButtonPermission() {
        boolean hasPermission = (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor);
        if (!hasPermission) {
            ToastUtils.showLong(getString(R.string.tuichorus_toast_anchor_can_only_operate_it));
        }
        return hasPermission;
    }

    //展示礼物面板
    private void showGiftPanel() {
        IGiftPanelView giftPanelView = new GiftPanelViewImp(this);
        giftPanelView.init(mGiftInfoDataHandler);
        giftPanelView.setGiftPanelDelegate(new GiftPanelDelegate() {
            @Override
            public void onGiftItemClick(GiftInfo giftInfo) {
                sendGift(giftInfo);
            }

            @Override
            public void onChargeClick() {

            }
        });
        giftPanelView.show();
    }

    //发送礼物消息出去同时展示礼物动画和弹幕
    private void sendGift(GiftInfo giftInfo) {
        GiftInfo giftInfoCopy = giftInfo.copy();
        giftInfoCopy.sendUser = mContext.getString(R.string.tuichorus_me);
        giftInfoCopy.sendUserHeadIcon = UserModelManager.getInstance().getUserModel().userAvatar;
        mGiftAnimatorLayout.show(giftInfoCopy);

        GiftSendJson jsonData = new GiftSendJson();
        jsonData.setSendUser(UserModelManager.getInstance().getUserModel().userName);
        jsonData.setSendUserHeadIcon(UserModelManager.getInstance().getUserModel().userAvatar);
        jsonData.setGiftId(giftInfo.giftId);
        Gson gson = new Gson();

        mTRTCChorusRoom.sendRoomCustomMsg(String.valueOf(TCConstants.IMCMD_GIFT),
                gson.toJson(jsonData), new TRTCChorusRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code != 0) {
                            Toast.makeText(mContext, R.string.tuichorus_toast_sent_message_failure, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * 处理礼物弹幕消息
     */
    private void handleGiftMsg(TRTCChorusRoomDef.UserInfo userInfo, String data) {
        if (mGiftInfoDataHandler != null) {
            Gson         gson     = new Gson();
            GiftSendJson jsonData = gson.fromJson(data, GiftSendJson.class);
            String       giftId   = jsonData.getGiftId();
            GiftInfo     giftInfo = mGiftInfoDataHandler.getGiftInfo(giftId);
            if (giftInfo != null) {
                if (userInfo != null) {
                    giftInfo.sendUserHeadIcon = userInfo.userAvatar;
                    if (!TextUtils.isEmpty(userInfo.userName)) {
                        giftInfo.sendUser = userInfo.userName;
                    } else {
                        giftInfo.sendUser = userInfo.userId;
                    }
                }
                mGiftAnimatorLayout.show(giftInfo);
            }
        }
    }

    public void refreshView() {
        if (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor) {
            mChorusMusicView.updateView(true);
        } else {
            mChorusMusicView.updateView(false);
        }
    }

    /**
     * 网络质量监听
     *
     * @param localQuality  上行网络质量。
     * @param remoteQuality 下行网络质量。
     */
    @Override
    public void onNetworkQuality(TRTCCloudDef.TRTCQuality localQuality, List<TRTCCloudDef.TRTCQuality> remoteQuality) {
        //需判断副唱是否连接
        if (!remoteQuality.isEmpty()) {
            //房主
            if (mChorusRoomInfoController.isRoomOwner()) {
                switchNetworkStatus(localQuality.quality, remoteQuality.get(0).quality);
            } else {
                switchNetworkStatus(remoteQuality.get(0).quality, localQuality.quality);
            }
        } else {
            switchNetworkStatus(localQuality == null ? TCConstants.TRTCQUALITY_NONE : localQuality.quality, TCConstants.TRTCQUALITY_NONE);
        }
    }

    //根据网络状态动态显示
    private void switchNetworkStatus(int localQuality, int remoteQuality) {
        switch (localQuality) {
            case TCConstants.TRTCQUALITY_EXCELLENT:
                mImageLocalNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_good);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_good);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_good));
                break;
            case TCConstants.TRTCQUALITY_GOOD:
                mImageLocalNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_good);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_good);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_good));
                break;
            case TCConstants.TRTCQUALITY_POOR:
                mImageLocalNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_poor);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_General);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_poor));
                break;
            case TCConstants.TRTCQUALITY_BAD:
                mImageLocalNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_poor);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_General);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_poor));
                break;
            case TCConstants.TRTCQUALITY_VBAD:
                mImageLocalNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_bad);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_poor);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_bad));
                break;
            case TCConstants.TRTCQUALITY_DOWN:
                mImageLocalNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_bad);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_poor);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_bad));
                break;
            default:
                //未定义
                mImageLocalNetwork.setImageResource(R.drawable.tuichorus_close_seat);
                mTextLocalNetwork.setText(R.string.tuichorus_network_quality_none);
                mTextLocalNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_none));
                break;
        }

        switch (remoteQuality) {
            case TCConstants.TRTCQUALITY_EXCELLENT:
                mImageRemoteNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_good);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_good);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_good));
                break;
            case TCConstants.TRTCQUALITY_GOOD:
                mImageRemoteNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_good);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_good);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_good));
                break;
            case TCConstants.TRTCQUALITY_POOR:
                mImageRemoteNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_poor);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_General);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_poor));
                break;
            case TCConstants.TRTCQUALITY_BAD:
                mImageRemoteNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_poor);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_General);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_poor));
                break;
            case TCConstants.TRTCQUALITY_VBAD:
                mImageRemoteNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_bad);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_poor);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_bad));
                break;
            case TCConstants.TRTCQUALITY_DOWN:
                mImageRemoteNetwork.setImageResource(R.drawable.trtcchorus_ic_signal_bad);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_poor);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_bad));
                break;
            default:
                //未定义
                mImageRemoteNetwork.setImageResource(R.drawable.tuichorus_close_seat);
                mTextRemoteNetwork.setText(R.string.tuichorus_network_quality_none);
                mTextRemoteNetwork.setTextColor(getResources().getColor(R.color.tuichorus_network_quality_none));
                break;
        }
    }

    protected void setSignalVisibility(int visibility) {
        mLocalNetWorkSignalLayout.setVisibility(visibility);
        mRemoteNetWorkSignalLayout.setVisibility(visibility);
    }

    /**
     *     /////////////////////////////////////////////////////////////////////////////////
     *     //
     *     //                      发送文本信息
     *     //
     *     /////////////////////////////////////////////////////////////////////////////////
     */
    /**
     * 发消息弹出框
     */
    private void showInputMsgDialog() {
        WindowManager              windowManager = getWindowManager();
        Display                    display       = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp            = mInputTextMsgDialog.getWindow().getAttributes();
        lp.width = display.getWidth(); //设置宽度
        mInputTextMsgDialog.getWindow().setAttributes(lp);
        mInputTextMsgDialog.setCancelable(true);
        mInputTextMsgDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mInputTextMsgDialog.show();
    }

    @Override
    public void onTextSend(String msg) {
        if (msg.length() == 0) {
            return;
        }
        byte[] byte_num = msg.getBytes(StandardCharsets.UTF_8);
        if (byte_num.length > 160) {
            Toast.makeText(this, getString(R.string.tuichorus_toast_please_enter_content), Toast.LENGTH_SHORT).show();
            return;
        }

        //消息回显
        MsgEntity entity = new MsgEntity();
        entity.userName = getString(R.string.tuichorus_me);
        entity.content = msg;
        entity.isChat = true;
        entity.userId = mSelfUserId;
        entity.type = MsgEntity.TYPE_NORMAL;
        showImMsg(entity);

        mTRTCChorusRoom.sendRoomTextMsg(msg, new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    ToastUtils.showShort(getString(R.string.tuichorus_toast_sent_successfully));
                } else {
                    ToastUtils.showShort(getString(R.string.tuichorus_toast_sent_message_failure));
                }
            }
        });
    }

    /**
     * 座位上点击按钮的反馈
     *
     * @param position
     */
    @Override
    public void onItemClick(int position) {
    }

    @Override
    public void onError(int code, String message) {

    }

    @Override
    public void onWarning(int code, String message) {

    }

    @Override
    public void onDebugLog(String message) {

    }

    @Override
    public void onRoomDestroy(String roomId) {
        ToastUtils.showLong(R.string.tuichorus_msg_close_room);
        mTRTCChorusRoom.exitRoom(null);
        //房主销毁房间,其他人退出房间,并清除自己的信息
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

    @Override
    public void onRoomInfoChange(TRTCChorusRoomDef.RoomInfo roomInfo) {
        mNeedRequest = roomInfo.needRequest;
        mRoomName = roomInfo.roomName;
        mTvRoomName.setText(roomInfo.roomName);
        mTvRoomId.setText(getString(R.string.tuichorus_room_id, roomInfo.roomId));
    }

    @Override
    public void onSeatListChange(final List<TRTCChorusRoomDef.SeatInfo> seatInfoList) {
        //先刷一遍界面
        final List<String> userids = new ArrayList<>();
        for (int i = 0; i < seatInfoList.size(); i++) {
            TRTCChorusRoomDef.SeatInfo newSeatInfo = seatInfoList.get(i);
            // 座位区域的列表
            ChorusRoomSeatEntity oldSeatEntity = mRoomSeatEntityList.get(i);
            if (newSeatInfo.userId != null && !newSeatInfo.userId.equals(oldSeatEntity.userId)) {
                //userId相同，可以不用重新获取信息了
                //但是如果有新的userId进来，那么应该去拿一下主播的详细信息
                userids.add(newSeatInfo.userId);
            }
            oldSeatEntity.userId = newSeatInfo.userId;
            // 座位的状态更新一下
            switch (newSeatInfo.status) {
                case TXSeatInfo.STATUS_UNUSED:
                    oldSeatEntity.isUsed = false;
                    oldSeatEntity.isClose = false;
                    break;
                case TXSeatInfo.STATUS_CLOSE:
                    oldSeatEntity.isUsed = false;
                    oldSeatEntity.isClose = true;
                    break;
                case TXSeatInfo.STATUS_USED:
                    oldSeatEntity.isUsed = true;
                    oldSeatEntity.isClose = false;
                    break;
                default:
                    break;
            }
            oldSeatEntity.isSeatMute = newSeatInfo.mute;
        }
        for (String userId : userids) {
            if (!mSeatUserMuteMap.containsKey(userId)) {
                mSeatUserMuteMap.put(userId, true);
            }
        }
        mChorusRoomSeatAdapter.notifyDataSetChanged();
        //所有的userId拿到手，开始去搜索详细信息了
        mTRTCChorusRoom.getUserInfoList(userids, new TRTCChorusRoomCallback.UserListCallback() {
            @Override
            public void onCallback(int code, String msg, List<TRTCChorusRoomDef.UserInfo> list) {
                // 解析所有人的userinfo
                Map<String, TRTCChorusRoomDef.UserInfo> map = new HashMap<>();
                for (TRTCChorusRoomDef.UserInfo userInfo : list) {
                    map.put(userInfo.userId, userInfo);
                }
                for (int i = 0; i < seatInfoList.size(); i++) {
                    TRTCChorusRoomDef.SeatInfo newSeatInfo = seatInfoList.get(i);
                    TRTCChorusRoomDef.UserInfo userInfo    = map.get(newSeatInfo.userId);
                    if (userInfo == null) {
                        continue;
                    }
                    boolean isUserMute = mSeatUserMuteMap.get(userInfo.userId);
                    // 接下来是座位区域的列表
                    ChorusRoomSeatEntity seatEntity = mRoomSeatEntityList.get(i);
                    if (userInfo.userId.equals(seatEntity.userId)) {
                        seatEntity.userName = userInfo.userName;
                        seatEntity.userAvatar = userInfo.userAvatar;
                        seatEntity.isUserMute = isUserMute;
                    }
                }
                mChorusRoomSeatAdapter.notifyDataSetChanged();
                if (!isInitSeat) {
                    getAudienceList();
                    isInitSeat = true;
                }
            }
        });
        mChorusRoomInfoController.setRoomSeatEntityList(mRoomSeatEntityList);

    }

    @Override
    public void onAnchorEnterSeat(int index, TRTCChorusRoomDef.UserInfo user) {
        Log.d(TAG, "onAnchorEnterSeat userInfo:" + user);
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.userName = user.userName;
        msgEntity.content = getString(R.string.tuichorus_tv_online_no_name, index + 1);
        showImMsg(msgEntity);
        mAudienceListAdapter.removeMember(user.userId);
        if (user.userId.equals(mSelfUserId)) {
            mCurrentRole = TRTCCloudDef.TRTCRoleAnchor;
            mSelfSeatIndex = index;
        }
        refreshView();
    }

    @Override
    public void onAnchorLeaveSeat(int index, TRTCChorusRoomDef.UserInfo user) {
        Log.d(TAG, "onAnchorLeaveSeat userInfo:" + user);
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.userName = user.userName;
        msgEntity.content = getString(R.string.tuichorus_tv_offline_no_name, index + 1);
        showImMsg(msgEntity);
        AudienceEntity entity = new AudienceEntity();
        entity.userId = user.userId;
        entity.userAvatar = user.userAvatar;
        mAudienceListAdapter.addMember(entity);
        if (user.userId.equals(mSelfUserId)) {
            mCurrentRole = TRTCCloudDef.TRTCRoleAudience;
            mSelfSeatIndex = -1;
            mChorusMusicService.deleteAllMusic(mSelfUserId, new ChorusMusicCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {

                }
            });
            mAudioManager.stopPlayMusic(null);
            mAudioManager.setCurrentStatus(TUIChorusAudioManager.MUSIC_STOP);
        }
        refreshView();
    }

    @Override
    public void onSeatMute(int index, boolean isMute) {
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        if (isMute) {
            msgEntity.content = getString(R.string.tuichorus_tv_the_position_has_muted, index + 1);
        } else {
            msgEntity.content = getString(R.string.tuichorus_tv_the_position_has_unmuted, index + 1);
        }
        showImMsg(msgEntity);
        ChorusRoomSeatEntity seatEntity = findSeatEntityFromUserId(index);
        if (seatEntity == null) {
            return;
        }
        if (index == mSelfSeatIndex) {
            if (isMute) {
                mTRTCChorusRoom.muteLocalAudio(true);
            } else if (!seatEntity.isUserMute) {
                mTRTCChorusRoom.muteLocalAudio(false);
            }
        }
    }

    @Override
    public void onSeatClose(int index, boolean isClose) {
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.content = isClose ? getString(R.string.tuichorus_tv_the_owner_ban_this_position, index + 1) :
                getString(R.string.tuichorus_tv_the_owner_not_ban_this_position, index + 1);
        showImMsg(msgEntity);
    }

    @Override
    public void onUserMicrophoneMute(String userId, boolean mute) {
        Log.d(TAG, "onUserMicrophoneMute userId:" + userId + " mute:" + mute);
        mSeatUserMuteMap.put(userId, mute);
    }

    private ChorusRoomSeatEntity findSeatEntityFromUserId(String userId) {
        if (mRoomSeatEntityList != null) {
            for (ChorusRoomSeatEntity seatEntity : mRoomSeatEntityList) {
                if (userId.equals(seatEntity.userId)) {
                    return seatEntity;
                }
            }
        }
        return null;
    }

    private ChorusRoomSeatEntity findSeatEntityFromUserId(int index) {
        if (index == -1) {
            return null;
        }
        if (mRoomSeatEntityList != null) {
            for (ChorusRoomSeatEntity seatEntity : mRoomSeatEntityList) {
                if (index == seatEntity.index) {
                    return seatEntity;
                }
            }
        }
        return null;
    }

    //下麦
    public void leaveSeat() {
        if (mAlertDialog == null) {
            mAlertDialog = new ConfirmDialogFragment();
        }
        if (mAlertDialog.isAdded()) {
            mAlertDialog.dismiss();
        }
        //正在合唱，主播无法下麦
        if (mIsChorusOn) {
            ToastUtils.showLong(R.string.tuichorus_toast_chorus_can_not_leave_seat);
            return;
        }
        mAlertDialog.setMessage(getString(R.string.tuichorus_leave_seat_ask));
        mAlertDialog.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                mTRTCChorusRoom.leaveSeat(new TRTCChorusRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code == 0) {
                            ToastUtils.showShort(R.string.tuichorus_toast_offline_successfully);
                            setSignalVisibility(View.GONE);
                        } else {
                            ToastUtils.showShort(getString(R.string.tuichorus_toast_offline_failure, msg));
                        }
                    }
                });
                mAlertDialog.dismiss();
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
    public void onAudienceEnter(TRTCChorusRoomDef.UserInfo userInfo) {
        Log.d(TAG, "onAudienceEnter userInfo:" + userInfo);
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.content = getString(R.string.tuichorus_tv_enter_room, "");
        msgEntity.userName = userInfo.userName;
        showImMsg(msgEntity);
        if (userInfo.userId.equals(mSelfUserId)) {
            return;
        }
        AudienceEntity entity = new AudienceEntity();
        entity.userId = userInfo.userId;
        entity.userAvatar = userInfo.userAvatar;
        mAudienceListAdapter.addMember(entity);
    }

    @Override
    public void onAudienceExit(TRTCChorusRoomDef.UserInfo userInfo) {
        Log.d(TAG, "onAudienceExit userInfo:" + userInfo);
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.userName = userInfo.userName;
        msgEntity.content = getString(R.string.tuichorus_tv_exit_room, "");
        showImMsg(msgEntity);
        mAudienceListAdapter.removeMember(userInfo.userId);
    }

    @Override
    public void onUserVolumeUpdate(List<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
        for (TRTCCloudDef.TRTCVolumeInfo info : userVolumes) {
            if (info != null) {
                int                  volume = info.volume;
                ChorusRoomSeatEntity entity = findSeatEntityFromUserId(info.userId);
                if (entity != null) {
                    entity.isTalk = volume > 20;
                    mChorusRoomSeatAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onRecvRoomTextMsg(String message, TRTCChorusRoomDef.UserInfo userInfo) {
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.userId = userInfo.userId;
        msgEntity.userName = userInfo.userName;
        msgEntity.content = message;
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.isChat = true;
        showImMsg(msgEntity);
    }

    @Override
    public void onRecvRoomCustomMsg(String cmd, String message, TRTCChorusRoomDef.UserInfo userInfo) {
        int type = Integer.parseInt(cmd);
        switch (type) {
            case TCConstants.IMCMD_GIFT:
                handleGiftMsg(userInfo, message);
                break;
            default:
                break;
        }
    }

    @Override
    public void onReceiveNewInvitation(String id, String inviter, String cmd, String content) {

    }

    @Override
    public void onInviteeAccepted(String id, String invitee) {

    }

    @Override
    public void onInviteeRejected(String id, String invitee) {

    }

    @Override
    public void onInvitationCancelled(String id, String invitee) {

    }

    @Override
    public void onAgreeClick(int position) {

    }

    @Override
    public void onOrderedManagerClick(int position) {

    }

    @Override
    public void onMusicProgressUpdate(final int musicID, long progress, long total) {
        //收到歌曲进度的回调后,更新歌词显示进度
        seekLrcToTime(progress);
    }

    @Override
    public void onMusicPrepareToPlay(int musicID) {
        mIsChorusOn = true;
        Log.d(TAG, "onMusicPrepareToPlay: musicId = " + musicID);
        mAudioManager.setCurrentStatus(TUIChorusAudioManager.MUSIC_PLAYING);
        mChorusMusicService.prepareToPlay(String.valueOf(musicID));
        //开始音波动画
        showStartChorusAnim(true);
    }

    @Override
    public void onMusicCompletePlaying(int musicID) {
        Log.d(TAG, "onMusicCompletePlaying: musicId = " + musicID);
        setLrcPath(null);
        mStartBGMModel = null;
        mIsChorusOn = false;
        if (mAudioManager != null) {
            mAudioManager.setCurrentStatus(TUIChorusAudioManager.MUSIC_STOP);
        }
        if (mChorusMusicService != null) {
            mChorusMusicService.completePlaying(String.valueOf(musicID));
        }
        //隐藏音波动画
        showStartChorusAnim(false);
        //隐藏倒计时
        mChorusMusicView.hideStartAnim();
    }

    private ChorusMusicModel mStartBGMModel = null;

    @Override
    public void onReceiveAnchorSendChorusMsg(final String musicID, final long startDelay) {
        //听众不处理,只有主播收到后去播放自己的BGM
        if (!mChorusRoomInfoController.isAnchor()) {
            return;
        }

        mIsChorusOn = true;
        //主播收到开始合唱请求，根据发送来的musicId来查询歌曲信息
        mChorusMusicService.chorusGetMusicPage(1, 10, new ChorusMusicCallback.MusicListCallback() {
            @Override
            public void onCallback(int code, String msg, List<ChorusMusicInfo> list) {
                for (ChorusMusicInfo musicInfo : list) {
                    if (musicInfo.musicId.equals(musicID)) {
                        mStartBGMModel = new ChorusMusicModel();
                        mStartBGMModel.musicId = musicInfo.musicId;
                        mStartBGMModel.musicName = musicInfo.musicName;
                        mStartBGMModel.singer = musicInfo.singer;
                        mStartBGMModel.contentUrl = musicInfo.contentUrl;
                        mStartBGMModel.coverUrl = musicInfo.coverUrl;
                        mStartBGMModel.lrcUrl = musicInfo.lrcUrl;
                        mStartBGMModel.isSelected = true;
                        //播放
                        mChorusMusicView.showStartAnimAndPlay(mStartBGMModel, startDelay);
                        break;
                    }
                }
            }
        });
    }

    protected void getAudienceList() {
        mTRTCChorusRoom.getUserInfoList(null, new TRTCChorusRoomCallback.UserListCallback() {
            @Override
            public void onCallback(int code, String msg, List<TRTCChorusRoomDef.UserInfo> list) {
                if (code == 0) {
                    Log.d(TAG, "getAudienceList list size:" + list.size());
                    for (TRTCChorusRoomDef.UserInfo userInfo : list) {
                        Log.d(TAG, "getAudienceList userInfo:" + userInfo);
                        if (!mSeatUserMuteMap.containsKey(userInfo.userId)) {
                            AudienceEntity audienceEntity = new AudienceEntity();
                            audienceEntity.userAvatar = userInfo.userAvatar;
                            audienceEntity.userId = userInfo.userId;
                            mAudienceListAdapter.addMember(audienceEntity);
                        }
                        if (userInfo.userId.equals(mSelfUserId)) {
                            continue;
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
                    }
                }
            }
        });
    }

    protected int changeSeatIndexToModelIndex(int srcSeatIndex) {
        return srcSeatIndex;
    }

    protected void showImMsg(final MsgEntity entity) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMsgEntityList.size() > 1000) {
                    while (mMsgEntityList.size() > 900) {
                        mMsgEntityList.remove(0);
                    }
                }
                if (!TextUtils.isEmpty(entity.userName)) {
                    if (mMessageColorIndex >= MESSAGE_USERNAME_COLOR_ARR.length) {
                        mMessageColorIndex = 0;
                    }
                    int color = MESSAGE_USERNAME_COLOR_ARR[mMessageColorIndex];
                    entity.color = getResources().getColor(color);
                    mMessageColorIndex++;
                }

                //判断当前消息类型是申请上麦消息
                if (entity != null && entity.type == MsgEntity.TYPE_WAIT_AGREE) {
                    //判断当前消息是同一个用户发出
                    if (mLastMsgUserId != null && mLastMsgUserId.equals(entity.userId)) {
                        for (MsgEntity temp : mMsgEntityList) {
                            if (temp != null && mLastMsgUserId.equals(temp.userId)) {
                                temp.type = MsgEntity.TYPE_AGREED;
                            }
                        }
                    }
                }
                mLastMsgUserId = entity.userId;
                mMsgEntityList.add(entity);
                mMsgListAdapter.notifyDataSetChanged();
                mRvImMsg.smoothScrollToPosition(mMsgListAdapter.getItemCount());
            }
        });
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        super.onDestroy();
    }

    private void initLoadLyricsView(final String path) {
        mLrcView.initLrcData();
        mLrcView.setLrcStatus(AbstractLrcView.LRCSTATUS_LOADING);
        final LyricsReader lyricsReader = new LyricsReader();
        if (path == null) {
            mIvStartChorus.setVisibility(View.GONE);
            mLrcView.setLyricsReader(null);
        } else {
            new LrcAsyncTask(path, lyricsReader).execute();
        }
    }

    class LrcAsyncTask extends AsyncTask {
        private String       mPath;
        private LyricsReader mLyricsReader;

        public LrcAsyncTask(String path, LyricsReader lyricsReader) {
            mPath = path;
            this.mLyricsReader = lyricsReader;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            if (mIsDestroyed) {
                return null;
            }
            try {
                File file = new File(mPath);
                mLyricsReader.loadLrc(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (mIsDestroyed) {
                return;
            }
            mLrcView.setLyricsReader(mLyricsReader);
        }
    }

    @Override
    public void setLrcPath(String path) {
        Log.d(TAG, "setLrcPath: path = " + path);
        if (mLrcView == null) {
            return;
        }
        showStartChorusAnim(path == null ? false : true);

        initLoadLyricsView(path);
    }

    //听众端gif显示
    private void showStartChorusAnim(boolean isShow) {
        mIvStartChorus.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void seekLrcToTime(long time) {
        mLrcView.play(time);
    }

    public void startRecordLocal() {
        File   sdcardDir = mContext.getExternalFilesDir(null);
        String dirPath;
        if (sdcardDir == null) {
            dirPath = "/sdcard/record.mp4";
        }
        Random random = new Random();
        dirPath = sdcardDir.getAbsolutePath() + "/record" + random.nextInt(1000) + ".mp4";
        TRTCCloudDef.TRTCLocalRecordingParams params = new TRTCCloudDef.TRTCLocalRecordingParams();
        params.filePath = dirPath;
        params.recordType = 0;
        params.interval = 2000;
        TRTCCloud.sharedInstance(this).startLocalRecording(params);
        ToastUtils.showLong("Start Record：" + dirPath);
    }

    public void stopRecordlocal() {
        TRTCCloud.sharedInstance(this).stopLocalRecording();
        ToastUtils.showLong("Record Over");
    }
}