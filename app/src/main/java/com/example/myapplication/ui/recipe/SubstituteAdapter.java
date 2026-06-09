package com.namgyun.tamakitchen.ui.recipe;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.List;

public class SubstituteAdapter extends RecyclerView.Adapter<SubstituteAdapter.VH> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<SubstituteItem> items = new ArrayList<>();
    private final OnRemoveListener onRemoveListener;

    public SubstituteAdapter(OnRemoveListener onRemoveListener) {
        this.onRemoveListener = onRemoveListener;
    }

    public List<SubstituteItem> getItems() {
        return items;
    }

    public void setItems(List<SubstituteItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addItem(SubstituteItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeAt(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_substitute, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SubstituteItem item = items.get(position);

        holder.tvOriginal.setText(item.getOriginal());

        String alt = TextUtils.join(", ", item.getAlternatives());
        holder.tvAlternatives.setText(alt);

        holder.btnRemove.setOnClickListener(v -> {
            if (onRemoveListener == null) return;

            // ✅ getBindingAdapterPosition() (구버전 미지원) -> getAdapterPosition()
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            onRemoveListener.onRemove(pos);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOriginal, tvAlternatives;
        Button btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvOriginal = itemView.findViewById(R.id.tvOriginal);
            tvAlternatives = itemView.findViewById(R.id.tvAlternatives);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
