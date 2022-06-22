package com.tencent.liteav.demo.chorusimpl;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.imsdk.v2.V2TIMGroupMemberInfo;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.imsdk.v2.V2TIMSimpleMsgListener;
import com.tencent.imsdk.v2.V2TIMUserInfo;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomDef;
import com.tencent.liteav.tuichorus.model.impl.base.TRTCLogger;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicInfo;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicCallback;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicService;
import com.tencent.liteav.tuichorus.ui.music.ChorusMusicServiceDelegate;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲管理实现类
 */
public class ChorusMusicServiceImpl extends ChorusMusicService {
    private static String TAG = "MusicServiceImpl";

    private List<ChorusMusicServiceDelegate> mSelectDelegates = new ArrayList<>();
    private ChorusMusicListener              mSimpleListener;

    private List<ChorusMusicModel> mMusicLibraryList;   //点歌列表
    private List<ChorusMusicModel> mMusicSelectedList;  //已点列表
    private String                 mRoomId;
    private String                 mOwnerId;            //房主的id
    private String                 mCurrentMusicId;
    private Context                mContext;

    public ChorusMusicServiceImpl() {
        mMusicLibraryList = new ArrayList<>();
        mMusicSelectedList = new ArrayList<>();
        mSimpleListener = new ChorusMusicListener();
        // 初始化IM
        initIMListener();
    }

    public ChorusMusicServiceImpl(Context context) {
        mContext = context;
        mMusicLibraryList = new ArrayList<>();
        mMusicSelectedList = new ArrayList<>();
        mSimpleListener = new ChorusMusicListener();
        // 初始化IM
        initIMListener();
    }

    @Override
    public void setServiceDelegate(ChorusMusicServiceDelegate delegate) {
        if (!mSelectDelegates.contains(delegate)) {
            mSelectDelegates.add(delegate);
        }
    }

    public boolean isOwner() {
        if (mOwnerId == null) {
            return false;
        }
        return mOwnerId.equals(UserModelManager.getInstance().getUserModel().userId);
    }

