package com.tencent.liteav.tuichorus.ui.music.impl;

import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicInfo;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicCallback;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicService;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicServiceDelegate;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;

import java.util.ArrayList;
import java.util.List;

public class ChorusMusicLibraryView extends CoordinatorLayout implements ChorusMusicServiceDelegate {
    private final Context                   mContext;
    private       ChorusMusicLibraryAdapter mLibraryListAdapter;
    private       List<ChorusMusicModel>    mLibraryLists;
    private final ChorusMusicService        mChorusMusicImpl;
    private       int                       mPage         = 1;
    private       int                       mLoadPageSize = 10;
    private       ChorusRoomInfoController  mChorusRoomInfoController;

    public ChorusMusicLibraryView(Context context, ChorusRoomInfoController chorusRoomInfoController) {
        super(context);
        mContext = context;
        mChorusRoomInfoController = chorusRoomInfoController;
        mChorusMusicImpl = chorusRoomInfoController.getMusicServiceImpl();
        mChorusMusicImpl.setServiceDelegate(this);
        mLibraryLists = new ArrayList<>();
        mChorusMusicImpl.chorusGetMusicPage(mPage, mLoadPageSize, new ChorusMusicCallback.MusicListCallback() {
            @Override
            public void onCallback(int code, String msg, List<ChorusMusicInfo> list) {
                mLibraryLists.clear();
                for (ChorusMusicInfo info : list) {
                    ChorusMusicModel model = new ChorusMusicModel();
                    model.musicId = info.musicId;
                    model.musicName = info.musicName;
                    model.singer = info.singer;
                    model.contentUrl = info.contentUrl;
                    model.coverUrl = info.coverUrl;
                    model.lrcUrl = info.lrcUrl;
                    model.isSelected = false;
                    mLibraryLists.add(model);
                }
            }
        });
        View rootView = LayoutInflater.from(context).inflate(R.layout.tuichorus_fragment_library_view, this);
        initView(rootView);
    }

    private void initView(View rootView) {
        RecyclerView rvList = (RecyclerView) rootView.findViewById(R.id.rl_library);
        mLibraryListAdapter = new ChorusMusicLibraryAdapter(mContext, mChorusRoomInfoController, mLibraryLists,
                new ChorusMusicLibraryAdapter.OnPickItemClickListener() {
                    @Override
                    public void onPickSongItemClick(String musicId, int layoutPosition) {
                        updateSelectedList(musicId);
                    }
                });
        rvList.setLayoutManager(new GridLayoutManager(mContext, 1));
        rvList.setAdapter(mLibraryListAdapter);
        mLibraryListAdapter.notifyDataSetChanged();
    }

    private void updateSelectedList(String musicId) {
        mChorusMusicImpl.pickMusic(musicId, new ChorusMusicCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {

            }
        });
    }

    @Override
    public void onMusicListChange(List<ChorusMusicModel> musicInfoList) {
        for (int i = 0; i < mLibraryLists.size(); i++) {
            boolean flag = false;
            for (int j = 0; j < musicInfoList.size(); j++) {
                if (mLibraryLists.get(i).musicId.equals(musicInfoList.get(j).musicId)) {
                    flag = true;
                }
            }
            mLibraryLists.get(i).isSelected = flag;
        }
        mLibraryListAdapter.notifyDataSetChanged();
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
