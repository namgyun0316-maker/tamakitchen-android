package com.namgyun.tamakitchen.ui.attendance;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AttendanceActivity extends AppCompatActivity {

    private TextView tvAttendanceDesc;
    private TextView tvAttendanceStatusPrefix;
    private TextView tvAttendanceStatusReward;
    private ImageView ivAttendanceStatusCoin;
    private TextView tvAttendanceCalendarTitle;
    private RecyclerView recyclerAttendance;
    private MaterialButton btnAttendanceCheck;
    private NestedScrollView scrollAttendance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        tvAttendanceDesc = findViewById(R.id.tvAttendanceDesc);
        tvAttendanceStatusPrefix = findViewById(R.id.tvAttendanceStatusPrefix);
        tvAttendanceStatusReward = findViewById(R.id.tvAttendanceStatusReward);
        ivAttendanceStatusCoin = findViewById(R.id.ivAttendanceStatusCoin);
        tvAttendanceCalendarTitle = findViewById(R.id.tvAttendanceCalendarTitle);
        recyclerAttendance = findViewById(R.id.recyclerAttendance);
        btnAttendanceCheck = findViewById(R.id.btnAttendanceCheck);
        scrollAttendance = findViewById(R.id.scrollAttendance);

        recyclerAttendance.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerAttendance.setNestedScrollingEnabled(false);
        recyclerAttendance.setHasFixedSize(false);

        btnAttendanceCheck.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            AttendancePrefs.AttendanceResult result = AttendancePrefs.checkToday(this);

            if (result.success) {
                ChecklistPrefs.markWeeklyAttendanceDone(this);
            }

            AppToast.show(this, result.message);
            bindUi(result.success ? result.attendanceCount : -1);
            scrollToTop();
        });

        bindUi(-1);
    }

    private void scrollToTop() {
        if (scrollAttendance != null) {
            scrollAttendance.post(() -> scrollAttendance.smoothScrollTo(0, 0));
        }
    }

    private void bindUi(int animatedStampDay) {
        AttendancePrefs.ensureMonthSynced(this);

        boolean canCheckToday = AttendancePrefs.canCheckToday(this);
        int attendanceCount = AttendancePrefs.getAttendanceCount(this);
        int nextDay = AttendancePrefs.getNextAttendanceDay(this);
        int displayRewardDay = canCheckToday ? nextDay : attendanceCount;

        tvAttendanceDesc.setText("이번 달 출석을 채우고 보상을 받아가자.");
        tvAttendanceCalendarTitle.setText("30일 출석 달력");
        ivAttendanceStatusCoin.setImageResource(R.drawable.ic_coin);

        if (canCheckToday) {
            tvAttendanceStatusPrefix.setText("오늘 보상");
            tvAttendanceStatusReward.setText(
                    String.valueOf(AttendancePrefs.getRewardForDay(displayRewardDay))
            );
        } else {
            tvAttendanceStatusPrefix.setText("오늘 출석 완료 · 보상");
            tvAttendanceStatusReward.setText(
                    String.valueOf(AttendancePrefs.getRewardForDay(displayRewardDay))
            );
        }

        recyclerAttendance.setAdapter(
                new AttendanceDayAdapter(
                        this,
                        buildItems(canCheckToday),
                        animatedStampDay
                )
        );

        btnAttendanceCheck.setEnabled(canCheckToday);
        btnAttendanceCheck.setText(
                canCheckToday ? "오늘 출석하기" : "오늘 출석 완료"
        );
    }

    private List<AttendanceDayItem> buildItems(boolean canCheckToday) {
        List<AttendanceDayItem> items = new ArrayList<>();

        Set<String> checkedDays = AttendancePrefs.getCheckedDays(this);
        int nextDay = AttendancePrefs.getNextAttendanceDay(this);

        for (int day = 1; day <= 30; day++) {
            boolean checked = checkedDays.contains(String.valueOf(day));
            boolean todayTarget = canCheckToday && day == nextDay;

            items.add(new AttendanceDayItem(
                    day,
                    AttendancePrefs.getRewardForDay(day),
                    checked,
                    todayTarget
            ));
        }

        return items;
    }
}