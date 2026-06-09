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

public class MissingIngredientAdapter
        extends RecyclerView.Adapter<MissingIngredientAdapter.VH> {

    private final List<MissingIngredientItem> items;

    public MissingIngredientAdapter(List<MissingIngredientItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_missing_ingredient, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MissingIngredientItem item = items.get(position);

        h.tvName.setText(item.getName());
        h.checkBox.setChecked(item.isChecked());

        // ✅ 체크박스 클릭
        h.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
            h.itemView.setSelected(isChecked);
        });

        // ✅ 행 전체 클릭해도 체크 토글
        h.itemView.setOnClickListener(v -> {
            boolean next = !item.isChecked();
            item.setChecked(next);
            h.checkBox.setChecked(next);
            h.itemView.setSelected(next);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ===== 외부 제어용 =====

    public void setAllChecked(boolean checked) {
        for (MissingIngredientItem i : items) {
            i.setChecked(checked);
        }
        notifyDataSetChanged();
    }

    public List<String> getSelectedNames() {
        List<String> out = new ArrayList<>();
        for (MissingIngredientItem i : items) {
            if (i.isChecked()) out.add(i.getName());
        }
        return out;
    }

    // ===== ViewHolder =====
    static class VH extends RecyclerView.ViewHolder {

        CheckBox checkBox;
        TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbIngredient);
            tvName = itemView.findViewById(R.id.tvIngredientName);
        }
    }
}
