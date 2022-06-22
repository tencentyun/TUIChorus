package com.tencent.liteav.tuichorus.ui.widget;

import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class SpaceDecoration extends RecyclerView.ItemDecoration {
    private int mSpace;
    private int mColNum;

    public SpaceDecoration(int space, int colNum) {
        this.mSpace = space;
        this.mColNum = colNum;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildLayoutPosition(view) % mColNum == 0) {
            outRect.right = mSpace / 2;
            outRect.bottom = mSpace;
        } else {
            outRect.left = mSpace / 2;
            outRect.bottom = mSpace;
        }
    }
}
