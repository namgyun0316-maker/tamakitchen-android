package com.namgyun.tamakitchen.ui.menu;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.namgyun.tamakitchen.BuildConfig;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.recipe.RecipeDetailActivity;
import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FavoriteRecipeAdapter extends RecyclerView.Adapter<FavoriteRecipeAdapter.ViewHolder> {

    public interface OnAddMealPlanClickListener {
        void onAddMealPlan(RecipeResponse item);
    }

    private static final String TAG = "FavoriteRecipeAdapter";

    private final Context context;
    private final OnAddMealPlanClickListener listener;
    private final List<RecipeResponse> items = new ArrayList<>();

    public FavoriteRecipeAdapter(Context context,
                                 OnAddMealPlanClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitItems(List<RecipeResponse> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecipeResponse item = items.get(position);

        holder.tvName.setText(safe(item != null ? item.getName() : null, "이름 없는 레시피"));
        holder.tvSummary.setText(safe(item != null ? item.getSummary() : null, "요약 정보가 없어요."));
        holder.tvCategory.setText(buildCategoryText(item));
        holder.tvIngredients.setText(buildIngredientsText(item));

        String rawImageUrl = getImageUrl(item);
        String finalImageUrl = normalizeImageUrl(rawImageUrl);

        Log.d(TAG, "rawImageUrl = " + rawImageUrl);
        Log.d(TAG, "finalImageUrl = " + finalImageUrl);

        Glide.with(context)
                .load(finalImageUrl)
                .placeholder(R.drawable.ic_menu_item_placeholder)
                .error(R.drawable.ic_menu_item_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.ivRecipeThumb);

        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(context, RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, item);
                intent.putExtra(RecipeDetailActivity.EXTRA_MATCH_PERCENT, item != null ? item.getMatchPercent() : 0);
                intent.putExtra(RecipeDetailActivity.EXTRA_LIST_TYPE, "ALL");
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        });

        holder.btnAddMealPlan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddMealPlan(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getImageUrl(RecipeResponse item) {
        if (item == null) return "";

        String thumb = item.getThumbnailUrl();
        if (!TextUtils.isEmpty(thumb) && !TextUtils.isEmpty(thumb.trim())) {
            return thumb.trim();
        }

        String image = item.getImageUrl();
        if (!TextUtils.isEmpty(image) && !TextUtils.isEmpty(image.trim())) {
            return image.trim();
        }

        return "";
    }

    private String normalizeImageUrl(String url) {
        if (TextUtils.isEmpty(url)) return "";

        String trimmed = url.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String baseUrl = BuildConfig.BASE_URL;
        if (TextUtils.isEmpty(baseUrl)) {
            return trimmed;
        }

        if (baseUrl.endsWith("/") && trimmed.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + trimmed;
        }

        if (!baseUrl.endsWith("/") && !trimmed.startsWith("/")) {
            return baseUrl + "/" + trimmed;
        }

        return baseUrl + trimmed;
    }

    private String safe(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String buildCategoryText(RecipeResponse item) {
        String category = item != null ? item.getCategory() : null;
        if (category == null || category.trim().isEmpty()) {
            return "카테고리 미설정";
        }
        return category.trim();
    }

    private String buildIngredientsText(RecipeResponse item) {
        String ingredients = item != null ? item.getIngredients() : null;
        if (ingredients == null || ingredients.trim().isEmpty()) {
            return "재료 정보 없음";
        }

        String trimmed = ingredients.trim();
        if (trimmed.length() > 80) {
            return trimmed.substring(0, 80) + "...";
        }
        return trimmed;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeThumb;
        TextView tvName;
        TextView tvSummary;
        TextView tvCategory;
        TextView tvIngredients;
        MaterialButton btnAddMealPlan;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeThumb = itemView.findViewById(R.id.ivRecipeThumb);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvSummary = itemView.findViewById(R.id.tvRecipeSummary);
            tvCategory = itemView.findViewById(R.id.tvRecipeCategory);
            tvIngredients = itemView.findViewById(R.id.tvRecipeIngredients);
            btnAddMealPlan = itemView.findViewById(R.id.btnAddMealPlan);
        }
    }
}