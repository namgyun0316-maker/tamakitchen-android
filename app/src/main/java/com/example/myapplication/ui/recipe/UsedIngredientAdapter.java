package com.namgyun.tamakitchen.ui.recipe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.List;

public class UsedIngredientAdapter extends RecyclerView.Adapter<UsedIngredientAdapter.VH> {

    private final List<String> items = new ArrayList<>();
    private final List<Boolean> checked = new ArrayList<>();

    public UsedIngredientAdapter(List<String> items) {
        setItems(items);
    }

    public void setItems(List<String> newItems) {
        items.clear();
        checked.clear();

        if (newItems != null) {
            for (String s : newItems) {
                if (s == null) continue;
                String t = s.trim();
                if (t.isEmpty()) continue;
                items.add(t);
                checked.add(false);
            }
        }
        notifyDataSetChanged();
    }

    public List<String> getSelected() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (checked.get(i)) {
                out.add(items.get(i));
            }
        }
        return out;
    }

    public boolean isAllSelected() {
        if (items.isEmpty()) return false;
        for (Boolean b : checked) {
            if (b == null || !b) return false;
        }
        return true;
    }

    public void setAllChecked(boolean value) {
        for (int i = 0; i < checked.size(); i++) {
            checked.set(i, value);
        }
        notifyDataSetChanged();
    }

    public void toggleSelectAll() {
        setAllChecked(!isAllSelected());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_finish_ingredient, parent, false);

        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            v.setLayoutParams(lp);
        }

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position), checked.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView tvName;
        TextView tvCount;

        VH(@NonNull View itemView) {
            super(itemView);
            cb = itemView.findViewById(R.id.cbIngredient);
            tvName = itemView.findViewById(R.id.tvIngredientName);
            tvCount = itemView.findViewById(R.id.tvIngredientCount);
        }

        void bind(String name, boolean isChecked) {
            tvName.setText(name);
            tvCount.setText("1개");

            cb.setOnCheckedChangeListener(null);

            // 기본 체크박스 버튼 숨기기
            cb.setButtonDrawable(android.R.color.transparent);
            cb.setPadding(0, 0, 0, 0);
            cb.setMinWidth(0);
            cb.setMinHeight(0);

            cb.setChecked(isChecked);
            applyCheckboxUi(isChecked);

            cb.setOnCheckedChangeListener((buttonView, checkedNow) -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                checked.set(pos, checkedNow);
                applyCheckboxUi(checkedNow);
            });

            View.OnClickListener toggleListener = v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                boolean now = !checked.get(pos);
                checked.set(pos, now);

                cb.setOnCheckedChangeListener(null);
                cb.setChecked(now);
                applyCheckboxUi(now);
                cb.setOnCheckedChangeListener((buttonView, checkedNow) -> {
                    int currentPos = getAdapterPosition();
                    if (currentPos == RecyclerView.NO_POSITION) return;
                    checked.set(currentPos, checkedNow);
                    applyCheckboxUi(checkedNow);
                });
            };

            itemView.setOnClickListener(toggleListener);
            tvName.setOnClickListener(toggleListener);
            tvCount.setOnClickListener(toggleListener);
        }

        private void applyCheckboxUi(boolean isChecked) {
            if (isChecked) {
                cb.setBackgroundResource(R.drawable.bg_recipe_finish_checkbox_checked);
            } else {
                cb.setBackgroundResource(R.drawable.bg_recipe_finish_checkbox_unchecked);
            }
        }
    }
}