package com.namgyun.tamakitchen.ui.common;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;

/**
 * ScrollView/NestedScrollView 안에서 RecyclerView가 wrap_content로 전체 높이 계산을 못하는 문제 해결용.
 * 아이템 전체 높이만큼 확장해서 측정하게 만든다.
 */
public class ExpandedRecyclerView extends RecyclerView {

    public ExpandedRecyclerView(Context context) {
        super(context);
    }

    public ExpandedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // 아주 큰 AT_MOST 높이를 주고, 그 안에서 필요한 만큼만 측정되도록 유도
        int expandedHeightSpec = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST
        );
        super.onMeasure(widthSpec, expandedHeightSpec);
    }
}
