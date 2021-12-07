package com.tencent.liteav.tuichorus.ui.music.impl;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;
import com.tencent.liteav.tuichorus.ui.audio.AudioEffectPanel;
import com.tencent.liteav.tuichorus.ui.audio.impl.TUIChorusAudioManager;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicInfo;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicService;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicServiceDelegate;
import com.tencent.liteav.tuichorus.ui.music.IUpdateLrcDelegate;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicCallback;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;

import java.util.ArrayList;
import java.util.List;

public class ChorusMusicView extends CoordinatorLayout implements ChorusMusicServiceDelegate {
    private static final String TAG = "ChorusMusicView";

    private final Context mContext;

    private static final int CHORUS_START_DELAY = 3000;
    private static final int PAGE_NUM           = 1;
    private static final int LOAD_PAGE_SIZE     = 10;

    private LinearLayout mLayoutInfo;
    private LinearLayout mLayoutSongInfo;
    private LinearLayout mLayoutEmpty;
    private TextView     mTvSeatName;
    private TextView     mTvUserName;
    private TextView     mTvSongName;
    private TextView     mTVTimer;
    private Button       mBtnChooseSong;
    private Button       mBtnEffect;
    private Button       mBtnChangeVoice;
    private Button       mBtnEmptyChoose;
    private Button       mBtnStartChorus;

    private ChorusMusicService         mMusicManagerImpl;
    private List<ChorusMusicModel>     mSelectedList;
    private List<ChorusMusicModel>     mLibraryList;
    private AudioEffectPanel           mAudioEffectPanel;
    private TUIChorusAudioManager      mAudioManager;
    private ChorusMusicDialog          mDialog;
    private IUpdateLrcDelegate         mLrcDelegate;
    private List<ChorusRoomSeatEntity> mRoomSeatEntityList;
    private ChorusMusicMsgDelegate     mMsgDelegate;
    private ChorusRoomInfoController   mChorusRoomInfoController;
    private ChorusMusicModel           mCurMusicModel;
    private ValueAnimator              mStartAnimator;

    public ChorusMusicView(Context context) {
        this(context, null);
    }

