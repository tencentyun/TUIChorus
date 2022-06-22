package com.tencent.liteav.tuichorus.ui.room;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.liteav.basic.ImageLoader;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.base.ChorusRoomSeatEntity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChorusRoomSeatAdapter extends
        RecyclerView.Adapter<ChorusRoomSeatAdapter.ViewHolder> {
    private static String TAG = ChorusRoomSeatAdapter.class.getSimpleName();

    private Context                    mContext;
    private List<ChorusRoomSeatEntity> mList;
    private OnItemClickListener        mOnItemClickListener;
    private String                     mBaseHeadIcon = "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar1.png";
    
    public ChorusRoomSeatAdapter(Context context, List<ChorusRoomSeatEntity> list,
                                 OnItemClickListener onItemClickListener) {
        this.mContext = context;
        this.mList = list;
        this.mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context        context  = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.tuichorus_item_seat_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChorusRoomSeatEntity item = mList.get(position);
        holder.bind(mContext, position, item, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView mImgSeatHead;
        public TextView        mTvName;
        public ImageView       mIvTalkBorder;

        public ViewHolder(View itemView) {
            super(itemView);
            initView(itemView);
        }

        public void bind(final Context context, int position,
                         final ChorusRoomSeatEntity model,
                         final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(getLayoutPosition());

                }
            });
            if (model.isClose) {
                mImgSeatHead.setImageResource(R.drawable.tuichorus_ic_lock);
                mTvName.setText("");
                mIvTalkBorder.setVisibility(View.GONE);
                return;
            }
            if (!model.isUsed) {
                // 占位图片
                mImgSeatHead.setImageResource(R.drawable.tuichorus_add_seat);
                mTvName.setText(context.getResources().getString(R.string.tuichorus_tv_seat_id, String.valueOf(position + 1)));
                mIvTalkBorder.setVisibility(View.GONE);
            } else {

                if (TextUtils.isEmpty(model.userAvatar) || !isUrl(model.userAvatar)) {
                    ImageLoader.loadImage(context, mImgSeatHead, mBaseHeadIcon, R.drawable.tuichorus_ic_cover);
                } else {
                    ImageLoader.loadImage(context.getApplicationContext(), mImgSeatHead,
                            model.userAvatar, R.drawable.tuichorus_ic_cover);
                }
                if (!TextUtils.isEmpty(model.userName)) {
                    mTvName.setText(model.userName);
                } else {
                    mTvName.setText(R.string.tuichorus_tv_the_anchor_name_is_still_looking_up);
                }
                //麦上说话显示光圈
                mIvTalkBorder.setVisibility(model.isTalk ? View.VISIBLE : View.GONE);
            }
        }

        private void initView(@NonNull final View itemView) {
            mImgSeatHead = (CircleImageView) itemView.findViewById(R.id.img_seat_head);
            mTvName = (TextView) itemView.findViewById(R.id.tv_name);
            mIvTalkBorder = (ImageView) itemView.findViewById(R.id.iv_talk_border);
        }

        private boolean isUrl(String url) {
            return url.startsWith("http://") || url.startsWith("https://");
        }
    }
}