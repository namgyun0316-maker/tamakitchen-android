package com.namgyun.tamakitchen;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.namgyun.tamakitchen.pet.PetPrefs;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_GO_FRIDGE = "go_fridge";

    private NavController navController;
    private BottomNavigationView bottomNav;

    private int originalBottomNavHeight = 0;
    private int originalBottomNavPaddingBottom = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android 15(API 35)에서 상태바/네비게이션바와 화면이 겹치는 문제 방지
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.WHITE);

        setContentView(R.layout.activity_main);

        PetPrefs.ensureEggGranted(this);

        Intent intent = getIntent();
        String userEmail = intent.getStringExtra("user_email");
        String userNickname = intent.getStringExtra("user_nickname");

        if (userEmail != null && userNickname != null) {
            SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_email", userEmail);
            editor.putString("user_nickname", userNickname);
            editor.apply();
        }

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            return;
        }

        navController = navHostFragment.getNavController();

        bottomNav = findViewById(R.id.bottom_navigation);
        NavigationUI.setupWithNavController(bottomNav, navController);

        disableBottomNavTooltips(bottomNav);
        applySystemBarInsets();

        handleStartupNavigation(intent);
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.mainRoot);
        View navHost = findViewById(R.id.nav_host_fragment);

        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (navHost != null) {
                navHost.setPadding(
                        navHost.getPaddingLeft(),
                        systemBars.top,
                        navHost.getPaddingRight(),
                        0
                );
            }

            if (bottomNav != null) {
                bottomNav.setPadding(
                        bottomNav.getPaddingLeft(),
                        bottomNav.getPaddingTop(),
                        bottomNav.getPaddingRight(),
                        systemBars.bottom
                );
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        PetPrefs.ensureEggGranted(this);

        handleStartupNavigation(intent);
    }

    private void handleStartupNavigation(Intent intent) {
        if (intent == null || bottomNav == null) return;

        boolean goFridge = intent.getBooleanExtra(EXTRA_GO_FRIDGE, false);
        if (goFridge) {
            intent.removeExtra(EXTRA_GO_FRIDGE);
            bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_fridge));
            return;
        }

        String openTab = intent.getStringExtra("open_tab");
        if ("recipe".equals(openTab)) {
            intent.removeExtra("open_tab");
            bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_recipe));
        }
    }

    private void disableBottomNavTooltips(BottomNavigationView bottomNav) {
        if (bottomNav == null || bottomNav.getMenu() == null) return;

        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            int itemId = bottomNav.getMenu().getItem(i).getItemId();
            View itemView = bottomNav.findViewById(itemId);

            if (itemView != null) {
                TooltipCompat.setTooltipText(itemView, null);
                itemView.setOnLongClickListener(v -> true);
                itemView.setHapticFeedbackEnabled(false);
            }
        }
    }

    public String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        return prefs.getString("user_email", null);
    }

    public String getUserNickname() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        return prefs.getString("user_nickname", null);
    }

    public void updateUserInfo(String email, String nickname) {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", email);
        editor.putString("user_nickname", nickname);
        editor.apply();
    }

    public void clearUserInfo() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}