    public ChorusMusicView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChorusMusicView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        View rootView = LayoutInflater.from(context).inflate(R.layout.tuichorus_layout_songtable, this);
        initView(rootView);
        initListener();
    }

    public void init(ChorusRoomInfoController chorusRoomInfoController) {
        mChorusRoomInfoController = chorusRoomInfoController;
        mMusicManagerImpl = chorusRoomInfoController.getMusicServiceImpl();
        initData(mContext);
    }

    public void setLrcDelegate(IUpdateLrcDelegate delegate) {
        mLrcDelegate = delegate;
    }

    public void updateView(boolean isShow) {
        if (isShow) {
            mBtnEffect.setVisibility(View.VISIBLE);
            mBtnChangeVoice.setVisibility(View.VISIBLE);
        } else {
            mBtnEffect.setVisibility(View.GONE);
            mBtnChangeVoice.setVisibility(View.GONE);
        }
        updateSongTableView(mSelectedList.size());
    }

    public void showMusicDialog(boolean show) {
        if (show) {
            showChooseSongDialog();
        }
    }

    private void initData(Context context) {
        mAudioManager = TUIChorusAudioManager.getInstance();
        //音效面板
        mAudioEffectPanel = new AudioEffectPanel(context);
        mAudioEffectPanel.setDelegate(mAudioManager);

        mSelectedList = new ArrayList<>();
        mLibraryList = new ArrayList<>();
        if (mMusicManagerImpl != null) {
            mMusicManagerImpl.setServiceDelegate(this);
            mMusicManagerImpl.chorusGetSelectedMusicList(new ChorusMusicCallback.MusicSelectedListCallback() {
                @Override
                public void onCallback(int code, String msg, List<ChorusMusicModel> list) {
                    mSelectedList.clear();
                    mSelectedList.addAll(list);
                }
            });
            mMusicManagerImpl.chorusGetMusicPage(PAGE_NUM, LOAD_PAGE_SIZE, new ChorusMusicCallback.MusicListCallback() {
                @Override
                public void onCallback(int code, String msg, List<ChorusMusicInfo> list) {
                    mLibraryList.clear();
                    for (ChorusMusicInfo info : list) {
                        ChorusMusicModel model = new ChorusMusicModel();
                        model.musicId = info.musicId;
                        model.musicName = info.musicName;
                        model.singer = info.singer;
                        model.contentUrl = info.contentUrl;
                        model.coverUrl = info.coverUrl;
                        model.lrcUrl = info.lrcUrl;
                        model.isSelected = false;
                        mLibraryList.add(model);
                    }
                }
            });
        }

        //初始化Dialog
        if (mDialog == null) {
            mDialog = new ChorusMusicDialog(mContext, mChorusRoomInfoController);
        }

        //已点列表为空时,显示空界面
        updateSongTableView(mSelectedList.size());
    }

    private void initView(View view) {
        mLayoutInfo = view.findViewById(R.id.ll_info);
        mLayoutSongInfo = view.findViewById(R.id.ll_song_info);
        mLayoutEmpty = view.findViewById(R.id.ll_empty);

        //默认显示空界面
        mLayoutSongInfo.setVisibility(View.GONE);
        mLayoutInfo.setVisibility(View.GONE);
        mLayoutEmpty.setVisibility(View.VISIBLE);

        mBtnEmptyChoose = (Button) view.findViewById(R.id.btn_empty_choose_song);
        mTvSeatName = (TextView) view.findViewById(R.id.tv_seat_name);
        mTvUserName = (TextView) view.findViewById(R.id.tv_user_name);
        mTvSongName = (TextView) view.findViewById(R.id.tv_song_name);
        mTVTimer = (TextView) view.findViewById(R.id.tv_timer);
        mBtnChooseSong = (Button) view.findViewById(R.id.btn_choose_song);
        mBtnEffect = (Button) view.findViewById(R.id.btn_effect);
        mBtnChangeVoice = (Button) view.findViewById(R.id.btn_change_voice);
        mBtnStartChorus = (Button) view.findViewById(R.id.btn_start_chorus);
    }

    private void updateSongTableView(int size) {
        if (size == 0) {
            mLayoutSongInfo.setVisibility(View.GONE);
            mLayoutEmpty.setVisibility(View.VISIBLE);
            mBtnStartChorus.setVisibility(GONE);
            mBtnChooseSong.setVisibility(GONE);
            mLayoutInfo.setVisibility(GONE);
            mTvUserName.setText("");
            mTvSeatName.setText("");
            //空界面清空歌词
            mLrcDelegate.setLrcPath(null);
        } else if (size > 0) {
            mLayoutSongInfo.setVisibility(View.VISIBLE);
            mLayoutEmpty.setVisibility(View.GONE);
            mBtnChooseSong.setVisibility(VISIBLE);
            mLayoutInfo.setVisibility(VISIBLE);
            //房主显示开始合唱按钮
            if (mChorusRoomInfoController.isRoomOwner()
                    && mAudioManager.getCurrentStatus() != TUIChorusAudioManager.MUSIC_PLAYING) {
                mBtnStartChorus.setVisibility(VISIBLE);
            }

            ChorusMusicModel songEntity = mSelectedList.get(0);
            mTvSongName.setText(songEntity.musicName);

            //根据用户Id,从麦位表获取当前歌曲的用户名和座位Id
            if (mChorusRoomInfoController != null) {
                mRoomSeatEntityList = mChorusRoomInfoController.getRoomSeatEntityList();
            }
            ChorusRoomSeatEntity seatEntity = null;
            if (mRoomSeatEntityList != null) {
                for (ChorusRoomSeatEntity entity : mRoomSeatEntityList) {
                    if (entity.userId != null && entity.userId.equals(songEntity.bookUser)) {
                        seatEntity = entity;
                        break;
                    }
                }
            }

            if (seatEntity != null) {
                mTvUserName.setText(seatEntity.userName);
                mTvSeatName.setText(getResources().getString(R.string.tuichorus_tv_seat_id,
                        String.valueOf(seatEntity.index + 1)));
            }
        }

        if (mChorusRoomInfoController != null && mChorusRoomInfoController.isAnchor()) {
            mLayoutInfo.setVisibility(VISIBLE);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mLayoutEmpty.getLayoutParams();
            layoutParams.setMargins(layoutParams.getMarginStart(), dp2px(mContext, 20), layoutParams.getMarginEnd(), 0);
        } else {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mLayoutEmpty.getLayoutParams();
            layoutParams.setMargins(layoutParams.getMarginStart(), dp2px(mContext, 58), layoutParams.getMarginEnd(), 0);
        }
    }

    private void initListener() {
        mBtnChooseSong.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooseSongDialog();
            }
        });
        mBtnEffect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkButtonPermission()) {
                    if (mAudioEffectPanel != null) {
                        mAudioEffectPanel.setType(AudioEffectPanel.CHANGE_VOICE);
                        mAudioEffectPanel.show();
                    }
                }
            }
        });
        mBtnChangeVoice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkButtonPermission()) {
                    if (mAudioEffectPanel != null) {
                        mAudioEffectPanel.setType(AudioEffectPanel.MUSIC_TYPE);
                        mAudioEffectPanel.show();
                    }
                }
            }
        });
        mBtnEmptyChoose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooseSongDialog();
            }
        });
        mBtnStartChorus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //开始合唱
                mBtnStartChorus.setVisibility(GONE);
                //开始倒计时
                showStartAnimAndPlay(mCurMusicModel, CHORUS_START_DELAY);
            }
        });
    }

    //打开点歌/已点面板
    private void showChooseSongDialog() {
        if (mDialog != null) {
            mDialog.show();
        } else {
            if (mChorusRoomInfoController == null) {
                return;
            }
            mDialog = new ChorusMusicDialog(mContext, mChorusRoomInfoController);
            mDialog.show();
        }
    }

    protected boolean checkButtonPermission() {
        if (!mChorusRoomInfoController.isAnchor()) {
            ToastUtils.showLong(getResources().getString(R.string.tuichorus_toast_anchor_can_only_operate_it));
        }
        return mChorusRoomInfoController.isAnchor();
    }

    @Override
    public void onMusicListChange(List<ChorusMusicModel> musicInfoList) {
        mSelectedList.clear();
        mSelectedList.addAll(musicInfoList);

        //更新歌曲播放界面的信息
        updateSongTableView(mSelectedList.size());
        if (musicInfoList != null && musicInfoList.size() > 0) {
            ChorusMusicModel model = musicInfoList.get(0);
            if (model != null) {
                mChorusRoomInfoController.setTopMusicModel(model);
            }
        }
    }

    @Override
    public void onShouldSetLyric(String musicID) {
        //如果是主播不需要设置歌词,主播自己根据自己的BGM进度更新歌词
        //房主除外,房主一直是主播
        if (!mChorusRoomInfoController.isRoomOwner() && mChorusRoomInfoController.isAnchor()) {
            return;
        }

        Log.d(TAG, "onShouldSetLyric: musicId = " + musicID);
        if (musicID == null || musicID.equals("0")) {
            mLrcDelegate.setLrcPath(null);
            return;
        }
        ChorusMusicModel entity = findFromList(musicID, mSelectedList);
        if (entity == null) {
            entity = findFromList(musicID, mLibraryList);
        }

        if (entity != null) {
            mLrcDelegate.setLrcPath(entity.lrcUrl);
        }
    }

    public ChorusMusicModel findFromList(String musicId, List<ChorusMusicModel> list) {
        if (musicId == null || list.size() == 0) {
            return null;
        }
        ChorusMusicModel entity = null;
        for (ChorusMusicModel temp : list) {
            if (temp != null && musicId.equals(temp.musicId)) {
                entity = temp;
                break;
            }
        }
        return entity;
    }

    @Override
    public void onShouldPlay(ChorusMusicModel model) {
        // 收到播放歌曲的通知后,如果是主播才播放,听众不能播放
        Log.d(TAG, "onShouldPlay: model = " + model);
        if ((mChorusRoomInfoController.isAnchor())) {
            mCurMusicModel = model;
        }
    }

    @Override
    public void onShouldStopPlay(ChorusMusicModel model) {
        //未开始播放，直接切歌
        if (mAudioManager.getCurrentStatus() == TUIChorusAudioManager.MUSIC_STOP) {
            mMusicManagerImpl.completePlaying(model.musicId);
            return;
        }

        if ((mChorusRoomInfoController.isAnchor())) {
            mAudioManager.stopPlayMusic(model);
            mAudioManager.setCurrentStatus(TUIChorusAudioManager.MUSIC_STOP);
        }
    }

    @Override
    public void onShouldShowMessage(ChorusMusicModel model) {
        if (mMsgDelegate != null) {
            mMsgDelegate.sendOrderMsg(model);
        }
    }

    public void showStartAnimAndPlay(final ChorusMusicModel model, long delay) {
        mAudioManager.startPlayMusic(model);
        mAudioManager.setCurrentStatus(TUIChorusAudioManager.MUSIC_PLAYING);
        mTVTimer.setVisibility(VISIBLE);
        delay = delay >= 0 ? delay : 0;
        //副唱开始延迟会略低于3s，若大于2.5s按3s计时
        int delayMs = (int) ((delay + 500) / 1000);
        mStartAnimator = ValueAnimator.ofInt(delayMs, 0);
        mStartAnimator.setInterpolator(new LinearInterpolator());
        mStartAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTVTimer.setText(String.valueOf(animation.getAnimatedValue()));
                Log.i(TAG, "onAnimationUpdate: " + animation.getAnimatedValue());
            }
        });
        mStartAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mTVTimer.setVisibility(View.GONE);
                mLrcDelegate.setLrcPath(model.lrcUrl);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        mStartAnimator.setDuration(delay);
        mStartAnimator.start();
    }

    public void hideStartAnim() {
        mTVTimer.setVisibility(View.GONE);
        if (mStartAnimator != null && mStartAnimator.isRunning()) {
            mStartAnimator.removeAllListeners();
            mStartAnimator.cancel();
        }
    }

    //点歌消息回调
    public void setMsgListener(ChorusMusicMsgDelegate delegate) {
        mMsgDelegate = delegate;
    }

    public int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    public interface ChorusMusicMsgDelegate {
        void sendOrderMsg(ChorusMusicModel model);
    }
}
