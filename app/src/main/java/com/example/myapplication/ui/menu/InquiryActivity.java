package com.namgyun.tamakitchen.ui.menu;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.network.SupportApiService;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryActivity extends AppCompatActivity {

    public static final String EXTRA_NICKNAME = "extra_nickname";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_EMAIL = "extra_email";

    private static final String TAG = "InquiryActivity";

    private ImageView btnClose;
    private TextView tvTitle;
    private TextInputEditText etSubject;
    private TextInputEditText etContent;
    private MaterialButton btnSend;

    private SupportApiService supportApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry);

        supportApi = RetrofitClient.getSupportApi();

        bindViews();
        bindActions();
    }

    private void bindViews() {
        btnClose = findViewById(R.id.btn_close);
        tvTitle = findViewById(R.id.tv_inquiry_title);
        etSubject = findViewById(R.id.et_inquiry_subject);
        etContent = findViewById(R.id.et_inquiry_content);
        btnSend = findViewById(R.id.btn_send_inquiry);

        if (tvTitle != null) {
            tvTitle.setText("문의사항");
        }
    }

    private void bindActions() {
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendInquiryToServer());
        }
    }

    private void sendInquiryToServer() {
        String subjectInput = safeText(etSubject);
        String contentInput = safeText(etContent);

        if (TextUtils.isEmpty(contentInput)) {
            toast("문의 내용을 입력해주세요.");

            if (etContent != null) {
                etContent.requestFocus();
            }

            return;
        }

        if (TextUtils.isEmpty(subjectInput)) {
            subjectInput = "문의합니다";
        }

        Long userId = null;
        String nickname = null;
        String email = null;

        if (getIntent() != null) {
            long uid = getIntent().getLongExtra(EXTRA_USER_ID, -1L);

            if (uid != -1L) {
                userId = uid;
            }

            nickname = getIntent().getStringExtra(EXTRA_NICKNAME);
            email = getIntent().getStringExtra(EXTRA_EMAIL);
        }

        nickname = normalizeNickname(nickname);
        email = normalizeEmail(email);

        InquiryRequest req = new InquiryRequest();
        req.setSubject(subjectInput);
        req.setContent(contentInput);

        req.setUserId(userId);
        req.setNickname(nickname);
        req.setEmail(email);

        req.setAppVersion(getAppVersionSafe());
        req.setDeviceManufacturer(Build.MANUFACTURER);
        req.setDeviceModel(Build.MODEL);
        req.setOsVersion("Android " + Build.VERSION.RELEASE);
        req.setSdkInt(Build.VERSION.SDK_INT);

        setSending(true);

        Call<Void> call = supportApi.sendInquiry(req);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setSending(false);

                if (response.isSuccessful()) {
                    toast("문의가 전송되었습니다. 감사합니다!");
                    finish();
                    return;
                }

                Log.e(TAG, "send inquiry failed. code=" + response.code());
                toast("문의 전송에 실패했어요. 잠시 후 다시 시도해주세요.");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setSending(false);

                Log.e(TAG, "send inquiry network failure", t);
                toast(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    private void setSending(boolean sending) {
        if (btnSend != null) {
            btnSend.setEnabled(!sending);
            btnSend.setText(sending ? "전송 중..." : "보내기");
        }

        if (btnClose != null) {
            btnClose.setEnabled(!sending);
        }

        if (etSubject != null) {
            etSubject.setEnabled(!sending);
        }

        if (etContent != null) {
            etContent.setEnabled(!sending);
        }
    }

    private String safeText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }

        return editText.getText().toString().trim();
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) return null;

        String t = nickname.trim();

        if (t.isEmpty()) return null;

        if ("닉네임 정보 없음".equals(t)) return null;
        if ("사용자".equals(t)) return null;
        if ("닉네임".equals(t)) return null;

        return t;
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;

        String t = email.trim();

        if (t.isEmpty()) return null;
        if (!t.contains("@")) return null;
        if (!t.contains(".")) return null;

        return t;
    }

    private String getAppVersionSafe() {
        try {
            PackageInfo info;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.PackageInfoFlags.of(0)
                );
            } else {
                info = getPackageManager().getPackageInfo(getPackageName(), 0);
            }

            long versionCode;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }

            return info.versionName + " (" + versionCode + ")";

        } catch (Exception e) {
            Log.w(TAG, "get app version failed", e);
            return "unknown";
        }
    }

    private void toast(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        AppToast.show(this, msg);
    }
}