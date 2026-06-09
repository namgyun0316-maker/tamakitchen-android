package com.namgyun.tamakitchen.ui.home;

public class FacePlacement {

    private final int widthDp;
    private final int heightDp;
    private final int offsetXDp;
    private final int offsetYDp;

    public FacePlacement(int widthDp, int heightDp, int offsetXDp, int offsetYDp) {
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.offsetXDp = offsetXDp;
        this.offsetYDp = offsetYDp;
    }

    public int getWidthDp() {
        return widthDp;
    }

    public int getHeightDp() {
        return heightDp;
    }

    public int getOffsetXDp() {
        return offsetXDp;
    }

    public int getOffsetYDp() {
        return offsetYDp;
    }

    public static FacePlacement defaultValue() {
        return new FacePlacement(110, 110, 0, 0);
    }
}