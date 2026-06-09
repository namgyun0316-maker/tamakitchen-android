package com.namgyun.tamakitchen.ui.attendance;

import android.content.Context;
import android.content.SharedPreferences;

import com.namgyun.tamakitchen.pet.PetPrefs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AttendancePrefs {

    private static final String PREF = "attendance_prefs";

    private static final String KEY_MONTH_KEY = "month_key";
    private static final String KEY_LAST_CHECK_DATE = "last_check_date";
    private static final String KEY_ATTENDANCE_COUNT = "attendance_count";
    private static final String KEY_CHECKED_DAYS = "checked_days";

    private static SharedPreferences sp(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static class AttendanceResult {
        public final boolean success;
        public final int attendanceCount;
        public final int reward;
        public final String message;

        public AttendanceResult(boolean success, int attendanceCount, int reward, String message) {
            this.success = success;
            this.attendanceCount = attendanceCount;
            this.reward = reward;
            this.message = message;
        }
    }

    public static void ensureMonthSynced(Context context) {
        SharedPreferences prefs = sp(context);

        String currentMonth = getCurrentMonthKey();
        String savedMonth = prefs.getString(KEY_MONTH_KEY, "");

        if (!currentMonth.equals(savedMonth)) {
            prefs.edit()
                    .putString(KEY_MONTH_KEY, currentMonth)
                    .putString(KEY_LAST_CHECK_DATE, "")
                    .putInt(KEY_ATTENDANCE_COUNT, 0)
                    .putStringSet(KEY_CHECKED_DAYS, new HashSet<>())
                    .apply();
            return;
        }

        normalizeAttendanceData(context);
    }

    public static boolean canCheckToday(Context context) {
        ensureMonthSynced(context);

        String today = getTodayKey();
        String last = sp(context).getString(KEY_LAST_CHECK_DATE, "");
        return !today.equals(last);
    }

    public static int getAttendanceCount(Context context) {
        ensureMonthSynced(context);
        return sp(context).getInt(KEY_ATTENDANCE_COUNT, 0);
    }

    public static Set<String> getCheckedDays(Context context) {
        ensureMonthSynced(context);
        return new HashSet<>(sp(context).getStringSet(KEY_CHECKED_DAYS, new HashSet<>()));
    }

    public static int getNextAttendanceDay(Context context) {
        ensureMonthSynced(context);

        Set<String> checkedDays = getCheckedDays(context);

        for (int day = 1; day <= 30; day++) {
            if (!checkedDays.contains(String.valueOf(day))) {
                return day;
            }
        }

        return 30;
    }

    public static int getRewardForDay(int day) {
        if (day <= 0 || day > 30) return 0;

        if (day == 30) return 500;
        if (day == 5 || day == 10 || day == 15 || day == 20 || day == 25) return 100;
        return 50;
    }

    public static AttendanceResult checkToday(Context context) {
        ensureMonthSynced(context);

        SharedPreferences prefs = sp(context);

        String today = getTodayKey();
        String last = prefs.getString(KEY_LAST_CHECK_DATE, "");

        if (today.equals(last)) {
            int count = prefs.getInt(KEY_ATTENDANCE_COUNT, 0);
            return new AttendanceResult(false, count, 0, "오늘은 이미 출석했어.");
        }

        Set<String> checked = new HashSet<>(prefs.getStringSet(KEY_CHECKED_DAYS, new HashSet<>()));
        int nextDay = 1;

        for (int day = 1; day <= 30; day++) {
            if (!checked.contains(String.valueOf(day))) {
                nextDay = day;
                break;
            }
            nextDay = 30;
        }

        if (checked.contains(String.valueOf(30)) && checked.size() >= 30) {
            return new AttendanceResult(false, 30, 0, "이미 30일차까지 모두 채웠어.");
        }

        int reward = getRewardForDay(nextDay);
        checked.add(String.valueOf(nextDay));

        int normalizedCount = checked.size();

        prefs.edit()
                .putString(KEY_LAST_CHECK_DATE, today)
                .putInt(KEY_ATTENDANCE_COUNT, normalizedCount)
                .putStringSet(KEY_CHECKED_DAYS, checked)
                .apply();

        PetPrefs.addCoins(context, reward);

        return new AttendanceResult(
                true,
                normalizedCount,
                reward,
                nextDay + "일차 출석 완료! " + reward + "코인을 받았어."
        );
    }

    public static AttendanceResult debugForceCheck(Context context) {
        ensureMonthSynced(context);

        SharedPreferences prefs = sp(context);
        Set<String> checked = new HashSet<>(prefs.getStringSet(KEY_CHECKED_DAYS, new HashSet<>()));

        int nextDay = -1;
        for (int day = 1; day <= 30; day++) {
            if (!checked.contains(String.valueOf(day))) {
                nextDay = day;
                break;
            }
        }

        if (nextDay == -1) {
            return new AttendanceResult(false, 30, 0, "이미 30일차까지 모두 채웠어.");
        }

        int reward = getRewardForDay(nextDay);
        checked.add(String.valueOf(nextDay));

        int normalizedCount = checked.size();

        prefs.edit()
                .putInt(KEY_ATTENDANCE_COUNT, normalizedCount)
                .putStringSet(KEY_CHECKED_DAYS, checked)
                .apply();

        PetPrefs.addCoins(context, reward);

        return new AttendanceResult(
                true,
                normalizedCount,
                reward,
                "[테스트] " + nextDay + "일차 출석 완료! " + reward + "코인을 받았어."
        );
    }

    public static void debugResetAttendance(Context context) {
        SharedPreferences prefs = sp(context);

        prefs.edit()
                .putString(KEY_MONTH_KEY, getCurrentMonthKey())
                .putString(KEY_LAST_CHECK_DATE, "")
                .putInt(KEY_ATTENDANCE_COUNT, 0)
                .putStringSet(KEY_CHECKED_DAYS, new HashSet<>())
                .apply();
    }

    private static void normalizeAttendanceData(Context context) {
        SharedPreferences prefs = sp(context);
        Set<String> checked = new HashSet<>(prefs.getStringSet(KEY_CHECKED_DAYS, new HashSet<>()));

        int normalizedCount = checked.size();
        int savedCount = prefs.getInt(KEY_ATTENDANCE_COUNT, 0);

        if (savedCount != normalizedCount) {
            prefs.edit()
                    .putInt(KEY_ATTENDANCE_COUNT, normalizedCount)
                    .apply();
        }
    }

    private static String getCurrentMonthKey() {
        return new SimpleDateFormat("yyyyMM", Locale.KOREA).format(Calendar.getInstance().getTime());
    }

    private static String getTodayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Calendar.getInstance().getTime());
    }
}