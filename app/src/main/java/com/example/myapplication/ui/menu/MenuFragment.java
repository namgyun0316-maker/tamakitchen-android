package com.namgyun.tamakitchen.ui.menu;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.mealplan.MealPlanActivity;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgeJoinRunner;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgeManageActivity;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgePrefs;
import com.namgyun.tamakitchen.ui.onboarding.SharedTime;
import com.namgyun.tamakitchen.ui.recipe.MyRecipeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.kakao.sdk.share.ShareClient;
import com.kakao.sdk.share.WebSharerClient;
import com.kakao.sdk.share.model.SharingResult;
import com.kakao.sdk.template.model.Button;
import com.kakao.sdk.template.model.Content;
import com.kakao.sdk.template.model.FeedTemplate;
import com.kakao.sdk.template.model.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MenuFragment extends Fragment {

    private static final String TAG = "MenuFragment";

    private TextView tvUserNickname;
    private ImageView imgProfile;

    private View profileContainer;

    private View menuSupport;
    private View menuCalculator;
    private View menuLedger;
    private View menuMealPlan;

    private View menuTools;
    private TextView tvToolsSummary;

    private View menuSharedInvite;
    private TextView tvSharedInviteSummary;

    private View menuSharedManage;
    private TextView tvSharedManageSummary;

    private View menuFavorites;
    private View menuMyRecipe;

    private static final String PREFS_USER_INFO = "user_info";
    private static final String PREFS_LOGIN = "login_prefs";

    private static final String KEY_NICKNAME_NEW = "nickname";
    private static final String KEY_NICKNAME_OLD = "user_nickname";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";

    private static final String EXEC_KEY_INVITE_CODE = "inviteCode";
    private static final String EXEC_KEY_FRIDGE_NAME = "fridgeName";
    private static final String EXEC_KEY_EXPIRES_AT = "expiresAt";

    private static final Pattern INVITE_CODE_PATTERN =
            Pattern.compile("^[0-9A-Za-z]{6,12}$");

    public MenuFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        tvUserNickname = view.findViewById(R.id.tv_user_nickname);
        imgProfile = view.findViewById(R.id.img_profile);
        profileContainer = view.findViewById(R.id.profile_container);

        menuSupport = view.findViewById(R.id.menu_support);
        menuCalculator = view.findViewById(R.id.menu_calculator);
        menuLedger = view.findViewById(R.id.menu_ledger);
        menuMealPlan = view.findViewById(R.id.menu_meal_plan);

        menuTools = view.findViewById(R.id.menu_tools);
        tvToolsSummary = view.findViewById(R.id.tv_tools_summary);

        menuSharedInvite = view.findViewById(R.id.menu_shared_invite);
        tvSharedInviteSummary = view.findViewById(R.id.tv_shared_invite_summary);

        menuSharedManage = view.findViewById(R.id.menu_shared_manage);
        tvSharedManageSummary = view.findViewById(R.id.tv_shared_manage_summary);

        menuFavorites = view.findViewById(R.id.menu_favorites);
        menuMyRecipe = view.findViewById(R.id.menu_my_recipe);

        displayUserInfo();
        updateToolsSummary();
        updateSharedInviteSummary();
        updateSharedManageSummary();
        bindClicks();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserInfo();
        updateToolsSummary();
        updateSharedInviteSummary();
        updateSharedManageSummary();
    }

    private void bindClicks() {

        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), ProfileActivity.class);
                    intent.putExtra("user_nickname", resolveNickname());
                    intent.putExtra("user_email", resolveEmail());
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "profile move fail", e);
                    AppToast.show(requireActivity(), "프로필 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuSupport != null) {
            menuSupport.setOnClickListener(v -> {
                try {
                    SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_INFO, 0);

                    String nickname = resolveNickname();
                    long userId = prefs.getLong(KEY_USER_ID, -1L);
                    String email = resolveEmail();

                    Intent intent = new Intent(requireContext(), InquiryActivity.class);
                    intent.putExtra(InquiryActivity.EXTRA_NICKNAME, nickname);

                    if (userId != -1L) intent.putExtra(InquiryActivity.EXTRA_USER_ID, userId);
                    if (email != null && !email.trim().isEmpty()) {
                        intent.putExtra(InquiryActivity.EXTRA_EMAIL, email.trim());
                    }

                    startActivity(intent);

                } catch (Exception e) {
                    Log.e(TAG, "inquiry move fail", e);
                    AppToast.show(requireActivity(), "문의 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuCalculator != null) {
            menuCalculator.setOnClickListener(v ->
                    startActivitySafe(new Intent(requireContext(), CalculatorActivity.class))
            );
        }

        if (menuLedger != null) {
            menuLedger.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), LedgerActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "ledger move fail", e);
                    AppToast.show(requireActivity(), "가계부 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuMealPlan != null) {
            menuMealPlan.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), MealPlanActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "meal plan move fail", e);
                    AppToast.show(requireActivity(), "나만의 식단 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuTools != null) {
            menuTools.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), ToolsSettingActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "tools move fail", e);
                    AppToast.show(requireActivity(), "조리도구 설정 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuFavorites != null) {
            menuFavorites.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), FavoriteRecipeActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "favorites move fail", e);
                    AppToast.show(requireActivity(), "즐겨찾기 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuMyRecipe != null) {
            menuMyRecipe.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), MyRecipeActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "my recipe move fail", e);
                    AppToast.show(requireActivity(), "내 레시피 관리 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuSharedInvite != null) {
            menuSharedInvite.setOnClickListener(v -> {
                try {
                    String type = OnboardingPrefs.getFridgeType(requireContext());

                    if (!"SHARED".equalsIgnoreCase(type)) {
                        showSharedEntryDialogOnlyWhenPersonal();
                        return;
                    }

                    SharedFridgePrefs.InviteInfo info = SharedFridgePrefs.ensureInviteExists(requireContext());
                    String code = (info == null) ? "" : info.code;
                    if (code == null) code = "";
                    code = code.trim().toUpperCase(Locale.ROOT);

                    if (code.isEmpty()) {
                        AppToast.show(requireActivity(), "초대코드를 만들지 못했어요. 다시 시도해주세요.");
                        return;
                    }

                    String fridgeName = SharedFridgePrefs.getFridgeName(requireContext());
                    if (fridgeName == null || fridgeName.trim().isEmpty()) fridgeName = "공동 냉장고";
                    fridgeName = fridgeName.trim();

                    long expiresAt = (info == null) ? 0L : info.expiresAt;

                    shareInviteToKakao(fridgeName, code, expiresAt);

                } catch (Exception e) {
                    Log.e(TAG, "shared invite fail", e);
                    AppToast.show(requireActivity(), "초대 공유에 실패했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }

        if (menuSharedManage != null) {
            menuSharedManage.setOnClickListener(v -> {
                try {
                    String type = OnboardingPrefs.getFridgeType(requireContext());

                    if (!"SHARED".equalsIgnoreCase(type)) {
                        showSharedEntryDialogOnlyWhenPersonal();
                        return;
                    }

                    Intent intent = new Intent(requireContext(), SharedFridgeManageActivity.class);
                    startActivity(intent);

                } catch (Exception e) {
                    Log.e(TAG, "shared manage move fail", e);
                    AppToast.show(requireActivity(), "공동 냉장고 관리 화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });
        }
    }

    private void showSharedEntryDialogOnlyWhenPersonal() {
        if (!isAdded()) return;

        String type = OnboardingPrefs.getFridgeType(requireContext());
        if ("SHARED".equalsIgnoreCase(type)) return;

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_shared_fridge, null, false);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        v.findViewById(R.id.btnCreate).setOnClickListener(b -> {
            dialog.dismiss();
            showCreateSharedFridgeDialog();
        });

        v.findViewById(R.id.btnJoin).setOnClickListener(b -> {
            dialog.dismiss();
            showJoinByInviteCodeDialog();
        });

        View btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(x -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showCreateSharedFridgeDialog() {
        if (!isAdded()) return;

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_shared_fridge, null, false);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = v.findViewById(R.id.etFridgeName);

        View btnCancel = v.findViewById(R.id.btnCancel);
        if (btnCancel != null) btnCancel.setOnClickListener(x -> dialog.dismiss());

        v.findViewById(R.id.btnCreate).setOnClickListener(b -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.length() < 2) {
                AppToast.show(requireActivity(), "이름은 2자 이상이 좋아요.");
                return;
            }

            SharedFridgeJoinRunner.createSharedFridge(requireContext(), name, () -> {
                if (!isAdded()) return;

                long id = SharedFridgePrefs.getFridgeId(requireContext());
                if (id > 0) {
                    OnboardingPrefs.saveFridgeType(requireContext(), "SHARED");

                    AppToast.show(requireActivity(), "공동 냉장고 생성 완료! 🙂");
                    updateSharedInviteSummary();
                    updateSharedManageSummary();
                } else {
                    AppToast.show(requireActivity(), "공동 냉장고 정보를 저장하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });

            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showJoinByInviteCodeDialog() {
        if (!isAdded()) return;

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_join_by_invite_code, null, false);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText et = v.findViewById(R.id.etInviteCode);

        v.findViewById(R.id.btnPaste).setOnClickListener(b -> {
            String clip = getClipboardText();
            if (!TextUtils.isEmpty(clip)) et.setText(clip.trim());
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(b -> dialog.dismiss());

        View btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(x -> dialog.dismiss());

        v.findViewById(R.id.btnJoin).setOnClickListener(b -> {
            String code = et.getText() == null ? "" : et.getText().toString().trim().toUpperCase(Locale.ROOT);

            if (!INVITE_CODE_PATTERN.matcher(code).matches()) {
                AppToast.show(requireActivity(), "초대코드 형식이 올바르지 않아요.");
                return;
            }

            SharedFridgeJoinRunner.joinSharedFridge(requireContext(), code, () -> {
                if (!isAdded()) return;

                long id = SharedFridgePrefs.getFridgeId(requireContext());
                if (id > 0) {
                    OnboardingPrefs.saveFridgeType(requireContext(), "SHARED");

                    AppToast.show(requireActivity(), "공동 냉장고 참여 완료! 🙂");
                    updateSharedInviteSummary();
                    updateSharedManageSummary();
                } else {
                    AppToast.show(requireActivity(), "공동 냉장고 정보를 저장하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            });

            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String getClipboardText() {
        try {
            ClipboardManager cm =
                    (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return "";
            ClipData d = cm.getPrimaryClip();
            return (d == null || d.getItemCount() == 0) ? "" :
                    d.getItemAt(0).coerceToText(requireContext()).toString();
        } catch (Throwable ignored) {
        }
        return "";
    }

    private void startActivitySafe(@NonNull Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "start activity fail", e);
            AppToast.show(requireActivity(), "화면을 열지 못했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private void displayUserInfo() {
        if (tvUserNickname != null) {
            tvUserNickname.setText(resolveNickname());
        }
        applyProfileImage();
    }

    private String resolveNickname() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_INFO, 0);

            String nick = prefs.getString(KEY_NICKNAME_NEW, null);
            if (nick != null && !nick.trim().isEmpty()) return nick.trim();

            String old = prefs.getString(KEY_NICKNAME_OLD, null);
            if (old != null && !old.trim().isEmpty()) {
                prefs.edit().putString(KEY_NICKNAME_NEW, old.trim()).apply();
                return old.trim();
            }

            String onboard = OnboardingPrefs.getNickname(requireContext());
            if (onboard != null && !onboard.trim().isEmpty()) {
                prefs.edit().putString(KEY_NICKNAME_NEW, onboard.trim()).apply();
                return onboard.trim();
            }

        } catch (Throwable ignored) {
        }

        return "닉네임 정보 없음";
    }

    private String resolveEmail() {
        try {
            SharedPreferences userPrefs = requireActivity().getSharedPreferences(PREFS_USER_INFO, 0);
            SharedPreferences loginPrefs = requireActivity().getSharedPreferences(PREFS_LOGIN, 0);

            String[] candidateKeys = new String[] {
                    "user_email",
                    "email",
                    "userEmail",
                    "kakao_email",
                    "naver_email",
                    "google_email"
            };

            for (String key : candidateKeys) {
                String value = userPrefs.getString(key, null);
                if (value != null && !value.trim().isEmpty()) {
                    value = value.trim();
                    userPrefs.edit().putString(KEY_USER_EMAIL, value).apply();
                    return value;
                }
            }

            for (String key : candidateKeys) {
                String value = loginPrefs.getString(key, null);
                if (value != null && !value.trim().isEmpty()) {
                    value = value.trim();
                    userPrefs.edit().putString(KEY_USER_EMAIL, value).apply();
                    return value;
                }
            }

            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account != null && account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
                String value = account.getEmail().trim();
                userPrefs.edit().putString(KEY_USER_EMAIL, value).apply();
                return value;
            }

        } catch (Throwable ignored) {
        }

        return "";
    }

    private void applyProfileImage() {
        if (imgProfile == null || !isAdded()) return;

        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_INFO, 0);
            String uriString = prefs.getString(KEY_PROFILE_IMAGE_URI, null);

            if (uriString == null || uriString.trim().isEmpty()) {
                imgProfile.setImageResource(R.drawable.ic_profile_placeholder);
                return;
            }

            imgProfile.setImageURI(Uri.parse(uriString));
        } catch (Throwable t) {
            imgProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void updateToolsSummary() {
        if (tvToolsSummary == null) return;

        Set<String> tools = OnboardingPrefs.getTools(requireContext());
        if (tools == null || tools.isEmpty()) {
            tvToolsSummary.setText("미설정");
            return;
        }

        List<String> labels = new ArrayList<>();
        for (String key : tools) {
            String label = toolLabel(key);
            if (label != null) labels.add(label);
        }

        if (labels.isEmpty()) {
            tvToolsSummary.setText("미설정");
            return;
        }

        Collections.sort(labels);

        if (labels.size() <= 3) {
            tvToolsSummary.setText(joinWithDot(labels));
        } else {
            tvToolsSummary.setText(labels.get(0) + " 외 " + (labels.size() - 1) + "개");
        }
    }

    private void updateSharedInviteSummary() {
        if (tvSharedInviteSummary == null) return;

        try {
            String type = OnboardingPrefs.getFridgeType(requireContext());

            if (!"SHARED".equalsIgnoreCase(type)) {
                tvSharedInviteSummary.setText("만들기/참여");
                return;
            }

            SharedFridgePrefs.InviteInfo info = SharedFridgePrefs.ensureInviteExists(requireContext());
            if (info == null || info.code == null || info.code.trim().isEmpty()) {
                tvSharedInviteSummary.setText("초대코드 생성");
                return;
            }

            String code = info.code.trim().toUpperCase(Locale.ROOT);
            tvSharedInviteSummary.setText(code);

        } catch (Throwable t) {
            tvSharedInviteSummary.setText("초대코드 공유");
        }
    }

    private void updateSharedManageSummary() {
        if (tvSharedManageSummary == null) return;

        try {
            String type = OnboardingPrefs.getFridgeType(requireContext());

            if (!"SHARED".equalsIgnoreCase(type)) {
                tvSharedManageSummary.setText("만들기/참여");
                return;
            }

            long fridgeId = SharedFridgePrefs.getFridgeId(requireContext());
            if (fridgeId <= 0) {
                tvSharedManageSummary.setText("참여 대기");
            } else {
                tvSharedManageSummary.setText("멤버/초대코드/나가기");
            }

        } catch (Throwable t) {
            tvSharedManageSummary.setText("멤버/초대코드/나가기");
        }
    }

    private String joinWithDot(@NonNull List<String> labels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append("·");
            sb.append(labels.get(i));
        }
        return sb.toString();
    }

    private String toolLabel(String key) {
        Map<String, String> map = new HashMap<>();
        map.put("air_fryer", "에어프라이어");
        map.put("oven", "오븐");
        map.put("microwave", "전자레인지");
        map.put("blender", "블렌더");
        map.put("rice_cooker", "전기밥솥");
        map.put("steamer", "찜기");
        map.put("gas_range", "가스레인지");
        map.put("induction", "인덕션");
        return map.get(key);
    }

    private void shareInviteToKakao(@NonNull String fridgeName,
                                    @NonNull String code,
                                    long expiresAt) {

        String pkg = requireContext().getPackageName();
        String playStoreUrl = "https://play.google.com/store/apps/details?id=" + pkg;

        Map<String, String> execParams = new HashMap<>();
        execParams.put(EXEC_KEY_INVITE_CODE, code);
        execParams.put(EXEC_KEY_FRIDGE_NAME, fridgeName);
        execParams.put(EXEC_KEY_EXPIRES_AT, String.valueOf(expiresAt));

        Map<String, String> iosParams = new HashMap<>();
        iosParams.put(EXEC_KEY_INVITE_CODE, code);
        iosParams.put(EXEC_KEY_FRIDGE_NAME, fridgeName);
        iosParams.put(EXEC_KEY_EXPIRES_AT, String.valueOf(expiresAt));

        Link link = new Link(
                playStoreUrl,
                playStoreUrl,
                execParams,
                iosParams
        );

        String title =
                "🍽️ 이걸로해먹자 공동냉장고 초대!\n"
                        + "냉장고: " + fridgeName + "\n"
                        + "초대코드: " + code
                        + (expiresAt > 0 ? ("\n" + SharedTime.formatExpireText(expiresAt)) : "")
                        + "\n\n(버튼을 누르면 앱이 열려요)";

        String imageUrl = "https://via.placeholder.com/800x400.png?text=Shared+Fridge";

        Content content = new Content(title, imageUrl, link);

        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("앱에서 참여하기", link));

        FeedTemplate template = new FeedTemplate(
                content,
                null,
                null,
                buttons
        );

        if (ShareClient.getInstance().isKakaoTalkSharingAvailable(requireContext())) {
            ShareClient.getInstance().shareDefault(requireContext(), template,
                    (SharingResult result, Throwable error) -> {
                        if (error != null) {
                            Log.e(TAG, "kakao share fail", error);

                            if (isAdded()) {
                                AppToast.show(
                                        requireActivity(),
                                        NetworkErrorUtil.getUserMessage(error)
                                );
                            }
                            return null;
                        }

                        if (result != null) {
                            try {
                                startActivity(result.getIntent());
                            } catch (Exception e) {
                                Log.e(TAG, "kakao share result start fail", e);
                                if (isAdded()) {
                                    AppToast.show(
                                            requireActivity(),
                                            "공유 화면을 열지 못했어요. 잠시 후 다시 시도해주세요."
                                    );
                                }
                            }
                        }

                        return null;
                    });
        } else {
            try {
                Uri sharerUrl = WebSharerClient.getInstance().makeDefaultUrl(template);
                startActivity(new Intent(Intent.ACTION_VIEW, sharerUrl));
            } catch (Exception e) {
                Log.e(TAG, "web sharer fail", e);

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)));
                } catch (ActivityNotFoundException ignored) {
                    if (isAdded()) {
                        AppToast.show(
                                requireActivity(),
                                "공유 화면을 열지 못했어요. 잠시 후 다시 시도해주세요."
                        );
                    }
                }
            }
        }
    }
}