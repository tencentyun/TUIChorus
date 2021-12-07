package com.tencent.liteav.tuichorus.ui.audio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;
import com.tencent.liteav.tuichorus.ui.base.EarMonitorInstance;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AudioEffectPanel extends BottomSheetDialog {
    private static final String TAG = "AudioEffectPanel";

    private static final int AUDIO_REVERB_TYPE_0        = 0;
    private static final int AUDIO_REVERB_TYPE_1        = 1;
    private static final int AUDIO_REVERB_TYPE_4        = 4;
    private static final int AUDIO_REVERB_TYPE_5        = 5;
    private static final int AUDIO_REVERB_TYPE_6        = 6;
    private static final int AUDIO_VOICECHANGER_TYPE_0  = 0;
    private static final int AUDIO_VOICECHANGER_TYPE_1  = 1;
    private static final int AUDIO_VOICECHANGER_TYPE_2  = 2;
    private static final int AUDIO_VOICECHANGER_TYPE_3  = 3;
    private static final int AUDIO_VOICECHANGER_TYPE_11 = 11;

    private Context             mContext;
    private RecyclerView        mRVAuidoChangeType;
    private RecyclerView        mRVAudioReverbType;
    private SeekBar             mSbMicVolume;
    private SeekBar             mSbBGMVolume;
    private SeekBar             mSbPitchLevel;
    private RecyclerViewAdapter mChangerRVAdapter;
    private RecyclerViewAdapter mReverbRVAdapter;
    private List<ItemEntity>    mChangerItemEntityList;
    private List<ItemEntity>    mReverbItemEntityList;

    private ConstraintLayout          mMainAudioEffectPanel;
    private Group                     mGroupMusic;
    private TextView                  mTvBGMVolume;
    private TextView                  mTvPitchLevel;
    private TextView                  mTvMicVolume;
    private TextView                  mTvTitle;
    private TextView                  mTvReverb;
    private View                      mMusicVolumeGroup;
    private View                      mMusicToneGroup;
    private View                      mMusicVoiceGroup;
    private IAudioEffectPanelDelegate mDelegate;

    private SwitchCompat mSwitchMusicAudition;

    private int mBGMVolume = 100;

    private int mVoiceChangerPosition = 0;
    private int mVoiceReverbPosition  = 0;

    private BottomSheetBehavior mBottomSheetBehavior;
    private TRTCChorusRoom      mTUIChorus;

    public static final String MUSIC_TYPE   = "music";
    public static final String CHANGE_VOICE = "change voice";
    private             String mMusicType;

    public AudioEffectPanel(@NonNull Context context) {
        super(context, R.style.TUIChorusDialogTheme);
        setContentView(R.layout.tuichorus_audio_effect_panel);
        mContext = context;
        initView();
        initData();
        mTUIChorus = TRTCChorusRoom.sharedInstance(context);
    }

    public void setType(String type) {
        mMusicType = type;
        updateView();
    }

    public void setDelegate(IAudioEffectPanelDelegate delegate) {
        mDelegate = delegate;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getBottomSheetBehavior();
        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            mBottomSheetBehavior.setHideable(false);
        }
    }

    private void getBottomSheetBehavior() {
        if (mBottomSheetBehavior != null) {
            return;
        }

        View view = getWindow().findViewById(R.id.design_bottom_sheet);
        if (view == null) {
            return;
        }
        mBottomSheetBehavior = BottomSheetBehavior.from(view);
    }

    private void initView() {
        mMainAudioEffectPanel = (ConstraintLayout) findViewById(R.id.audio_main_ll);
        mGroupMusic = (Group) findViewById(R.id.group_music);

        mMusicVolumeGroup = findViewById(R.id.cl_music_volume_change);
        mMusicVoiceGroup = findViewById(R.id.cl_music_voice);
        mMusicToneGroup = findViewById(R.id.cl_music_tone_change);
        mRVAuidoChangeType = (RecyclerView) findViewById(R.id.rv_audio_change_type);
        mRVAudioReverbType = (RecyclerView) findViewById(R.id.rv_audio_reverb_type);
        mSwitchMusicAudition = (SwitchCompat) findViewById(R.id.switch_music_audition);

        mTvBGMVolume = (TextView) findViewById(R.id.tv_bgm_volume);
        mTvMicVolume = (TextView) findViewById(R.id.tv_mic_volume);
        mTvPitchLevel = (TextView) findViewById(R.id.tv_pitch_level);

        mSbMicVolume = (SeekBar) findViewById(R.id.sb_mic_volume);
        mSbBGMVolume = (SeekBar) findViewById(R.id.sb_bgm_volume);
        mSbPitchLevel = (SeekBar) findViewById(R.id.sb_pitch_level);

        mTvTitle = (TextView) findViewById(R.id.music_effect);
        mTvReverb = (TextView) findViewById(R.id.tv_reverb);

        if (mMusicType != null) {
            updateView();
        }
    }

    private void updateView() {
        if (mMusicType.equals(MUSIC_TYPE)) {
            mTvTitle.setText(R.string.tuichorus_sound_effects);
            mGroupMusic.setVisibility(View.VISIBLE);
            mRVAuidoChangeType.setVisibility(View.GONE);
        } else if (mMusicType.equals(CHANGE_VOICE)) {
            mTvTitle.setText(R.string.tuichorus_changer);
            mGroupMusic.setVisibility(View.GONE);
            mRVAuidoChangeType.setVisibility(View.VISIBLE);
        }
    }

    private void initData() {
        mSbMicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTvMicVolume.setText(progress + "");
                if (mDelegate != null) {
                    mDelegate.onMicVolumChanged(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mSbBGMVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBGMVolume = progress;
                mTvBGMVolume.setText(progress + "");
                if (mDelegate != null) {
                    mDelegate.onMusicVolumChanged(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mSbPitchLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = ((progress - 50) / (float) 50);
                mTvPitchLevel.setText(pitch + "");
                if (mDelegate != null) {
                    mDelegate.onPitchLevelChanged(pitch);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mChangerItemEntityList = createAudioChangeItems();
        mReverbItemEntityList = createReverbItems();

        // 选变声
        mChangerRVAdapter = new RecyclerViewAdapter(mContext, mChangerItemEntityList, new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                int type = mChangerItemEntityList.get(position).mType;
                Log.d(TAG, "select changer type " + type);
                if (mVoiceChangerPosition == position) {
                    return;
                }
                if (mDelegate != null) {
                    mDelegate.onChangeRV(type);
                }
                mChangerItemEntityList.get(position).mIsSelected = true;
                mChangerItemEntityList.get(mVoiceChangerPosition).mIsSelected = false;
                mVoiceChangerPosition = position;
                mChangerRVAdapter.notifyDataSetChanged();
            }
        });
        mChangerItemEntityList.get(0).mIsSelected = true;
        mChangerRVAdapter.notifyDataSetChanged();
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRVAuidoChangeType.setLayoutManager(layoutManager);
        mRVAuidoChangeType.setAdapter(mChangerRVAdapter);
        // 选混响
        mReverbRVAdapter = new RecyclerViewAdapter(mContext, mReverbItemEntityList, new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                int type = mReverbItemEntityList.get(position).mType;
                Log.d(TAG, "select reverb type " + type);
                if (mVoiceReverbPosition == position) {
                    return;
                }
                if (mDelegate != null) {
                    mDelegate.onReverbRV(type);
                }
                mReverbItemEntityList.get(position).mIsSelected = true;
                mReverbItemEntityList.get(mVoiceReverbPosition).mIsSelected = false;
                mVoiceReverbPosition = position;
                mReverbRVAdapter.notifyDataSetChanged();
            }
        });
        mReverbItemEntityList.get(0).mIsSelected = true;
        mReverbRVAdapter.notifyDataSetChanged();
        LinearLayoutManager reverbLayoutManager = new LinearLayoutManager(mContext);
        reverbLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRVAudioReverbType.setLayoutManager(reverbLayoutManager);
        mRVAudioReverbType.setAdapter(mReverbRVAdapter);

        findViewById(R.id.link_music).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://cloud.tencent.com/product/ame"));
                mContext.startActivity(intent);
            }
        });
        mSwitchMusicAudition.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mTUIChorus != null) {
                    mTUIChorus.setVoiceEarMonitorEnable(isChecked);
                }
                EarMonitorInstance.getInstance().updateEarMonitorState(isChecked);
            }
        });
    }

    @Override
    public void show() {
        super.show();
        boolean isOpen = EarMonitorInstance.getInstance().ismEarMonitorOpen();
        mSwitchMusicAudition.setChecked(isOpen);
    }

    private List<ItemEntity> createAudioChangeItems() {
        List<ItemEntity> list = new ArrayList<>();
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_no_effect), R.drawable.tuichorus_changetype_no_select_nomal, R.drawable.tuichorus_changetype_no_select_hover, AUDIO_VOICECHANGER_TYPE_0));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_change_type_child), R.drawable.tuichorus_changetype_child_normal, R.drawable.tuichorus_changetype_child_hover, AUDIO_VOICECHANGER_TYPE_1));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_change_type_luoli), R.drawable.tuichorus_changetype_luoli_normal, R.drawable.tuichorus_changetype_luoli_hover, AUDIO_VOICECHANGER_TYPE_2));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_change_type_dashu), R.drawable.tuichorus_changetype_dashu_normal, R.drawable.tuichorus_changetype_dashu_hover, AUDIO_VOICECHANGER_TYPE_3));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_change_type_kongling), R.drawable.tuichorus_changetype_kongling_normal, R.drawable.tuichorus_changetype_kongling_hover, AUDIO_VOICECHANGER_TYPE_11));
        return list;
    }

    private List<ItemEntity> createReverbItems() {
        List<ItemEntity> list = new ArrayList<>();
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_no_effect), R.drawable.tuichorus_changetype_no_select_nomal, R.drawable.tuichorus_changetype_no_select_hover, AUDIO_REVERB_TYPE_0));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_reverb_type_ktv), R.drawable.tuichorus_reverbtype_chorus_normal, R.drawable.tuichorus_reverbtype_chorus_hover, AUDIO_REVERB_TYPE_1));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_reverb_type_lowdeep), R.drawable.tuichorus_reverbtype_lowdeep_normal, R.drawable.tuichorus_reverbtype_lowdeep_hover, AUDIO_REVERB_TYPE_4));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_reverb_type_heavymetal), R.drawable.tuichorus_reverbtype_heavymetal_normal, R.drawable.tuichorus_reverbtype_heavymetal_hover, AUDIO_REVERB_TYPE_6));
        list.add(new ItemEntity(mContext.getResources().getString(R.string.tuichorus_audio_reverb_type_hongliang), R.drawable.tuichorus_reverbtype_hongliang_normal, R.drawable.tuichorus_reverbtype_hongliang_hover, AUDIO_REVERB_TYPE_5));
        return list;
    }

    public class ItemEntity {
        public String  mTitle;
        public int     mIconId;
        public int     mSelectIconId;
        public int     mType;
        public boolean mIsSelected = false;

        public ItemEntity(String title, int iconId, int selectIconId, int type) {
            mTitle = title;
            mIconId = iconId;
            mSelectIconId = selectIconId;
            mType = type;
        }
    }

    public class RecyclerViewAdapter extends
            RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private Context             context;
        private List<ItemEntity>    list;
        private OnItemClickListener onItemClickListener;

        public RecyclerViewAdapter(Context context, List<ItemEntity> list,
                                   OnItemClickListener onItemClickListener) {
            this.context = context;
            this.list = list;
            this.onItemClickListener = onItemClickListener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private CircleImageView mItemImg;
            private TextView        mTitleTv;

            public ViewHolder(View itemView) {
                super(itemView);
                initView(itemView);
            }

            public void bind(final ItemEntity model, final int position,
                             final OnItemClickListener listener) {
                mItemImg.setImageResource(model.mIconId);
                mTitleTv.setText(model.mTitle);
                if (model.mIsSelected) {
                    mItemImg.setImageResource(model.mSelectIconId);
                    mTitleTv.setTextColor(mContext.getResources().getColor(R.color.tuichorus_color_blue));
                } else {
                    mItemImg.setImageResource(model.mIconId);
                    mTitleTv.setTextColor(mContext.getResources().getColor(R.color.tuichorus_white));
                }
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onItemClick(position);
                    }
                });
            }

            private void initView(final View itemView) {
                mItemImg = (CircleImageView) itemView.findViewById(R.id.img_item);
                mTitleTv = (TextView) itemView.findViewById(R.id.tv_title);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.tuichorus_audio_main_entry_item, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            ItemEntity item = list.get(position);
            holder.bind(item, position, onItemClickListener);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void reset() {
        mSbMicVolume.setProgress(100);
        mTvMicVolume.setText("100");

        mBGMVolume = 100;
        mSbBGMVolume.setProgress(mBGMVolume);

        mSbPitchLevel.setProgress(50);
        mTvPitchLevel.setText("50");

        mChangerItemEntityList.get(mVoiceChangerPosition).mIsSelected = false;
        mChangerRVAdapter.notifyDataSetChanged();
        mVoiceChangerPosition = 0;

        mReverbItemEntityList.get(mVoiceReverbPosition).mIsSelected = false;
        mReverbRVAdapter.notifyDataSetChanged();
        mVoiceReverbPosition = 0;
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
