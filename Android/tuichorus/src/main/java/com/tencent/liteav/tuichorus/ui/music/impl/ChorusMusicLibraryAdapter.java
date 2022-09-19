package com.tencent.liteav.tuichorus.ui.music.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.base.ChorusMusicModel;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;
import com.tencent.liteav.tuichorus.ui.widget.RoundCornerImageView;

import java.util.List;

public class ChorusMusicLibraryAdapter extends RecyclerView.Adapter<ChorusMusicLibraryAdapter.ViewHolder> {
    protected Context                  mContext;
    protected List<ChorusMusicModel>   mLibraryList;
    protected OnPickItemClickListener  onPickItemClickListener;
    private   ChorusRoomInfoController mChorusRoomInfoController;

    public ChorusMusicLibraryAdapter(Context context, ChorusRoomInfoController controller,
                                     List<ChorusMusicModel> libraryList,
                                     OnPickItemClickListener onPickItemClickListener) {
        this.mContext = context;
        this.mChorusRoomInfoController = controller;
        this.mLibraryList = libraryList;
        this.onPickItemClickListener = onPickItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.tuichorus_fragment_library_itemview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChorusMusicModel item = mLibraryList.get(position);
        holder.bind(mContext, item, onPickItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mLibraryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private RoundCornerImageView mImageCover;
        private TextView             mTvSongName;
        private TextView             mTvSinger;
        private Button               mBtnChoose;
        private boolean              mSelect;

        public ViewHolder(View itemView) {
            super(itemView);
            initView(itemView);
        }

        private void initView(final View itemView) {
            mImageCover = (RoundCornerImageView) itemView.findViewById(R.id.img_cover);
            mTvSongName = (TextView) itemView.findViewById(R.id.tv_song_name);
            mTvSinger = (TextView) itemView.findViewById(R.id.tv_singer);
            mBtnChoose = (Button) itemView.findViewById(R.id.btn_choose_song);
        }

        public void bind(Context context, final ChorusMusicModel model,
                         final OnPickItemClickListener listener) {

            mBtnChoose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //只有大主播(房主)可以点歌,其他人不可以点歌
                    if (!mChorusRoomInfoController.isRoomOwner()) {
                        ToastUtils.showLong(R.string.tuichorus_toast_anchor_can_only_operate_it);
                        return;
                    }
                    updateChooseButton(!mSelect);
                    listener.onPickSongItemClick(model.musicId, getLayoutPosition());
                }
            });
            updateChooseButton(model.isSelected);
            mTvSongName.setText(model.musicName);
            mTvSinger.setText(model.singer);
        }

        public void updateChooseButton(boolean isSelect) {
            if (isSelect) {
                mBtnChoose.setText(mContext.getText(R.string.tuichorus_btn_choosed_song));
                mBtnChoose.setBackgroundResource(R.drawable.tuichorus_button_choose_song);
                mBtnChoose.setTextColor(mContext.getResources().getColor(R.color.tuichorus_text_color_second));
                mBtnChoose.setEnabled(false);
                mSelect = true;
            } else {
                mBtnChoose.setText(mContext.getText(R.string.tuichorus_btn_choose_song));
                mBtnChoose.setBackgroundResource(R.drawable.tuichorus_button_border);
                mBtnChoose.setEnabled(true);
                mSelect = false;
            }
        }
    }

    public interface OnPickItemClickListener {
        void onPickSongItemClick(String id, int layoutPosition);
    }

}
