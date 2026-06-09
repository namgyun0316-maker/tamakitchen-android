package com.namgyun.tamakitchen.ui.recipe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.network.ImageUploadResponse;
import com.namgyun.tamakitchen.network.RecipeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.pet.PetExpManager;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.checklist.ChecklistPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.model.AspectRatio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
public class RecipeWriteActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE = "extra_recipe";

    private static final String TAG = "RecipeWriteActivity";

    private EditText etName, etSummary, etIngredients;
    private EditText etOptionalIngredients;

    private RecyclerView rvSteps;
    private Button btnAddStep, btnSubmit;
    private Button btnPrevStep;

    private ImageView ivMainImage;
    private Button btnSelectMainImage;

    private Button btnKorean, btnWestern, btnJapanese, btnChinese, btnEtc;
    private Button btnDessert, btnDiet;

    private String selectedCategory = null;

    private RecipeStepAdapter stepAdapter;

    private String mainImageUriString = null;

    @Nullable
    private String mainThumbnailUrl = null;

    private enum TargetType { MAIN, STEP }

    private TargetType pendingTarget = null;
    private int pendingStepIndex = -1;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    @Nullable
    private RecipeResponse editingRecipe = null;

    @Nullable
    private Long editingRecipeId = null;

    private boolean isNewMainImage = false;

    private EditText etSubOriginal;
    private EditText etSubAlternatives;
    private Button btnAddSubstitute;
    private RecyclerView rvSubstitutes;
    private SubstituteAdapter substituteAdapter;

    private EditText etDetailName;
    private EditText etDetailQtyText;
    private RadioGroup rgDetailType;
    private RadioButton rbDetailRequired;
    private RadioButton rbDetailOptional;
    private Button btnAddDetailIngredient;
    private RecyclerView rvDetailIngredients;
    private DetailIngredientAdapter detailIngredientAdapter;
    private ScrollView recipeWriteScroll;
    private long lastAddDetailClickMs = 0L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        setContentView(R.layout.activity_recipe_write);

        recipeWriteScroll = findViewById(R.id.recipeWriteScroll);
        View rootScroll = findViewById(R.id.recipeWriteScroll);
        setupKeyboardPadding(rootScroll);
        etName = findViewById(R.id.etName);
        etSummary = findViewById(R.id.etSummary);

        etIngredients = findViewById(R.id.etIngredients);
        etOptionalIngredients = findViewById(R.id.etOptionalIngredients);

        ivMainImage = findViewById(R.id.ivMainImage);
        btnSelectMainImage = findViewById(R.id.btnSelectMainImage);

        rvSteps = findViewById(R.id.rvSteps);
        btnAddStep = findViewById(R.id.btnAddStep);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnPrevStep = findViewById(R.id.btnPrevStep);

        btnKorean = findViewById(R.id.btnKorean);
        btnWestern = findViewById(R.id.btnWestern);
        btnJapanese = findViewById(R.id.btnJapanese);
        btnChinese = findViewById(R.id.btnChinese);
        btnEtc = findViewById(R.id.btnEtc);

        btnDessert = findViewById(R.id.btnDessert);
        btnDiet = findViewById(R.id.btnDiet);

        etSubOriginal = findViewById(R.id.etSubOriginal);
        etSubAlternatives = findViewById(R.id.etSubAlternatives);
        btnAddSubstitute = findViewById(R.id.btnAddSubstitute);
        rvSubstitutes = findViewById(R.id.rvSubstitutes);

        etDetailName = findViewById(R.id.etDetailName);
        etDetailQtyText = findViewById(R.id.etDetailQtyText);
        rgDetailType = findViewById(R.id.rgDetailType);
        rbDetailRequired = findViewById(R.id.rbDetailRequired);
        rbDetailOptional = findViewById(R.id.rbDetailOptional);
        btnAddDetailIngredient = findViewById(R.id.btnAddDetailIngredient);
        rvDetailIngredients = findViewById(R.id.rvDetailIngredients);

        setupCategoryButtons();
        setupImagePickers();
        setupStepAdapter();

        setupSubstituteAdapter();
        setupDetailIngredientAdapter();

        editingRecipe = (RecipeResponse) getIntent().getSerializableExtra(EXTRA_RECIPE);
        if (editingRecipe != null) {
            editingRecipeId = editingRecipe.getId();
            fillFormForEdit(editingRecipe);
            btnSubmit.setText("레시피 수정하기");
        }

        btnAddStep.setOnClickListener(v -> {
            if (stepAdapter == null) return;
            stepAdapter.goToNextStepOrAdd();
            Log.d("STEP", "현재 단계 = " + stepAdapter.getCurrentStepNumber());
        });

        btnPrevStep.setOnClickListener(v -> {
            if (stepAdapter == null) return;
            stepAdapter.goToPrevStep();
            Log.d("STEP", "현재 단계 = " + stepAdapter.getCurrentStepNumber());
        });

        btnSelectMainImage.setOnClickListener(v -> {
            pendingTarget = TargetType.MAIN;
            pendingStepIndex = -1;
            pickImageLauncher.launch("image/*");
        });

        btnAddSubstitute.setOnClickListener(v -> onClickAddSubstitute());

        btnAddDetailIngredient.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastAddDetailClickMs < 300) return;
            lastAddDetailClickMs = now;
            onClickAddDetailIngredient();
        });

        btnSubmit.setOnClickListener(v -> submitRecipe());
        setupAutoScrollOnFocus();
    }
    private void setupAutoScrollOnFocus() {
        setupFocusScroll(etName);
        setupFocusScroll(etSummary);
        setupFocusScroll(etIngredients);
        setupFocusScroll(etOptionalIngredients);
        setupFocusScroll(etSubOriginal);
        setupFocusScroll(etSubAlternatives);
        setupFocusScroll(etDetailName);
        setupFocusScroll(etDetailQtyText);
    }

    private void setupFocusScroll(View targetView) {
        if (targetView == null || recipeWriteScroll == null) return;

        targetView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) return;

            v.postDelayed(() -> {
                int[] location = new int[2];
                v.getLocationOnScreen(location);

                recipeWriteScroll.smoothScrollTo(
                        0,
                        recipeWriteScroll.getScrollY() + location[1] - dpToPx(180)
                );
            }, 300);
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupKeyboardPadding(View rootView) {
        if (rootView == null) return;

        final int baseBottomPadding = dpToPx(120);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            int bottomPadding = keyboardVisible
                    ? imeInsets.bottom + dpToPx(24)
                    : baseBottomPadding + navInsets.bottom;

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottomPadding
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(rootView);
    }

    private String resolveAnyImageUrl(String raw) {
        if (raw == null) return null;

        String url = raw.trim();
        if (url.isEmpty()) return null;

        if (url.startsWith("content://")) {
            if (url.startsWith("content://media/picker_get_content")) return null;
            return url;
        }

        if (url.startsWith("/uploads")) {
            return joinBaseUrl(FridgeApi.BASE_URL, url);
        }

        if (url.startsWith("uploads/")) {
            return joinBaseUrl(FridgeApi.BASE_URL, "/" + url);
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (url.contains("/uploads/")) {
                return replaceHostWithBase(url, FridgeApi.BASE_URL);
            }
        }

        return url;
    }

    private String joinBaseUrl(String base, String pathStartingWithSlash) {
        if (TextUtils.isEmpty(base)) return pathStartingWithSlash;
        String b = base.trim();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + pathStartingWithSlash;
    }

    private String replaceHostWithBase(String originalUrl, String baseUrl) {
        try {
            Uri o = Uri.parse(originalUrl);
            Uri b = Uri.parse(baseUrl);

            String scheme = (b.getScheme() != null) ? b.getScheme() : o.getScheme();
            String host = b.getHost();
            int port = (b.getPort() != -1) ? b.getPort() : o.getPort();

            if (host == null) return originalUrl;

            Uri.Builder nb = new Uri.Builder()
                    .scheme(scheme)
                    .encodedAuthority(port == -1 ? host : (host + ":" + port))
                    .encodedPath(o.getEncodedPath());

            if (o.getEncodedQuery() != null) nb.encodedQuery(o.getEncodedQuery());

            return nb.build().toString();

        } catch (Exception e) {
            return originalUrl;
        }
    }

    private String deriveThumbnailUrlFromMain(String rawMainUrl) {
        String main = resolveAnyImageUrl(rawMainUrl);
        if (TextUtils.isEmpty(main)) return null;

        if (main.contains("_thumb.webp")) return main;

        if (main.endsWith(".webp")) {
            return main.substring(0, main.length() - ".webp".length()) + "_thumb.webp";
        }

        return null;
    }

    private static class DetailIngredientRow {
        String name;
        String quantityText;
        String type;

        DetailIngredientRow(String name, String quantityText, String type) {
            this.name = name;
            this.quantityText = quantityText;
            this.type = type;
        }
    }

    private static class DetailIngredientAdapter extends RecyclerView.Adapter<DetailIngredientAdapter.VH> {

        interface OnRemove {
            void onRemove(int position);
        }

        private final List<DetailIngredientRow> items = new ArrayList<>();
        private final OnRemove onRemove;

        DetailIngredientAdapter(OnRemove onRemove) {
            this.onRemove = onRemove;
        }

        void setItems(List<DetailIngredientRow> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        void addItem(DetailIngredientRow row) {
            items.add(row);
            notifyItemInserted(items.size() - 1);
        }

        void removeAt(int position) {
            if (position < 0 || position >= items.size()) return;
            items.remove(position);
            notifyItemRemoved(position);
        }

        List<DetailIngredientRow> getItems() {
            return items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_detail_ingredient_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DetailIngredientRow row = items.get(position);

            h.tvName.setText(row.name);

            String qty = row.quantityText == null ? "" : row.quantityText.trim();
            h.tvQty.setText(TextUtils.isEmpty(qty) ? "-" : qty);

            h.tvType.setText("REQUIRED".equals(row.type) ? "필수" : "선택");

            h.btnRemove.setOnClickListener(v -> {
                if (onRemove == null) return;

                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                onRemove.onRemove(pos);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvQty, tvType;
            Button btnRemove;

            VH(@NonNull View itemView) {
                super(itemView);

                tvName = itemView.findViewById(R.id.tvRowName);
                tvQty = itemView.findViewById(R.id.tvRowQty);
                tvType = itemView.findViewById(R.id.tvRowType);
                btnRemove = itemView.findViewById(R.id.btnRowRemove);
            }
        }
    }

    private void setupDetailIngredientAdapter() {
        if (rvDetailIngredients == null) {
            Log.e("DETAIL", "rvDetailIngredients is null. 레이아웃 id 확인!");
            return;
        }

        detailIngredientAdapter = new DetailIngredientAdapter(position -> {
            if (detailIngredientAdapter != null) {
                detailIngredientAdapter.removeAt(position);
            }
        });

        rvDetailIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvDetailIngredients.setAdapter(detailIngredientAdapter);
        rvDetailIngredients.setNestedScrollingEnabled(false);

        if (rbDetailRequired != null) {
            rbDetailRequired.setChecked(true);
        }
    }

    private void onClickAddDetailIngredient() {
        if (detailIngredientAdapter == null
                || etDetailName == null
                || etDetailQtyText == null
                || rgDetailType == null) {

            Log.e("DETAIL", "상세 재료 UI가 null. layout id 확인 필요");
            AppToast.show(this, "상세 재료 UI가 준비되지 않았습니다. 레이아웃을 확인하세요.");
            return;
        }

        String n = etDetailName.getText().toString().trim();
        String q = etDetailQtyText.getText().toString().trim();

        if (TextUtils.isEmpty(n)) {
            AppToast.show(this, "재료명을 입력하세요.");
            return;
        }

        String type = "REQUIRED";
        int checkedId = rgDetailType.getCheckedRadioButtonId();

        if (checkedId == R.id.rbDetailOptional) {
            type = "OPTIONAL";
        }

        detailIngredientAdapter.addItem(new DetailIngredientRow(n, q, type));

        etDetailName.setText("");
        etDetailQtyText.setText("");

        if (rbDetailRequired != null) {
            rbDetailRequired.setChecked(true);
        }

        AppToast.show(this, "상세 재료가 추가되었습니다.");
    }

    private List<IngredientItemRequest> buildIngredientItemsRequest() {
        List<IngredientItemRequest> out = new ArrayList<>();

        if (detailIngredientAdapter == null) return out;

        for (DetailIngredientRow row : detailIngredientAdapter.getItems()) {
            if (row == null) continue;

            String name = row.name == null ? "" : row.name.trim();
            if (name.isEmpty()) continue;

            String qty = row.quantityText == null ? "" : row.quantityText.trim();
            String type = row.type == null ? "REQUIRED" : row.type;

            IngredientItemRequest req = new IngredientItemRequest();
            req.setName(name);
            req.setType(type);
            req.setQuantityText(qty);
            req.setAmount(null);
            req.setUnit(null);
            req.setSubstituteFor(null);

            out.add(req);
        }

        return out;
    }

    private void setupSubstituteAdapter() {
        if (rvSubstitutes == null) return;

        substituteAdapter = new SubstituteAdapter(position -> substituteAdapter.removeAt(position));
        rvSubstitutes.setLayoutManager(new LinearLayoutManager(this));
        rvSubstitutes.setAdapter(substituteAdapter);
        rvSubstitutes.setNestedScrollingEnabled(false);
    }

    private void onClickAddSubstitute() {
        String original = etSubOriginal != null
                ? etSubOriginal.getText().toString().trim()
                : "";

        String alternativesRaw = etSubAlternatives != null
                ? etSubAlternatives.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(original)) {
            AppToast.show(this, "대표재료를 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(alternativesRaw)) {
            AppToast.show(this, "대체재료를 입력하세요.");
            return;
        }

        List<String> alts = new ArrayList<>();

        String[] parts = alternativesRaw.split(",");
        for (String p : parts) {
            if (p == null) continue;

            String t = p.trim();
            if (!t.isEmpty()) {
                alts.add(t);
            }
        }

        if (alts.isEmpty()) {
            AppToast.show(this, "대체재료를 올바르게 입력하세요. (예: 물엿, 올리고당)");
            return;
        }

        mergeOrAddSubstitute(original, alts);

        if (etSubOriginal != null) {
            etSubOriginal.setText("");
        }

        if (etSubAlternatives != null) {
            etSubAlternatives.setText("");
        }

        AppToast.show(this, "대체 가능 재료가 추가되었습니다.");
    }

    private void mergeOrAddSubstitute(String original, List<String> newAlts) {
        if (substituteAdapter == null) return;

        List<SubstituteItem> list = substituteAdapter.getItems();

        for (int i = 0; i < list.size(); i++) {
            SubstituteItem it = list.get(i);

            if (it == null) continue;

            if (original.equals(it.getOriginal())) {
                List<String> merged = new ArrayList<>(it.getAlternatives());

                for (String a : newAlts) {
                    if (!merged.contains(a)) {
                        merged.add(a);
                    }
                }

                it.setAlternatives(merged);
                substituteAdapter.notifyItemChanged(i);
                return;
            }
        }

        substituteAdapter.addItem(new SubstituteItem(original, newAlts));
    }

    @Nullable
    private String buildSubstitutesJsonFromList() {
        try {
            List<SubstituteItem> list = substituteAdapter != null
                    ? substituteAdapter.getItems()
                    : null;

            if (list == null || list.isEmpty()) return null;

            Map<String, List<String>> map = new LinkedHashMap<>();

            for (SubstituteItem it : list) {
                if (it == null) continue;

                String original = it.getOriginal();
                if (TextUtils.isEmpty(original)) continue;

                List<String> alts = it.getAlternatives();
                if (alts == null || alts.isEmpty()) continue;

                List<String> cleaned = new ArrayList<>();

                for (String a : alts) {
                    if (a == null) continue;

                    String t = a.trim();

                    if (!t.isEmpty() && !cleaned.contains(t)) {
                        cleaned.add(t);
                    }
                }

                if (!cleaned.isEmpty()) {
                    map.put(original, cleaned);
                }
            }

            if (map.isEmpty()) return null;

            JSONObject obj = new JSONObject();

            for (String key : map.keySet()) {
                JSONArray arr = new JSONArray();

                List<String> values = map.get(key);
                if (values == null) continue;

                for (String a : values) {
                    arr.put(a);
                }

                obj.put(key, arr);
            }

            return obj.toString();

        } catch (Exception e) {
            Log.e("SUBSTITUTE", "buildSubstitutesJsonFromList error", e);
            return null;
        }
    }

    private void fillFormForEdit(RecipeResponse recipe) {
        if (recipe == null) return;

        etName.setText(nullToEmpty(recipe.getName()));
        etSummary.setText(nullToEmpty(recipe.getSummary()));
        etIngredients.setText(nullToEmpty(recipe.getIngredients()));

        String optText = recipe.getOptionalIngredientsText();

        if (!TextUtils.isEmpty(optText)) {
            etOptionalIngredients.setText(optText);
        } else {
            etOptionalIngredients.setText(buildOptionalCsvFromItems(recipe.getOptionalIngredientsItems()));
        }

        selectedCategory = recipe.getCategory();

        Button targetBtn = null;

        if ("한식".equals(selectedCategory)) targetBtn = btnKorean;
        else if ("양식".equals(selectedCategory)) targetBtn = btnWestern;
        else if ("일식".equals(selectedCategory)) targetBtn = btnJapanese;
        else if ("중식".equals(selectedCategory)) targetBtn = btnChinese;
        else if ("기타".equals(selectedCategory)) targetBtn = btnEtc;
        else if ("디저트".equals(selectedCategory)) targetBtn = btnDessert;
        else if ("다이어트".equals(selectedCategory)) targetBtn = btnDiet;

        updateCategoryButtonUI(targetBtn);

        mainImageUriString = recipe.getImageUrl();
        isNewMainImage = false;

        try {
            mainThumbnailUrl = recipe.getThumbnailUrl();
        } catch (Exception ignored) {
        }

        if (TextUtils.isEmpty(mainThumbnailUrl)) {
            mainThumbnailUrl = deriveThumbnailUrlFromMain(mainImageUriString);
        } else {
            mainThumbnailUrl = resolveAnyImageUrl(mainThumbnailUrl);
        }

        String mainUrl = resolveAnyImageUrl(mainImageUriString);

        if (!TextUtils.isEmpty(mainUrl)) {
            if (!TextUtils.isEmpty(mainThumbnailUrl) && !mainUrl.equals(mainThumbnailUrl)) {
                Glide.with(this)
                        .load(mainUrl)
                        .thumbnail(
                                Glide.with(this)
                                        .load(mainThumbnailUrl)
                                        .centerCrop()
                        )
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .centerCrop()
                        .into(ivMainImage);
            } else {
                Glide.with(this)
                        .load(mainUrl)
                        .placeholder(R.drawable.bg_recipe_placeholder)
                        .error(R.drawable.bg_recipe_placeholder)
                        .centerCrop()
                        .into(ivMainImage);
            }
        } else {
            ivMainImage.setImageResource(R.drawable.bg_recipe_placeholder);
        }

        String stepsRaw = recipe.getSteps();

        if (!TextUtils.isEmpty(stepsRaw) && stepAdapter != null) {
            List<RecipeStepForm> parsedSteps = parseStepsString(stepsRaw);

            if (!parsedSteps.isEmpty()) {
                stepAdapter.setSteps(parsedSteps);
            }
        }

        List<DetailIngredientRow> rows = new ArrayList<>();

        try {
            List<RecipeResponse.RecipeIngredientItem> req = recipe.getRequiredIngredientsItems();

            if (req != null) {
                for (RecipeResponse.RecipeIngredientItem it : req) {
                    if (it == null) continue;

                    String n = nullToEmpty(it.getName()).trim();
                    if (TextUtils.isEmpty(n)) continue;

                    String q = it.getPrettyQuantity();
                    rows.add(new DetailIngredientRow(n, q, "REQUIRED"));
                }
            }

            List<RecipeResponse.RecipeIngredientItem> opt = recipe.getOptionalIngredientsItems();

            if (opt != null) {
                for (RecipeResponse.RecipeIngredientItem it : opt) {
                    if (it == null) continue;

                    String n = nullToEmpty(it.getName()).trim();
                    if (TextUtils.isEmpty(n)) continue;

                    String q = it.getPrettyQuantity();
                    rows.add(new DetailIngredientRow(n, q, "OPTIONAL"));
                }
            }

        } catch (Exception e) {
            Log.e("EDIT", "fill detail ingredients error", e);
        }

        if (detailIngredientAdapter != null) {
            detailIngredientAdapter.setItems(rows);
        }

        if (substituteAdapter != null) {
            substituteAdapter.setItems(new ArrayList<>());
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String buildOptionalCsvFromItems(@Nullable List<RecipeResponse.RecipeIngredientItem> items) {
        if (items == null || items.isEmpty()) return "";

        List<String> names = new ArrayList<>();

        for (RecipeResponse.RecipeIngredientItem it : items) {
            if (it == null) continue;

            String n = it.getName();

            if (!TextUtils.isEmpty(n)) {
                names.add(n.trim());
            }
        }

        return names.isEmpty() ? "" : TextUtils.join(", ", names);
    }

    private List<RecipeStepForm> parseStepsString(String stepsRaw) {
        List<RecipeStepForm> result = new ArrayList<>();

        if (TextUtils.isEmpty(stepsRaw)) return result;

        String[] lines = stepsRaw.split("\\r?\\n");

        RecipeStepForm current = null;
        StringBuilder textBuilder = null;

        for (String rawLine : lines) {
            if (rawLine == null) continue;

            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.contains("단계")) {
                if (current != null) {
                    if (textBuilder != null) {
                        current.setText(textBuilder.toString().trim());
                    }

                    result.add(current);
                }

                current = new RecipeStepForm();
                textBuilder = new StringBuilder();

            } else if (line.startsWith("이미지URI:")) {
                if (current != null) {
                    String uri = line.substring("이미지URI:".length()).trim();
                    current.setImageUrl(uri);
                }

            } else {
                if (current != null) {
                    if (textBuilder == null) {
                        textBuilder = new StringBuilder();
                    }

                    if (textBuilder.length() > 0) {
                        textBuilder.append("\n");
                    }

                    textBuilder.append(line);
                }
            }
        }

        if (current != null) {
            if (textBuilder != null) {
                current.setText(textBuilder.toString().trim());
            }

            result.add(current);
        }

        return result;
    }

    private void setupCategoryButtons() {
        View.OnClickListener listener = v -> {
            int id = v.getId();

            if (id == R.id.btnKorean) selectedCategory = "한식";
            else if (id == R.id.btnWestern) selectedCategory = "양식";
            else if (id == R.id.btnJapanese) selectedCategory = "일식";
            else if (id == R.id.btnChinese) selectedCategory = "중식";
            else if (id == R.id.btnEtc) selectedCategory = "기타";
            else if (id == R.id.btnDessert) selectedCategory = "디저트";
            else if (id == R.id.btnDiet) selectedCategory = "다이어트";

            updateCategoryButtonUI((Button) v);
        };

        if (btnKorean != null) btnKorean.setOnClickListener(listener);
        if (btnWestern != null) btnWestern.setOnClickListener(listener);
        if (btnJapanese != null) btnJapanese.setOnClickListener(listener);
        if (btnChinese != null) btnChinese.setOnClickListener(listener);
        if (btnEtc != null) btnEtc.setOnClickListener(listener);
        if (btnDessert != null) btnDessert.setOnClickListener(listener);
        if (btnDiet != null) btnDiet.setOnClickListener(listener);

        updateCategoryButtonUI(null);
    }

    private void updateCategoryButtonUI(@Nullable Button selectedBtn) {
        int selectedBg = Color.parseColor("#5CB6E4");
        int unselectedBg = Color.parseColor("#87CEEB");
        int selectedText = Color.WHITE;
        int unselectedText = Color.WHITE;

        Button[] buttons = {
                btnKorean,
                btnWestern,
                btnJapanese,
                btnChinese,
                btnEtc,
                btnDessert,
                btnDiet
        };

        for (Button b : buttons) {
            if (b == null) continue;

            if (b == selectedBtn) {
                b.setBackgroundTintList(ColorStateList.valueOf(selectedBg));
                b.setTextColor(selectedText);
            } else {
                b.setBackgroundTintList(ColorStateList.valueOf(unselectedBg));
                b.setTextColor(unselectedText);
            }
        }
    }

    private void setupImagePickers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && pendingTarget != null) {
                        startCrop(uri);
                    } else {
                        pendingTarget = null;
                        pendingStepIndex = -1;
                    }
                }
        );

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleCropResult
        );
    }

    private void setupStepAdapter() {
        stepAdapter = new RecipeStepAdapter(position -> {
            pendingTarget = TargetType.STEP;
            pendingStepIndex = position;
            pickImageLauncher.launch("image/*");
        });

        rvSteps.setLayoutManager(new LinearLayoutManager(this));
        rvSteps.setAdapter(stepAdapter);
        rvSteps.setNestedScrollingEnabled(false);
    }

    private void startCrop(Uri sourceUri) {
        File imagesDir = new File(getFilesDir(), "images");

        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        String fileName = "crop_" + UUID.randomUUID() + ".jpg";
        File destFile = new File(imagesDir, fileName);
        Uri destUri = Uri.fromFile(destFile);

        UCrop.Options options = new UCrop.Options();

        options.setCompressionQuality(90);
        options.setFreeStyleCropEnabled(true);
        options.setShowCropGrid(true);

        options.setToolbarTitle("사진 편집");
        options.setToolbarColor(Color.WHITE);
        options.setStatusBarColor(Color.WHITE);
        options.setToolbarWidgetColor(Color.BLACK);
        options.setRootViewBackgroundColor(Color.WHITE);
        options.setHideBottomControls(false);

        options.setAspectRatioOptions(
                0,
                new AspectRatio("1:1", 1, 1),
                new AspectRatio("4:3", 4, 3),
                new AspectRatio("16:9", 16, 9)
        );
        Intent intent = UCrop.of(sourceUri, destUri)
                .withMaxResultSize(1600, 1600)
                .withOptions(options)
                .getIntent(this);

        cropImageLauncher.launch(intent);
    }

    private void handleCropResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri resultUri = UCrop.getOutput(result.getData());

            if (resultUri != null && pendingTarget != null) {
                String uriString = resultUri.toString();

                Log.d("UPLOAD", "UCrop result uri = " + uriString);

                if (pendingTarget == TargetType.MAIN) {
                    mainImageUriString = uriString;
                    mainThumbnailUrl = null;
                    ivMainImage.setImageURI(resultUri);
                    isNewMainImage = true;

                } else if (pendingTarget == TargetType.STEP && pendingStepIndex >= 0) {
                    if (stepAdapter != null) {
                        stepAdapter.setStepImage(pendingStepIndex, uriString);
                    }
                }
            }

        } else {
            AppToast.show(this, "이미지 편집에 실패했어요.");
        }

        pendingTarget = null;
        pendingStepIndex = -1;
    }

    private void submitRecipe() {
        String name = etName.getText().toString().trim();
        String summary = etSummary.getText().toString().trim();

        String ingredientsCsv = etIngredients.getText().toString().trim();

        String optionalIngredientsCsv = (etOptionalIngredients != null)
                ? etOptionalIngredients.getText().toString().trim()
                : "";

        String substitutesJson = buildSubstitutesJsonFromList();

        if (TextUtils.isEmpty(name)) {
            AppToast.show(this, "레시피 이름을 입력하세요.");
            return;
        }

        if (selectedCategory == null) {
            AppToast.show(this, "카테고리를 선택하세요.");
            return;
        }

        if (TextUtils.isEmpty(mainImageUriString)) {
            AppToast.show(this, "대표 이미지를 선택하세요.");
            return;
        }

        List<IngredientItemRequest> ingredientItems = buildIngredientItemsRequest();
        List<RecipeStepForm> steps = (stepAdapter != null)
                ? stepAdapter.getSteps()
                : new ArrayList<>();

        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.6f);

        uploadImagesThenSaveRecipe(
                name,
                summary,
                ingredientsCsv,
                optionalIngredientsCsv,
                steps,
                substitutesJson,
                ingredientItems
        );
    }

    private void uploadImagesThenSaveRecipe(
            String name,
            String summary,
            String ingredientsCsv,
            String optionalIngredientsCsv,
            @NonNull List<RecipeStepForm> steps,
            @Nullable String substitutesJson,
            @NonNull List<IngredientItemRequest> ingredientItems
    ) {
        boolean shouldUploadMain = editingRecipeId == null || isNewMainImage;

        if (shouldUploadMain) {
            uploadSingleImage(mainImageUriString, new ImageUploadCallback() {
                @Override
                public void onSuccess(@Nullable String imageUrl, @Nullable String thumbnailUrl) {
                    mainImageUriString = resolveAnyImageUrl(imageUrl);
                    mainThumbnailUrl = resolveAnyImageUrl(thumbnailUrl);

                    uploadStepImagesSequentially(
                            0,
                            steps,
                            () -> saveRecipeAfterImageUpload(
                                    name,
                                    summary,
                                    mainImageUriString,
                                    mainThumbnailUrl,
                                    ingredientsCsv,
                                    optionalIngredientsCsv,
                                    steps,
                                    substitutesJson,
                                    ingredientItems
                            )
                    );
                }

                @Override
                public void onFailure(String message) {
                    restoreSubmitButton();
                    showSafeToast(message);
                }
            });

        } else {
            uploadStepImagesSequentially(
                    0,
                    steps,
                    () -> saveRecipeAfterImageUpload(
                            name,
                            summary,
                            resolveAnyImageUrl(mainImageUriString),
                            resolveAnyImageUrl(mainThumbnailUrl),
                            ingredientsCsv,
                            optionalIngredientsCsv,
                            steps,
                            substitutesJson,
                            ingredientItems
                    )
            );
        }
    }

    private void uploadStepImagesSequentially(
            int index,
            @NonNull List<RecipeStepForm> steps,
            @NonNull Runnable onDone
    ) {
        if (index >= steps.size()) {
            onDone.run();
            return;
        }

        RecipeStepForm step = steps.get(index);

        if (step == null || !step.needsUpload()) {
            uploadStepImagesSequentially(index + 1, steps, onDone);
            return;
        }

        uploadSingleImage(step.getImageUrl(), new ImageUploadCallback() {
            @Override
            public void onSuccess(@Nullable String imageUrl, @Nullable String thumbnailUrl) {
                step.applyUploadedImage(
                        resolveAnyImageUrl(imageUrl),
                        resolveAnyImageUrl(thumbnailUrl)
                );

                uploadStepImagesSequentially(index + 1, steps, onDone);
            }

            @Override
            public void onFailure(String message) {
                restoreSubmitButton();
                showSafeToast(message);
            }
        });
    }

    private void saveRecipeAfterImageUpload(
            String name,
            String summary,
            @Nullable String imageUrl,
            @Nullable String thumbnailUrl,
            String ingredientsCsv,
            String optionalIngredientsCsv,
            @NonNull List<RecipeStepForm> steps,
            @Nullable String substitutesJson,
            @NonNull List<IngredientItemRequest> ingredientItems
    ) {
        String stepsText = buildStepsText(steps);

        if (editingRecipeId == null) {
            createRecipeToServer(
                    name,
                    summary,
                    imageUrl,
                    thumbnailUrl,
                    ingredientsCsv,
                    optionalIngredientsCsv,
                    stepsText,
                    substitutesJson,
                    ingredientItems
            );

        } else {
            updateRecipeToServer(
                    editingRecipeId,
                    name,
                    summary,
                    imageUrl,
                    thumbnailUrl,
                    ingredientsCsv,
                    optionalIngredientsCsv,
                    stepsText,
                    substitutesJson,
                    ingredientItems
            );
        }
    }

    private void uploadSingleImage(
            @Nullable String uriString,
            @NonNull ImageUploadCallback callback
    ) {
        try {
            String raw = uriString == null ? "" : uriString.trim();

            if (raw.isEmpty()) {
                callback.onSuccess(null, null);
                return;
            }

            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                callback.onSuccess(raw, deriveThumbnailUrlFromMain(raw));
                return;
            }

            Uri uri = Uri.parse(raw);

            File file = copyUriToTempFile(uri);

            if (file == null || !file.exists()) {
                callback.onFailure("이미지 파일을 읽을 수 없습니다.");
                return;
            }

            RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/*"));

            MultipartBody.Part body = MultipartBody.Part.createFormData(
                    "file",
                    file.getName(),
                    requestFile
            );

            RetrofitClient.getUploadApi().uploadImage(body)
                    .enqueue(new Callback<ImageUploadResponse>() {
                        @Override
                        public void onResponse(
                                Call<ImageUploadResponse> call,
                                Response<ImageUploadResponse> response
                        ) {
                            if (response.isSuccessful() && response.body() != null) {
                                ImageUploadResponse res = response.body();

                                String imageUrl = resolveAnyImageUrl(res.getUrl());
                                String thumbUrl = resolveAnyImageUrl(res.getThumbnailUrl());

                                callback.onSuccess(imageUrl, thumbUrl);

                            } else {
                                Log.e(TAG, "image upload response failed. code=" + response.code());
                                callback.onFailure("이미지 업로드에 실패했어요. 잠시 후 다시 시도해주세요.");
                            }
                        }

                        @Override
                        public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                            Log.e(TAG, "image upload failure", t);
                            callback.onFailure(NetworkErrorUtil.getUserMessage(t));
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "uploadSingleImage error", e);
            callback.onFailure("이미지 업로드 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private void restoreSubmitButton() {
        if (btnSubmit == null) return;

        btnSubmit.setEnabled(true);
        btnSubmit.setAlpha(1f);
    }

    private interface ImageUploadCallback {
        void onSuccess(@Nullable String imageUrl, @Nullable String thumbnailUrl);

        void onFailure(String message);
    }

    private String buildStepsText(List<RecipeStepForm> steps) {
        if (steps == null) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < steps.size(); i++) {
            RecipeStepForm step = steps.get(i);

            boolean hasText = step != null && !TextUtils.isEmpty(step.getText());
            boolean hasImage = step != null && !TextUtils.isEmpty(step.getImageUrl());

            sb.append("[")
                    .append(i + 1)
                    .append("단계]\n");

            if (hasText) {
                sb.append(step.getText()).append("\n");
            }

            if (hasImage) {
                sb.append("이미지URI: ")
                        .append(step.getImageUrl())
                        .append("\n");
            }

            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private void uploadImageAndSave(
            String name,
            String summary,
            String ingredientsCsv,
            String optionalIngredientsCsv,
            String stepsText,
            String imageUriString,
            @Nullable String substitutesJson,
            @NonNull List<IngredientItemRequest> ingredientItems
    ) {
        try {
            Uri uri = Uri.parse(imageUriString);

            File file = copyUriToTempFile(uri);

            if (file == null || !file.exists()) {
                restoreSubmitButton();
                AppToast.show(this, "이미지 파일을 읽을 수 없습니다.");
                return;
            }

            RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/*"));

            MultipartBody.Part body = MultipartBody.Part.createFormData(
                    "file",
                    file.getName(),
                    requestFile
            );

            RetrofitClient.getUploadApi().uploadImage(body)
                    .enqueue(new Callback<ImageUploadResponse>() {
                        @Override
                        public void onResponse(
                                Call<ImageUploadResponse> call,
                                Response<ImageUploadResponse> response
                        ) {
                            if (response.isSuccessful() && response.body() != null) {
                                ImageUploadResponse res = response.body();

                                String imageUrl = res.getUrl();
                                String thumbUrl = res.getThumbnailUrl();

                                Log.d("CHECK_UPLOAD", "upload imageUrl=" + imageUrl + ", thumb=" + thumbUrl);

                                imageUrl = resolveAnyImageUrl(imageUrl);
                                thumbUrl = resolveAnyImageUrl(thumbUrl);

                                mainImageUriString = imageUrl;
                                mainThumbnailUrl = thumbUrl;

                                if (editingRecipeId == null) {
                                    createRecipeToServer(
                                            name,
                                            summary,
                                            imageUrl,
                                            thumbUrl,
                                            ingredientsCsv,
                                            optionalIngredientsCsv,
                                            stepsText,
                                            substitutesJson,
                                            ingredientItems
                                    );

                                } else {
                                    updateRecipeToServer(
                                            editingRecipeId,
                                            name,
                                            summary,
                                            imageUrl,
                                            thumbUrl,
                                            ingredientsCsv,
                                            optionalIngredientsCsv,
                                            stepsText,
                                            substitutesJson,
                                            ingredientItems
                                    );
                                }

                            } else {
                                restoreSubmitButton();
                                Log.e(TAG, "legacy image upload response failed. code=" + response.code());
                                AppToast.show(
                                        RecipeWriteActivity.this,
                                        "이미지 업로드에 실패했어요. 잠시 후 다시 시도해주세요."
                                );
                            }
                        }

                        @Override
                        public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                            restoreSubmitButton();
                            Log.e(TAG, "legacy image upload failure", t);
                            AppToast.show(
                                    RecipeWriteActivity.this,
                                    NetworkErrorUtil.getUserMessage(t)
                            );
                        }
                    });

        } catch (Exception e) {
            restoreSubmitButton();
            Log.e(TAG, "uploadImageAndSave error", e);
            AppToast.show(this, "이미지 업로드 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    @Nullable
    private File copyUriToTempFile(Uri uri) {
        try {
            File dir = new File(getCacheDir(), "upload");

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(dir, fileName);

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new java.io.FileOutputStream(outFile)) {

                if (in == null) return null;

                byte[] buffer = new byte[8192];
                int len;

                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }

                out.flush();
            }

            return outFile;

        } catch (Exception e) {
            Log.e(TAG, "copyUriToTempFile error", e);
            return null;
        }
    }

    private Long getLoginUserIdOrNull() {
        try {
            long uid = AuthPrefs.getUserId(this);
            return uid > 0 ? uid : null;

        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String getLoginUserNicknameOrNull() {
        try {
            String nickname = AuthPrefs.getUserNickname(this);

            if (nickname == null) return null;

            nickname = nickname.trim();

            return nickname.isEmpty() ? null : nickname;

        } catch (Exception e) {
            return null;
        }
    }

    private void applyAuthorToRequest(@NonNull RecipeCreateRequest request) {
        request.setUserId(getLoginUserIdOrNull());
        request.setUserNickname(getLoginUserNicknameOrNull());
    }

    private void createRecipeToServer(
            String name,
            String summary,
            @Nullable String imageUrl,
            @Nullable String thumbnailUrl,
            String ingredientsCsv,
            String optionalIngredientsCsv,
            String stepsText,
            @Nullable String substitutesJson,
            @NonNull List<IngredientItemRequest> ingredientItems
    ) {
        Log.d("CHECK_IMAGE", "imageUrl=" + imageUrl + ", thumb=" + thumbnailUrl);

        RecipeCreateRequest request = new RecipeCreateRequest();
        request.setExternalId(UUID.randomUUID().toString());
        request.setName(name);
        request.setSummary(summary);
        request.setImageUrl(imageUrl);
        request.setThumbnailUrl(thumbnailUrl);
        request.setIngredients(ingredientsCsv);
        request.setOptionalIngredients(optionalIngredientsCsv);
        request.setSteps(stepsText);
        request.setCategory(selectedCategory);
        request.setSubstitutesJson(substitutesJson);
        request.setIngredientItems(ingredientItems);

        applyAuthorToRequest(request);

        Retrofit retrofit = FridgeApi.getClient();
        RecipeApiService api = retrofit.create(RecipeApiService.class);

        api.createRecipe(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    ChecklistPrefs.addRecipeCreateCount(RecipeWriteActivity.this);
                    PetExpManager.giveRecipeCreateExp(RecipeWriteActivity.this);
                    AppToast.show(RecipeWriteActivity.this, "레시피 등록 성공!");
                    finish();

                } else {
                    restoreSubmitButton();
                    Log.e(TAG, "createRecipe failed. code=" + response.code());
                    AppToast.show(
                            RecipeWriteActivity.this,
                            "레시피 저장에 실패했어요. 잠시 후 다시 시도해주세요."
                    );
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                restoreSubmitButton();
                Log.e(TAG, "createRecipe failure", t);
                AppToast.show(
                        RecipeWriteActivity.this,
                        NetworkErrorUtil.getUserMessage(t)
                );
            }
        });
    }

    private void updateRecipeToServer(
            long id,
            String name,
            String summary,
            @Nullable String imageUrl,
            @Nullable String thumbnailUrl,
            String ingredientsCsv,
            String optionalIngredientsCsv,
            String stepsText,
            @Nullable String substitutesJson,
            @NonNull List<IngredientItemRequest> ingredientItems
    ) {
        RecipeCreateRequest request = new RecipeCreateRequest();
        request.setExternalId(UUID.randomUUID().toString());
        request.setName(name);
        request.setSummary(summary);
        request.setImageUrl(imageUrl);
        request.setThumbnailUrl(thumbnailUrl);
        request.setIngredients(ingredientsCsv);
        request.setOptionalIngredients(optionalIngredientsCsv);
        request.setSteps(stepsText);
        request.setCategory(selectedCategory);
        request.setSubstitutesJson(substitutesJson);
        request.setIngredientItems(ingredientItems);

        applyAuthorToRequest(request);

        Retrofit retrofit = FridgeApi.getClient();
        RecipeApiService api = retrofit.create(RecipeApiService.class);

        api.updateRecipe(id, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    AppToast.show(RecipeWriteActivity.this, "레시피 수정 성공!");
                    finish();

                } else {
                    restoreSubmitButton();
                    Log.e(TAG, "updateRecipe failed. code=" + response.code());
                    AppToast.show(
                            RecipeWriteActivity.this,
                            "레시피 저장에 실패했어요. 잠시 후 다시 시도해주세요."
                    );
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                restoreSubmitButton();
                Log.e(TAG, "updateRecipe failure", t);
                AppToast.show(
                        RecipeWriteActivity.this,
                        NetworkErrorUtil.getUserMessage(t)
                );
            }
        });
    }

    private void showSafeToast(@Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            AppToast.show(this, "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요.");
            return;
        }

        String lower = message.toLowerCase();

        if (lower.contains("unknownhostexception")
                || lower.contains("unable to resolve host")
                || lower.contains("no address associated with hostname")
                || lower.contains("failed to connect")
                || lower.contains("sockettimeoutexception")
                || lower.contains("connectexception")) {
            AppToast.show(this, NetworkErrorUtil.getNetworkMessage());
            return;
        }

        AppToast.show(this, message);
    }
}