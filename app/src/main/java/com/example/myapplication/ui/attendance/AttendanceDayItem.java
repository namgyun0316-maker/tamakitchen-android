package com.namgyun.tamakitchen.ui.attendance;

public class AttendanceDayItem {

    private final int day;
    private final int reward;
    private final boolean checked;
    private final boolean todayTarget;

    public AttendanceDayItem(int day, int reward, boolean checked, boolean todayTarget) {
        this.day = day;
        this.reward = reward;
        this.checked = checked;
        this.todayTarget = todayTarget;
    }

    public int getDay() {
        return day;
    }

    public int getReward() {
        return reward;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isTodayTarget() {
        return todayTarget;
    }
}