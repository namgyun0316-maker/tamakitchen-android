package com.namgyun.tamakitchen.ui.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.ui.fridge.FridgeItem;
import com.namgyun.tamakitchen.ui.fridge.GuestFridgeStore;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuestDataMigrator {

    public interface DoneCallback {
        void onDone();
    }

    private GuestDataMigrator() {}

    public static void migrateFridgeIfNeeded(
            @NonNull Activity activity,
            long userId,
            @NonNull FridgeApiService apiService,
            @NonNull DoneCallback callback
    ) {
        if (userId <= 0) {
            callback.onDone();
            return;
        }

        List<FridgeItem> local = GuestFridgeStore.load(activity);
        if (local == null || local.isEmpty()) {
            callback.onDone();
            return;
        }

        // ✅ 커스텀 다이얼로그
        View v = LayoutInflater.from(activity).inflate(R.layout.dialog_guest_data_migrate, null, false);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvMessage = v.findViewById(R.id.tvMessage);
        MaterialButton btnSkip = v.findViewById(R.id.btnSkip);
        MaterialButton btnImport = v.findViewById(R.id.btnImport);

        if (tvTitle != null) tvTitle.setText("비회원 데이터 가져오기");
        if (tvMessage != null) {
            tvMessage.setText("비회원으로 등록한 냉장고 데이터를 로그인 계정으로 가져올까요?\n가져오면 기기 저장 데이터는 삭제됩니다.");
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(v)
                .setCancelable(false)
                .create();

        // ✅ 배경 투명 (레이아웃 배경이 카드 역할)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnSkip != null) {
            btnSkip.setOnClickListener(view -> {
                dialog.dismiss();
                callback.onDone();
            });
        }

        if (btnImport != null) {
            btnImport.setOnClickListener(view -> {
                dialog.dismiss();
                showWhiteToast(activity, "데이터를 가져오는 중...");
                uploadFridgeItems(activity, userId, apiService, local, callback);
            });
        }

        dialog.show();

        // ✅ 폭 살짝 줄여 카드 느낌
        if (dialog.getWindow() != null) {
            int w = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void uploadFridgeItems(
            @NonNull Activity activity,
            long userId,
            @NonNull FridgeApiService apiService,
            @NonNull List<FridgeItem> localItems,
            @NonNull DoneCallback callback
    ) {
        List<FridgeItem> items = new ArrayList<>();
        for (FridgeItem it : localItems) {
            if (it == null) continue;
            it.setId(null); // 서버가 새로 생성
            items.add(it);
        }

        if (items.isEmpty()) {
            callback.onDone();
            return;
        }

        AtomicInteger remaining = new AtomicInteger(items.size());
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (FridgeItem item : items) {
            apiService.addFridgeItem(userId, item).enqueue(new Callback<FridgeItem>() {
                @Override
                public void onResponse(Call<FridgeItem> call, Response<FridgeItem> response) {
                    if (response.isSuccessful()) success.incrementAndGet();
                    else fail.incrementAndGet();
                    finishOne();
                }

                @Override
                public void onFailure(Call<FridgeItem> call, Throwable t) {
                    fail.incrementAndGet();
                    finishOne();
                }

                private void finishOne() {
                    if (remaining.decrementAndGet() == 0) {
                        if (!activity.isFinishing()) {
                            Context ctx = activity;
                            if (success.get() > 0) {
                                GuestFridgeStore.clear(ctx);
                            }
                            showWhiteToast(activity,
                                    "가져오기 완료: 성공 " + success.get() + " / 실패 " + fail.get());
                        }
                        callback.onDone();
                    }
                }
            });
        }
    }

    // =========================
    // ✅ 흰 토스트(앱 내부 오버레이) - toast_white.xml 사용
    // =========================
    private static void showWhiteToast(@NonNull Activity activity, @NonNull String msg) {
        if (activity.isFinishing()) return;

        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> {
            try {
                ViewGroup root = activity.findViewById(android.R.id.content);
                if (root == null) return;

                View v = LayoutInflater.from(activity).inflate(R.layout.toast_white, root, false);
                TextView tv = v.findViewById(R.id.tvToast);
                if (tv != null) tv.setText(msg);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                lp.bottomMargin = (int) (110 * activity.getResources().getDisplayMetrics().density);
                v.setLayoutParams(lp);

                v.setAlpha(0f);
                root.addView(v);
                v.animate().alpha(1f).setDuration(140).start();

                v.postDelayed(() -> {
                    try { root.removeView(v); } catch (Throwable ignored) {}
                }, 1300);

            } catch (Throwable ignored) {
                // 여기서는 시스템 토스트로 fallback도 안 씀(검정 토스트 방지)
            }
        });
    }
}