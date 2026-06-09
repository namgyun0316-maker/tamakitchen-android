package com.namgyun.tamakitchen.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.namgyun.tamakitchen.MainActivity;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class OnboardingActivity extends AppCompatActivity
        implements StepFridgeTypeFragment.OnboardingNav {

    private static final String TAG = "OnboardingActivity";

    private ViewPager2 viewPager;
    private TextView tvStep;
    private TextView btnPrev;
    private TextView btnNext;

    private OnboardingPagerAdapter pagerAdapter;

    private String nickname = "";
    private String fridgeType = "PERSONAL";
    private final Set<String> tools = new HashSet<>();

    private boolean isLoginDialogShowing = false;

    private static final Pattern ALLOWED_CHARS =
            Pattern.compile("^[0-9A-Za-z가-힣ㄱ-ㅎㅏ-ㅣ]+$");
    private static final Pattern MUST_CONTAIN_NORMAL =
            Pattern.compile(".*([0-9A-Za-z가-힣]).*");

    private boolean nextEnabledByStep2 = false;
    private boolean nextEnabledByStep3 = false;

    private String pendingToastMessage = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private View inAppToastView;
    private Runnable hideToastRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_onboarding);

            viewPager = findViewById(R.id.viewPager);
            tvStep = findViewById(R.id.tvStep);
            btnPrev = findViewById(R.id.btnPrev);
            btnNext = findViewById(R.id.btnNext);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBar), (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                int base = (int) (16 * getResources().getDisplayMetrics().density);
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), base + bottomInset);
                return insets;
            });

            viewPager.setUserInputEnabled(false);

            pagerAdapter = new OnboardingPagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);
            viewPager.setOffscreenPageLimit(2);

            try {
                View child = viewPager.getChildAt(0);
                if (child instanceof androidx.recyclerview.widget.RecyclerView) {
                    ((androidx.recyclerview.widget.RecyclerView) child).setItemAnimator(null);
                }
            } catch (Throwable t) {
                Log.w(TAG, "disable viewPager item animator failed", t);
            }

            updateStepUi(0);

            btnPrev.setOnClickListener(v -> {
                int cur = viewPager.getCurrentItem();
                if (cur > 0) viewPager.setCurrentItem(cur - 1, true);
            });

            btnNext.setOnClickListener(v -> {
                if (btnNext != null && !btnNext.isEnabled()) return;

                int cur = viewPager.getCurrentItem();

                if (cur == 0) {
                    if (!isNicknameValid(nickname)) {
                        showIosToast("닉네임은 2~8자, 한글·영문·숫자만 입력 가능합니다.");
                        return;
                    }
                    viewPager.setCurrentItem(1, true);
                    return;
                }

                if (cur == 1) {
                    if (!nextEnabledByStep2) {
                        showIosToast("냉장고 유형을 선택해주세요.");
                        return;
                    }

                    boolean isGuest = AuthPrefs.isGuest(this);
                    if (isGuest && "SHARED".equalsIgnoreCase(fridgeType)) {
                        showLoginRequiredDialog();
                        return;
                    }

                    viewPager.setCurrentItem(2, true);
                    return;
                }

                if (cur == 2) {
                    if (tools.isEmpty()) {
                        showIosToast("조리도구를 1개 이상 선택해주세요.");
                        return;
                    }
                    finishOnboardingSoft();
                }
            });

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateStepUi(position);

                    if (position == 1) {
                        setNextEnabled(false);
                    }

                    if (position == 2 && pagerAdapter != null) {
                        StepToolsFragment f = pagerAdapter.getStepToolsFragment();
                        if (f != null) {
                            f.requestResetSelection();
                        }
                        nextEnabledByStep3 = false;
                        updateStepUi(2);
                    }

                    if (pendingToastMessage != null) {
                        final String msg = pendingToastMessage;
                        pendingToastMessage = null;
                        viewPager.postDelayed(() -> showIosToast(msg), 80);
                    }
                }
            });

        } catch (Throwable t) {
            Log.e(TAG, "onboarding failed, fallback to Main", t);
            moveToMain();
        }
    }

    @Override
    protected void onDestroy() {
        removeInAppToast();
        super.onDestroy();
    }

    private void updateStepUi(int page) {
        if (tvStep != null) tvStep.setText((page + 1) + " / 3");
        if (btnPrev != null) btnPrev.setEnabled(page > 0);
        if (btnNext != null) btnNext.setText(page == 2 ? "완료" : "다음");

        if (page == 0) {
            internalSetNextButtonEnabled(true);
        } else if (page == 1) {
            internalSetNextButtonEnabled(nextEnabledByStep2);
        } else {
            internalSetNextButtonEnabled(nextEnabledByStep3);
        }
    }

    private boolean isNicknameValid(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() < 2 || t.length() > 8) return false;
        if (t.contains(" ")) return false;
        if (!ALLOWED_CHARS.matcher(t).matches()) return false;
        return MUST_CONTAIN_NORMAL.matcher(t).matches();
    }

    @Override
    public void setNextEnabled(boolean enabled) {
        nextEnabledByStep2 = enabled;
        if (viewPager != null && viewPager.getCurrentItem() == 1) {
            internalSetNextButtonEnabled(enabled);
        }
    }

    public void onToolsSelectionChanged(int selectedCount) {
        nextEnabledByStep3 = selectedCount >= 1;

        if (viewPager != null && viewPager.getCurrentItem() == 2) {
            updateStepUi(2);
        }
    }

    @Override
    public void goNextStep() {
        if (viewPager == null) return;
        int cur = viewPager.getCurrentItem();
        if (cur < 2) viewPager.setCurrentItem(cur + 1, true);
    }

    private void internalSetNextButtonEnabled(boolean enabled) {
        if (btnNext == null) return;
        btnNext.setEnabled(enabled);
        btnNext.setAlpha(enabled ? 1f : 0.45f);
    }

    public void enqueueToastAfterTransition(String message) {
        if (message == null) return;
        String msg = message.trim();
        if (msg.isEmpty()) return;
        pendingToastMessage = msg;
    }

    private void showLoginRequiredDialog() {
        if (isLoginDialogShowing) return;
        isLoginDialogShowing = true;

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_login_required, null, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_login_required);
        }

        View btnOk = v.findViewById(R.id.btnOk);
        if (btnOk != null) btnOk.setOnClickListener(view -> dialog.dismiss());

        dialog.setOnDismissListener(d -> isLoginDialogShowing = false);
        dialog.show();

        if (dialog.getWindow() != null) {
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void finishOnboardingSoft() {
        try {
            OnboardingPrefs.saveNickname(this, nickname);
            OnboardingPrefs.saveFridgeType(this, fridgeType);
            OnboardingPrefs.saveTools(this, tools);

            SharedPreferences p = getSharedPreferences("user_info", MODE_PRIVATE);
            p.edit().putString("nickname", nickname == null ? "" : nickname.trim()).apply();

            OnboardingPrefs.markDone(this);

        } catch (Throwable t) {
            Log.e(TAG, "save onboarding prefs failed", t);
        }

        moveToMain();
    }

    private void moveToMain() {
        try {
            startActivity(new Intent(this, MainActivity.class));
        } catch (Throwable t) {
            Log.e(TAG, "move to main failed", t);
        }
        finish();
    }

    public void setNickname(String nickname) {
        this.nickname = (nickname == null) ? "" : nickname.trim();
    }

    public void setFridgeType(String fridgeType) {
        if (fridgeType == null || fridgeType.trim().isEmpty()) return;
        this.fridgeType = fridgeType.trim();
    }

    public void setTools(Set<String> selected) {
        tools.clear();
        if (selected != null) tools.addAll(selected);
    }

    public void showIosToast(String message) {
        if (isFinishing() || isDestroyed()) return;
        if (message == null) return;

        String msg = message.trim();
        if (msg.isEmpty()) return;

        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> showIosToast(msg));
            return;
        }

        final ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        removeInAppToast();

        View v = getLayoutInflater().inflate(R.layout.custom_toast, root, false);
        TextView tv = v.findViewById(R.id.toast_message);
        if (tv != null) tv.setText(msg);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = (int) (110 * getResources().getDisplayMetrics().density);
        v.setLayoutParams(lp);

        v.setAlpha(0f);
        v.setScaleX(0.98f);
        v.setScaleY(0.98f);

        root.addView(v);
        inAppToastView = v;

        v.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        hideToastRunnable = () -> {
            if (inAppToastView == v) {
                v.animate()
                        .alpha(0f)
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .setDuration(120)
                        .withEndAction(() -> {
                            if (inAppToastView == v) {
                                try {
                                    root.removeView(v);
                                } catch (Throwable t) {
                                    Log.w(TAG, "remove toast view failed", t);
                                }
                                inAppToastView = null;
                            }
                        })
                        .start();
            }
        };
        mainHandler.postDelayed(hideToastRunnable, 1200);
    }

    private void removeInAppToast() {
        try {
            ViewGroup root = findViewById(android.R.id.content);
            if (root != null && inAppToastView != null) {
                root.removeView(inAppToastView);
            }
        } catch (Throwable t) {
            Log.w(TAG, "remove in app toast failed", t);
        }

        inAppToastView = null;

        if (hideToastRunnable != null) {
            mainHandler.removeCallbacks(hideToastRunnable);
            hideToastRunnable = null;
        }
    }
}