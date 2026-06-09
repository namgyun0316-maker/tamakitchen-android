package com.namgyun.tamakitchen.ui.fridge;

import com.namgyun.tamakitchen.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FridgeSelectionController {

    private boolean selectionMode = false;

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void enter(FridgeAdapter adapter, FloatingActionButton fab) {
        selectionMode = true;
        if (adapter != null) {
            adapter.setSelectionMode(true);
            adapter.clearSelection();
        }
        updateFab(fab);
    }

    public void exit(FridgeAdapter adapter, FloatingActionButton fab) {
        if (!selectionMode) return;
        selectionMode = false;
        if (adapter != null) {
            adapter.clearSelection();
            adapter.setSelectionMode(false);
        }
        updateFab(fab);
    }

    public void updateFab(FloatingActionButton fab) {
        if (fab == null) return;
        if (selectionMode) {
            fab.setImageResource(R.drawable.ic_trash);
            fab.setContentDescription("선택 삭제");
        } else {
            fab.setImageResource(android.R.drawable.ic_input_add);
            fab.setContentDescription("추가");
        }
    }

    public int getSelectedCount(FridgeAdapter adapter) {
        if (adapter == null) return 0;
        return adapter.getSelectedCount();
    }
}