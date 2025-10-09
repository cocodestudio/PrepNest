package com.cocode.prepnest;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class HorizontalMarginItemDecoration extends RecyclerView.ItemDecoration {
    private final int startMargin;
    private final int endMargin;
    private final int betweenMargin;

    public HorizontalMarginItemDecoration(int start, int end, int between) {
        this.startMargin = start;
        this.endMargin = end;
        this.betweenMargin = between;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter().getItemCount();

        if (position == 0) {
            outRect.set(startMargin, 0, betweenMargin / 2, 0);
        } else if (position == itemCount - 1) {
            outRect.set(betweenMargin / 2, 0, endMargin, 0);
        } else {
            outRect.set(betweenMargin / 2, 0, betweenMargin / 2, 0);
        }
    }
}