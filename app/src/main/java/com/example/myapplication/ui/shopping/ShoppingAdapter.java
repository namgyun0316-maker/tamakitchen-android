package com.namgyun.tamakitchen.ui.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.text.DecimalFormat;
import java.util.List;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    private final List<ShoppingItem> items;

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    // 가격 포맷
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");

    public interface OnItemClickListener {
        void onItemClick(int position, ShoppingItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, ShoppingItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public ShoppingAdapter(List<ShoppingItem> items) {
        this.items = items;
        setHasStableIds(false); // id 안정화 필요하면 true + getItemId 구현하면 됨
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvPrice, tvHint;

        public ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_item_icon);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvPrice = itemView.findViewById(R.id.tv_item_price);
            tvHint = itemView.findViewById(R.id.tv_item_hint);
        }
    }

    @NonNull
    @Override
    public ShoppingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    private String safeText(String s) {
        return s == null ? "" : s.trim();
    }

    private String safeUnit(String unit) {
        String u = safeText(unit);
        return u.isEmpty() ? "개" : u;
    }

    private String formatQty(double q) {
        if (Math.abs(q - Math.round(q)) < 1e-9) {
            return String.valueOf((long) Math.round(q));
        }
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(q);
    }

    private String formatMoney(int value) {
        if (value < 0) value = 0;
        return moneyFormat.format(value);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingAdapter.ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);

        // --- 이름/수량/단위 ---
        String name = safeText(item.getName());
        if (name.isEmpty()) name = "상품";

        String unit = safeUnit(item.getUnit());
        String qtyText = formatQty(item.getQuantity());

        holder.tvName.setText(name + " (" + qtyText + unit + ")");

        // --- 가격 표시 ---
        int price = item.getPrice();
        if (price < 0) price = 0;

        // ✅ 1) "단가"로 표시
        holder.tvPrice.setText(formatMoney(price) + "원");

        // ✅ 2) "수량 반영 총액"으로 보여주고 싶으면 위 줄 대신 아래 주석 해제
        // int total = (int) Math.round(price * item.getQuantity());
        // holder.tvPrice.setText(formatMoney(total) + "원");

        // --- 힌트 ---
        if (holder.tvHint != null) {
            holder.tvHint.setText("길게 눌러 삭제");
        }

        // --- 아이콘 ---
        int resId = item.getIconResId();
        if (resId == 0) {
            String iconKey = safeText(item.getIconKey());
            resId = IconCatalog.findResIdByRawKey(iconKey);
            item.setIconResId(resId);
        }
        holder.ivIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_launcher_foreground);

        // ✅ 클릭 시점에 adapterPosition 다시 가져오기 (구버전 호환)
        holder.itemView.setOnClickListener(v -> {
            int p = holder.getAdapterPosition(); // ✅ getBindingAdapterPosition() -> getAdapterPosition()
            if (p == RecyclerView.NO_POSITION) return;
            if (clickListener != null) clickListener.onItemClick(p, items.get(p));
        });

        holder.itemView.setOnLongClickListener(v -> {
            int p = holder.getAdapterPosition(); // ✅ getBindingAdapterPosition() -> getAdapterPosition()
            if (p == RecyclerView.NO_POSITION) return true;
            if (longClickListener != null) longClickListener.onItemLongClick(p, items.get(p));
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public int getTotalPrice() {
        double total = 0;
        if (items == null) return 0;

        for (ShoppingItem item : items) {
            if (item == null) continue;
            int price = item.getPrice();
            if (price < 0) price = 0;
            total += price * item.getQuantity();
        }

        return (int) Math.round(total);
    }
}
