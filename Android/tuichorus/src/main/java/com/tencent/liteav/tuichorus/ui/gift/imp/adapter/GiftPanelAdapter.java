package com.tencent.liteav.tuichorus.ui.gift.imp.adapter;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.liteav.basic.ImageLoader;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.gift.GiftConstant;
import com.tencent.liteav.tuichorus.ui.gift.imp.GiftInfo;
import com.tencent.liteav.tuichorus.ui.gift.imp.RecyclerViewController;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GiftPanelAdapter extends RecyclerView.Adapter<GiftPanelAdapter.ViewHolder> {
    private Context                mContext;
    private RecyclerView           mRecyclerView;
    private RecyclerViewController mRecyclerViewController;
    private int                    mPageIndex;
    private List<GiftInfo>         mSelectGiftInfoList;
    private List<GiftInfo>         mGiftInfoList;
    private OnItemClickListener    mOnItemClickListener;
    private String                 mDefalutPanelType = GiftConstant.GIFT_PANEL_TYPE_SINGLEROW;

    public GiftPanelAdapter(RecyclerView recyclerView, int pageIndex, List<GiftInfo> list,
                            Context context, List<GiftInfo> selectGiftInfoList) {
        super();
        mRecyclerView = recyclerView;
        mGiftInfoList = list;
        mContext = context;
        mPageIndex = pageIndex;
        mSelectGiftInfoList = selectGiftInfoList;
        recyclerViewClickListener(list, mContext);
    }

    private void recyclerViewClickListener(final List<GiftInfo> list, Context mContext) {
        mRecyclerViewController = new RecyclerViewController(mContext, mRecyclerView);
        mRecyclerViewController.setOnItemClickListener(new RecyclerViewController.OnItemClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                final GiftInfo giftModel = list.get(position);
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, giftModel, position, mPageIndex);
                }
                clearSelectState();
                giftModel.isSelected = true;
                mSelectGiftInfoList.add(giftModel);
                notifyDataSetChanged();
            }
        });
    }

    private void clearSelectState() {
        for (GiftInfo giftInfo : mSelectGiftInfoList) {
            giftInfo.isSelected = false;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.tuichorus_recycle_item_gift_panel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final GiftInfo giftInfo = mGiftInfoList.get(position);
        ImageLoader.loadImage(mContext, holder.mImageGift, giftInfo.giftPicUrl);
        holder.mTextGiftName.setText(giftInfo.title);
        holder.mTextGiftPrice.setText(String.format(mContext.getString(R.string.tuichorus_gift_game_currency),
                giftInfo.price));
        if (GiftConstant.GIFT_PANEL_TYPE_MULTIROW.equals(mDefalutPanelType) && giftInfo.isSelected) {
            holder.mLayoutRootView.setBackgroundResource(R.drawable.tuichorus_gift_shape_normal);
            holder.mTextGiftName.setVisibility(View.GONE);
        } else {
            holder.mLayoutRootView.setBackground(null);
            holder.mTextGiftName.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mGiftInfoList.size();
    }

    public void clearSelection(int pageIndex) {
        if (mPageIndex != pageIndex) {
            notifyDataSetChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout    mLayoutRootView;
        CircleImageView mImageGift;
        TextView        mTextGiftName;
        TextView        mTextGiftPrice;

        public ViewHolder(View view) {
            super(view);
            mLayoutRootView = (LinearLayout) view.findViewById(R.id.ll_gift_root);
            mImageGift = (CircleImageView) view.findViewById(R.id.iv_gift_icon);
            mTextGiftName = (TextView) view.findViewById(R.id.tv_gift_name);
            mTextGiftPrice = (TextView) view.findViewById(R.id.tv_gift_price);
            if (GiftConstant.GIFT_PANEL_TYPE_SINGLEROW.equals(mDefalutPanelType)) {
                mTextGiftPrice.setVisibility(View.GONE);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, GiftInfo giftInfo, int position, int pageIndex);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
}
