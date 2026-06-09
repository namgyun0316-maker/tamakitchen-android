package com.namgyun.tamakitchen.ui.onboarding;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.R;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;
import java.util.regex.Pattern;

public class StepFridgeTypeFragment extends Fragment {

    public interface OnboardingNav {
        void setNextEnabled(boolean enabled);
        void goNextStep();
    }

    private MaterialCardView cardPersonal;
    private MaterialCardView cardShared;

    private int colorSelectedStroke;
    private int colorNormalStroke;
    private int colorSelectedBg;
    private int colorNormalBg;

    private static final Pattern INVITE_CODE_PATTERN =
            Pattern.compile("^[0-9A-Za-z]{6,12}$");

    private OnboardingNav nav;

    private boolean selectedPersonal = false;
    private boolean selectedShared = false;

    private boolean sharedReady = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnboardingNav) {
            nav = (OnboardingNav) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_fridge_type, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        cardPersonal = view.findViewById(R.id.cardPersonal);
        cardShared = view.findViewById(R.id.cardShared);

        colorSelectedStroke = ContextCompat.getColor(requireContext(), R.color.pascal_blue);
        colorNormalStroke = 0xFFECECEC;
        colorSelectedBg = 0xFFF6FBFF;
        colorNormalBg = 0xFFFFFFFF;

        restoreStateFromPrefsButForceSharedToRequireAction();
        updateNextEnabled();

        cardPersonal.setOnClickListener(v -> {
            selectPersonal(true);
            updateNextEnabled();
        });

        cardShared.setOnClickListener(v -> {
            selectShared(true);
            sharedReady = false;
            updateNextEnabled();
            showSharedChoiceDialog();
        });
    }

    private void restoreStateFromPrefsButForceSharedToRequireAction() {
        String savedType = OnboardingPrefs.getFridgeType(requireContext());

        if ("PERSONAL".equalsIgnoreCase(savedType)) {
            selectedPersonal = true;
            selectedShared = false;
            sharedReady = false;
            apply(cardPersonal, true);
            apply(cardShared, false);

        } else if ("SHARED".equalsIgnoreCase(savedType)) {
            selectedPersonal = false;
            selectedShared = true;
            sharedReady = false;
            apply(cardPersonal, false);
            apply(cardShared, true);

        } else {
            selectedPersonal = false;
            selectedShared = false;
            sharedReady = false;
            apply(cardPersonal, false);
            apply(cardShared, false);
        }
    }

    private void showSharedChoiceDialog() {
        if (!isAdded()) return;

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_shared_fridge, null, false);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        Window w = dialog.getWindow();
        if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

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

        dialog.setOnDismissListener(d -> updateNextEnabled());
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = v.findViewById(R.id.etFridgeName);

        View btnCancel = v.findViewById(R.id.btnCancel);
        if (btnCancel != null) btnCancel.setOnClickListener(x -> dialog.dismiss());

        dialog.setOnDismissListener(d -> updateNextEnabled());

        v.findViewById(R.id.btnCreate).setOnClickListener(b -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.length() < 2) {
                showIosToast("이름은 2자 이상 입력해주세요.");
                return;
            }

            SharedFridgeJoinRunner.createSharedFridge(requireContext(), name, () -> {
                if (!isAdded()) return;

                long id = SharedFridgePrefs.getFridgeId(requireContext());
                sharedReady = (id > 0);

                updateNextEnabled();

                if (sharedReady) {
                    // ✅ 전환 후 토스트 예약(딱 1개만 뜨게)
                    if (getActivity() instanceof OnboardingActivity) {
                        ((OnboardingActivity) getActivity())
                                .enqueueToastAfterTransition("공동 냉장고가 생성되었습니다.");
                    }

                    if (nav != null) {
                        nav.setNextEnabled(true);
                        nav.goNextStep();
                    }
                } else {
                    showIosToast("생성은 완료됐지만 냉장고 정보 저장에 실패했습니다.");
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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

        dialog.setOnDismissListener(d -> updateNextEnabled());

        v.findViewById(R.id.btnJoin).setOnClickListener(b -> {
            String code = et.getText() == null ? "" : et.getText().toString().trim().toUpperCase(Locale.ROOT);

            if (!INVITE_CODE_PATTERN.matcher(code).matches()) {
                showIosToast("초대코드 형식이 올바르지 않습니다.");
                return;
            }

            SharedFridgeJoinRunner.joinSharedFridge(requireContext(), code, () -> {
                if (!isAdded()) return;

                long id = SharedFridgePrefs.getFridgeId(requireContext());
                sharedReady = (id > 0);

                updateNextEnabled();

                if (sharedReady) {
                    // ✅ 전환 후 토스트 예약(딱 1개만 뜨게)
                    if (getActivity() instanceof OnboardingActivity) {
                        ((OnboardingActivity) getActivity())
                                .enqueueToastAfterTransition("공동 냉장고 참여가 완료되었습니다.");
                    }

                    if (nav != null) {
                        nav.setNextEnabled(true);
                        nav.goNextStep();
                    }
                } else {
                    showIosToast("참여는 완료됐지만 냉장고 정보 저장에 실패했습니다.");
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

    private void selectPersonal(boolean notify) {
        selectedPersonal = true;
        selectedShared = false;
        sharedReady = false;

        apply(cardPersonal, true);
        apply(cardShared, false);

        if (notify) OnboardingPrefs.saveFridgeType(requireContext(), "PERSONAL");

        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).setFridgeType("PERSONAL");
        }
    }

    private void selectShared(boolean notify) {
        selectedPersonal = false;
        selectedShared = true;

        apply(cardPersonal, false);
        apply(cardShared, true);

        if (notify) OnboardingPrefs.saveFridgeType(requireContext(), "SHARED");

        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).setFridgeType("SHARED");
        }
    }

    private void updateNextEnabled() {
        boolean enabled;

        if (!selectedPersonal && !selectedShared) {
            enabled = false;
        } else if (selectedPersonal) {
            enabled = true;
        } else {
            long id = SharedFridgePrefs.getFridgeId(requireContext());
            enabled = sharedReady && (id > 0);
        }

        if (nav != null) nav.setNextEnabled(enabled);
    }

    private void apply(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? dp(2) : dp(1));
        card.setStrokeColor(selected ? colorSelectedStroke : colorNormalStroke);
        card.setCardBackgroundColor(selected ? colorSelectedBg : colorNormalBg);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private String getClipboardText() {
        ClipboardManager cm =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return "";
        ClipData d = cm.getPrimaryClip();
        return (d == null || d.getItemCount() == 0) ? "" :
                d.getItemAt(0).coerceToText(requireContext()).toString();
    }

    // ✅ 온보딩에서는 시스템 Toast 금지: Activity 오버레이로만 표시
    private void showIosToast(String message) {
        if (!isAdded()) return;
        if (message == null || message.trim().isEmpty()) return;

        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).showIosToast(message);
        }
    }
}