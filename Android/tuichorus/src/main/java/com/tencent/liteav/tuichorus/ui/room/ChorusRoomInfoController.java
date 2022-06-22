package com.tencent.liteav.tuichorus.ui.room;

import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicService;

import java.util.List;

public class ChorusRoomInfoController {

    private String                     mRoomOwnerId; //房主的Id
    private String                     mSelfUserId;  //用户的Id
    private List<ChorusRoomSeatEntity> mRoomSeatEntityList;
    private ChorusMusicService         mChorusMusicServiceImpl;

    private ChorusMusicModel mTopMusicModel;

    public String getSelfUserId() {
        return mSelfUserId;
    }

    public ChorusRoomInfoController() {
        mSelfUserId = UserModelManager.getInstance().getUserModel().userId;
    }

    public String getRoomOwnerId() {
        return mRoomOwnerId;
    }

    public void setRoomOwnerId(String roomOwnerId) {
        mRoomOwnerId = roomOwnerId;
    }

    public void setRoomSeatEntityList(List<ChorusRoomSeatEntity> roomSeatEntityList) {
        mRoomSeatEntityList = roomSeatEntityList;
    }

    public List<ChorusRoomSeatEntity> getRoomSeatEntityList() {
        return mRoomSeatEntityList;
    }

    //是否是主播
    public boolean isAnchor() {
        if (mSelfUserId == null || mRoomSeatEntityList == null || mRoomSeatEntityList.size() <= 0) {
            return false;
        }
        for (ChorusRoomSeatEntity entity : mRoomSeatEntityList) {
            if (entity != null && mSelfUserId.equals(entity.userId)) {
                return true;
            }
        }
        return false;
    }

    //是否是房主
    public boolean isRoomOwner() {
        if (mSelfUserId != null && mRoomOwnerId != null) {
            return mSelfUserId.equals(mRoomOwnerId);
        }
        return false;
    }

    public void setMusicImpl(ChorusMusicService chorusMusicService) {
        mChorusMusicServiceImpl = chorusMusicService;
    }

    public ChorusMusicService getMusicServiceImpl() {
        return mChorusMusicServiceImpl;
    }

    public ChorusRoomSeatEntity getCurrentSeatEntity(String userId) {
        if (userId == null || mRoomSeatEntityList == null || mRoomSeatEntityList.size() <= 0) {
            return null;
        }
        for (ChorusRoomSeatEntity entity : mRoomSeatEntityList) {
            if (entity != null && userId.equals(entity.userId)) {
                return entity;
            }
        }
        return null;
    }

    public void setTopMusicModel(ChorusMusicModel model) {
        mTopMusicModel = model;
    }

    public ChorusMusicModel getTopMusicModel() {
        return mTopMusicModel;
    }
}
