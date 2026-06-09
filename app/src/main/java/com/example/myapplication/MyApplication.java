package com.namgyun.tamakitchen;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static volatile Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ 항상 Application Context로 고정
        appContext = getApplicationContext();

        // ✅ 폰트 프리로드: 첫 화면에서 텍스트가 "늦게 뜨는" 현상 완화/해결
        preloadFonts();

        // ✅ (선택) 첫 프레임 이후 한 번 더 워밍업
        // - 특정 기기에서 첫 프레임 전에 리소스 로딩이 덜 되는 경우가 있어 안전장치로 둠
        warmupAfterFirstFrame();
    }

    public static Context getContext() {
        // ✅ 혹시라도 프로세스/클래스 로딩 타이밍 꼬였을 때 방어
        if (appContext == null) {
            throw new IllegalStateException(
                    "MyApplication.getContext() called before Application.onCreate(). " +
                            "AndroidManifest.xml에 android:name=\".MyApplication\" 설정이 있는지 확인하세요."
            );
        }
        return appContext;
    }

    private void preloadFonts() {
        try {
            // ⚠️ 너 프로젝트에서 실제 쓰는 폰트 리소스 ID로 맞춰야 함
            // 예: res/font/gmarket_sans_bold.ttf, res/font/gmarket_sans_medium.ttf
            ResourcesCompat.getFont(this, R.font.gmarket_sans_bold);
            ResourcesCompat.getFont(this, R.font.gmarket_sans_medium);

            Log.d(TAG, "Fonts preloaded (gmarket_sans_bold, gmarket_sans_medium)");
        } catch (Throwable t) {
            // 폰트가 없거나(xml 다운로드 폰트) 로딩 실패해도 앱이 죽으면 안 됨
            Log.w(TAG, "Font preload failed. Check res/font files (ttf recommended).", t);
        }
    }

    private void warmupAfterFirstFrame() {
        try {
            Handler h = new Handler(Looper.getMainLooper());
            h.post(() -> {
                try {
                    // 한 번 더 호출해서 캐시/Typeface 준비를 확실히
                    ResourcesCompat.getFont(this, R.font.gmarket_sans_bold);
                    ResourcesCompat.getFont(this, R.font.gmarket_sans_medium);

                    Log.d(TAG, "Fonts warmed up after first frame");
                } catch (Throwable t) {
                    Log.w(TAG, "Font warmup failed", t);
                }
            });
        } catch (Throwable ignored) {}
    }
}