package com.namgyun.tamakitchen.ui.fridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.analytics.AppAnalytics;
import com.namgyun.tamakitchen.pet.PetExpManager;
import com.namgyun.tamakitchen.pet.PetPrefs;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingActivity;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgeJoinRunner;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgeManageActivity;
import com.namgyun.tamakitchen.ui.onboarding.SharedFridgePrefs;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FridgeFragment extends Fragment {

    private View titleContainer;
    private TextView tvTitle;
    private TextView tvSummary;
    private ChipGroup chipGroup;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private ImageButton btnOptions;

    private FridgeAdapter adapter;

    private final List<FridgeItem> itemList = new ArrayList<>();
    private final List<FridgeItem> displayList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addItemLauncher;

    private final FridgeModeManager mode = new FridgeModeManager();
    private final FridgeRepository repo = new FridgeRepository();
    private final FridgeSelectionController selection = new FridgeSelectionController();
    private final FridgeOptionsPopupHelper optionsPopup = new FridgeOptionsPopupHelper();

    private FridgeFilterSorter.SortMode sortMode = FridgeFilterSorter.SortMode.EXPIRY;

    public FridgeFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addItemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        FridgeItem newItem = buildItemFromAddResult(data);
                        if (newItem != null) addItem(newItem);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_fridge, container, false);

        initViews(view);

        FridgeChipStyler.apply(requireContext(), chipGroup);

        setupRecyclerView();
        setupListeners();

        applyFridgeTitle();
        refreshAll();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        AppAnalytics.logScreen(requireContext(), "fridge_screen");

        applyFridgeTitle();
        FridgeChipStyler.apply(requireContext(), chipGroup);
        refreshAll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        optionsPopup.dismiss();
    }

    private void initViews(View v) {
        titleContainer = v.findViewById(R.id.titleContainer);
        tvTitle = v.findViewById(R.id.tv_title);
        tvSummary = v.findViewById(R.id.tv_summary);
        chipGroup = v.findViewById(R.id.chip_group);
        recyclerView = v.findViewById(R.id.rv_fridge_items);
        fab = v.findViewById(R.id.btn_add_item);
        btnOptions = v.findViewById(R.id.btn_options);

        if (titleContainer != null) {
            titleContainer.setOnClickListener(x -> showFridgeSwitchDialog());
        } else if (tvTitle != null) {
            tvTitle.setOnClickListener(x -> showFridgeSwitchDialog());
        }
    }

    private void setupRecyclerView() {
        adapter = new FridgeAdapter(getContext(), displayList);
        adapter.setMyNickname(getMyNicknameSafe());

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((item, position) -> {
            if (item == null) return;

            if (selection.isSelectionMode()) {
                adapter.toggleSelection(position);
                selection.updateFab(fab);
            } else {
                FridgeEditDialogHelper.showEditDialog(
                        requireContext(),
                        item,
                        updated -> saveEditedItem(updated)
                );
            }
        });

        adapter.setOnItemLongClickListener((item, position) -> {
            if (item == null) return;

            if (selection.isSelectionMode()) {
                adapter.toggleSelection(position);
                selection.updateFab(fab);
            } else {
                FridgeDeleteDialogHelper.showDeleteOne(
                        requireContext(),
                        item,
                        items -> deleteItems(items)
                );
            }
        });
    }

    private void setupListeners() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            applyFilterAndRefreshUI(getCurrentFilter());
            selection.exit(adapter, fab);
            FridgeUiUtils.showCustomToast(requireActivity(), "선택 삭제를 종료했습니다");
        });

        fab.setOnClickListener(v -> {
            if (selection.isSelectionMode()) {
                int cnt = selection.getSelectedCount(adapter);
                if (cnt <= 0) {
                    FridgeUiUtils.showCustomToast(requireActivity(), "삭제할 항목을 선택해주세요");
                    return;
                }

                FridgeDeleteDialogHelper.showSelectDeleteConfirm(
                        requireContext(),
                        cnt,
                        adapter.getSelectedItems(),
                        items -> deleteItems(items)
                );
            } else {
                addItemLauncher.launch(new Intent(getActivity(), FridgeAddActivity.class));
            }
        });

        btnOptions.setOnClickListener(v -> optionsPopup.toggle(
                requireContext(),
                btnOptions,
                selection.isSelectionMode(),
                sortMode,
                new FridgeOptionsPopupHelper.Callbacks() {
                    @Override
                    public void onSortExpiry() {
                        sortMode = FridgeFilterSorter.SortMode.EXPIRY;
                        sortAndRefresh();
                    }

                    @Override
                    public void onSortName() {
                        sortMode = FridgeFilterSorter.SortMode.NAME;
                        sortAndRefresh();
                    }

                    @Override
                    public void onToggleSelectDelete() {
                        if (selection.isSelectionMode()) {
                            selection.exit(adapter, fab);
                            FridgeUiUtils.showCustomToast(requireActivity(), "선택 삭제를 종료했습니다");
                        } else {
                            selection.enter(adapter, fab);
                            FridgeUiUtils.showCustomToast(requireActivity(), "삭제할 항목을 선택하세요");
                        }
                    }

                    @Override
                    public void onDeleteExpired() {
                        FridgeDeleteDialogHelper.showDeleteExpired(
                                requireContext(),
                                itemList,
                                items -> deleteItems(items)
                        );
                    }

                    @Override
                    public void onDeleteAll() {
                        FridgeDeleteDialogHelper.showDeleteAll(
                                requireContext(),
                                itemList,
                                items -> deleteItems(items)
                        );
                    }
                }
        ));
    }

    private void refreshAll() {
        if (getContext() == null) return;

        selection.exit(adapter, fab);

        if (mode.isGuestMode(requireContext())) {
            repo.loadLocal(requireContext(), new FridgeRepository.DataCallback<List<FridgeItem>>() {
                @Override
                public void onSuccess(List<FridgeItem> data) {
                    itemList.clear();
                    itemList.addAll(data);
                    sortAndRefresh();
                    FridgeUiUtils.showCustomToast(requireActivity(), "비회원 모드: 기기 내에 저장됩니다");
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });
            return;
        }

        if (mode.isSharedFridgeMode(requireContext())) {
            long fridgeId = mode.getSharedFridgeIdSafe(requireContext());
            long userId = mode.getUserIdSafe(requireContext());

            if (fridgeId <= 0) {
                mode.handleMissingSharedFridgeId(requireContext(), () -> {
                    FridgeUiUtils.showCustomToast(requireActivity(), "개인 냉장고로 전환했습니다");
                    applyFridgeTitle();
                    refreshAll();
                });
                return;
            }

            if (userId <= 0) {
                FridgeUiUtils.showCustomToast(requireActivity(), "로그인이 필요합니다");
                return;
            }

            repo.loadShared(fridgeId, userId, new FridgeRepository.DataCallback<List<FridgeItem>>() {
                @Override
                public void onSuccess(List<FridgeItem> data) {
                    itemList.clear();
                    itemList.addAll(data);
                    if (adapter != null) adapter.setMyNickname(getMyNicknameSafe());
                    sortAndRefresh();
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });

        } else {
            long userId = mode.getUserIdSafe(requireContext());

            if (userId <= 0) {
                FridgeUiUtils.showCustomToast(requireActivity(), "로그인이 필요합니다");
                return;
            }

            repo.loadPersonal(userId, new FridgeRepository.DataCallback<List<FridgeItem>>() {
                @Override
                public void onSuccess(List<FridgeItem> data) {
                    itemList.clear();
                    itemList.addAll(data);
                    if (adapter != null) adapter.setMyNickname(getMyNicknameSafe());
                    sortAndRefresh();
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });
        }
    }

    private void sortAndRefresh() {
        if (sortMode == FridgeFilterSorter.SortMode.EXPIRY) {
            FridgeFilterSorter.sortByExpiry(itemList);
        } else {
            FridgeFilterSorter.sortByName(itemList);
        }

        applyFilterAndRefreshUI(getCurrentFilter());
        updateSummary();
        selection.updateFab(fab);

        if (mode.isGuestMode(requireContext())) {
            repo.saveLocal(requireContext(), itemList);
        }
    }

    private void applyFilterAndRefreshUI(String filter) {
        displayList.clear();
        displayList.addAll(FridgeFilterSorter.filter(itemList, filter));
        adapter.updateItems(new ArrayList<>(displayList));
        selection.updateFab(fab);
    }

    private void updateSummary() {
        if (tvSummary == null) return;
        tvSummary.setText(FridgeSummaryFormatter.buildSummary(itemList));
    }

    private String getCurrentFilter() {
        String filter = "전체";
        int checkedId = chipGroup.getCheckedChipId();

        if (checkedId != View.NO_ID) {
            Chip c = chipGroup.findViewById(checkedId);
            if (c != null) filter = c.getText().toString();
        }

        return filter;
    }

    private void addItem(FridgeItem item) {
        if (getContext() == null || item == null) return;

        if (mode.isGuestMode(requireContext())) {
            itemList.add(item);
            repo.saveLocal(requireContext(), itemList);
            sortAndRefresh();

            ChecklistPrefs.markFridgeAddDone(
                    requireContext(),
                    PetPrefs.getLevel(requireContext())
            );

            PetExpManager.giveFridgeAddExp(requireActivity());

            if (item.isExpiryManuallySet()) {
                ChecklistPrefs.markExpiryAddDone(
                        requireContext(),
                        PetPrefs.getLevel(requireContext())
                );
                PetExpManager.giveExpiryManualAddExp(requireActivity());
            }

            FridgeUiUtils.showCustomToast(
                    requireActivity(),
                    KoreanJosaUtil.buildAddedMessage(item.getName()) + " (비회원 저장)"
            );
            return;
        }

        if (mode.isSharedFridgeMode(requireContext())) {
            long fridgeId = mode.getSharedFridgeIdSafe(requireContext());
            long userId = mode.getUserIdSafe(requireContext());

            if (fridgeId <= 0) {
                mode.handleMissingSharedFridgeId(requireContext(), () -> {
                    FridgeUiUtils.showCustomToast(requireActivity(), "개인 냉장고로 전환했습니다");
                    applyFridgeTitle();
                    refreshAll();
                });
                return;
            }

            if (userId <= 0) {
                FridgeUiUtils.showCustomToast(requireActivity(), "로그인이 필요합니다");
                return;
            }

            if (TextUtils.isEmpty(item.getAddedByNickname())) {
                String myNick = getMyNicknameSafe();
                FridgeUiUtils.setAddedByNicknameSafe(
                        item,
                        TextUtils.isEmpty(myNick) ? "나" : myNick
                );
            }

            repo.addShared(fridgeId, userId, item, new FridgeRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    refreshAll();

                    ChecklistPrefs.markFridgeAddDone(
                            requireContext(),
                            PetPrefs.getLevel(requireContext())
                    );

                    PetExpManager.giveFridgeAddExp(requireActivity());

                    if (item.isExpiryManuallySet()) {
                        ChecklistPrefs.markExpiryAddDone(
                                requireContext(),
                                PetPrefs.getLevel(requireContext())
                        );
                        PetExpManager.giveExpiryManualAddExp(requireActivity());
                    }

                    AppAnalytics.logFridgeAdd(requireContext(), item.getName());

                    FridgeUiUtils.showCustomToast(
                            requireActivity(),
                            KoreanJosaUtil.buildAddedMessage(item.getName())
                    );
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });

        } else {
            long userId = mode.getUserIdSafe(requireContext());

            if (userId <= 0) {
                FridgeUiUtils.showCustomToast(requireActivity(), "로그인이 필요합니다");
                return;
            }

            repo.addPersonal(userId, item, new FridgeRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    refreshAll();

                    ChecklistPrefs.markFridgeAddDone(
                            requireContext(),
                            PetPrefs.getLevel(requireContext())
                    );

                    PetExpManager.giveFridgeAddExp(requireActivity());

                    if (item.isExpiryManuallySet()) {
                        ChecklistPrefs.markExpiryAddDone(
                                requireContext(),
                                PetPrefs.getLevel(requireContext())
                        );
                        PetExpManager.giveExpiryManualAddExp(requireActivity());
                    }

                    FridgeUiUtils.showCustomToast(
                            requireActivity(),
                            KoreanJosaUtil.buildAddedMessage(item.getName())
                    );
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });
        }
    }

    private void saveEditedItem(FridgeItem item) {
        if (getContext() == null || item == null) return;

        if (mode.isGuestMode(requireContext())) {
            repo.saveLocal(requireContext(), itemList);
            sortAndRefresh();

            if (item.isExpiryManuallySet()) {
                ChecklistPrefs.markExpiryAddDone(
                        requireContext(),
                        PetPrefs.getLevel(requireContext())
                );
                PetExpManager.giveExpiryManualAddExp(requireActivity());
            }

            FridgeUiUtils.showCustomToast(requireActivity(), "수정되었습니다 (비회원 저장)");
            return;
        }

        if (mode.isSharedFridgeMode(requireContext())) {
            long userId = mode.getUserIdSafe(requireContext());

            repo.updateShared(userId, item, new FridgeRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    refreshAll();

                    if (item.isExpiryManuallySet()) {
                        ChecklistPrefs.markExpiryAddDone(
                                requireContext(),
                                PetPrefs.getLevel(requireContext())
                        );
                        PetExpManager.giveExpiryManualAddExp(requireActivity());
                    }

                    FridgeUiUtils.showCustomToast(requireActivity(), "수정되었습니다");
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });

        } else {
            repo.updatePersonal(item, new FridgeRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    refreshAll();

                    if (item.isExpiryManuallySet()) {
                        ChecklistPrefs.markExpiryAddDone(
                                requireContext(),
                                PetPrefs.getLevel(requireContext())
                        );
                        PetExpManager.giveExpiryManualAddExp(requireActivity());
                    }

                    FridgeUiUtils.showCustomToast(requireActivity(), "수정되었습니다");
                }

                @Override
                public void onError(String message) {
                    FridgeUiUtils.showCustomToast(requireActivity(), message);
                }
            });
        }
    }

    private void deleteItems(List<FridgeItem> items) {
        if (getContext() == null || items == null || items.isEmpty()) return;

        int expiredCount = countExpiredItems(items);

        if (mode.isGuestMode(requireContext())) {
            itemList.removeAll(items);
            repo.saveLocal(requireContext(), itemList);
            sortAndRefresh();
            selection.exit(adapter, fab);

            ChecklistPrefs.markDeleteDone(
                    requireContext(),
                    PetPrefs.getLevel(requireContext())
            );

            giveExpiredDeleteExpByCount(expiredCount);

            FridgeUiUtils.showCustomToast(requireActivity(), "삭제되었습니다 (비회원 저장)");
            return;
        }

        final int total = items.size();
        final int[] done = {0};

        for (FridgeItem it : items) {
            FridgeRepository.DataCallback<Void> cb = new FridgeRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    done[0]++;

                    if (done[0] >= total) {
                        selection.exit(adapter, fab);

                        ChecklistPrefs.markDeleteDone(
                                requireContext(),
                                PetPrefs.getLevel(requireContext())
                        );

                        giveExpiredDeleteExpByCount(expiredCount);

                        FridgeUiUtils.showCustomToast(requireActivity(), "삭제되었습니다");
                        refreshAll();
                    }
                }

                @Override
                public void onError(String message) {
                    onSuccess(null);
                }
            };

            if (mode.isSharedFridgeMode(requireContext())) {
                long userId = mode.getUserIdSafe(requireContext());
                repo.deleteShared(userId, it, cb);
            } else {
                repo.deletePersonal(it, cb);
            }
        }
    }

    private int countExpiredItems(List<FridgeItem> items) {
        if (items == null || items.isEmpty()) return 0;

        int count = 0;
        for (FridgeItem item : items) {
            if (item != null && item.isExpired()) {
                count++;
            }
        }
        return count;
    }

    private void giveExpiredDeleteExpByCount(int expiredCount) {
        if (expiredCount <= 0 || !isAdded()) return;

        for (int i = 0; i < expiredCount; i++) {
            PetExpManager.giveExpiredDeleteExp(requireActivity());
        }
    }

    private void applyFridgeTitle() {
        if (tvTitle == null || getContext() == null) return;

        if (mode.isSharedFridgeMode(requireContext())) {
            String sharedName = SharedFridgePrefs.getFridgeName(requireContext());
            if (sharedName == null) sharedName = "";
            sharedName = sharedName.trim();

            long sharedId = SharedFridgePrefs.getFridgeId(requireContext());
            SharedFridgePrefs.PendingInvite pending =
                    SharedFridgePrefs.getPendingInvite(requireContext());

            String title;

            if (sharedId > 0) {
                title = sharedName.isEmpty() ? "공동 냉장고" : (sharedName + " 냉장고");
            } else if (pending != null && !TextUtils.isEmpty(pending.inviteCode)) {
                String n = pending.fridgeName == null ? "" : pending.fridgeName.trim();
                if (n.isEmpty()) n = "공동";
                title = (n + " 냉장고 (참여 필요)").replace("  ", " ");
            } else {
                title = "공동 냉장고";
            }

            tvTitle.setText(title);
            return;
        }

        tvTitle.setText(getPersonalFridgeTitle());
    }

    private String getPersonalFridgeTitle() {
        String nick = getNicknameForTitle();
        if (TextUtils.isEmpty(nick)) return "냉장고";
        return nick + " 냉장고";
    }

    private void showFridgeSwitchDialog() {
        if (getContext() == null) return;

        String current = mode.isSharedFridgeMode(requireContext()) ? "SHARED" : "PERSONAL";

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_fridge_switch, null);

        View layoutOptionPersonal = dialogView.findViewById(R.id.layout_option_personal);
        View layoutOptionShared = dialogView.findViewById(R.id.layout_option_shared);

        RadioButton rbPersonal = dialogView.findViewById(R.id.rb_personal);
        RadioButton rbShared = dialogView.findViewById(R.id.rb_shared);

        TextView btnClose = dialogView.findViewById(R.id.btn_close);
        TextView btnSwitch = dialogView.findViewById(R.id.btn_switch);

        final String[] selectedType = {"SHARED".equals(current) ? "SHARED" : "PERSONAL"};

        Runnable refreshSelectionUi = () -> {
            boolean personalSelected = "PERSONAL".equals(selectedType[0]);
            boolean sharedSelected = "SHARED".equals(selectedType[0]);

            rbPersonal.setChecked(personalSelected);
            rbShared.setChecked(sharedSelected);

            layoutOptionPersonal.setSelected(personalSelected);
            layoutOptionShared.setSelected(sharedSelected);
        };

        layoutOptionPersonal.setOnClickListener(v -> {
            selectedType[0] = "PERSONAL";
            refreshSelectionUi.run();
        });

        layoutOptionShared.setOnClickListener(v -> {
            selectedType[0] = "SHARED";
            refreshSelectionUi.run();
        });

        refreshSelectionUi.run();

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSwitch.setOnClickListener(v -> {
            if ("PERSONAL".equals(selectedType[0])) {
                OnboardingPrefs.saveFridgeType(requireContext(), "PERSONAL");
                applyFridgeTitle();
                refreshAll();
                dialog.dismiss();
                return;
            }

            long sharedId = SharedFridgePrefs.getFridgeId(requireContext());
            SharedFridgePrefs.PendingInvite pending =
                    SharedFridgePrefs.getPendingInvite(requireContext());

            if (sharedId <= 0 && (pending == null || TextUtils.isEmpty(pending.inviteCode))) {
                dialog.dismiss();

                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("공동 냉장고 없음")
                        .setMessage("공동 냉장고가 아직 생성/참여되지 않았습니다.\n공동 냉장고 설정을 열까요?")
                        .setPositiveButton("이동", (dd, ww2) -> showSharedFridgeStartDialog())
                        .setNegativeButton("취소", null)
                        .show();
                return;
            }

            OnboardingPrefs.saveFridgeType(requireContext(), "SHARED");

            if (sharedId <= 0 && pending != null && !TextUtils.isEmpty(pending.inviteCode)) {
                dialog.dismiss();

                try {
                    Intent i = new Intent(requireContext(), SharedFridgeManageActivity.class);
                    i.putExtra("pending_invite_code", pending.inviteCode);
                    i.putExtra("pending_fridge_name", pending.fridgeName);
                    i.putExtra("pending_expires_at", pending.expiresAt);
                    startActivity(i);
                } catch (Exception e) {
                    Intent i = new Intent(requireContext(), OnboardingActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
                return;
            }

            applyFridgeTitle();
            refreshAll();
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showSharedFridgeStartDialog() {
        if (getContext() == null) return;

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_shared_fridge, null, false);

        TextView btnClose = view.findViewById(R.id.btnClose);
        TextView btnCreate = view.findViewById(R.id.btnCreate);
        TextView btnJoin = view.findViewById(R.id.btnJoin);

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(view)
                        .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateSharedFridgeDialog();
        });

        btnJoin.setOnClickListener(v -> {
            dialog.dismiss();
            showJoinSharedFridgeDialog();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showCreateSharedFridgeDialog() {
        if (getContext() == null) return;

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_shared_fridge, null, false);

        EditText etFridgeName = view.findViewById(R.id.etFridgeName);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnCreate = view.findViewById(R.id.btnCreate);

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(view)
                        .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etFridgeName.getText() == null
                    ? ""
                    : etFridgeName.getText().toString().trim();

            SharedFridgeJoinRunner.createSharedFridge(requireContext(), name, () -> {
                dialog.dismiss();

                OnboardingPrefs.saveFridgeType(requireContext(), "SHARED");
                applyFridgeTitle();
                refreshAll();

                try {
                    startActivity(new Intent(requireContext(), SharedFridgeManageActivity.class));
                } catch (Exception ignored) {}
            });
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showJoinSharedFridgeDialog() {
        if (getContext() == null) return;

        EditText input = new EditText(requireContext());
        input.setHint("초대코드 입력");
        input.setSingleLine(true);

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("초대코드로 참여하기")
                        .setMessage("받은 초대코드를 입력해주세요.")
                        .setView(input)
                        .setNegativeButton("취소", null)
                        .setPositiveButton("참여", null)
                        .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        String code = input.getText() == null
                                ? ""
                                : input.getText().toString().trim();

                        SharedFridgeJoinRunner.joinSharedFridge(requireContext(), code, () -> {
                            dialog.dismiss();

                            OnboardingPrefs.saveFridgeType(requireContext(), "SHARED");
                            applyFridgeTitle();
                            refreshAll();

                            try {
                                startActivity(new Intent(requireContext(), SharedFridgeManageActivity.class));
                            } catch (Exception ignored) {}
                        });
                    });
        });

        dialog.show();
    }

    private String getNicknameForTitle() {
        if (getContext() == null) return "";

        try {
            String onb = OnboardingPrefs.getNickname(requireContext());
            if (onb != null) onb = onb.trim();
            if (!TextUtils.isEmpty(onb)) return onb;
        } catch (Exception ignored) {}

        try {
            Method m = AuthPrefs.class.getMethod("getNickname", android.content.Context.class);
            Object out = m.invoke(null, requireContext());

            if (out instanceof String) {
                String s = ((String) out).trim();
                if (!TextUtils.isEmpty(s)) return s;
            }
        } catch (Exception ignored) {}

        String nick = requireContext()
                .getSharedPreferences("AuthPrefs", android.content.Context.MODE_PRIVATE)
                .getString("user_nickname", "");

        return nick == null ? "" : nick.trim();
    }

    private String getMyNicknameSafe() {
        if (getContext() == null) return "";

        try {
            Method m = AuthPrefs.class.getMethod("getNickname", android.content.Context.class);
            Object out = m.invoke(null, requireContext());

            if (out instanceof String) {
                return ((String) out).trim();
            }
        } catch (Exception ignored) {}

        String nick = requireContext()
                .getSharedPreferences("AuthPrefs", android.content.Context.MODE_PRIVATE)
                .getString("user_nickname", "");

        return nick == null ? "" : nick.trim();
    }

    private FridgeItem buildItemFromAddResult(Intent data) {
        if (data == null) return null;

        String name = data.getStringExtra("name");
        String storage = data.getStringExtra("storage");

        double quantityDouble = data.getDoubleExtra("quantity_double", -1.0);
        int quantityInt = data.getIntExtra("quantity", 1);
        double quantity = (quantityDouble >= 0) ? quantityDouble : (double) quantityInt;

        int iconResId = data.getIntExtra("iconResId", 0);
        String iconKey = data.getStringExtra("iconKey");
        String expiryDate = data.getStringExtra("expiryDate");
        boolean expiryManuallySet = data.getBooleanExtra("expiryManuallySet", false);
        boolean isDirectAdd = data.getBooleanExtra("isDirectAdd", false);
        String unit = data.getStringExtra("unit");
        String addedByNickname = data.getStringExtra("addedByNickname");

        if (unit == null || unit.trim().isEmpty()) unit = "개";
        if (name == null) name = "";
        if (storage == null) storage = "기타";
        if (expiryDate == null) expiryDate = "";
        if (iconKey == null) iconKey = "";

        iconKey = iconKey.trim();

        if (isDirectAdd && !expiryManuallySet) {
            expiryDate = "";
        }

        if (addedByNickname == null) addedByNickname = "";
        addedByNickname = addedByNickname.trim();
        if (addedByNickname.isEmpty()) addedByNickname = "나";

        FridgeItem newItem = new FridgeItem(
                name,
                storage,
                quantity,
                false,
                iconResId,
                expiryDate
        );

        newItem.setUnit(unit);
        newItem.setExpiryManuallySet(expiryManuallySet);
        FridgeUiUtils.setAddedByNicknameSafe(newItem, addedByNickname);

        if (!TextUtils.isEmpty(iconKey)) {
            newItem.setIconName(iconKey);
        } else if (iconResId != 0) {
            String resName = ResourceNameHelper.getNameFromResId(iconResId);
            if (!TextUtils.isEmpty(resName)) {
                newItem.setIconName(resName);
            }
        }

        return newItem;
    }
}