package com.namgyun.tamakitchen.ui.fridge;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.shopping.IconCatalog;
import com.namgyun.tamakitchen.ui.shopping.IconItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FridgeAdapter extends RecyclerView.Adapter<FridgeAdapter.ViewHolder> {

    private List<FridgeItem> items;
    private final Context context;

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    private boolean selectionMode = false;
    private final Set<Long> selectedIds = new HashSet<>();

    private static final DecimalFormat QTY_FMT = new DecimalFormat("0.########");

    private String myNickname = "";
    private boolean showAddedBy = false;

    public interface OnItemClickListener {
        void onItemClick(FridgeItem item, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FridgeItem item, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setMyNickname(String nickname) {
        this.myNickname = (nickname == null) ? "" : nickname.trim();
        notifyDataSetChanged();
    }

    public void setShowAddedBy(boolean show) {
        this.showAddedBy = show;
        notifyDataSetChanged();
    }

    public FridgeAdapter(Context context, List<FridgeItem> items) {
        this.context = context;
        this.items = (items != null) ? items : new ArrayList<>();
        setHasStableIds(true);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvAddedBy, tvQuantity, tvExpiry;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_item_icon);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvAddedBy = itemView.findViewById(R.id.tv_item_added_by);
            tvQuantity = itemView.findViewById(R.id.tv_item_quantity);
            tvExpiry = itemView.findViewById(R.id.tv_item_expiry);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fridge_grid, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) return RecyclerView.NO_ID;
        return stableKey(items.get(position));
    }

    private long stableKey(FridgeItem item) {
        if (item == null) return Long.MIN_VALUE;

        if (item.getId() != null) return item.getId();

        String name = safe(item.getName());
        String expiry = safe(item.getExpiryDate());
        String iconName = safe(item.getIconName());
        return (name + "|" + expiry + "|" + iconName).hashCode();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FridgeItem item = items.get(position);

        holder.ivIcon.setImageDrawable(null);
        holder.ivIcon.setImageResource(0);
        holder.ivIcon.setVisibility(View.INVISIBLE);

        holder.tvName.setText(item != null ? safe(item.getName()) : "");

        if (showAddedBy && item != null) {
            String addedBy = safe(item.getAddedByNickname());

            if (TextUtils.isEmpty(addedBy)) {
                addedBy = "나";
            }

            if (!TextUtils.isEmpty(myNickname) && myNickname.equals(addedBy)) {
                addedBy = "나";
            }

            holder.tvAddedBy.setText(addedBy);
            holder.tvAddedBy.setVisibility(View.VISIBLE);
        } else {
            holder.tvAddedBy.setText("");
            holder.tvAddedBy.setVisibility(View.GONE);
        }

        if (item != null) {
            holder.tvQuantity.setText(
                    QTY_FMT.format(item.getQuantity()) + " " + safeUnit(item.getUnit())
            );
        } else {
            holder.tvQuantity.setText("");
        }

        int iconRes = resolveBestIconRes(item);
        holder.ivIcon.setImageResource(iconRes);
        holder.ivIcon.setVisibility(View.VISIBLE);

        if (item != null) {
            String dDay = item.getDDayString();

            if (!TextUtils.isEmpty(dDay)) {
                holder.tvExpiry.setText(dDay);
                holder.tvExpiry.setTextColor(resolveDDayColor(dDay));
                holder.tvExpiry.setVisibility(View.VISIBLE);
            } else {
                holder.tvExpiry.setText("");
                holder.tvExpiry.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.tvExpiry.setText("");
            holder.tvExpiry.setVisibility(View.INVISIBLE);
        }

        boolean selected = item != null && selectedIds.contains(stableKey(item));
        if (selectionMode && selected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_item_selected);
        } else {
            holder.itemView.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(item, position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item, position);
                return true;
            }
            return false;
        });
    }

    private int resolveBestIconRes(FridgeItem item) {
        if (item == null) return R.drawable.ic_custom_item;

        String productName = normalize(item.getName());
        String iconName = normalize(item.getIconName());

        int byProductName = findCatalogResByProductName(productName);
        if (byProductName != 0) {
            return byProductName;
        }

        int byIconName = findCatalogResByIconName(iconName);
        if (byIconName != 0) {
            return byIconName;
        }

        int byDrawableName = findDrawableByName(iconName);
        if (byDrawableName != 0) {
            return byDrawableName;
        }

        if (mustUseCustomFallback(productName, iconName)) {
            return R.drawable.ic_custom_item;
        }

        return R.drawable.ic_custom_item;
    }

    private boolean mustUseCustomFallback(String productName, String iconName) {
        if (TextUtils.isEmpty(productName) && TextUtils.isEmpty(iconName)) {
            return true;
        }

        if ("직접 추가".equals(productName) || "ic_custom_item".equals(iconName)) {
            return true;
        }

        return false;
    }

    private int findCatalogResByProductName(String productName) {
        if (TextUtils.isEmpty(productName)) return 0;

        List<IconItem> all = IconCatalog.getAllIcons();
        for (IconItem icon : all) {
            if (icon == null) continue;

            String displayName = normalize(icon.getName());
            String rawKey = normalize(icon.getRawKey());

            if (productName.equals(displayName) || productName.equals(rawKey)) {
                return icon.getResId();
            }
        }
        return 0;
    }

    private int findCatalogResByIconName(String iconName) {
        if (TextUtils.isEmpty(iconName)) return 0;

        int res = IconCatalog.findResIdByRawKey(iconName);
        if (res != 0) return res;

        List<IconItem> all = IconCatalog.getAllIcons();
        for (IconItem icon : all) {
            if (icon == null) continue;

            String displayName = normalize(icon.getName());
            String rawKey = normalize(icon.getRawKey());

            if (iconName.equals(displayName) || iconName.equals(rawKey)) {
                return icon.getResId();
            }
        }
        return 0;
    }

    private int findDrawableByName(String name) {
        if (context == null || TextUtils.isEmpty(name)) return 0;

        int resId = context.getResources().getIdentifier(
                name,
                "drawable",
                context.getPackageName()
        );
        if (resId != 0) return resId;

        resId = context.getResources().getIdentifier(
                name,
                "mipmap",
                context.getPackageName()
        );
        return resId;
    }

    private int resolveDDayColor(String dDay) {
        if ("유통기한 지남".equals(dDay) || "D-Day".equals(dDay)) {
            return Color.parseColor("#F44336");
        }
        if (dDay.startsWith("D-")) {
            try {
                int days = Integer.parseInt(dDay.substring(2));
                if (days <= 3) return Color.parseColor("#FF7043");
                if (days <= 7) return Color.parseColor("#FFA726");
                return Color.parseColor("#4CAF50");
            } catch (Exception ignore) {
                return Color.GRAY;
            }
        }
        return Color.GRAY;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.getDefault());
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String safeUnit(String unit) {
        if (unit == null) return "개";
        String u = unit.trim();
        return u.isEmpty() ? "개" : u;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<FridgeItem> newItems) {
        this.items = (newItems != null) ? itemsOrEmpty(newItems) : new ArrayList<>();
        selectedIds.clear();
        notifyDataSetChanged();
    }

    private List<FridgeItem> itemsOrEmpty(List<FridgeItem> src) {
        return src == null ? new ArrayList<>() : src;
    }

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!selectionMode) selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public void toggleSelection(int position) {
        if (position < 0 || position >= items.size()) return;

        FridgeItem it = items.get(position);
        if (it == null) return;

        long key = stableKey(it);
        if (selectedIds.contains(key)) {
            selectedIds.remove(key);
        } else {
            selectedIds.add(key);
        }

        notifyItemChanged(position);
    }

    public List<com.namgyun.tamakitchen.ui.fridge.FridgeItem> getSelectedItems() {
        List<com.namgyun.tamakitchen.ui.fridge.FridgeItem> out = new ArrayList<>();
        for (FridgeItem it : items) {
            if (it == null) continue;
            if (selectedIds.contains(stableKey(it))) out.add(it);
        }
        return out;
    }
}