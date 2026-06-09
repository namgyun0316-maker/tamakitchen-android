package com.namgyun.tamakitchen.ui.onboarding;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.network.SharedFridgeResponse;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedFridgeJoinRunner {

    private static final String TAG = "SharedFridgeJoinRunner";

    private SharedFridgeJoinRunner() {
    }

    private static void showToastShort(Context context, String message) {
        showToast(context, message);
    }

    private static void showToastLong(Context context, String message) {
        showToast(context, message);
    }

    private static void showToast(Context context, String message) {
        if (context == null) return;
        if (message == null || message.trim().isEmpty()) return;

        try {
            AppToast.show(
                    context instanceof android.app.Activity
                            ? (android.app.Activity) context
                            : null,
                    message.trim()
            );
        } catch (Exception e) {
            Log.w(TAG, "show toast failed", e);
        }
    }

    public static void createSharedFridge(
            Context context,
            String fridgeName,
            Runnable onSuccess
    ) {
        if (context == null) return;

        long userId = AuthPrefs.getUserId(context);

        if (userId <= 0) {
            showToastShort(context, "로그인이 필요합니다.");
            return;
        }

        if (fridgeName == null) {
            fridgeName = "";
        }

        fridgeName = fridgeName.trim();

        if (fridgeName.length() < 2) {
            showToastShort(context, "이름은 2자 이상 입력해주세요.");
            return;
        }

        String nickTemp = OnboardingPrefs.getNickname(context);

        if (TextUtils.isEmpty(nickTemp)) {
            nickTemp = AuthPrefs.getNickname(context);
        }

        if (TextUtils.isEmpty(nickTemp)) {
            nickTemp = "사용자";
        }

        final String finalMyNick = nickTemp.trim();

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("fridgeName", fridgeName);
        body.put("nickname", finalMyNick);

        FridgeApiService api = FridgeApi.getClient().create(FridgeApiService.class);

        api.createSharedFridge(body).enqueue(new Callback<SharedFridgeResponse>() {
            @Override
            public void onResponse(
                    Call<SharedFridgeResponse> call,
                    Response<SharedFridgeResponse> response
            ) {
                Log.d(TAG, "CREATE response code=" + response.code());

                if (!response.isSuccessful() || response.body() == null) {
                    showToastShort(context, "공동 냉장고 생성에 실패했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                SharedFridgeResponse r = response.body();

                long fridgeId = r.getFridgeId();
                String name = r.getFridgeName();
                String code = r.getInviteCode();
                long expiresAt = r.getExpiresAt();

                String myNickname = r.getMyNickname();

                if (TextUtils.isEmpty(myNickname)) {
                    myNickname = finalMyNick;
                }

                String myRole = r.getMyRole();

                if (TextUtils.isEmpty(myRole)) {
                    myRole = "OWNER";
                }

                if (fridgeId <= 0) {
                    Log.e(TAG, "CREATE invalid fridgeId=" + fridgeId);
                    showToastLong(context, "공동 냉장고 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                if (String.valueOf(fridgeId).length() >= 12) {
                    Log.e(TAG, "CREATE invalid fridgeId looks like timestamp: " + fridgeId);
                    showToastLong(context, "공동 냉장고 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                SharedFridgePrefs.clearMembers(context);

                SharedFridgePrefs.applyJoinedFridgeFromServer(
                        context,
                        fridgeId,
                        name,
                        code,
                        expiresAt,
                        myNickname,
                        myRole
                );

                OnboardingPrefs.saveFridgeId(context, fridgeId);
                OnboardingPrefs.saveFridgeType(context, "SHARED");

                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onFailure(
                    Call<SharedFridgeResponse> call,
                    Throwable t
            ) {
                Log.e(TAG, "CREATE failed", t);
                showToastShort(context, NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    public static void joinSharedFridge(
            Context context,
            String inviteCode
    ) {
        joinSharedFridge(context, inviteCode, null);
    }

    public static void joinSharedFridge(
            Context context,
            String inviteCode,
            Runnable onSuccess
    ) {
        if (context == null) return;

        long userId = AuthPrefs.getUserId(context);

        if (userId <= 0) {
            showToastShort(context, "로그인이 필요합니다.");
            return;
        }

        String codeToSend = inviteCode == null
                ? ""
                : inviteCode.trim().toUpperCase(Locale.ROOT);

        if (codeToSend.isEmpty()) {
            showToastShort(context, "초대코드를 입력해주세요.");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("inviteCode", codeToSend);

        FridgeApiService api = FridgeApi.getClient().create(FridgeApiService.class);

        api.joinSharedFridge(body).enqueue(new Callback<SharedFridgeResponse>() {
            @Override
            public void onResponse(
                    Call<SharedFridgeResponse> call,
                    Response<SharedFridgeResponse> response
            ) {
                Log.d(TAG, "JOIN response code=" + response.code());

                if (!response.isSuccessful() || response.body() == null) {
                    showToastShort(context, "공동 냉장고 참여에 실패했어요. 초대코드를 확인해주세요.");
                    return;
                }

                SharedFridgeResponse r = response.body();

                long fridgeId = r.getFridgeId();
                String fridgeName = r.getFridgeName();
                String code = r.getInviteCode();
                long expiresAt = r.getExpiresAt();

                String myNick = r.getMyNickname();

                if (TextUtils.isEmpty(myNick)) {
                    myNick = OnboardingPrefs.getNickname(context);
                }

                if (TextUtils.isEmpty(myNick)) {
                    myNick = AuthPrefs.getNickname(context);
                }

                if (TextUtils.isEmpty(myNick)) {
                    myNick = "나";
                }

                String myRole = r.getMyRole();

                if (TextUtils.isEmpty(myRole)) {
                    myRole = "MEMBER";
                }

                if (fridgeId <= 0) {
                    Log.e(TAG, "JOIN invalid fridgeId=" + fridgeId);
                    showToastLong(context, "공동 냉장고 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                if (String.valueOf(fridgeId).length() >= 12) {
                    Log.e(TAG, "JOIN invalid fridgeId looks like timestamp: " + fridgeId);
                    showToastLong(context, "공동 냉장고 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                SharedFridgePrefs.applyJoinedFridgeFromServer(
                        context,
                        fridgeId,
                        fridgeName,
                        code,
                        expiresAt,
                        myNick,
                        myRole
                );

                OnboardingPrefs.saveFridgeId(context, fridgeId);
                OnboardingPrefs.saveFridgeType(context, "SHARED");

                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onFailure(
                    Call<SharedFridgeResponse> call,
                    Throwable t
            ) {
                Log.e(TAG, "JOIN failed", t);
                showToastShort(context, NetworkErrorUtil.getUserMessage(t));
            }
        });
    }
}