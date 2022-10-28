package com.tencent.liteav.tuichorus.ui.gift.imp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.ui.gift.GiftConstant;
import com.tencent.liteav.tuichorus.ui.gift.GiftPanelDelegate;
import com.tencent.liteav.tuichorus.ui.gift.IGiftPanelView;
import com.tencent.liteav.tuichorus.ui.gift.imp.adapter.GiftPanelAdapter;
import com.tencent.liteav.tuichorus.ui.gift.imp.adapter.GiftViewPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class GiftPanelViewImp extends BottomSheetDialog implements IGiftPanelView {
    private static final String TAG = "GiftPanelViewImp";

    private static int COLUMNS = 4;
    private static int ROWS    = 2;

    private Context             mContext;
    private List<View>          mGiftViews;     //每页显示的礼物view
    private GiftController      mGiftController;
    private LayoutInflater      mInflater;
    private LinearLayout        mDotsLayout;
    private ViewPager           mViewpager;
    private GiftPanelDelegate   mGiftPanelDelegate;
    private GiftInfoDataHandler mGiftInfoDataHandler;
    private String              mDefalutPanelType = GiftConstant.GIFT_PANEL_TYPE_SINGLEROW;

    public GiftPanelViewImp(Context context) {
        super(context, R.style.TUIChorusDialogTheme);
        mContext = context;
        mGiftViews = new ArrayList<>();
        setContentView(R.layout.tuichorus_dialog_gift_panel);
        initView();
    }

    private void initView() {
        mInflater = LayoutInflater.from(mContext);
        mViewpager = findViewById(R.id.gift_panel_view_pager);
        mDotsLayout = findViewById(R.id.dots_container);
        if (GiftConstant.GIFT_PANEL_TYPE_SINGLEROW.equals(mDefalutPanelType)) {
            COLUMNS = 5;
            ROWS = 1;
            findViewById(R.id.btn_send_gift).setVisibility(View.GONE);
            findViewById(R.id.separate_line).setVisibility(View.GONE);
            TextView textView = findViewById(R.id.tv_gift_panel_title);
            textView.setTextSize(24);
            textView.setPadding(20, 32, 0, 0);
            mDotsLayout.setVisibility(View.GONE);
            LinearLayout linearLayout = findViewById(R.id.giftLayout);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();
            layoutParams.height *= 0.6;
            linearLayout.setLayoutParams(layoutParams);
        }
        findViewById(R.id.btn_charge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGiftPanelDelegate != null) {
                    Log.d(TAG, "on charge btn click");
                    mGiftPanelDelegate.onChargeClick();
                }
            }
        });
        findViewById(R.id.btn_send_gift).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGiftController == null) {
                    return;
                }
                GiftInfo giftInfo = mGiftController.getSelectGiftInfo();
                if (giftInfo != null && mGiftPanelDelegate != null) {
                    Log.d(TAG, "onGiftItemClick: " + giftInfo);
                    mGiftPanelDelegate.onGiftItemClick(giftInfo);
                }
            }
        });
        setCanceledOnTouchOutside(true);
    }

    /**
     * 初始化礼物面板
     */
    private void initGiftData(List<GiftInfo> giftInfoList) {
        if (mGiftController == null) {
            mGiftController = new GiftController();
        }
        mGiftController.setGiftClickListener(new GiftController.GiftClickListener() {
            @Override
            public void onClick(int position, GiftInfo giftInfo) {
                if (mGiftController == null) {
                    return;
                }
                if (giftInfo != null && mGiftPanelDelegate != null) {
                    Log.d(TAG, "onGiftItemClick: " + giftInfo);
                    mGiftPanelDelegate.onGiftItemClick(giftInfo);
                }
            }
        });
        int pageSize = mGiftController.getPagerCount(giftInfoList.size(), COLUMNS, ROWS);
        // 获取页数
        for (int i = 0; i < pageSize; i++) {
            mGiftViews.add(mGiftController.viewPagerItem(mContext, i, giftInfoList, COLUMNS, ROWS));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(10, 0, 10, 0);
            if (pageSize > 1) {
                mDotsLayout.addView(dotsItem(i), params);
            }
        }
        if (GiftConstant.GIFT_PANEL_TYPE_SINGLEROW.equals(mDefalutPanelType) && pageSize > 1) {
            mDotsLayout.setVisibility(View.VISIBLE);
        } else {
            mDotsLayout.setVisibility(View.GONE);
        }
        GiftViewPagerAdapter giftViewPagerAdapter = new GiftViewPagerAdapter(mGiftViews);
        mViewpager.setAdapter(giftViewPagerAdapter);
        mViewpager.addOnPageChangeListener(new PageChangeListener());
        mViewpager.setCurrentItem(0);
        if (pageSize > 1) {
            mDotsLayout.getChildAt(0).setSelected(true);
        }
    }

    /**
     * 礼物页切换时，底部小圆点
     *
     * @param position
     * @return
     */
    private ImageView dotsItem(int position) {
        View layout = mInflater.inflate(R.layout.tuichorus_layout_gift_dot, null);
        ImageView iv = (ImageView) layout.findViewById(R.id.face_dot);
        iv.setId(position);
        return iv;
    }

    /**
     * 礼物页改变时，dots效果也要跟着改变
     */
    class PageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            for (int i = 0; i < mDotsLayout.getChildCount(); i++) {
                mDotsLayout.getChildAt(i).setSelected(false);
            }
            mDotsLayout.getChildAt(position).setSelected(true);
            for (int i = 0; i < mGiftViews.size(); i++) {
                //清除选中，当礼物页面切换到另一个礼物页面
                RecyclerView view = (RecyclerView) mGiftViews.get(i);
                GiftPanelAdapter adapter = (GiftPanelAdapter) view.getAdapter();
                if (mGiftController != null) {
                    int selectPageIndex = mGiftController.getSelectPageIndex();
                    adapter.clearSelection(selectPageIndex);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    @Override
    public void init(GiftInfoDataHandler giftInfoDataHandler) {
        mGiftInfoDataHandler = giftInfoDataHandler;
    }

    @Override
    public void show() {
        super.show();
        if (mGiftInfoDataHandler != null) {
            mGiftInfoDataHandler.queryGiftInfoList(new GiftInfoDataHandler.GiftQueryCallback() {
                @Override
                public void onQuerySuccess(final List<GiftInfo> giftInfoList) {
                    //确保更新UI数据在主线程中执行
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            initGiftData(giftInfoList);
                        }
                    });
                }

                @Override
                public void onQueryFailed(String errorMsg) {
                    Log.d(TAG, "request data failed, the message:" + errorMsg);
                }
            });
        }
    }

    @Override
    public void hide() {
        dismiss();
    }

    @Override
    public void setGiftPanelDelegate(GiftPanelDelegate delegate) {
        mGiftPanelDelegate = delegate;
    }
}
