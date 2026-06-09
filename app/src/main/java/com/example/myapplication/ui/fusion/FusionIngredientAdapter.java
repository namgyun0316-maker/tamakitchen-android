package com.namgyun.tamakitchen.ui.fusion;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.IngredientCharacter;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class FusionIngredientAdapter extends RecyclerView.Adapter<FusionIngredientAdapter.ViewHolder> {

    public interface OnIngredientClickListener {
        void onIngredientClick(@NonNull IngredientCharacter character);
    }

    private final Context context;
    private final List<IngredientCharacter> items;
    private final OnIngredientClickListener listener;

    public FusionIngredientAdapter(
            @NonNull Context context,
            @NonNull List<IngredientCharacter> items,
            @NonNull OnIngredientClickListener listener
    ) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fusion_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngredientCharacter item = items.get(position);

        int imageResId = resolveFusionDisplayImageResId(item);
        if (imageResId != 0) {
            holder.ivCharacter.setImageResource(imageResId);
        } else {
            holder.ivCharacter.setImageDrawable(null);
        }

        holder.tvName.setText(item.getDisplayName());

        int eligibleCount = getEligibleCount(item);
        String levelText = getEligibleLevelText(item);

        holder.tvLevel.setText(levelText);
        holder.tvCount.setText("x" + eligibleCount);

        boolean selected = false;
        if (context instanceof FusionActivity) {
            selected = ((FusionActivity) context).isSelectedIngredient(item);
        }

        holder.cardRoot.setStrokeWidth(dpToPx(selected ? 2 : 1));
        holder.cardRoot.setStrokeColor(selected ? Color.parseColor("#8CCFEF") : Color.parseColor("#22000000"));
        holder.cardRoot.setCardBackgroundColor(Color.parseColor("#FFFFFF"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIngredientClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int getEligibleCount(@Nullable IngredientCharacter ingredient) {
        if (context instanceof FusionActivity) {
            return ((FusionActivity) context).getEligibleIngredientCount(ingredient);
        }
        return 0;
    }

    private String getEligibleLevelText(@Nullable IngredientCharacter ingredient) {
        if (context instanceof FusionActivity) {
            return ((FusionActivity) context).formatEligibleLevelText(ingredient);
        }
        return "Lv?";
    }

    private int resolveFusionDisplayImageResId(@NonNull IngredientCharacter ingredient) {
        String originalName = ingredient.getDrawableName();
        if (originalName == null || originalName.trim().isEmpty()) return 0;

        String trimmedBodyName = originalName;
        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[] {
                trimmedBodyName + "_catalog",
                originalName + "_catalog",
                originalName
        };

        for (String candidate : candidates) {
            int resId = context.getResources().getIdentifier(
                    candidate,
                    "drawable",
                    context.getPackageName()
            );
            if (resId != 0) {
                return resId;
            }
        }

        return 0;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardRoot;
        ImageView ivCharacter;
        TextView tvName;
        TextView tvLevel;
        TextView tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardFusionIngredient);
            ivCharacter = itemView.findViewById(R.id.ivFusionIngredientCharacter);
            tvName = itemView.findViewById(R.id.tvFusionIngredientName);
            tvLevel = itemView.findViewById(R.id.tvFusionIngredientLevel);
            tvCount = itemView.findViewById(R.id.tvFusionIngredientCount);
        }
    }
}