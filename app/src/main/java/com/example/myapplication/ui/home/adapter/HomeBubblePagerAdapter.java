package com.namgyun.tamakitchen.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.home.model.HomeBubbleItem;

import java.util.ArrayList;
import java.util.List;

public class HomeBubblePagerAdapter extends RecyclerView.Adapter<HomeBubblePagerAdapter.VH> {

    public interface OnBubbleClickListener {
        void onClick(HomeBubbleItem item);
    }

    private final List<HomeBubbleItem> items = new ArrayList<>();
    private OnBubbleClickListener listener;

    public void setOnBubbleClickListener(OnBubbleClickListener l) {
        this.listener = l;
    }

    public void setItems(List<HomeBubbleItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public int getRealCount() {
        return items.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_bubble, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        HomeBubbleItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvMsg.setText(item.getMessage());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvMsg;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBubbleTitle);
            tvMsg = itemView.findViewById(R.id.tvBubbleMessage);
        }
    }
}