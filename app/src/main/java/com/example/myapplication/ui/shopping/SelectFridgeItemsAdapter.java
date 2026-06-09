// File: app/src/main/java/com/example/myapplication/ui/shopping/SelectFridgeItemsAdapter.java
package com.namgyun.tamakitchen.ui.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.List;

public class SelectFridgeItemsAdapter extends RecyclerView.Adapter<SelectFridgeItemsAdapter.VH> {

    public interface OnCheckedChangedListener { void onChanged(); }

    private final List<String> displayList;
    private final boolean[] checked;
    private final OnCheckedChangedListener listener;

    public SelectFridgeItemsAdapter(List<String> displayList, boolean[] checked,
                                    OnCheckedChangedListener listener) {
        this.displayList = displayList;
        this.checked = checked;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_fridge, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.tvName.setText(displayList.get(position));

        holder.cb.setOnCheckedChangeListener(null);
        holder.cb.setChecked(checked[position]);

        holder.itemView.setOnClickListener(v -> {
            checked[position] = !checked[position];
            notifyItemChanged(position);
            if (listener != null) listener.onChanged();
        });

        holder.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checked[position] = isChecked;
            if (listener != null) listener.onChanged();
        });
    }

    @Override
    public int getItemCount() { return displayList.size(); }

    public void setAll(boolean value) {
        for (int i = 0; i < checked.length; i++) checked[i] = value;
        notifyDataSetChanged();
        if (listener != null) listener.onChanged();
    }

    public boolean isAllChecked() {
        if (checked.length == 0) return false;
        for (boolean b : checked) if (!b) return false;
        return true;
    }

    public boolean isNoneChecked() {
        for (boolean b : checked) if (b) return false;
        return true;
    }

    public boolean[] getCheckedArray() { return checked; }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);

            // ✅ 레이아웃 id가 cb/tvName 이거나 cbItem/tvItemText 이어도 안전하게
            cb = itemView.findViewById(R.id.cb);
            if (cb == null) cb = itemView.findViewById(R.id.cbItem);

            tvName = itemView.findViewById(R.id.tvName);
            if (tvName == null) tvName = itemView.findViewById(R.id.tvItemText);
        }
    }
}
