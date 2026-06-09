package com.namgyun.tamakitchen.ui.menu;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.auth.LoginActivity;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private static final String PREFS_LOGIN = "login_prefs";
    private static final String KEY_USER_ID_LOGIN = "userId";

    private static final String PREFS_USER_INFO = "user_info";
    private static final String KEY_NICKNAME_NEW = "nickname";
    private static final String KEY_NICKNAME_OLD = "user_nickname";

    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";
    private static final String KEY_LAST_NICKNAME_CHANGED_AT = "last_nickname_changed_at";

    private static final long NICKNAME_CHANGE_INTERVAL_MS = 30L * 24L * 60L * 60L * 1000L;

    private ShapeableImageView ivProfile;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvCurrentNickname;
    private TextView tvNicknameChangeInfo;
    private MaterialButton btnChangeProfileImage;
    private MaterialButton btnResetProfileImage;
    private MaterialButton btnChangeNickname;
    private MaterialButton btnLogout;
    private MaterialButton btnWithdraw;

    private SharedPreferences userInfoPrefs;
    private SharedPreferences loginPrefs;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                saveProfileImage(uri);
                applyProfileImage();
                AppToast.show(this, "프로필 이미지를 변경했어요.");
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userInfoPrefs = getSharedPreferences(PREFS_USER_INFO, MODE_PRIVATE);
        loginPrefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);

        bindViews();
        bindEvents();
        bindUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindUserInfo();
    }

    private void bindViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvCurrentNickname = findViewById(R.id.tvCurrentNickname);
        tvNicknameChangeInfo = findViewById(R.id.tvNicknameChangeInfo);

        btnChangeProfileImage = findViewById(R.id.btnChangeProfileImage);
        btnResetProfileImage = findViewById(R.id.btnResetProfileImage);
        btnChangeNickname = findViewById(R.id.btnChangeNickname);
        btnLogout = findViewById(R.id.btnLogout);
        btnWithdraw = findViewById(R.id.btnWithdraw);
    }

    private void bindEvents() {

        btnChangeProfileImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnResetProfileImage.setOnClickListener(v -> {
            userInfoPrefs.edit().remove(KEY_PROFILE_IMAGE_URI).apply();
            applyProfileImage();
            AppToast.show(this, "기본 프로필 이미지로 변경했어요.");
        });

        btnChangeNickname.setOnClickListener(v -> {
            if (!canChangeNicknameNow()) {
                AppToast.show(this, getNicknameBlockedMessage());
                return;
            }
            showChangeNicknameDialog();
        });

        btnLogout.setOnClickListener(v -> doLogoutAllAndGoLogin());
        btnWithdraw.setOnClickListener(v -> showWithdrawConfirmDialog());
    }

    private void bindUserInfo() {
        String nickname = resolveNickname();
        boolean isGuest = AuthPrefs.isGuest(this);

        tvName.setText(nickname);
        tvCurrentNickname.setText(nickname);

        if (isGuest) {
            tvEmail.setVisibility(View.GONE);
        } else {
            String email = resolveEmail();
            if (!TextUtils.isEmpty(email)) {
                tvEmail.setText(email);
                tvEmail.setVisibility(View.VISIBLE);
            } else {
                tvEmail.setText("이메일 정보 없음");
                tvEmail.setVisibility(View.VISIBLE);
            }
        }

        updateNicknameChangeInfo();
        applyProfileImage();
    }

    private String resolveNickname() {
        String nick = userInfoPrefs.getString(KEY_NICKNAME_NEW, null);
        if (!TextUtils.isEmpty(nick)) return nick.trim();

        String old = userInfoPrefs.getString(KEY_NICKNAME_OLD, null);
        if (!TextUtils.isEmpty(old)) {
            userInfoPrefs.edit().putString(KEY_NICKNAME_NEW, old.trim()).apply();
            return old.trim();
        }

        String fromIntent = getIntent().getStringExtra("user_nickname");
        if (!TextUtils.isEmpty(fromIntent)) {
            userInfoPrefs.edit().putString(KEY_NICKNAME_NEW, fromIntent.trim()).apply();
            return fromIntent.trim();
        }

        return "사용자";
    }

    private String resolveEmail() {
        String[] candidateKeys = new String[] {
                "user_email",
                "email",
                "userEmail",
                "kakao_email",
                "naver_email",
                "google_email"
        };

        String fromIntent = getIntent().getStringExtra("user_email");
        if (!TextUtils.isEmpty(fromIntent)) {
            String value = fromIntent.trim();
            userInfoPrefs.edit().putString("user_email", value).apply();
            return value;
        }

        for (String key : candidateKeys) {
            String value = userInfoPrefs.getString(key, null);
            if (!TextUtils.isEmpty(value)) {
                value = value.trim();
                userInfoPrefs.edit().putString("user_email", value).apply();
                return value;
            }
        }

        for (String key : candidateKeys) {
            String value = loginPrefs.getString(key, null);
            if (!TextUtils.isEmpty(value)) {
                value = value.trim();
                userInfoPrefs.edit().putString("user_email", value).apply();
                return value;
            }
        }

        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleAccount != null && !TextUtils.isEmpty(googleAccount.getEmail())) {
            String value = googleAccount.getEmail().trim();
            userInfoPrefs.edit().putString("user_email", value).apply();
            return value;
        }

        return "";
    }

    private void applyProfileImage() {
        String uriString = userInfoPrefs.getString(KEY_PROFILE_IMAGE_URI, null);

        if (TextUtils.isEmpty(uriString)) {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
            return;
        }

        try {
            ivProfile.setImageURI(Uri.parse(uriString));
        } catch (Exception e) {
            Log.w(TAG, "apply profile image failed", e);
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void saveProfileImage(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception e) {
            Log.w(TAG, "take persistable uri permission failed", e);
        }

        userInfoPrefs.edit()
                .putString(KEY_PROFILE_IMAGE_URI, uri.toString())
                .apply();
    }

    private boolean canChangeNicknameNow() {
        long lastChangedAt = userInfoPrefs.getLong(KEY_LAST_NICKNAME_CHANGED_AT, 0L);
        if (lastChangedAt <= 0L) return true;

        long now = System.currentTimeMillis();
        return now - lastChangedAt >= NICKNAME_CHANGE_INTERVAL_MS;
    }

    private long getNextNicknameChangeAt() {
        long lastChangedAt = userInfoPrefs.getLong(KEY_LAST_NICKNAME_CHANGED_AT, 0L);
        if (lastChangedAt <= 0L) return 0L;
        return lastChangedAt + NICKNAME_CHANGE_INTERVAL_MS;
    }

    private void updateNicknameChangeInfo() {
        if (canChangeNicknameNow()) {
            tvNicknameChangeInfo.setText("지금 닉네임 변경 가능");
            btnChangeNickname.setEnabled(true);
            btnChangeNickname.setText("닉네임 변경");
            btnChangeNickname.setPaintFlags(btnChangeNickname.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            return;
        }

        long nextAt = getNextNicknameChangeAt();
        String untilDate = formatDate(nextAt);

        tvNicknameChangeInfo.setText("다음 닉네임 변경 가능일: " + untilDate);
        btnChangeNickname.setEnabled(false);
        btnChangeNickname.setText("닉네임 변경 (" + untilDate + "까지)");
        btnChangeNickname.setPaintFlags(btnChangeNickname.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }

    private String getNicknameBlockedMessage() {
        String untilDate = formatDate(getNextNicknameChangeAt());
        return "닉네임은 30일에 1번만 변경 가능해요. (" + untilDate + "까지)";
    }

    private void showChangeNicknameDialog() {
        final EditText input = new EditText(this);
        input.setText(resolveNickname());
        input.setSelection(input.getText().length());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        input.setHint("닉네임 입력 (2~12자)");

        new AlertDialog.Builder(this)
                .setTitle("닉네임 변경")
                .setMessage("닉네임은 30일에 1번만 변경할 수 있어요.")
                .setView(input)
                .setNegativeButton("취소", null)
                .setPositiveButton("변경", (dialog, which) -> {
                    String newNickname = input.getText() == null ? "" : input.getText().toString().trim();

                    if (newNickname.length() < 2 || newNickname.length() > 12) {
                        AppToast.show(this, "닉네임은 2자 이상 12자 이하로 입력해주세요.");
                        return;
                    }

                    String currentNickname = resolveNickname();
                    if (currentNickname.equals(newNickname)) {
                        AppToast.show(this, "현재 닉네임과 같아요.");
                        return;
                    }

                    userInfoPrefs.edit()
                            .putString(KEY_NICKNAME_NEW, newNickname)
                            .putLong(KEY_LAST_NICKNAME_CHANGED_AT, System.currentTimeMillis())
                            .apply();

                    tvName.setText(newNickname);
                    tvCurrentNickname.setText(newNickname);
                    updateNicknameChangeInfo();

                    AppToast.show(this, "닉네임이 변경됐어요.");
                })
                .show();
    }

    private void showWithdrawConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("탈퇴하기")
                .setMessage("탈퇴하면 이 기기에 저장된 로그인 정보와 프로필 정보가 삭제되고 로그인 화면으로 이동해요.\n\n정말 탈퇴할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("탈퇴", (dialog, which) -> doWithdraw())
                .show();
    }

    private void doWithdraw() {
        clearAllLocalUserData();
        logoutProviders();

        AppToast.show(this, "로컬 데이터가 삭제됐어요.");
        goToLogin();
    }

    private void doLogoutAllAndGoLogin() {
        clearAllLocalUserData();
        logoutProviders();

        AppToast.show(this, "로그아웃 완료");
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearAllLocalUserData() {
        try {
            loginPrefs.edit().clear().apply();
            userInfoPrefs.edit().clear().apply();
        } catch (Exception e) {
            Log.w(TAG, "clear local data failed", e);
        }
    }

    private void logoutProviders() {
        try {
            NaverIdLoginSDK.INSTANCE.logout();
        } catch (Exception e) {
            Log.w(TAG, "naver logout failed", e);
        }

        try {
            UserApiClient.getInstance().logout(error -> {
                if (error != null) Log.w(TAG, "kakao logout failed", error);
                return null;
            });
        } catch (Exception e) {
            Log.w(TAG, "kakao logout exception", e);
        }

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
            client.signOut().addOnCompleteListener(task -> Log.d(TAG, "google signOut complete"));
        } catch (Exception e) {
            Log.w(TAG, "google signOut failed", e);
        }
    }

    private String formatDate(long timeMs) {
        if (timeMs <= 0L) return "";
        return new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new java.util.Date(timeMs));
    }
}