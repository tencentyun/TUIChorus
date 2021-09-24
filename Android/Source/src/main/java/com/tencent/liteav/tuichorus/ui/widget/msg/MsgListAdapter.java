package com.tencent.liteav.tuichorus.ui.widget.msg;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tencent.liteav.tuichorus.R;

import java.util.List;

/**
 * Chorus消息互动显示的适配器
 *
 * 根据消息的类型显示不同的样式，消息的发送者的username可以对颜色进行设置。
 * 普通消息：      TYPE_NORMAL        消息的内容会在界面显示出来
 * 邀请等待的消息： TYPE_WAIT_AGREE    消息中会有同意的按钮，可以进行事件处理
 * 邀请已同意消息： TYPE_AGREED        邀请消息已被处理，事件按钮被隐藏
 * 欢迎消息：      TYPE_WELCOME       会出现在界面中，同时有跳转的链接url
 * 点歌消息：      TYPE_ORDERED_SONG  消息中会有管理点歌的按钮，房主可以进行事件处理
 */
public class MsgListAdapter extends
        RecyclerView.Adapter<MsgListAdapter.ViewHolder> {

    private static final String TAG = MsgListAdapter.class.getSimpleName();

    private Context             mContext;
    private List<MsgEntity>     mList;
    private OnItemClickListener mOnItemClickListener;

    public MsgListAdapter(Context context, List<MsgEntity> list,
                          OnItemClickListener onItemClickListener) {
        this.mContext = context;
        this.mList = list;
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context        context    = parent.getContext();
        LayoutInflater inflater   = LayoutInflater.from(context);
        View           view       = inflater.inflate(R.layout.tuichorus_item_msg, parent, false);
        ViewHolder     viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MsgEntity item = mList.get(position);
        holder.bind(item, mOnItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public interface OnItemClickListener {
        void onAgreeClick(int position);
        void onOrderedManagerClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTvMsgContent;
        private TextView mBtnMsg;

        public ViewHolder(View itemView) {
            super(itemView);
            initView(itemView);
        }

        private void initView(View itemView) {
            mTvMsgContent = (TextView) itemView.findViewById(R.id.tv_msg_content);
            mBtnMsg = (TextView) itemView.findViewById(R.id.btn_msg_func);
        }

        public void bind(final MsgEntity model,
                         final OnItemClickListener listener) {
            String userName = !TextUtils.isEmpty(model.userName) ? model.userName : model.userId;
            //消息类型为进房欢迎类型，此类型的消息为定制消息，用户进房展示的欢迎语，消息中会有相应的跳转链接
            if (model.type == MsgEntity.TYPE_WELCOME) {
                String result = model.content + model.linkUrl;
                SpannableStringBuilder builder = new SpannableStringBuilder(result);
                ForegroundColorSpan welcomeTitleSpan = new ForegroundColorSpan(mContext.getResources().getColor(R.color.tuichorus_color_welcome));
                ForegroundColorSpan linkSpan = new ForegroundColorSpan(mContext.getResources().getColor(R.color.tuichorus_color_link));
                UnderlineSpan linkUnderline  = new UnderlineSpan();
                builder.setSpan(welcomeTitleSpan, 0, model.content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(linkSpan, model.content.length(), result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(linkUnderline, model.content.length(), result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mTvMsgContent.setText(builder);
                mTvMsgContent.setBackground(null);
            //消息发送者颜色定制，消息发送者的username会根据设置的颜色显示
            } else if(model.type == MsgEntity.TYPE_ORDERED_SONG){
                String                 split   = " ";
                String result = model.content + split + model.userName + model.linkUrl;
                SpannableStringBuilder builder = new SpannableStringBuilder(result);
                ForegroundColorSpan redSpan = new ForegroundColorSpan(model.color);
                int start = model.content.length() + 1;
                int end = start + model.userName.length();
                builder.setSpan(redSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mTvMsgContent.setText(builder);
                mTvMsgContent.setBackgroundResource(R.drawable.tuichorus_bg_msg_item);
            } else if (!TextUtils.isEmpty(userName) && model.color != 0) {
                String split = model.isChat ? ": " : " ";
                String result = model.userName + split + model.content;
                SpannableStringBuilder builder = new SpannableStringBuilder(result);
                ForegroundColorSpan redSpan = new ForegroundColorSpan(model.color);
                builder.setSpan(redSpan, 0, model.userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mTvMsgContent.setText(builder);
                mTvMsgContent.setBackgroundResource(R.drawable.tuichorus_bg_msg_item);
            } else {
                mTvMsgContent.setText(model.content);
                mTvMsgContent.setBackgroundResource(R.drawable.tuichorus_bg_msg_item);
            }

            updateMsgButtom(model);

            mBtnMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        if(model.type == MsgEntity.TYPE_WAIT_AGREE){
                            listener.onAgreeClick(getLayoutPosition());
                        } else if(model.type == MsgEntity.TYPE_ORDERED_SONG) {
                            listener.onOrderedManagerClick(getLayoutPosition());
                        }
                    }
                }
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (model.type == MsgEntity.TYPE_WELCOME) {
                        startLinkActivity(model.linkUrl);
                    }
                }
            });
        }

        private void updateMsgButtom(MsgEntity model) {
            if (model.type == MsgEntity.TYPE_AGREED) {
                mBtnMsg.setVisibility(View.GONE);
                mBtnMsg.setEnabled(false);
            } else if (model.type == MsgEntity.TYPE_WAIT_AGREE) {
                mBtnMsg.setVisibility(View.VISIBLE);
                mBtnMsg.setEnabled(true);
                mBtnMsg.setText(R.string.tuichorus_agree);
            } else if (model.type == MsgEntity.TYPE_ORDERED_SONG) {
                mBtnMsg.setVisibility(View.VISIBLE);
                mBtnMsg.setEnabled(true);
                mBtnMsg.setText(R.string.tuichorus_manager_ordered_song);
            } else {
                mBtnMsg.setVisibility(View.GONE);
            }
        }
    }

    private void startLinkActivity(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        mContext.startActivity(intent);
    }

}