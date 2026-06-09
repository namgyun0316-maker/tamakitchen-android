package com.namgyun.tamakitchen.ui.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.List;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {

    private List<IconItem> iconList;
    private OnIconClickListener listener;

    public interface OnIconClickListener {
        void onIconClick(IconItem icon);
    }

    public IconAdapter(List<IconItem> iconList, OnIconClickListener listener) {
        this.iconList = iconList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvIconName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvIconName = itemView.findViewById(R.id.tv_icon_name);
        }

        public void bind(IconItem icon, OnIconClickListener listener) {
            ivIcon.setImageResource(icon.getResId());
            tvIconName.setText(icon.getName());
            itemView.setOnClickListener(v -> listener.onIconClick(icon));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_icon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(iconList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return iconList.size();
    }
}
