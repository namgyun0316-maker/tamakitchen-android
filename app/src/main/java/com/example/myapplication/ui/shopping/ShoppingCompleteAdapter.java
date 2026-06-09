package com.namgyun.tamakitchen.ui.shopping;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShoppingCompleteAdapter extends RecyclerView.Adapter<ShoppingCompleteAdapter.VH> {

    public interface OnSelectionChangedListener {
        void onChanged(int selectedCount, int totalCount, boolean allSelected);
    }

    private final List<ShoppingItem> items;
    private boolean[] checked;
    private OnSelectionChangedListener selectionListener;

    public ShoppingCompleteAdapter(List<ShoppingItem> items) {
        this.items = (items != null) ? items : new ArrayList<>();
        this.checked = new boolean[this.items.size()];
        // ✅ 여기서 notifySelection() 호출하면 listener가 아직 null이라 의미없음 -> 제거
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        this.selectionListener = l;
        notifySelection(); // ✅ 리스너 연결 시 현재 상태 1회 알림
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        ImageView iv;
        TextView tvName;
        TextView tvQty;

        VH(@NonNull View itemView) {
            super(itemView);
            cb = itemView.findViewById(R.id.cb_complete);
            iv = itemView.findViewById(R.id.iv_complete_icon);
            tvName = itemView.findViewById(R.id.tv_complete_name);
            tvQty = itemView.findViewById(R.id.tv_complete_qty);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_complete_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        if (position < 0 || position >= items.size()) return;

        ShoppingItem item = items.get(position);

        // ===== 텍스트 바인딩 =====
        String name = (item == null || item.getName() == null) ? "" : item.getName().trim();
        h.tvName.setText(name);

        String qtyText = "1개";
        if (item != null) {
            qtyText = formatQty(item.getQuantity()) + ShoppingUtils.safeUnit(item.getUnit());
        }
        h.tvQty.setText(qtyText);

        // ===== 체크 상태 바인딩 =====
        h.cb.setOnCheckedChangeListener(null);

        boolean isChecked = safeCheckedAt(position);
        h.cb.setChecked(isChecked);

        // ===== 아이콘 바인딩 =====
        int resId = (item != null) ? item.getIconResId() : 0;

        if (resId == 0 && item != null) {
            String iconKey = item.getIconKey();
            if (!TextUtils.isEmpty(iconKey)) {
                resId = IconCatalog.findResIdByRawKey(iconKey);
            }

            if (resId == 0) {
                IconItem icon = findIconByName(item.getName());
                if (icon != null) {
                    if (TextUtils.isEmpty(item.getIconKey())) item.setIconKey(icon.getRawKey());
                    resId = icon.getResId();
                }
            }

            item.setIconResId(resId);
        }

        h.iv.setImageResource(resId != 0 ? resId : R.drawable.ic_launcher_foreground);

        // ===== 클릭 동작 =====
        // ✅ 행 클릭 -> 토글
        h.itemView.setOnClickListener(v -> toggleChecked(h.getAdapterPosition()));

        // ✅ 체크박스 직접 클릭 -> 상태 반영
        h.cb.setOnCheckedChangeListener((buttonView, checkedNow) -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (pos < 0 || pos >= checked.length) return;

            checked[pos] = checkedNow;
            notifySelection();
        });
    }

    // =========================================================
    // ✅ 외부에서 쓰는 기능들 (전체선택/해제 토글)
    // =========================================================
    public boolean isAllSelected() {
        if (items.isEmpty()) return false;
        for (boolean b : checked) if (!b) return false;
        return true;
    }

    /** ✅ 버튼에서 "전체선택/전체해제" 토글할 때 사용 */
    public void toggleSelectAll() {
        setAllChecked(!isAllSelected());
    }

    /** ✅ 전체 선택/해제 강제 */
    public void setAllChecked(boolean value) {
        for (int i = 0; i < checked.length; i++) checked[i] = value;
        notifyDataSetChanged();
        notifySelection();
    }

    // =========================================================
    // ✅ 선택 결과
    // =========================================================
    public List<ShoppingItem> getSelectedItems() {
        List<ShoppingItem> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i < checked.length && checked[i]) out.add(items.get(i));
        }
        return out;
    }

    // =========================================================
    // ✅ 데이터 갱신
    // =========================================================
    public void updateItems(List<ShoppingItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        checked = new boolean[items.size()];
        notifyDataSetChanged();
        notifySelection();
    }

    // =========================================================
    // 내부 유틸
    // =========================================================
    private void toggleChecked(int pos) {
        if (pos == RecyclerView.NO_POSITION) return;
        if (pos < 0 || pos >= checked.length) return;

        checked[pos] = !checked[pos];
        notifyItemChanged(pos);
        notifySelection();
    }

    private boolean safeCheckedAt(int position) {
        if (position < 0 || position >= checked.length) return false;
        return checked[position];
    }

    private void notifySelection() {
        if (selectionListener == null) return;

        int total = items.size();
        int selected = 0;
        for (boolean b : checked) if (b) selected++;

        boolean allSelected = total > 0 && selected == total;
        selectionListener.onChanged(selected, total, allSelected);
    }

    private IconItem findIconByName(String name) {
        if (TextUtils.isEmpty(name)) return null;
        List<IconItem> all = IconCatalog.getAllIcons();
        for (IconItem icon : all) {
            if (icon == null) continue;
            if (name.trim().equals(icon.getName())) return icon;
        }
        return null;
    }

    private String formatQty(double q) {
        // 1.0 -> 1, 1.5 -> 1.5 형태
        if (Math.abs(q - Math.round(q)) < 1e-9) return String.valueOf((long) Math.round(q));
        String s = String.format(Locale.KOREA, "%.1f", q);
        return s.replace(".0", "");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
