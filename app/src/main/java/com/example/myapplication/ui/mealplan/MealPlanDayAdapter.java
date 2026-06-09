package com.namgyun.tamakitchen.ui.mealplan;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;

import java.util.ArrayList;
import java.util.List;

public class MealPlanDayAdapter extends RecyclerView.Adapter<MealPlanDayAdapter.VH> {

    public interface OnDayClickListener {
        void onClick(MealPlanDayItem item);
    }

    private final List<MealPlanDayItem> items = new ArrayList<>();
    private final OnDayClickListener listener;

    public MealPlanDayAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<MealPlanDayItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_plan_day, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MealPlanDayItem item = items.get(position);

        holder.tvDay.setText(item.getDayLabel() + " " + item.getDayOfMonth());

        if (item.hasRecipe()) {
            holder.tvName.setText(item.getRecipeName());
            holder.tvSummary.setText(
                    TextUtils.isEmpty(item.getRecipeSummary())
                            ? "등록된 식단 메뉴야."
                            : item.getRecipeSummary()
            );
            holder.tvFavorite.setVisibility(item.isFavorite() ? View.VISIBLE : View.GONE);
            holder.tvEmptyBadge.setVisibility(View.GONE);

            String url = resolveImageUrl(item.getThumbnailUrl());
            if (TextUtils.isEmpty(url)) url = resolveImageUrl(item.getImageUrl());

            if (!TextUtils.isEmpty(url)) {
                Glide.with(holder.itemView.getContext())
                        .load(url)
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .centerCrop()
                        .into(holder.ivThumb);
            } else {
                holder.ivThumb.setImageResource(R.drawable.bg_recipe_placeholder);
            }

            holder.itemView.setAlpha(1f);
        } else {
            holder.tvName.setText("레시피 추가");
            holder.tvSummary.setText("이 날짜에 먹을 메뉴를 정해보자.");
            holder.tvFavorite.setVisibility(View.GONE);
            holder.tvEmptyBadge.setVisibility(View.VISIBLE);
            holder.ivThumb.setImageResource(R.drawable.bg_recipe_placeholder);

            holder.itemView.setAlpha(1f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDay;
        TextView tvName;
        TextView tvSummary;
        TextView tvFavorite;
        TextView tvEmptyBadge;
        ImageView ivThumb;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvMealPlanDay);
            tvName = itemView.findViewById(R.id.tvMealPlanName);
            tvSummary = itemView.findViewById(R.id.tvMealPlanSummary);
            tvFavorite = itemView.findViewById(R.id.tvMealPlanFavorite);
            tvEmptyBadge = itemView.findViewById(R.id.tvMealPlanEmptyBadge);
            ivThumb = itemView.findViewById(R.id.ivMealPlanThumb);
        }
    }

    private String resolveImageUrl(String raw) {
        if (TextUtils.isEmpty(raw)) return null;
        String url = raw.trim();

        if (url.startsWith("content://")) return url;

        if (url.startsWith("/uploads")) {
            String base = FridgeApi.BASE_URL;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            return base + url;
        }

        if (url.startsWith("uploads/")) {
            String base = FridgeApi.BASE_URL;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            return base + "/" + url;
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        try {
            Uri.parse(url);
            return url;
        } catch (Exception e) {
            return null;
        }
    }
}