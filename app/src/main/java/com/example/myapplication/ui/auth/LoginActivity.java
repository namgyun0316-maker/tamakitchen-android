package com.namgyun.tamakitchen.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingActivity;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgePrefs;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    public static final String EXTRA_INVITE_CODE = "invite_code";

    private ShapeableImageView btnNaverLogin;
    private ShapeableImageView btnKakaoLogin;
    private ShapeableImageView btnGoogleLogin;
    private MaterialButton btnGuest;

    private View layoutLoginLoading;
    private TextView tvLoginLoading;

    private static final String PREFS_LOGIN = "login_prefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_GUEST = "isGuest";

    private static final String PREFS_USER_INFO = "user_info";
    private static final String KEY_USER_ID_USER_INFO = "user_id";
    private static final String KEY_USER_EMAIL_USER_INFO = "user_email";
    private static final String KEY_USER_NICKNAME_USER_INFO = "nickname";

    private static final String KEY_GUEST_EMAIL = "guest_email";
    private static final String USERS_ENDPOINT = "api/users";
    private static final int RC_GOOGLE_SIGN_IN = 10001;

    private GoogleSignInClient googleSignInClient;
    private FridgeApiService fridgeApiService;

    private volatile boolean userStartedAuthFlow = false;
    private volatile boolean guestModeSelected = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private View currentToastView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fridgeApiService = FridgeApi.getClient().create(FridgeApiService.class);

        KakaoSdk.init(this, getString(R.string.kakao_app_key));
        printKakaoKeyHash();

        NaverIdLoginSDK.INSTANCE.initialize(
                this,
                getString(R.string.naver_client_id),
                getString(R.string.naver_client_secret),
                getString(R.string.app_name)
        );

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnNaverLogin = findViewById(R.id.btn_naver_login);
        btnKakaoLogin = findViewById(R.id.btn_kakao_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        btnGuest = findViewById(R.id.btnGuest);

        layoutLoginLoading = findViewById(R.id.layoutLoginLoading);
        tvLoginLoading = findViewById(R.id.tvLoginLoading);

        if (btnGuest != null) {
            btnGuest.setEnabled(true);
            btnGuest.setClickable(true);
            btnGuest.setOnClickListener(v -> {
                userStartedAuthFlow = true;
                guestModeSelected = true;
                showLoginLoading("게스트 계정 생성 중...");
                startAsGuest();
            });
        }

        if (btnNaverLogin != null) {
            btnNaverLogin.setOnClickListener(v -> {
                if (guestModeSelected) return;

                userStartedAuthFlow = true;
                showLoginLoading("네이버 로그인 중...");

                NaverIdLoginSDK.INSTANCE.authenticate(this, new OAuthLoginCallback() {
                    @Override
                    public void onSuccess() {
                        if (guestModeSelected) return;
                        String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();
                        getUserProfile(accessToken);
                    }

                    @Override
                    public void onFailure(int errorCode, String message) {
                        hideLoginLoading();
                        Log.e(TAG, "naver login failure code=" + errorCode + " message=" + message);
                        showWhiteToast("네이버 로그인에 실패했어요. 잠시 후 다시 시도해주세요.");
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        onFailure(errorCode, message);
                    }
                });
            });
        }

        if (btnKakaoLogin != null) {
            btnKakaoLogin.setOnClickListener(v -> {
                if (guestModeSelected) return;

                userStartedAuthFlow = true;
                showLoginLoading("카카오 로그인 중...");

                if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
                    UserApiClient.getInstance().loginWithKakaoTalk(this, (oAuthToken, error) -> {
                        if (guestModeSelected) return null;

                        if (error != null) {
                            loginWithKakaoAccount();
                        } else {
                            getKakaoUserInfoWithEmailScopeIfNeeded();
                        }
                        return null;
                    });
                } else {
                    loginWithKakaoAccount();
                }
            });
        }

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                if (guestModeSelected) return;

                userStartedAuthFlow = true;
                showLoginLoading("구글 로그인 중...");

                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
            });
        }

        warmUpServer();
    }

    @Override
    protected void onDestroy() {
        hideLoginLoading();
        removeWhiteToast();
        super.onDestroy();
    }

    private void showLoginLoading(String message) {
        runOnUiThread(() -> {
            if (layoutLoginLoading != null) {
                layoutLoginLoading.setVisibility(View.VISIBLE);
            }

            if (tvLoginLoading != null) {
                tvLoginLoading.setText(message == null ? "로그인 중..." : message);
            }

            setLoginButtonsEnabled(false);
        });
    }

    private void hideLoginLoading() {
        runOnUiThread(() -> {
            if (layoutLoginLoading != null) {
                layoutLoginLoading.setVisibility(View.GONE);
            }

            setLoginButtonsEnabled(true);
        });
    }

    private void setLoginButtonsEnabled(boolean enabled) {
        if (btnNaverLogin != null) {
            btnNaverLogin.setEnabled(enabled);
            btnNaverLogin.setClickable(enabled);
        }

        if (btnKakaoLogin != null) {
            btnKakaoLogin.setEnabled(enabled);
            btnKakaoLogin.setClickable(enabled);
        }

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setEnabled(enabled);
            btnGoogleLogin.setClickable(enabled);
        }

        if (btnGuest != null) {
            btnGuest.setEnabled(enabled);
            btnGuest.setClickable(enabled);
        }
    }

    private void warmUpServer() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(buildUrl(FridgeApi.BASE_URL, "api/recipes/light"));

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getResponseCode();

                Log.d(TAG, "warm up success");

            } catch (Exception e) {
                Log.e(TAG, "warm up fail", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void startAsGuest() {
        AuthPrefs.setGuestMode(this, true);

        String guestEmail = getOrCreateGuestEmail();
        String guestName = "게스트";

        saveEmailToAllPrefs(guestEmail);
        saveNicknameToAllPrefs(guestName);

        showWhiteToast("비회원으로 시작합니다.");
        Log.d(TAG, "startAsGuest() guestEmail=" + guestEmail);

        sendGuestInfoToServer(guestEmail, guestName);
    }

    private String getOrCreateGuestEmail() {
        SharedPreferences loginPrefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);
        String saved = loginPrefs.getString(KEY_GUEST_EMAIL, null);

        if (saved != null && !saved.trim().isEmpty()) {
            return saved;
        }

        String email = "guest_" + UUID.randomUUID().toString().replace("-", "") + "@guest.local";
        loginPrefs.edit().putString(KEY_GUEST_EMAIL, email).apply();

        return email;
    }

    private void showWhiteToast(String msg) {
        if (isFinishing() || isDestroyed()) return;
        if (msg == null || msg.trim().isEmpty()) return;

        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> showWhiteToast(msg));
            return;
        }

        final ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        removeWhiteToast();

        View v = getLayoutInflater().inflate(R.layout.toast_white, root, false);
        TextView tv = v.findViewById(R.id.tvToast);

        if (tv != null) {
            tv.setText(msg.trim());
        }

        android.widget.FrameLayout.LayoutParams flp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        flp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        flp.bottomMargin = (int) (110 * getResources().getDisplayMetrics().density);
        v.setLayoutParams(flp);

        v.setAlpha(0f);
        root.addView(v);
        currentToastView = v;

        v.animate().alpha(1f).setDuration(140).start();

        v.postDelayed(() -> {
            if (currentToastView == v) {
                v.animate().alpha(0f).setDuration(140).withEndAction(() -> {
                    try {
                        root.removeView(v);
                    } catch (Throwable ignored) {
                    }

                    if (currentToastView == v) {
                        currentToastView = null;
                    }
                }).start();
            }
        }, 1200);
    }

    private void removeWhiteToast() {
        try {
            if (currentToastView != null) {
                ViewGroup root = findViewById(android.R.id.content);

                if (root != null) {
                    root.removeView(currentToastView);
                }
            }
        } catch (Throwable ignored) {
        }

        currentToastView = null;
    }

    private void routeAfterAuth() {
        hideLoginLoading();

        if (!userStartedAuthFlow) {
            Log.w(TAG, "routeAfterAuth blocked: user didn't start auth flow.");
            return;
        }

        Intent intent;

        if (OnboardingPrefs.isOnboardingDone(this)) {
            intent = new Intent(this, com.namgyun.tamakitchen.MainActivity.class);
        } else {
            intent = new Intent(this, OnboardingActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private void tryJoinSharedFridgeIfNeeded(long userId, @Nullable String nicknameForJoin) {
        Intent intent = getIntent();

        if (intent == null) {
            routeAfterAuth();
            return;
        }

        String inviteCode = intent.getStringExtra(EXTRA_INVITE_CODE);

        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            Log.d(TAG, "inviteCode 없음 → join 스킵");
            routeAfterAuth();
            return;
        }

        final String finalInvite = inviteCode.trim().toUpperCase();
        final String finalNick = (nicknameForJoin == null || nicknameForJoin.trim().isEmpty())
                ? "사용자"
                : nicknameForJoin.trim();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            HttpURLConnection conn = null;

            try {
                String joinUrl = buildUrl(FridgeApi.BASE_URL, "api/shared-fridge/join");

                URL url = new URL(joinUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("userId", userId);
                body.put("inviteCode", finalInvite);
                body.put("nickname", finalNick);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                StringBuilder response = readHttpResponse(conn, code);

                if (code >= 200 && code < 300) {
                    JSONObject res = new JSONObject(response.toString());

                    long fridgeId = res.optLong("fridgeId", 0L);
                    String fridgeName = res.optString("fridgeName", "공동 냉장고");
                    String serverInviteCode = res.optString("inviteCode", finalInvite);
                    long expiresAt = res.optLong("inviteExpiresAt", 0L);

                    SharedFridgePrefs.saveFridgeId(this, fridgeId);
                    SharedFridgePrefs.saveFridgeName(this, fridgeName);
                    SharedFridgePrefs.saveInvite(this, serverInviteCode, expiresAt);
                    OnboardingPrefs.saveFridgeType(this, "SHARED");

                } else {
                    Log.e(TAG, "join failed http=" + code + " body=" + response);
                }

            } catch (Exception e) {
                Log.e(TAG, "join error", e);

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }

                runOnUiThread(this::routeAfterAuth);
            }
        });
    }

    private void printKakaoKeyHash() {
        try {
            PackageInfo packageInfo;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );

                if (packageInfo.signingInfo != null) {
                    Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();

                    for (Signature signature : signatures) {
                        printSingleKakaoKeyHash(signature);
                    }
                }

            } else {
                packageInfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNATURES
                );

                Signature[] signatures = packageInfo.signatures;

                if (signatures != null) {
                    for (Signature signature : signatures) {
                        printSingleKakaoKeyHash(signature);
                    }
                }
            }

        } catch (Exception e) {
            Log.e("KAKAO_KEYHASH", "키해시 생성 실패", e);
        }
    }

    private void printSingleKakaoKeyHash(Signature signature) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(signature.toByteArray());

        String keyHash = Base64.encodeToString(
                md.digest(),
                Base64.NO_WRAP
        );

        Log.d("KAKAO_KEYHASH", keyHash);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        if (guestModeSelected) return;

        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account == null) {
                hideLoginLoading();
                showWhiteToast("구글 로그인에 실패했어요. 잠시 후 다시 시도해주세요.");
                return;
            }

            String email = account.getEmail() != null ? account.getEmail() : "";
            String name = account.getDisplayName() != null ? account.getDisplayName() : "이름 없음";

            saveEmailToAllPrefs(email);
            saveNicknameToAllPrefs(name);
            sendUserInfoToServer(email, name);

        } catch (ApiException e) {
            hideLoginLoading();
            Log.e(TAG, "google login failure code=" + e.getStatusCode(), e);
            showWhiteToast("구글 로그인에 실패했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private void loginWithKakaoAccount() {
        if (guestModeSelected) return;

        UserApiClient.getInstance().loginWithKakaoAccount(this, (oAuthToken, error) -> {
            if (guestModeSelected) return null;

            if (error != null) {
                hideLoginLoading();
                Log.e(TAG, "kakao login failure", error);
                showWhiteToast("카카오 로그인에 실패했어요. 잠시 후 다시 시도해주세요.");
            } else {
                getKakaoUserInfoWithEmailScopeIfNeeded();
            }

            return null;
        });
    }

    private void getKakaoUserInfoWithEmailScopeIfNeeded() {
        if (guestModeSelected) return;

        UserApiClient.getInstance().me((user, error) -> {
            if (guestModeSelected) return null;

            if (error != null || user == null) {
                hideLoginLoading();
                Log.e(TAG, "kakao user info failure", error);
                showWhiteToast("카카오 사용자 정보를 불러오지 못했어요.");
                return null;
            }

            String nickname = (user.getKakaoAccount() != null
                    && user.getKakaoAccount().getProfile() != null
                    && user.getKakaoAccount().getProfile().getNickname() != null)
                    ? user.getKakaoAccount().getProfile().getNickname()
                    : "별명 없음";

            String email = (user.getKakaoAccount() != null)
                    ? user.getKakaoAccount().getEmail()
                    : null;

            if (email != null && !email.trim().isEmpty()) {
                saveEmailToAllPrefs(email);
                saveNicknameToAllPrefs(nickname);
                sendUserInfoToServer(email, nickname);
                return null;
            }

            List<String> scopes = Arrays.asList("account_email");
            String state = "email_consent";

            UserApiClient.getInstance().loginWithNewScopes(
                    LoginActivity.this,
                    scopes,
                    state,
                    (OAuthToken token, Throwable scopeError) -> {
                        if (guestModeSelected) return Unit.INSTANCE;

                        if (scopeError != null) {
                            hideLoginLoading();
                            Log.e(TAG, "kakao email scope failure", scopeError);
                            showWhiteToast("이메일 동의가 필요해요.");
                        } else {
                            getKakaoUserInfoWithEmailScopeIfNeeded();
                        }

                        return Unit.INSTANCE;
                    }
            );

            return null;
        });
    }

    private void getUserProfile(String accessToken) {
        if (guestModeSelected) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            StringBuilder result = new StringBuilder();
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL("https://openapi.naver.com/v1/nid/me");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = urlConnection.getResponseCode();

                InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK)
                        ? urlConnection.getInputStream()
                        : urlConnection.getErrorStream();

                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    reader.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "naver user profile failure", e);
                runOnUiThread(() -> {
                    hideLoginLoading();
                    showWhiteToast(NetworkErrorUtil.getUserMessage(e));
                });
                return;

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            runOnUiThread(() -> parseUserProfile(result.toString()));
        });
    }

    private void parseUserProfile(String jsonString) {
        if (guestModeSelected) return;

        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            if ("00".equals(jsonObject.optString("resultcode", ""))) {
                JSONObject response = jsonObject.getJSONObject("response");

                String email = response.optString("email", "");
                String nickname = response.optString("nickname", "별명 없음");

                saveEmailToAllPrefs(email);
                saveNicknameToAllPrefs(nickname);
                sendUserInfoToServer(email, nickname);

            } else {
                hideLoginLoading();
                showWhiteToast("네이버 로그인 응답을 확인할 수 없어요.");
            }

        } catch (JSONException e) {
            hideLoginLoading();
            Log.e(TAG, "naver parse failure", e);
            showWhiteToast("네이버 로그인 정보를 확인할 수 없어요.");
        }
    }

    private void forceSetLoggedInState(long userId) {
        SharedPreferences loginPrefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);

        loginPrefs.edit()
                .putBoolean(KEY_IS_GUEST, false)
                .putLong(KEY_USER_ID, userId)
                .apply();

        SharedPreferences userInfo = getSharedPreferences(PREFS_USER_INFO, MODE_PRIVATE);

        userInfo.edit()
                .putLong(KEY_USER_ID_USER_INFO, userId)
                .apply();

        AuthPrefs.setGuestMode(this, false);
    }

    private void forceSetGuestState(long userId) {
        SharedPreferences loginPrefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);

        loginPrefs.edit()
                .putBoolean(KEY_IS_GUEST, true)
                .putLong(KEY_USER_ID, userId)
                .apply();

        SharedPreferences userInfo = getSharedPreferences(PREFS_USER_INFO, MODE_PRIVATE);

        userInfo.edit()
                .putLong(KEY_USER_ID_USER_INFO, userId)
                .apply();

        AuthPrefs.setGuestMode(this, true);
    }

    private long parseUserIdFromServerResponse(String responseJson) {
        try {
            if (responseJson == null || responseJson.isEmpty()) return -1L;

            JSONObject obj = new JSONObject(responseJson);
            return obj.optLong("id", -1L);

        } catch (Exception e) {
            return -1L;
        }
    }

    private long getSavedUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    private String buildUrl(String baseUrl, String endpoint) {
        if (baseUrl == null) baseUrl = "";
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";

        while (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }

        return baseUrl + endpoint;
    }

    private StringBuilder readHttpResponse(HttpURLConnection conn, int responseCode) throws Exception {
        StringBuilder response = new StringBuilder();

        InputStream stream = responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            return response;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, "utf-8")
        )) {
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }

        return response;
    }

    private void saveEmailToAllPrefs(@Nullable String email) {
        String safeEmail = email == null ? "" : email.trim();

        SharedPreferences loginPrefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);

        loginPrefs.edit()
                .putString("email", safeEmail)
                .apply();

        SharedPreferences userInfoPrefs = getSharedPreferences(PREFS_USER_INFO, MODE_PRIVATE);

        userInfoPrefs.edit()
                .putString(KEY_USER_EMAIL_USER_INFO, safeEmail)
                .putString("email", safeEmail)
                .apply();

        AuthPrefs.saveEmail(this, safeEmail);
    }

    private void saveNicknameToAllPrefs(@Nullable String nickname) {
        String safeNickname = nickname == null ? "" : nickname.trim();

        SharedPreferences userInfoPrefs = getSharedPreferences(PREFS_USER_INFO, MODE_PRIVATE);

        userInfoPrefs.edit()
                .putString("login_display_name", safeNickname)
                .apply();

        String currentAppNickname = OnboardingPrefs.getNickname(this);

        if (currentAppNickname == null || currentAppNickname.trim().isEmpty()) {
            OnboardingPrefs.saveNickname(this, safeNickname);

            userInfoPrefs.edit()
                    .putString(KEY_USER_NICKNAME_USER_INFO, safeNickname)
                    .putString("user_nickname", safeNickname)
                    .apply();
        }
    }

    private void sendUserInfoToServer(@NonNull String email, @NonNull String nickname) {
        if (guestModeSelected) return;

        final String safeEmail = email == null ? "" : email.trim();
        final String safeNickname = nickname == null ? "" : nickname.trim();

        saveEmailToAllPrefs(safeEmail);
        saveNicknameToAllPrefs(safeNickname);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            HttpURLConnection conn = null;

            try {
                String serverUrl = buildUrl(FridgeApi.BASE_URL, USERS_ENDPOINT);

                URL url = new URL(serverUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", safeEmail);
                jsonParam.put("name", safeNickname);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                StringBuilder response = readHttpResponse(conn, responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    long id = parseUserIdFromServerResponse(response.toString());

                    if (id > 0) {
                        forceSetLoggedInState(id);
                    }

                    long userId = getSavedUserId();

                    runOnUiThread(() -> {
                        if (guestModeSelected) return;

                        AuthPrefs.saveLoginUser(
                                LoginActivity.this,
                                userId,
                                safeNickname,
                                safeEmail
                        );

                        GuestDataMigrator.migrateFridgeIfNeeded(
                                LoginActivity.this,
                                userId,
                                fridgeApiService,
                                () -> tryJoinSharedFridgeIfNeeded(userId, safeNickname)
                        );
                    });

                } else {
                    Log.e(TAG, "sendUserInfoToServer failed responseCode=" + responseCode + " response=" + response);
                    runOnUiThread(() -> {
                        hideLoginLoading();
                        showWhiteToast("로그인 정보를 저장하지 못했어요. 잠시 후 다시 시도해주세요.");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "sendUserInfoToServer network error", e);
                runOnUiThread(() -> {
                    hideLoginLoading();
                    showWhiteToast(NetworkErrorUtil.getUserMessage(e));
                });

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void sendGuestInfoToServer(@NonNull String email, @NonNull String nickname) {
        final String safeEmail = email == null ? "" : email.trim();
        final String safeNickname = nickname == null ? "" : nickname.trim();

        saveEmailToAllPrefs(safeEmail);
        saveNicknameToAllPrefs(safeNickname);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            HttpURLConnection conn = null;

            try {
                String serverUrl = buildUrl(FridgeApi.BASE_URL, USERS_ENDPOINT);

                URL url = new URL(serverUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", safeEmail);
                jsonParam.put("name", safeNickname);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                StringBuilder response = readHttpResponse(conn, responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    long id = parseUserIdFromServerResponse(response.toString());

                    if (id > 0) {
                        forceSetGuestState(id);
                    }

                    long userId = getSavedUserId();

                    runOnUiThread(() -> {
                        AuthPrefs.saveGuestUser(
                                LoginActivity.this,
                                userId,
                                safeNickname,
                                safeEmail
                        );

                        tryJoinSharedFridgeIfNeeded(userId, safeNickname);
                    });

                } else {
                    Log.e(TAG, "sendGuestInfoToServer failed responseCode=" + responseCode + " response=" + response);
                    runOnUiThread(() -> {
                        hideLoginLoading();
                        showWhiteToast("게스트 계정을 만들지 못했어요. 잠시 후 다시 시도해주세요.");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "sendGuestInfoToServer network error", e);
                runOnUiThread(() -> {
                    hideLoginLoading();
                    showWhiteToast(NetworkErrorUtil.getUserMessage(e));
                });

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}