    private String buildGroupMsg(String cmd, List<ChorusMusicModel> list) {
        ChorusJsonData jsonData = new ChorusJsonData();
        try {
            jsonData.setVersion(ChorusConstants.CHORUS_VALUE_CMD_VERSION);
            jsonData.setBusinessID(ChorusConstants.CHORUS_VALUE_CMD_BUSINESSID);
            jsonData.setPlatform(ChorusConstants.CHORUS_VALUE_CMD_PLATFORM);

            ChorusJsonData.Data data = new ChorusJsonData.Data();
            data.setRoomId(mRoomId);
            data.setInstruction(cmd);

            Gson gsonContent = new Gson();
            String content = gsonContent.toJson(mMusicSelectedList);

            data.setContent(content);
            jsonData.setData(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();

        return gson.toJson(jsonData);
    }

    private String buildSingleMsg(String cmd, String content) {
        ChorusJsonData jsonData = new ChorusJsonData();
        try {
            jsonData.setVersion(ChorusConstants.CHORUS_VALUE_CMD_VERSION);
            jsonData.setBusinessID(ChorusConstants.CHORUS_VALUE_CMD_BUSINESSID);
            jsonData.setPlatform(ChorusConstants.CHORUS_VALUE_CMD_PLATFORM);

            ChorusJsonData.Data data = new ChorusJsonData.Data();
            data.setRoomId(mRoomId);
            data.setInstruction(cmd);
            data.setContent(content);

            jsonData.setData(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        return gson.toJson(jsonData);
    }

    @Override
    public void chorusGetMusicPage(int page, int pageSize, ChorusMusicCallback.MusicListCallback callback) {
        MusicInfoController musicInfoController = new MusicInfoController(mContext);
        List<ChorusMusicInfo> list = musicInfoController.getLibraryList();
        callback.onCallback(0, "success", list);

        if (mMusicLibraryList.size() <= 0) {
            for (ChorusMusicInfo info : list) {
                ChorusMusicModel model = new ChorusMusicModel();
                model.musicId = info.musicId;
                model.musicName = info.musicName;
                model.singer = info.singer;
                model.contentUrl = info.contentUrl;
                model.coverUrl = info.coverUrl;
                model.lrcUrl = info.lrcUrl;
                model.isSelected = false;
                mMusicLibraryList.add(model);
            }
        }
    }

    @Override
    public void chorusGetSelectedMusicList(ChorusMusicCallback.MusicSelectedListCallback callback) {
        synchronized (this) {
            if (isOwner()) {
                if (mMusicSelectedList.size() > 0) {
                    if (mSelectDelegates != null) {
                        for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                            delegate.onShouldSetLyric(mMusicSelectedList.get(0).musicId);
                        }
                    }
                }
                callback.onCallback(0, "success", mMusicSelectedList);
            } else {
                sendRequestSelectedList();
            }
        }
    }

    @Override
    public void pickMusic(String musicID, ChorusMusicCallback.ActionCallback callback) {
        boolean shouldPlay = mMusicSelectedList.size() == 0;
        ChorusMusicModel songEntity = findEntityFromLibrary(musicID);
        songEntity.isSelected = true;

        //房主点歌,自己更新列表,且如果是点的第一首歌,则播放
        if (isOwner()) {
            songEntity.bookUser = mOwnerId;
            mMusicSelectedList.add(songEntity);
            callback.onCallback(0, "succeed");
            notiListChange();
            if (shouldPlay) {
                if (mSelectDelegates != null) {
                    for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                        delegate.onShouldPlay(songEntity);
                    }
                }
            }
            if (mSelectDelegates != null) {
                for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                    delegate.onShouldShowMessage(songEntity);
                }
            }
        } else {
            //其他主播点歌,发通知给房主
            sendInstruction(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPICK, mOwnerId, songEntity.musicId);
        }
    }

    @Override
    public void deleteMusic(String musicID, ChorusMusicCallback.ActionCallback callback) {
        if (isOwner()) {
            synchronized (this) {
                ChorusMusicModel entity = findEntityFromLibrary(musicID);
                if (mMusicSelectedList != null && mMusicSelectedList.size() > 0) {
                    mMusicSelectedList.remove(entity);
                }
                notiListChange();
            }
        } else {
            sendDeleteMusic(musicID);
        }
    }

    @Override
    public void deleteAllMusic(String userID, ChorusMusicCallback.ActionCallback callback) {
        if (mMusicSelectedList.size() <= 0 || userID == null) {
            return;
        }
        //房主下麦
        if (isOwner()) {
            synchronized (this) {
                if (mMusicSelectedList.size() > 0) {
                    List<ChorusMusicModel> list = new ArrayList<>();
                    for (ChorusMusicModel temp : mMusicSelectedList) {
                        if (temp != null && userID.equals(temp.bookUser)) {
                            list.add(temp);
                        }
                    }
                    mMusicSelectedList.removeAll(list);
                    notiListChange();
                }
            }
        } else {
            //如果是其他主播下麦,通知房主删除歌曲,并更新
            sendDeleteAll();
        }
    }

    @Override
    public void topMusic(String musicID, ChorusMusicCallback.ActionCallback callback) {
        if (mMusicSelectedList.size() <= 2 || !isOwner() || musicID == null) {
            return;
        }

        ChorusMusicModel entity = null;
        for (ChorusMusicModel temp : mMusicSelectedList) {
            if (temp != null && musicID.equals(temp.musicId)) {
                entity = temp;
                break;
            }
        }
        mMusicSelectedList.remove(entity);
        mMusicSelectedList.add(1, entity);

        notiListChange();
    }

    @Override
    public void nextMusic(ChorusMusicCallback.ActionCallback callback) {
        if (mMusicSelectedList.size() <= 0 || !isOwner()) {
            return;
        }
        ChorusMusicModel entity = mMusicSelectedList.get(0); //备份

        //如果房主切的是自己的歌
        if (entity.bookUser.equals(mOwnerId)) {
            if (mSelectDelegates != null) {
                for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                    delegate.onShouldStopPlay(entity);
                }
            }
        } else {
            //如果房主切的其他人的歌,先通知其他人停止播放,然后调用complete发通知给房主,房主确定下一首谁播
            sendShouldStop(entity.bookUser, entity.musicId);

        }
    }

    @Override
    public void downLoadMusic(String musicId, ChorusMusicCallback.ActionCallback callback) {

    }

    @Override
    public void downLoadLrc(String musicId, ChorusMusicCallback.ActionCallback callback) {

    }

    @Override
    public void setRoomInfo(TRTCChorusRoomDef.RoomInfo roomInfo) {
        mRoomId = String.valueOf(roomInfo.roomId);
        mOwnerId = roomInfo.ownerId;
    }

