package com.namgyun.tamakitchen.ui.recipe;

public class MissingIngredientItem {

    private String name;
    private boolean checked;

    public MissingIngredientItem(String name, boolean checked) {
        this.name = name;
        this.checked = checked;
    }

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
