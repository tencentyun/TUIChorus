package com.tencent.liteav.tuichorus.ui.gift.imp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.tencent.liteav.tuichorus.R;

import java.util.LinkedList;

public class GiftAnimatorLayout extends LinearLayout {

    private static final int MAX_SHOW_GIFT_BULLET_SIZE = 3; //礼物弹幕最多展示的个数

    private Context            mContext;
    private LinearLayout       mGiftBulletGroup;
    private LinkedList<String> mAnimationUrlList;

    public GiftAnimatorLayout(Context context) {
        super(context);
    }

    public GiftAnimatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAnimationUrlList = new LinkedList<>();
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.tuichorus_layout_lottie_animator, this, true);
        mGiftBulletGroup = (LinearLayout) findViewById(R.id.gift_bullet_group);
    }

    public void show(GiftInfo info) {
        if (info == null) {
            return;
        }
        showGiftBullet(info);
    }

    private void showGiftBullet(GiftInfo info) {
        if (mGiftBulletGroup.getChildCount() >= MAX_SHOW_GIFT_BULLET_SIZE) {
            //如果礼物超过3个，就将第一个出现的礼物弹幕从界面上移除
            View firstShowBulletView = mGiftBulletGroup.getChildAt(0);
            if (firstShowBulletView != null) {
                GiftBulletFrameLayout bulletView = (GiftBulletFrameLayout) firstShowBulletView;
                bulletView.clearHandler();
                mGiftBulletGroup.removeView(bulletView);
            }
        }
        GiftBulletFrameLayout giftFrameLayout = new GiftBulletFrameLayout(mContext);
        mGiftBulletGroup.addView(giftFrameLayout);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mGiftBulletGroup.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if (giftFrameLayout.setGift(info)) {
            giftFrameLayout.startAnimation();
        }
    }
}