    @Override
    public void prepareToPlay(String musicID) {
        notiPrepare(musicID);
    }

    @Override
    public void completePlaying(String musicID) {
        if (!isOwner()) {
            return;
        }

        if (mMusicSelectedList.size() <= 0) {
            if (mSelectDelegates != null) {
                for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                    delegate.onShouldSetLyric("0");
                }
            }
            notiPrepare("0");
            return;
        }
        synchronized (this) {
            if (musicID.equals(mMusicSelectedList.get(0).musicId)) {
                mMusicSelectedList.remove(0); //移除第一首歌
                notiListChange();
                notiComplete(musicID);
            }
        }
        synchronized (this) {
            //如果切歌后已点列表还有歌,判断歌曲是谁的,通知播放.
            if (mMusicSelectedList.size() <= 0) {
                return;
            }
            ChorusMusicModel curEntity = mMusicSelectedList.get(0);
            if (curEntity != null && curEntity.bookUser.equals(mOwnerId)) {
                if (mSelectDelegates != null) {
                    for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                        delegate.onShouldPlay(mMusicSelectedList.get(0));
                    }
                }
            } else {
                sendShouldPlay(curEntity.bookUser, curEntity.musicId);
            }
        }
    }

    @Override
    public void onExitRoom() {
        unInitImListener();
        if (mMusicLibraryList != null) {
            mMusicLibraryList.clear();
            mMusicLibraryList = null;
        }
        if (mMusicSelectedList != null) {
            mMusicSelectedList.clear();
            mMusicSelectedList = null;
        }
        if (mSelectDelegates != null) {
            mSelectDelegates.clear();
            mSelectDelegates = null;
        }
    }

    public ChorusMusicModel findEntityFromLibrary(String musicId) {
        if (musicId == null || mMusicLibraryList == null) {
            return null;
        }
        for (ChorusMusicModel entity : mMusicLibraryList) {
            if (entity.musicId.equals(musicId)) {
                return entity;
            }
        }
        return null;
    }

    // 收发信息的管理
    // 准备播放，发通知，收到通知后应准备好歌词
    private void notiPrepare(String musicId) {
        String data = buildSingleMsg(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPREPARE, musicId);
        Log.d(TAG, "sendNoti: cmd= " + ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPREPARE);
        sendNoti(data);
    }

    // 播放完成时，应发送complete消息,然后房主进行列表更新,并通知其他人
    private void notiComplete(String musicId) {
        if (mSelectDelegates != null) {
            for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                delegate.onShouldSetLyric("0");
            }
        }
        String data = buildSingleMsg(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE, musicId);
        Log.d(TAG, "sendNoti: cmd= " + ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE);
        sendNoti(data);
    }

    // 给某人发送应该播放音乐了（下一个是你）
    private void sendShouldPlay(String userId, String musicId) {
        sendInstruction(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPLAYMUSIC, userId, musicId);
    }

    // 给某人发送应该停止了（被切歌了）
    private void sendShouldStop(String userId, String musicId) {
        sendInstruction(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MSTOP, userId, musicId);
    }

    //主播删除自己的歌
    private void sendDeleteMusic(String musicId) {
        sendInstruction(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MDELETE, mOwnerId, musicId);
    }

    //主播下麦发通知给房主清除列表
    private void sendDeleteAll() {
        sendInstruction(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MDELETEALL, mOwnerId, "");
    }

    private void sendRequestSelectedList() {
        sendInstruction(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MGETLIST, mOwnerId, "");
    }

    // 广播通知列表发生变化
    private void notiListChange() {
        if (mSelectDelegates != null) {
            for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                delegate.onMusicListChange(mMusicSelectedList);
            }
        }

        String data = buildGroupMsg(ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE, mMusicSelectedList);
        TRTCLogger.d(TAG, "sendNoti: cmd= " + ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE);
        sendNoti(data);
    }

    private void initIMListener() {
        V2TIMManager.getMessageManager();
        V2TIMManager.getInstance().addSimpleMsgListener(mSimpleListener);
    }

    private void unInitImListener() {
        V2TIMManager.getInstance().removeSimpleMsgListener(mSimpleListener);
    }

    public void sendNoti(String data) {
        V2TIMManager.getInstance().sendGroupCustomMessage(data.getBytes(), mRoomId,
                V2TIMMessage.V2TIM_PRIORITY_NORMAL, new V2TIMValueCallback<V2TIMMessage>() {
                    @Override
                    public void onSuccess(V2TIMMessage v2TIMMessage) {
                    }

                    @Override
                    public void onError(int code, String desc) {
                        TRTCLogger.d(TAG, "sendNoti onError:" + desc);
                    }
                });
    }

    public void sendInstruction(String cmd, String userId, String content) {
        Log.d(TAG, "sendInstruction: cmd = " + cmd + " , content = " + content);
        String data = buildSingleMsg(cmd, content);
        V2TIMManager.getInstance().sendC2CCustomMessage(data.getBytes(), userId,
                new V2TIMValueCallback<V2TIMMessage>() {
                    @Override
                    public void onSuccess(V2TIMMessage v2TIMMessage) {
                    }

                    @Override
                    public void onError(int code, String desc) {
                        TRTCLogger.d(TAG, "sendInstruction onError: code = " + code);
                    }
                });
    }

    private class ChorusMusicListener extends V2TIMSimpleMsgListener {
        public ChorusMusicListener() {
            super();
        }

        @Override
        public void onRecvC2CTextMessage(String msgID, V2TIMUserInfo sender, String text) {
            super.onRecvC2CTextMessage(msgID, sender, text);
        }

        @Override
        public void onRecvC2CCustomMessage(String msgID, V2TIMUserInfo sender, byte[] customData) {
            String customStr = new String(customData);
            if (TextUtils.isEmpty(customStr)) {
                Log.d(TAG, "onRecvC2CCustomMessage  the customData is null");
                return;
            }
            try {
                Gson gson = new Gson();
                ChorusJsonData jsonData = gson.fromJson(customStr, ChorusJsonData.class);
                String businessID = jsonData.getBusinessID();
                if (!ChorusConstants.CHORUS_VALUE_CMD_BUSINESSID.equals(businessID)) {
                    return;
                }
                ChorusJsonData.Data data = jsonData.getData();
                String instruction = data.getInstruction();
                String musicId = data.getContent();
                ChorusMusicModel entity = findEntityFromLibrary(musicId);
                TRTCLogger.d(TAG, "RecvC2CMessage: instruction = " + instruction + " customStr = " + customStr);
                switch (instruction) {
                    case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MGETLIST:
                        //房主收到其他人的进房请求后,更新列表,然后通知去设置歌词
                        if (!isOwner()) {
                            return;
                        }
                        notiListChange();
                        if (mMusicSelectedList.size() > 0) {
                            notiPrepare(mMusicSelectedList.get(0).musicId);
                        }
                        break;
                    case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPICK:
                        pick(entity, sender);
                        break;
                    case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPLAYMUSIC:
                        if (mSelectDelegates != null) {
                            for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                                delegate.onShouldPlay(entity);
                            }
                        }
                        break;
                    case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MSTOP:
                        if (mSelectDelegates != null && mSelectDelegates.size() > 0) {
                            for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                                delegate.onShouldStopPlay(entity);
                            }
                        }
                        break;
                    case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MDELETE:
                        //房主收到主播删除歌曲的请求后,直接删除歌曲
                        if (mMusicSelectedList.size() > 0) {
                            mMusicSelectedList.remove(entity);
                        }
                        notiListChange();
                        break;
                    case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MDELETEALL:
                        deleteAll(sender);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //房主收到"CHORUS_VALUE_CMD_INSTRUCTION_MPICK"消息
        private void pick(ChorusMusicModel entity, V2TIMUserInfo sender) {
            //房主收到其他人的点歌后,去更新列表并通知主播去播放;其他人不处理该通知
            if (!isOwner()) {
                return;
            }
            entity.bookUser = sender.getUserID();
            boolean shouldPlay = mMusicSelectedList.size() == 0;
            mMusicSelectedList.add(entity);
            notiListChange();
            if (shouldPlay) {
                sendShouldPlay(sender.getUserID(), entity.musicId);
            }
            if (mSelectDelegates != null) {
                for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                    delegate.onShouldShowMessage(entity);
                }
            }
        }

        //房主收到"CHORUS_VALUE_CMD_INSTRUCTION_MDELETEALL"消息
        private void deleteAll(V2TIMUserInfo sender) {
            // 房主处理,其他人收到不处理
            if (!isOwner()) {
                return;
            }
            if (mMusicSelectedList.size() > 0) {
                List<ChorusMusicModel> list = new ArrayList<>();
                for (ChorusMusicModel temp : mMusicSelectedList) {
                    if (sender != null && sender.getUserID().equals(temp.bookUser)) {
                        list.add(temp);
                    }
                }
                mMusicSelectedList.removeAll(list);
                notiListChange();
            }
        }

        @Override
        public void onRecvGroupTextMessage(String msgID, String groupID, V2TIMGroupMemberInfo sender, String text) {
            super.onRecvGroupTextMessage(msgID, groupID, sender, text);
        }

        @Override
        public void onRecvGroupCustomMessage(String msgID, String groupID,
                                             V2TIMGroupMemberInfo sender, byte[] customData) {
            String customStr = new String(customData);
            if (TextUtils.isEmpty(customStr)) {
                Log.d(TAG, "onRecvC2CCustomMessage  the customData is null");
                return;
            }
            Gson gson = new Gson();
            ChorusJsonData jsonData = gson.fromJson(customStr, ChorusJsonData.class);
            String businessID = jsonData.getBusinessID();
            if (!ChorusConstants.CHORUS_VALUE_CMD_BUSINESSID.equals(businessID)) {
                return;
            }
            ChorusJsonData.Data data = jsonData.getData();
            String instruction = data.getInstruction();
            String musicId = data.getContent();
            TRTCLogger.d(TAG, "RecvGroupMessage instruction = " + instruction + " ,data = " + data);
            switch (instruction) {
                case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE:
                    List<ChorusMusicModel> list = new ArrayList<>();
                    Gson gsonTemp = new Gson();
                    list = gsonTemp.fromJson(data.getContent(),
                            new TypeToken<List<ChorusMusicModel>>() {
                            }.getType());
                    //列表变化处理
                    musicListChange(list);
                    break;
                case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MPREPARE:
                    mCurrentMusicId = musicId;
                    if (mSelectDelegates != null) {
                        for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                            delegate.onShouldSetLyric(musicId);
                        }
                    }
                    break;
                case ChorusConstants.CHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE:
                    if (mCurrentMusicId == null || musicId.equals(mCurrentMusicId)) {
                        if (mSelectDelegates != null) {
                            for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                                delegate.onShouldSetLyric("0");
                            }
                        }
                    }
                    complete(musicId);
                    break;
                default:
                    break;
            }
        }

        //收到"CHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE"消息
        private void musicListChange(List<ChorusMusicModel> list) {
            mMusicSelectedList.clear();
            //避免ios端和Android数据不一致导致的数据异常
            //根据musicId对齐两端的信息
            if (list.size() > 0) {
                for (ChorusMusicModel temp : list) {
                    if (temp != null) {
                        ChorusMusicModel tempEntity = findEntityFromLibrary(temp.musicId);
                        if (tempEntity != null) {
                            tempEntity.bookUser = temp.bookUser;
                            tempEntity.isSelected = temp.isSelected;
                            mMusicSelectedList.add(tempEntity);
                        }
                    }
                }
            }
            //收到列表变化的通知,去更新自己的界面信息
            if (mSelectDelegates != null && mSelectDelegates.size() > 0) {
                for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                    delegate.onMusicListChange(mMusicSelectedList);
                }
            }
        }

        //收到"CHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE"消息
        private void complete(String musicId) {
            //房主收到后处理,其他人不处理
            if (!isOwner()) {
                return;
            }
            if (mMusicSelectedList.size() > 0 && musicId != null) {
                ChorusMusicModel temp = null;
                for (ChorusMusicModel model : mMusicSelectedList) {
                    if (model != null && musicId.equals(model.musicId)) {
                        temp = model;
                    }
                }
                if (temp != null) {
                    mMusicSelectedList.remove(temp);
                    notiListChange();
                }
            }
            //如果切歌后已点列表还有歌,判断歌曲是谁的,通知播放.
            if (mMusicSelectedList.size() > 0) {
                ChorusMusicModel curEntity = mMusicSelectedList.get(0);
                if (curEntity.bookUser.equals(mOwnerId)) {
                    if (mSelectDelegates != null) {
                        for (ChorusMusicServiceDelegate delegate : mSelectDelegates) {
                            delegate.onShouldPlay(curEntity);
                        }
                    }
                } else {
                    sendShouldPlay(curEntity.bookUser, curEntity.musicId);
                }
            }
        }
    }
}
