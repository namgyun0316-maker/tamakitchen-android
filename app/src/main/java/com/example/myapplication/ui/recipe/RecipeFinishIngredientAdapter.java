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

public class RecipeFinishIngredientAdapter
        extends RecyclerView.Adapter<RecipeFinishIngredientAdapter.VH> {

    public interface OnSelectionChangedListener {
        void onChanged(int selectedCount, int totalCount, boolean allSelected);
    }

    public static class Item {
        private final String name;
        private boolean checked;

        public Item(@NonNull String name, boolean checked) {
            this.name = name;
            this.checked = checked;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private OnSelectionChangedListener selectionChangedListener;

    public RecipeFinishIngredientAdapter(@NonNull List<Item> src) {
        items.clear();
        items.addAll(src);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
        notifySelectionChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_finish_ingredient, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= items.size()) return;

        Item item = items.get(position);
        holder.tvIngredientName.setText(item.getName());
        holder.tvIngredientCount.setText("1개");

        holder.cbIngredient.setOnCheckedChangeListener(null);
        holder.cbIngredient.setChecked(item.isChecked());

        holder.itemView.setOnClickListener(v -> toggle(holder.getAdapterPosition()));

        holder.cbIngredient.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (pos < 0 || pos >= items.size()) return;

            items.get(pos).setChecked(isChecked);
            notifySelectionChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isAllSelected() {
        if (items.isEmpty()) return false;
        for (Item item : items) {
            if (!item.isChecked()) return false;
        }
        return true;
    }

    public void setAllChecked(boolean checked) {
        for (Item item : items) {
            item.setChecked(checked);
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public List<String> getSelectedNames() {
        List<String> out = new ArrayList<>();
        for (Item item : items) {
            if (item.isChecked()) {
                out.add(item.getName());
            }
        }
        return out;
    }

    private void toggle(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        if (position < 0 || position >= items.size()) return;

        Item item = items.get(position);
        item.setChecked(!item.isChecked());
        notifyItemChanged(position);
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener == null) return;

        int total = items.size();
        int selected = 0;
        for (Item item : items) {
            if (item.isChecked()) selected++;
        }
        selectionChangedListener.onChanged(selected, total, total > 0 && selected == total);
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbIngredient;
        TextView tvIngredientName;
        TextView tvIngredientCount;

        VH(@NonNull View itemView) {
            super(itemView);
            cbIngredient = itemView.findViewById(R.id.cbIngredient);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            tvIngredientCount = itemView.findViewById(R.id.tvIngredientCount);
        }
    }
}