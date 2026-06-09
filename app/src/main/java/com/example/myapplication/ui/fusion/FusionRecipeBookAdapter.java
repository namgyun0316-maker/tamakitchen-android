package com.namgyun.tamakitchen.ui.fusion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.pet.IngredientCharacter;

import java.util.List;

public class FusionRecipeBookAdapter extends RecyclerView.Adapter<FusionRecipeBookAdapter.ViewHolder> {

    private final Context context;
    private final List<FusionRecipeBookItem> items;

    public FusionRecipeBookAdapter(@NonNull Context context, @NonNull List<FusionRecipeBookItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fusion_recipe_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FusionRecipeBookItem item = items.get(position);

        IngredientCharacter first = item.getFirstIngredient();
        IngredientCharacter second = item.getSecondIngredient();
        FusionFood result = item.getResultFood();

        if (context instanceof FusionRecipeBookActivity) {
            FusionRecipeBookActivity activity = (FusionRecipeBookActivity) context;

            int firstRes = activity.resolveIngredientImageResId(first);
            int secondRes = activity.resolveIngredientImageResId(second);
            int resultRes = activity.resolveFoodImageResId(result);

            if (firstRes != 0) {
                holder.ivIngredient1.setImageResource(firstRes);
            } else {
                holder.ivIngredient1.setImageDrawable(null);
            }

            if (secondRes != 0) {
                holder.ivIngredient2.setImageResource(secondRes);
            } else {
                holder.ivIngredient2.setImageDrawable(null);
            }

            if (resultRes != 0) {
                holder.ivResult.setImageResource(resultRes);
            } else {
                holder.ivResult.setImageDrawable(null);
            }
        }

        holder.tvIngredient1.setText(first.getDisplayName());
        holder.tvIngredient2.setText(second.getDisplayName());
        holder.tvResultName.setText(result.getDisplayName());
        holder.tvRecipeFormula.setText(first.getDisplayName() + " + " + second.getDisplayName() + " = " + result.getDisplayName());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIngredient1;
        ImageView ivIngredient2;
        ImageView ivResult;

        TextView tvIngredient1;
        TextView tvIngredient2;
        TextView tvResultName;
        TextView tvRecipeFormula;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIngredient1 = itemView.findViewById(R.id.ivRecipeIngredient1);
            ivIngredient2 = itemView.findViewById(R.id.ivRecipeIngredient2);
            ivResult = itemView.findViewById(R.id.ivRecipeResult);

            tvIngredient1 = itemView.findViewById(R.id.tvRecipeIngredient1);
            tvIngredient2 = itemView.findViewById(R.id.tvRecipeIngredient2);
            tvResultName = itemView.findViewById(R.id.tvRecipeResultName);
            tvRecipeFormula = itemView.findViewById(R.id.tvRecipeFormula);
        }
    }
}