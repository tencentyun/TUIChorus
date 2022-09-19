package com.tencent.liteav.tuichorus.ui.music.impl;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;
import com.tencent.liteav.tuichorus.ui.widget.RoundCornerImageView;
import com.tencent.liteav.basic.ImageLoader;

import java.util.List;

public class ChorusMusicSelectedAdapter extends RecyclerView.Adapter<ChorusMusicSelectedAdapter.ViewHolder>
        implements View.OnClickListener {

    protected Context                   mContext;
    protected List<ChorusMusicModel>    mSelectedList;
    protected OnUpdateItemClickListener mOnUpdateItemClickListener;
    private   OnDeleteClickLister       mDeleteClickListener;
    private   OnItemClickListener       mListener;
    private   ChorusRoomInfoController  mChorusRoomInfoController;

    public ChorusMusicSelectedAdapter(Context context, ChorusRoomInfoController controller,
                                      List<ChorusMusicModel> selectedList,
                                      OnUpdateItemClickListener listener) {
        this.mContext = context;
        this.mChorusRoomInfoController = controller;
        this.mSelectedList = selectedList;
        this.mOnUpdateItemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.tuichorus_fragment_selected_itemview, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChorusMusicModel item = mSelectedList.get(holder.getAdapterPosition());
        holder.bind(mContext, position, item, mOnUpdateItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mSelectedList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private RoundCornerImageView mImageCover;
        private TextView             mTvSongID;
        private ImageView            mImgIcon;
        private TextView             mTvSongName;
        private TextView             mTvSinger;
        private ImageButton          mBtnNext;
        private ImageButton          mBtnSetTop;
        private TextView             mTvDelete;
        private TextView             mTvUserName;
        private TextView             mTvSeatName;

        public ViewHolder(View itemView) {
            super(itemView);
            initView(itemView);
        }

        private void initView(final View itemView) {
            mTvSongID = itemView.findViewById(R.id.tv_song_id);
            mImgIcon = itemView.findViewById(R.id.img_song_id);
            mImageCover = (RoundCornerImageView) itemView.findViewById(R.id.img_cover);
            mTvSongName = itemView.findViewById(R.id.tv_song_name);
            mTvSinger = itemView.findViewById(R.id.tv_singer);
            mBtnNext = itemView.findViewById(R.id.btn_nextsong);
            mBtnSetTop = itemView.findViewById(R.id.btn_set_top);
            mTvDelete = (TextView) itemView.findViewById(R.id.tv_delete);
            mTvUserName = (TextView) itemView.findViewById(R.id.tv_user_name);
            mTvSeatName = (TextView) itemView.findViewById(R.id.tv_seat_name);
        }

        public void bind(Context context, final int position, final ChorusMusicModel model,
                         final OnUpdateItemClickListener listener) {

            //共有信息
            mTvSongName.setText(model.musicName);
            mTvSinger.setText(mContext.getString(R.string.tuichorus_singer, model.singer));
            //根据用户Id,从麦位表获取当前歌曲的用户名和座位Id
            if (mChorusRoomInfoController != null) {
                ChorusRoomSeatEntity seatEntity = mChorusRoomInfoController.getCurrentSeatEntity(model.bookUser);
                if (seatEntity != null) {
                    mTvUserName.setText(seatEntity.userName);
                    mTvSeatName.setText(context.getString(R.string.tuichorus_tv_seat_id,
                            String.valueOf(seatEntity.index + 1)));
                    ImageLoader.loadImage(context, mImageCover, seatEntity.userAvatar, R.drawable.tuichorus_ic_cover);
                }
            }

            //如果当前不是房主,只能查看已点列表
            if (mChorusRoomInfoController != null && !mChorusRoomInfoController.isRoomOwner()) {
                mBtnNext.setVisibility(View.GONE);
                mBtnSetTop.setVisibility(View.GONE);

                if (getAdapterPosition() == 0) {
                    mTvSongID.setVisibility(View.GONE);
                    mImgIcon.setVisibility(View.VISIBLE);
                    ImageLoader.loadGifImage(mContext, mImgIcon, R.drawable.tuichorus_bg_music);
                } else {
                    mImgIcon.setVisibility(View.GONE);
                    mTvSongID.setVisibility(View.VISIBLE);
                    mTvSongID.setText(String.valueOf(getAdapterPosition() + 1));
                }

                //判断是当前用户,可以删除自己的歌
                String mSelfUserID = UserModelManager.getInstance().getUserModel().userId;
                if (model.bookUser != null && model.bookUser.equals(mSelfUserID)) {
                    mTvDelete.setVisibility(View.VISIBLE);
                    if (!mTvDelete.hasOnClickListeners()) {
                        mTvDelete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mDeleteClickListener != null) {
                                    mDeleteClickListener.onDeleteClick(v, getAdapterPosition());
                                }
                            }
                        });
                    }
                } else {
                    mTvDelete.setVisibility(View.GONE);
                }
            } else {
                //实现左滑删除
                if (!mTvDelete.hasOnClickListeners()) {
                    mTvDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mDeleteClickListener != null) {
                                if (getLayoutPosition() != 0) {
                                    mDeleteClickListener.onDeleteClick(v, getAdapterPosition());
                                }
                            }
                        }
                    });
                }

                //处理第一首已点歌曲
                if (getAdapterPosition() == 0) {
                    mBtnNext.setVisibility(View.VISIBLE);
                    mBtnSetTop.setVisibility(View.GONE);
                    mTvSongID.setVisibility(View.GONE);
                    mImgIcon.setVisibility(View.VISIBLE);
                    ImageLoader.loadGifImage(mContext, mImgIcon, R.drawable.tuichorus_bg_music);
                } else if (getAdapterPosition() == 1) {
                    mBtnNext.setVisibility(View.GONE);
                    mBtnSetTop.setVisibility(View.VISIBLE);
                    mBtnSetTop.setBackgroundResource(R.drawable.tuichorus_ic_settop_normal);
                    mImgIcon.setVisibility(View.GONE);
                    mTvSongID.setVisibility(View.VISIBLE);
                    mTvSongID.setText(String.valueOf(getAdapterPosition() + 1));
                } else {
                    mBtnNext.setVisibility(View.GONE);
                    mBtnSetTop.setVisibility(View.VISIBLE);
                    mBtnSetTop.setBackgroundResource(R.drawable.tuichorus_ic_settop_hover);
                    mImgIcon.setVisibility(View.GONE);
                    mTvSongID.setVisibility(View.VISIBLE);
                    mTvSongID.setText(String.valueOf(getAdapterPosition() + 1));
                }

                mBtnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onNextSongClick(model.musicId);
                    }
                });
                mBtnSetTop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position != 1) {
                            listener.onSetTopClick(model.musicId);
                        }
                    }
                });
            }
        }
    }

    public interface OnUpdateItemClickListener {
        void onNextSongClick(String id);

        void onSetTopClick(String musicId);
    }

    //左滑删除监听接口
    public void setOnDeleteClickListener(OnDeleteClickLister listener) {
        this.mDeleteClickListener = listener;
    }

    public interface OnDeleteClickLister {
        void onDeleteClick(View view, int position);
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onItemClick(this, v, (Integer) v.getTag());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.Adapter adapter, View v, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mListener = onItemClickListener;
    }
}
