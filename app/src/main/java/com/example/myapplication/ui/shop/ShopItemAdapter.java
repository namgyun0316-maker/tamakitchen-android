package com.namgyun.tamakitchen.ui.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.VH> {

    public interface Listener {
        void onBuyClicked(ShopItem item);
    }

    private final List<ShopItem> items;
    private final Listener listener;

    public ShopItemAdapter(List<ShopItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ShopItem item = items.get(position);

        h.ivIcon.setImageResource(item.iconRes);
        h.tvName.setText(item.name);
        h.tvDesc.setText(item.desc);
        h.tvPrice.setText(String.valueOf(item.price));

        h.btnBuy.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBuyClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvDesc, tvPrice;
        MaterialButton btnBuy;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivItemIcon);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvDesc = itemView.findViewById(R.id.tvItemDesc);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            btnBuy = itemView.findViewById(R.id.btnBuy);
        }
    }
}