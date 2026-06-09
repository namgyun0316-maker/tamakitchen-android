package com.namgyun.tamakitchen.ui.collection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CharacterCollectionAdapter extends RecyclerView.Adapter<CharacterCollectionAdapter.ViewHolder> {

    public interface OnCollectionActionListener {
        void onUseClicked(CharacterCollectionItem item);
    }

    private final Context context;
    private final List<CharacterCollectionItem> items = new ArrayList<>();
    private final OnCollectionActionListener listener;

    private boolean sellMode = false;

    public CharacterCollectionAdapter(
            Context context,
            List<CharacterCollectionItem> items,
            OnCollectionActionListener listener
    ) {
        this.context = context;
        if (items != null) {
            this.items.addAll(items);
        }
        this.listener = listener;
    }

    public void submitList(List<CharacterCollectionItem> newItems) {
        items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setSellMode(boolean sellMode) {
        this.sellMode = sellMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_character_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CharacterCollectionItem item = items.get(position);

        boolean isBasicEgg = CollectionCatalog.KEY_EGG_BASIC.equals(item.getKey());

        int displayImageResId = resolveCollectionDisplayImageResId(item);

        if (displayImageResId != 0) {
            holder.ivCharacter.setImageResource(displayImageResId);
        } else if (item.getImageResId() != 0) {
            holder.ivCharacter.setImageResource(item.getImageResId());
        } else {
            holder.ivCharacter.setImageDrawable(null);
        }

        // 알은 "재료" 텍스트를 숨기되 공간은 유지해서 버튼 위치를 동일하게 맞춤
        if (isBasicEgg) {
            holder.tvCategory.setText("");
            holder.tvCategory.setVisibility(View.INVISIBLE);
        } else {
            holder.tvCategory.setText(item.getCategory().getDisplayName());
            holder.tvCategory.setVisibility(View.VISIBLE);
        }

        if (item.isUnlocked()) {
            holder.ivCharacter.setAlpha(1f);

            // 기본 알은 항상 "알"로 표시
            if (isBasicEgg) {
                holder.tvCharacterName.setText("알");
            } else {
                holder.tvCharacterName.setText(item.getDisplayNameWithCount());
            }

            holder.tvLevel.setText(item.getLevelText());
            holder.ivLock.setVisibility(View.GONE);

            if (!item.isAvailableToUse()) {
                holder.btnUse.setText("없음");
                holder.btnUse.setEnabled(false);
                holder.btnUse.setAlpha(0.58f);
            } else if (sellMode) {
                holder.btnUse.setText("판매하기");
                holder.btnUse.setEnabled(true);
                holder.btnUse.setAlpha(1f);
            } else if (item.isSelected() && item.getOwnedCount() <= 1) {
                holder.btnUse.setText("사용중");
                holder.btnUse.setEnabled(false);
                holder.btnUse.setAlpha(0.68f);
            } else {
                holder.btnUse.setText("가져오기");
                holder.btnUse.setEnabled(true);
                holder.btnUse.setAlpha(1f);
            }
        } else {
            holder.ivCharacter.setAlpha(0.22f);
            holder.tvCharacterName.setText("??");

            // 미발견 상태에서도 알은 공간만 유지
            if (isBasicEgg) {
                holder.tvCategory.setText("");
                holder.tvCategory.setVisibility(View.INVISIBLE);
            } else {
                holder.tvCategory.setText(item.getCategory().getDisplayName());
                holder.tvCategory.setVisibility(View.VISIBLE);
            }

            holder.tvLevel.setText("Lv?");
            holder.ivLock.setVisibility(View.VISIBLE);

            holder.btnUse.setText("미발견");
            holder.btnUse.setEnabled(false);
            holder.btnUse.setAlpha(0.55f);
        }

        holder.btnUse.setOnClickListener(v -> {
            if (listener != null && item.isUnlocked() && item.isAvailableToUse()) {
                listener.onUseClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int resolveCollectionDisplayImageResId(@NonNull CharacterCollectionItem item) {
        if (item.getImageResId() == 0) {
            return 0;
        }

        String originalName;
        try {
            originalName = context.getResources()
                    .getResourceEntryName(item.getImageResId());
        } catch (Exception e) {
            return item.getImageResId();
        }

        if (originalName == null || originalName.trim().isEmpty()) {
            return item.getImageResId();
        }

        String packageName = context.getPackageName();

        String trimmedBodyName = originalName;
        if (originalName.endsWith("_body")) {
            trimmedBodyName = originalName.substring(0, originalName.length() - 5);
        }

        String[] candidates = new String[]{
                trimmedBodyName + "_catalog",
                originalName + "_catalog"
        };

        for (String candidate : candidates) {
            int resId = context.getResources()
                    .getIdentifier(candidate, "drawable", packageName);
            if (resId != 0) {
                return resId;
            }
        }

        return item.getImageResId();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivCharacter;
        ImageView ivLock;
        TextView tvCharacterName;
        TextView tvCategory;
        TextView tvLevel;
        MaterialButton btnUse;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCharacter = itemView.findViewById(R.id.ivCharacter);
            ivLock = itemView.findViewById(R.id.ivLock);
            tvCharacterName = itemView.findViewById(R.id.tvCharacterName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            btnUse = itemView.findViewById(R.id.btnUseCharacter);
        }
    }
}