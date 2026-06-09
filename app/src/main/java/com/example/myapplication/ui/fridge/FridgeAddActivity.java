package com.namgyun.tamakitchen.ui.fridge;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.namgyun.tamakitchen.ui.shopping.IconAdapter;
import com.namgyun.tamakitchen.ui.shopping.IconCatalog;
import com.namgyun.tamakitchen.ui.shopping.IconItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FridgeAddActivity extends AppCompatActivity {

    private EditText etSearch;
    private TextInputLayout inputLayout;

    private RecyclerView rvIcons;
    private IconAdapter iconAdapter;

    private ImageView ivSelectedProduct;
    private EditText etSelectedProduct;

    private ChipGroup chipGroupStorage;

    private TextView btnDecrease, btnIncrease;
    private TextView tvQuantity;

    private double quantity = 1.0;

    private TextView tvUnit;
    private String selectedUnit = "개";

    private EditText etExpiryDate;
    private ImageView btnCalendar;
    private String selectedExpiryDate = "";
    private boolean expiryManuallySet = false;

    private Button btnAdd;
    private TabLayout tabCategories;

    private List<IconItem> allIconsList;
    private List<IconItem> filteredIconsList;

    private View layoutSelectedItem;
    private LinearLayout layoutAddButton;

    private IconItem selectedIcon;

    private Button btnDirectAdd;

    private ImageButton btnClosePanel;

    private static final String PREFS_NAME = "IconClickPrefs";
    private static final String CLICK_COUNT_PREFIX = "click_count_";
    private SharedPreferences clickPrefs;

    private boolean sortDirty = false;

    private boolean isDirectAddMode = false;

    private static final char HANGUL_BEGIN_UNICODE = 44032;
    private static final char HANGUL_END_UNICODE = 55203;
    private static final char HANGUL_BASE_UNIT = 588;

    private static final char[] INITIAL_SOUND = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ',
            'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ',
            'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private final List<String> unitList = Arrays.asList(
            "개", "병", "박스", "캔", "kg", "L",
            "팩", "봉", "장", "컵", "g", "ml"
    );
    private int unitPageIndex = 0;

    private static final DecimalFormat QTY_FMT = new DecimalFormat("0.########");

    private String formatQty(double v) {
        return QTY_FMT.format(v);
    }

    private String getInitialSound(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray()) {
            if (ch >= HANGUL_BEGIN_UNICODE && ch <= HANGUL_END_UNICODE) {
                int index = (ch - HANGUL_BEGIN_UNICODE) / HANGUL_BASE_UNIT;
                if (index >= 0 && index < INITIAL_SOUND.length) {
                    sb.append(INITIAL_SOUND[index]);
                } else {
                    sb.append(ch);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private boolean isInitialSound(char ch) {
        for (char initial : INITIAL_SOUND) {
            if (ch == initial) return true;
        }
        return false;
    }

    private boolean containsOnlyInitials(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        for (char ch : str.toCharArray()) {
            if (!isInitialSound(ch) && !Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.getDefault()).trim();
    }

    private boolean containsText(String name, String query) {
        return safeLower(name).contains(safeLower(query));
    }

    private boolean startsWithText(String name, String query) {
        return safeLower(name).startsWith(safeLower(query));
    }

    private boolean containsInitialSound(String name, String query) {
        if (name == null || query == null) return false;
        String nameInitial = getInitialSound(name);
        String q = query.trim();
        if (q.isEmpty()) return false;
        return nameInitial.contains(q);
    }

    private int getSearchPriority(String name, String query) {
        if (query == null || query.trim().isEmpty()) return 999;

        String trimmedQuery = query.trim();

        if (containsOnlyInitials(trimmedQuery)) {
            if (startsWithText(name, trimmedQuery)) return 0;
            if (containsText(name, trimmedQuery)) return 1;
            if (containsInitialSound(name, trimmedQuery)) return 2;
            return 999;
        }

        if (startsWithText(name, trimmedQuery)) return 0;
        if (containsText(name, trimmedQuery)) return 1;

        return 999;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_add);

        clickPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        setupIconRecyclerView();
        setupTabCategories();
        setupEventListeners();

        tvUnit.setText(selectedUnit);
        tvQuantity.setText(formatQty(quantity));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sortDirty && allIconsList != null) {
            sortIconsByClickCount(allIconsList);
            filterIcons(etSearch != null ? etSearch.getText().toString() : "", getCurrentCategory());
            sortDirty = false;
        }
    }

    private void initViews() {
        inputLayout = findViewById(R.id.text_input_layout);
        etSearch = findViewById(R.id.et_search);

        rvIcons = findViewById(R.id.rv_icons);
        ivSelectedProduct = findViewById(R.id.iv_selected_product);
        etSelectedProduct = findViewById(R.id.et_selected_product);

        chipGroupStorage = findViewById(R.id.chip_group_storage);

        btnDecrease = findViewById(R.id.btn_decrease);
        btnIncrease = findViewById(R.id.btn_increase);
        tvQuantity = findViewById(R.id.tv_quantity);

        etExpiryDate = findViewById(R.id.et_expiry_date);
        btnCalendar = findViewById(R.id.btn_calendar);

        btnAdd = findViewById(R.id.btn_add);
        tabCategories = findViewById(R.id.tab_categories);

        layoutSelectedItem = findViewById(R.id.layout_selected_item);
        layoutAddButton = findViewById(R.id.layout_add_button);

        tvUnit = findViewById(R.id.tv_unit);

        btnDirectAdd = findViewById(R.id.btn_direct_add);
        btnClosePanel = findViewById(R.id.btn_close_panel);

        if (layoutSelectedItem != null) layoutSelectedItem.setVisibility(View.GONE);
        if (layoutAddButton != null) layoutAddButton.setVisibility(View.GONE);
        if (btnAdd != null) btnAdd.setEnabled(false);
    }

    private void setupEventListeners() {
        inputLayout.setEndIconOnClickListener(v -> {
            etSearch.setText("");
            filterIcons("", getCurrentCategory());
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterIcons(s.toString(), getCurrentCategory());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDecrease.setOnClickListener(v -> {
            double next = quantity - 1.0;
            if (next < 0.1) next = 0.1;
            quantity = next;
            tvQuantity.setText(formatQty(quantity));
        });

        btnIncrease.setOnClickListener(v -> {
            double next = quantity + 1.0;
            if (next > 9999) next = 9999;
            quantity = next;
            tvQuantity.setText(formatQty(quantity));
        });

        tvQuantity.setOnClickListener(v -> showQuantityInputDialog());

        btnCalendar.setOnClickListener(v -> showDatePicker());
        etExpiryDate.setOnClickListener(v -> showDatePicker());

        tvUnit.setOnClickListener(v -> showUnitPickerDialog());

        if (btnClosePanel != null) {
            btnClosePanel.setOnClickListener(v -> hideSelectedPanel(true));
        }

        btnDirectAdd.setOnClickListener(v -> {
            isDirectAddMode = true;
            selectedIcon = new IconItem(R.drawable.ic_custom_item, "직접 추가", "기타", "");

            showSelectedPanel();

            ivSelectedProduct.setImageResource(selectedIcon.getResId());
            etSelectedProduct.setText("");
            etSelectedProduct.requestFocus();

            chipGroupStorage.check(R.id.chip_storage_fridge);

            quantity = 1.0;
            tvQuantity.setText(formatQty(quantity));

            selectedUnit = "개";
            tvUnit.setText(selectedUnit);

            selectedExpiryDate = "";
            expiryManuallySet = false;
            etExpiryDate.setText("");
        });

        btnAdd.setOnClickListener(v -> {
            if (selectedIcon == null) return;

            String productName = etSelectedProduct.getText().toString().trim();
            if (productName.isEmpty()) productName = selectedIcon.getName();

            int checkedId = chipGroupStorage.getCheckedChipId();
            String storage = "기타";
            if (checkedId != View.NO_ID) {
                Chip checkedChip = chipGroupStorage.findViewById(checkedId);
                if (checkedChip != null) storage = checkedChip.getText().toString();
            }

            String unit = selectedUnit;
            if (unit == null || unit.isEmpty()) unit = "개";

            String iconKey = selectedIcon.getRawKey();
            if (iconKey == null) iconKey = "";
            iconKey = iconKey.trim();

            if (selectedExpiryDate == null) selectedExpiryDate = "";

            // 직접 추가일 때는 자동 유통기한 절대 넣지 않음
            if (!isDirectAddMode && selectedExpiryDate.trim().isEmpty()) {
                String autoIso = ExpiryDefaultUtil.computeDefaultExpiryDateIso(productName, storage);
                if (autoIso != null && !autoIso.trim().isEmpty()) {
                    selectedExpiryDate = autoIso.trim();
                    expiryManuallySet = false;

                    try {
                        SimpleDateFormat fmt = new SimpleDateFormat("MM월 dd일", Locale.getDefault());
                        SimpleDateFormat src = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        etExpiryDate.setText(fmt.format(src.parse(selectedExpiryDate)));
                    } catch (Exception ignored) {
                        etExpiryDate.setText(selectedExpiryDate);
                    }
                }
            }

            String addedByNickname = OnboardingPrefs.getNickname(this);
            if (addedByNickname == null || addedByNickname.trim().isEmpty()) {
                addedByNickname = "사용자";
            } else {
                addedByNickname = addedByNickname.trim();
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("name", productName);
            resultIntent.putExtra("quantity_double", quantity);
            resultIntent.putExtra("expiryDate", selectedExpiryDate);
            resultIntent.putExtra("expiryManuallySet", expiryManuallySet);
            resultIntent.putExtra("iconResId", selectedIcon.getResId());
            resultIntent.putExtra("iconKey", iconKey);
            resultIntent.putExtra("storage", storage);
            resultIntent.putExtra("unit", unit);
            resultIntent.putExtra("addedByNickname", addedByNickname);
            resultIntent.putExtra("isDirectAdd", isDirectAddMode);

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupIconRecyclerView() {
        allIconsList = new ArrayList<>(IconCatalog.getAllIcons());

        sortIconsByClickCount(allIconsList);

        filteredIconsList = new ArrayList<>(allIconsList);

        rvIcons.setLayoutManager(new GridLayoutManager(this, 4));

        iconAdapter = new IconAdapter(filteredIconsList, icon -> {
            if (icon == null) return;

            incrementClickCount(icon);
            sortDirty = true;

            if (selectedIcon != null && selectedIcon == icon) {
                hideSelectedPanel(true);
                return;
            }

            isDirectAddMode = false;
            selectedIcon = icon;

            ivSelectedProduct.setImageResource(icon.getResId());
            etSelectedProduct.setText(icon.getName());

            showSelectedPanel();

            selectStorageChip(icon.getCategory());
            btnAdd.setEnabled(true);

            selectedExpiryDate = "";
            expiryManuallySet = false;
            etExpiryDate.setText("");
        });

        rvIcons.setAdapter(iconAdapter);
    }

    private void showSelectedPanel() {
        if (layoutSelectedItem != null) layoutSelectedItem.setVisibility(View.VISIBLE);
        if (layoutAddButton != null) layoutAddButton.setVisibility(View.VISIBLE);
        if (btnAdd != null) btnAdd.setEnabled(true);
    }

    private void hideSelectedPanel(boolean animate) {
        selectedIcon = null;
        isDirectAddMode = false;

        selectedExpiryDate = "";
        expiryManuallySet = false;
        if (etExpiryDate != null) etExpiryDate.setText("");

        quantity = 1.0;
        if (tvQuantity != null) tvQuantity.setText(formatQty(quantity));

        selectedUnit = "개";
        if (tvUnit != null) tvUnit.setText(selectedUnit);

        if (btnAdd != null) btnAdd.setEnabled(false);

        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }

        if (layoutAddButton != null) layoutAddButton.setVisibility(View.GONE);

        if (layoutSelectedItem == null) return;

        if (!animate) {
            layoutSelectedItem.setVisibility(View.GONE);
            return;
        }

        layoutSelectedItem.animate()
                .translationY(layoutSelectedItem.getHeight() > 0 ? layoutSelectedItem.getHeight() : 400f)
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutSelectedItem.setTranslationY(0f);
                    layoutSelectedItem.setAlpha(1f);
                    layoutSelectedItem.setVisibility(View.GONE);
                })
                .start();
    }

    private void incrementClickCount(IconItem icon) {
        if (icon == null) return;

        String unique = icon.getRawKey();
        if (unique == null || unique.trim().isEmpty()) unique = icon.getName();
        if (unique == null) unique = "";

        String key = CLICK_COUNT_PREFIX + unique;
        int currentCount = clickPrefs.getInt(key, 0);
        clickPrefs.edit().putInt(key, currentCount + 1).apply();
    }

    private void sortIconsByClickCount(List<IconItem> list) {
        if (list == null || list.size() <= 1) return;

        list.sort((icon1, icon2) -> {
            String u1 = (icon1 != null) ? icon1.getRawKey() : "";
            if (u1 == null || u1.trim().isEmpty()) u1 = (icon1 != null ? icon1.getName() : "");

            String u2 = (icon2 != null) ? icon2.getRawKey() : "";
            if (u2 == null || u2.trim().isEmpty()) u2 = (icon2 != null ? icon2.getName() : "");

            int count1 = clickPrefs.getInt(CLICK_COUNT_PREFIX + u1, 0);
            int count2 = clickPrefs.getInt(CLICK_COUNT_PREFIX + u2, 0);

            int c = Integer.compare(count2, count1);
            if (c != 0) return c;

            String n1 = (icon1 != null && icon1.getName() != null) ? icon1.getName() : "";
            String n2 = (icon2 != null && icon2.getName() != null) ? icon2.getName() : "";

            c = n1.compareTo(n2);
            if (c != 0) return c;

            int r1 = (icon1 != null) ? icon1.getResId() : Integer.MAX_VALUE;
            int r2 = (icon2 != null) ? icon2.getResId() : Integer.MAX_VALUE;
            return Integer.compare(r1, r2);
        });
    }

    private void setupTabCategories() {
        String[] categories = {
                "전체",
                "채소",
                "육류",
                "과일",
                "해산물",
                "버섯류",
                "견과류",
                "유제품",
                "냉동식품",
                "기타"
        };

        tabCategories.removeAllTabs();

        for (String category : categories) {
            tabCategories.addTab(tabCategories.newTab().setText(category));
        }

        tabCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                hideSelectedPanel(false);
                filterIcons(etSearch.getText().toString(), tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day);

                    SimpleDateFormat fmt = new SimpleDateFormat("MM월 dd일", Locale.getDefault());
                    etExpiryDate.setText(fmt.format(selected.getTime()));

                    selectedExpiryDate =
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(selected.getTime());

                    expiryManuallySet = true;
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private String getCurrentCategory() {
        TabLayout.Tab selectedTab = tabCategories.getTabAt(tabCategories.getSelectedTabPosition());
        return selectedTab != null ? selectedTab.getText().toString() : "전체";
    }

    private void filterIcons(String searchText, String category) {
        if (allIconsList == null || iconAdapter == null || filteredIconsList == null) return;

        filteredIconsList.clear();

        String query = searchText == null ? "" : searchText.trim();
        boolean ignoreCategory = !query.isEmpty();

        List<SearchMatchItem> matches = new ArrayList<>();

        for (IconItem icon : allIconsList) {
            if (icon == null) continue;

            String name = icon.getName() != null ? icon.getName() : "";
            String iconCategory = icon.getCategory() != null ? icon.getCategory() : "";

            boolean matchesCategory = category.equals("전체") || iconCategory.equals(category);
            if (ignoreCategory) {
                matchesCategory = true;
            }

            if (!matchesCategory) continue;

            if (query.isEmpty()) {
                matches.add(new SearchMatchItem(icon, 999));
                continue;
            }

            int priority = getSearchPriority(name, query);
            if (priority != 999) {
                matches.add(new SearchMatchItem(icon, priority));
            }
        }

        matches.sort((a, b) -> {
            int c = Integer.compare(a.priority, b.priority);
            if (c != 0) return c;

            String nameA = a.icon != null && a.icon.getName() != null ? a.icon.getName() : "";
            String nameB = b.icon != null && b.icon.getName() != null ? b.icon.getName() : "";
            c = nameA.compareTo(nameB);
            if (c != 0) return c;

            int resA = a.icon != null ? a.icon.getResId() : Integer.MAX_VALUE;
            int resB = b.icon != null ? b.icon.getResId() : Integer.MAX_VALUE;
            return Integer.compare(resA, resB);
        });

        for (SearchMatchItem item : matches) {
            filteredIconsList.add(item.icon);
        }

        iconAdapter.notifyDataSetChanged();
    }

    private void selectStorageChip(String storage) {
        if (chipGroupStorage == null) return;

        chipGroupStorage.clearCheck();
        switch (storage) {
            case "냉장":
                chipGroupStorage.check(R.id.chip_storage_fridge);
                break;
            case "냉동":
                chipGroupStorage.check(R.id.chip_storage_freezer);
                break;
            case "실온":
                chipGroupStorage.check(R.id.chip_storage_room);
                break;
            default:
                chipGroupStorage.check(R.id.chip_storage_etc);
                break;
        }
    }

    private void showQuantityInputDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_input, null);

        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnOk = dialogView.findViewById(R.id.btn_ok);
        EditText etQty = dialogView.findViewById(R.id.et_quantity);

        etQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etQty.setText(formatQty(quantity));
        etQty.setSelection(etQty.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String raw = etQty.getText().toString().trim();
            if (raw.isEmpty()) {
                dialog.dismiss();
                return;
            }
            try {
                double val = Double.parseDouble(raw);

                if (val < 0.1) val = 0.1;
                if (val > 9999) val = 9999;

                quantity = val;
                tvQuantity.setText(formatQty(quantity));
            } catch (NumberFormatException ignored) {
            }
            dialog.dismiss();
        });

        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        etQty.requestFocus();
    }

    private void showUnitPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_unit_picker, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvPage = dialogView.findViewById(R.id.tv_page);

        TextView btnUnit1 = dialogView.findViewById(R.id.btn_unit_1);
        TextView btnUnit2 = dialogView.findViewById(R.id.btn_unit_2);
        TextView btnUnit3 = dialogView.findViewById(R.id.btn_unit_3);
        TextView btnUnit4 = dialogView.findViewById(R.id.btn_unit_4);
        TextView btnUnit5 = dialogView.findViewById(R.id.btn_unit_5);
        TextView btnUnit6 = dialogView.findViewById(R.id.btn_unit_6);

        TextView btnPrev = dialogView.findViewById(R.id.btn_prev);
        TextView btnNext = dialogView.findViewById(R.id.btn_next);
        TextView btnClose = dialogView.findViewById(R.id.btn_close);

        List<TextView> unitButtons = Arrays.asList(
                btnUnit1, btnUnit2, btnUnit3,
                btnUnit4, btnUnit5, btnUnit6
        );

        int pageSize = 6;
        int totalPages = (int) Math.ceil(unitList.size() / (double) pageSize);

        Runnable updatePage = () -> {
            int start = unitPageIndex * pageSize;
            int end = Math.min(start + pageSize, unitList.size());

            tvPage.setText((unitPageIndex + 1) + " / " + totalPages);

            int btnIndex = 0;
            for (int i = start; i < end; i++) {
                String unit = unitList.get(i);
                TextView btn = unitButtons.get(btnIndex);
                btn.setVisibility(View.VISIBLE);
                btn.setText(unit);
                btnIndex++;
            }

            while (btnIndex < unitButtons.size()) {
                unitButtons.get(btnIndex).setVisibility(View.INVISIBLE);
                unitButtons.get(btnIndex).setText("");
                btnIndex++;
            }

            btnPrev.setEnabled(unitPageIndex > 0);
            btnNext.setEnabled(unitPageIndex < totalPages - 1);

            btnPrev.setAlpha(unitPageIndex > 0 ? 1.0f : 0.45f);
            btnNext.setAlpha(unitPageIndex < totalPages - 1 ? 1.0f : 0.45f);
        };

        View.OnClickListener unitClickListener = v -> {
            TextView tv = (TextView) v;
            String unit = tv.getText().toString().trim();

            if (!unit.isEmpty()) {
                selectedUnit = unit;
                tvUnit.setText(selectedUnit);
                dialog.dismiss();
            }
        };

        for (TextView tv : unitButtons) {
            tv.setOnClickListener(unitClickListener);
        }

        btnPrev.setOnClickListener(v -> {
            if (unitPageIndex > 0) {
                unitPageIndex--;
                updatePage.run();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (unitPageIndex < totalPages - 1) {
                unitPageIndex++;
                updatePage.run();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        updatePage.run();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private static class SearchMatchItem {
        final IconItem icon;
        final int priority;

        SearchMatchItem(IconItem icon, int priority) {
            this.icon = icon;
            this.priority = priority;
        }
    }
}