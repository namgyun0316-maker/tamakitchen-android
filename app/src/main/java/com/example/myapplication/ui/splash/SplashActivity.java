package com.namgyun.tamakitchen.ui.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.namgyun.tamakitchen.MainActivity;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.ui.auth.LoginActivity;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingActivity;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    // ✅ 최소 스플래시 유지 시간
    private static final long SPLASH_DELAY_MS = 1200L;

    // ✅ 로그인 prefs
    private static final String PREFS_LOGIN = "login_prefs";
    private static final String KEY_USER_ID = "userId";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ✅ 앱 시작 즉시 서버 깨우기
        warmUpServer();

        // ✅ 스플래시 유지 후 이동
        mainHandler.postDelayed(this::routeApp, SPLASH_DELAY_MS);
    }

    /**
     * ✅ 앱 진입 분기
     *
     * 1. 로그인 안됨 -> LoginActivity
     * 2. 로그인 O + 온보딩 안함 -> OnboardingActivity
     * 3. 로그인 O + 온보딩 완료 -> MainActivity
     */
    private void routeApp() {

        SharedPreferences prefs =
                getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);

        long userId = prefs.getLong(KEY_USER_ID, -1L);

        Intent intent;

        // ✅ 로그인 안됨
        if (userId <= 0) {

            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );

        } else {

            // ✅ 로그인 상태

            if (OnboardingPrefs.isOnboardingDone(this)) {

                // ✅ 온보딩 완료
                intent = new Intent(
                        SplashActivity.this,
                        MainActivity.class
                );

            } else {

                // ✅ 온보딩 미완료
                intent = new Intent(
                        SplashActivity.this,
                        OnboardingActivity.class
                );
            }
        }

        startActivity(intent);
        finish();

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }

    /**
     * ✅ Render 서버 미리 깨우기
     * 로그인 전에 실행되므로 첫 로그인 체감이 빨라짐
     */
    private void warmUpServer() {

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            HttpURLConnection conn = null;

            try {

                URL url = new URL(
                        FridgeApi.BASE_URL +
                                "api/recipes/light"
                );

                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");

                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                conn.getResponseCode();

                Log.d(TAG, "✅ warm up success");

            } catch (Exception e) {

                Log.e(TAG, "❌ warm up fail", e);

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}