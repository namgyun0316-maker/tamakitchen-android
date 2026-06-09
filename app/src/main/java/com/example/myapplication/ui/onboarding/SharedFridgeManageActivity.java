package com.namgyun.tamakitchen.ui.onboarding;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.FridgeApiService;
import com.namgyun.tamakitchen.network.SharedFridgeMemberDto;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedFridgeManageActivity extends AppCompatActivity {

    private static final String TAG = "SharedFridgeManage";

    private TextView tvTitleStroke;
    private TextView tvTitle;
    private LinearLayout btnLeaveShared;
    private RecyclerView rvMembers;

    private SharedMemberAdapter adapter;
    private FridgeApiService api;

    private long fridgeId;
    private long myUserId;
    private boolean iAmOwner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_fridge_manage);

        api = FridgeApi.getClient().create(FridgeApiService.class);

        tvTitleStroke = findViewById(R.id.tvTitleStroke);
        tvTitle = findViewById(R.id.tvTitle);
        btnLeaveShared = findViewById(R.id.btnLeaveShared);
        rvMembers = findViewById(R.id.rvMembers);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SharedMemberAdapter();
        rvMembers.setAdapter(adapter);

        myUserId = AuthPrefs.getUserId(this);
        fridgeId = SharedFridgePrefs.getFridgeId(this);

        adapter.setMyUserId(myUserId);
        adapter.setListener(this::showMemberActionDialog);

        String name = SharedFridgePrefs.getFridgeName(this);
        if (TextUtils.isEmpty(name)) name = "공동";

        String titleText = name + " 공동냉장고";
        tvTitle.setText(titleText);
        tvTitleStroke.setText(titleText);

        btnLeaveShared.setOnClickListener(v -> onClickLeave());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMembersFromServer();
    }

    private void showIosToastShort(String message) {
        showIosToast(message);
    }

    private void showIosToastLong(String message) {
        showIosToast(message);
    }

    private void showIosToast(String message) {
        if (message == null || message.trim().isEmpty()) return;

        String msg = message.trim()
                .replace("\n", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        AppToast.show(this, msg);
    }

    private void refreshMembersFromServer() {
        if (fridgeId <= 0 || myUserId <= 0) {
            Log.w(TAG, "refreshMembersFromServer skipped. fridgeId=" + fridgeId + " myUserId=" + myUserId);
            adapter.submit(new ArrayList<>());
            adapter.setIAmOwner(false);
            iAmOwner = false;
            return;
        }

        api.getSharedFridgeMembers(fridgeId, myUserId).enqueue(new Callback<List<SharedFridgeMemberDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<SharedFridgeMemberDto>> call,
                                   @NonNull Response<List<SharedFridgeMemberDto>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "member load failed. code=" + response.code());
                    showIosToastShort("멤버 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                List<SharedFridgeMemberDto> list = response.body();
                if (list == null) list = new ArrayList<>();

                iAmOwner = false;

                for (SharedFridgeMemberDto m : list) {
                    if (m != null && m.userId == myUserId && "OWNER".equalsIgnoreCase(safeUpper(m.role))) {
                        iAmOwner = true;
                        break;
                    }
                }

                adapter.setIAmOwner(iAmOwner);
                adapter.submit(list);
            }

            @Override
            public void onFailure(@NonNull Call<List<SharedFridgeMemberDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "members failed", t);
                showIosToastShort(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    private String safeUpper(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase(Locale.ROOT);
    }

    private void showMemberActionDialog(@NonNull SharedFridgeMemberDto target) {
        if (!iAmOwner) return;

        if (target.userId <= 0) {
            showIosToastLong("멤버 정보를 확인할 수 없어 관리할 수 없습니다.");
            return;
        }

        if (target.userId == myUserId) return;

        String nick = TextUtils.isEmpty(target.nickname) ? "멤버" : target.nickname;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_member_actions, null, false);

        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvDialogDesc = view.findViewById(R.id.tvDialogDesc);
        LinearLayout btnTransfer = view.findViewById(R.id.btnTransferOwner);
        LinearLayout btnKick = view.findViewById(R.id.btnKickMember);
        TextView btnCancel = view.findViewById(R.id.btnCancel);

        tvDialogTitle.setText("공동냉장고 멤버 관리");
        tvDialogDesc.setText(nick + "님 작업을 선택해주세요.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnTransfer.setOnClickListener(v -> {
            dialog.dismiss();
            showTransferConfirmDialog(target.userId, nick);
        });

        btnKick.setOnClickListener(v -> {
            dialog.dismiss();
            showKickConfirmDialog(target.userId, nick);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showKickConfirmDialog(long targetUserId, @NonNull String nick) {
        if (targetUserId <= 0) return;

        if (!iAmOwner) {
            showIosToastShort("OWNER만 추방할 수 있습니다.");
            return;
        }

        if (targetUserId == myUserId) {
            showIosToastShort("본인은 추방할 수 없습니다.");
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_kick_member, null, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnConfirm = view.findViewById(R.id.btnConfirm);

        tvTitle.setText("추방하기");
        tvDesc.setText(nick + "님을 추방할까요? 추방 후 재초대가 필요합니다.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            doKickMember(targetUserId, nick);
        });

        dialog.show();
    }

    private void doKickMember(long targetUserId, @NonNull String nick) {
        if (fridgeId <= 0 || myUserId <= 0 || targetUserId <= 0) return;

        Map<String, Object> body = new HashMap<>();
        body.put("requesterUserId", myUserId);
        body.put("targetUserId", targetUserId);

        api.kickMember(fridgeId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {

                if (!response.isSuccessful()) {
                    Log.e(TAG, "kick failed. code=" + response.code());
                    showIosToastShort("추방에 실패했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                showIosToastShort(nick + "님을 추방했습니다.");
                refreshMembersFromServer();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "kick failed", t);
                showIosToastShort(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    private void showTransferConfirmDialog(long toUserId, @NonNull String nick) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transfer_owner, null, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnConfirm = view.findViewById(R.id.btnConfirm);

        tvTitle.setText("권한 위임");
        tvDesc.setText(nick + "님에게 OWNER 권한을 위임할까요?");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            doTransferOwner(toUserId);
        });

        dialog.show();
    }

    private void doTransferOwner(long toUserId) {
        if (fridgeId <= 0 || myUserId <= 0) return;

        Map<String, Object> body = new HashMap<>();
        body.put("fromOwnerUserId", myUserId);
        body.put("toUserId", toUserId);

        api.transferOwner(fridgeId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {

                if (!response.isSuccessful()) {
                    Log.e(TAG, "transfer owner failed. code=" + response.code());
                    showIosToastShort("권한 위임에 실패했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                showIosToastShort("권한을 위임했습니다.");
                refreshMembersFromServer();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "transferOwner failed", t);
                showIosToastShort(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    private void onClickLeave() {
        if (fridgeId <= 0 || myUserId <= 0) {
            showIosToastShort("공동 냉장고 정보가 없습니다.");
            finish();
            return;
        }

        int memberCount = adapter.getMemberCount();

        if (iAmOwner) {
            if (memberCount <= 1) {
                showLeaveConfirmWhenAloneOwnerStyled();
                return;
            }

            showOwnerCannotLeaveStyledDialog();
            return;
        }

        showLeaveConfirmMember();
    }

    private void showLeaveConfirmWhenAloneOwnerStyled() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_leave_alone_owner, null, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnLeave = view.findViewById(R.id.btnLeave);

        tvTitle.setText("공동냉장고 나가기");
        tvDesc.setText("현재 멤버가 없어 위임 없이 나갈 수 있습니다. 나갈까요?");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnLeave.setOnClickListener(v -> {
            dialog.dismiss();
            doLeave();
        });

        dialog.show();
    }

    private void showLeaveConfirmMember() {
        new AlertDialog.Builder(this)
                .setTitle("공동냉장고 나가기")
                .setMessage("정말 나갈까요? 나가면 개인 냉장고로 전환됩니다.")
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("나가기", (d, w) -> doLeave())
                .show();
    }

    private void showOwnerCannotLeaveStyledDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_owner_cannot_leave, null, false);

        TextView btnOk = view.findViewById(R.id.btnOk);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void doLeave() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", myUserId);

        api.leaveSharedFridge(fridgeId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {

                if (!response.isSuccessful()) {
                    Log.e(TAG, "leave shared fridge failed. code=" + response.code());
                    showIosToastShort("공동 냉장고 나가기에 실패했어요. 잠시 후 다시 시도해주세요.");
                    return;
                }

                SharedFridgePrefs.leaveSharedFridge(
                        SharedFridgeManageActivity.this,
                        AuthPrefs.getNickname(SharedFridgeManageActivity.this)
                );

                showIosToastShort("개인 냉장고로 전환되었습니다.");
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "leave failed", t);
                showIosToastShort(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }
}