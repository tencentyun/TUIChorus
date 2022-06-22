package com.tencent.liteav.tuichorus.ui.music.impl;

import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ChorusMusicPagerAdapter extends PagerAdapter {
    private ArrayList<View> mViewLists;

    public ChorusMusicPagerAdapter(ArrayList<View> viewLists) {
        super();
        this.mViewLists = viewLists;
    }

    @Override
    public int getCount() {
        return mViewLists.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mViewLists.get(position));
        return mViewLists.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewLists.get(position));
    }
}
