package com.tencent.liteav.tuichorus.ui.music.impl;

import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicCallback;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicService;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicServiceDelegate;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;
import com.tencent.liteav.tuichorus.ui.widget.SlideRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChorusMusicSelectView extends CoordinatorLayout implements ChorusMusicServiceDelegate {
    private final String TAG = "MusicSelectView";

    private final Context                    mContext;
    private       SlideRecyclerView          mRvList;
    private       ChorusMusicSelectedAdapter mSelectedAdapter;
    private       List<ChorusMusicModel>     mSelectedList;
    private final ChorusMusicService         mChorusMusicImpl;
    private       ChorusRoomInfoController   mChorusRoomInfoController;
    private       long                       lastCLickTime = -1;

    public ChorusMusicSelectView(Context context, ChorusRoomInfoController chorusRoomInfoController) {
        super(context);
        mContext = context;
        mChorusRoomInfoController = chorusRoomInfoController;
        mChorusMusicImpl = chorusRoomInfoController.getMusicServiceImpl();
        mChorusMusicImpl.setServiceDelegate(this);
        mSelectedList = new ArrayList<>();
        mChorusMusicImpl.chorusGetSelectedMusicList(new ChorusMusicCallback.MusicSelectedListCallback() {
            @Override
            public void onCallback(int code, String msg, List<ChorusMusicModel> list) {
                mSelectedList.clear();
                mSelectedList.addAll(list);
            }
        });
        View rootView = LayoutInflater.from(context).inflate(R.layout.tuichorus_fragment_selected_view, this);
        initView(rootView);
    }

    private void initView(View rootView) {
        mRvList = (SlideRecyclerView) rootView.findViewById(R.id.rl_select);
        mSelectedAdapter = new ChorusMusicSelectedAdapter(mContext, mChorusRoomInfoController, mSelectedList,
                new ChorusMusicSelectedAdapter.OnUpdateItemClickListener() {
                    @Override
                    public void onNextSongClick(String id) {
                        if (lastCLickTime > 0) {
                            long current = System.currentTimeMillis();
                            if (current - lastCLickTime < 300) {
                                return;
                            }
                        }
                        lastCLickTime = System.currentTimeMillis();
                        mChorusMusicImpl.nextMusic(new ChorusMusicCallback.ActionCallback() {
                            @Override
                            public void onCallback(int code, String msg) {
                                Log.d(TAG, "nextMusic: code = " + code);
                            }
                        });
                    }

                    @Override
                    public void onSetTopClick(String musicId) {
                        mChorusMusicImpl.topMusic(musicId, new ChorusMusicCallback.ActionCallback() {
                            @Override
                            public void onCallback(int code, String msg) {
                                Log.d(TAG, "topMusic: code = " + code);
                            }
                        });
                    }
                });
        mSelectedAdapter.setOnDeleteClickListener(new ChorusMusicSelectedAdapter.OnDeleteClickLister() {
            @Override
            public void onDeleteClick(View view, int position) {
                if (mSelectedList.size() > 1) {
                    mChorusMusicImpl.deleteMusic(mSelectedList.get(position).musicId, new ChorusMusicCallback.ActionCallback() {
                        @Override
                        public void onCallback(int code, String msg) {
                            Log.d(TAG, "deleteMusic: code = " + code);
                        }
                    });
                    mRvList.closeMenu();
                }
            }
        });
        mRvList.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mRvList.setAdapter(mSelectedAdapter);
    }

    @Override
    public void onMusicListChange(List<ChorusMusicModel> musicInfoList) {
        mSelectedList.clear();
        mSelectedList.addAll(musicInfoList);
        mSelectedAdapter.notifyDataSetChanged();
    }

    @Override
    public void onShouldSetLyric(String musicID) {

    }

    @Override
    public void onShouldPlay(ChorusMusicModel model) {

    }

    @Override
    public void onShouldStopPlay(ChorusMusicModel model) {

    }

    @Override
    public void onShouldShowMessage(ChorusMusicModel model) {

    }
}
