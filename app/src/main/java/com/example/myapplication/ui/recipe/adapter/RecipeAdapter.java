package com.namgyun.tamakitchen.ui.recipe.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.FridgeApi;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.recipe.RecipeResponse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RecipeAdapter extends ListAdapter<RecipeResponse, RecipeAdapter.ViewHolder> {

    public enum ListType { ALL, FRIDGE }

    private static final String TAG_IMAGE = "RECIPE_IMAGE";
    private static final String PREFS_FAVORITE = "favorite_prefs";
    private static final String KEY_FAV_PREFIX = "fav_";

    private static final boolean DEBUG_TOOL_TEST_ICON = false;

    private final Context context;
    private ListType listType;

    private final Set<String> currentFridgeNormalized = new HashSet<>();
    private final Set<String> userCookTools = new HashSet<>();

    private long lastSortSignature = Long.MIN_VALUE;
    private List<RecipeResponse> lastSortedCache = null;

    public interface OnRecipeDeleteListener {
        void onRequestDelete(RecipeResponse item, int position);
    }

    public interface OnRecipeClickListener {
        void onClick(RecipeResponse item);
    }

    public interface OnFavoriteChangedListener {
        void onFavoriteChanged();
    }

    private OnRecipeDeleteListener deleteListener;
    private OnRecipeClickListener clickListener;
    private OnFavoriteChangedListener favoriteChangedListener;

    public void setOnRecipeDeleteListener(OnRecipeDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnFavoriteChangedListener(OnFavoriteChangedListener listener) {
        this.favoriteChangedListener = listener;
    }

    public RecipeAdapter(Context context, ListType listType) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listType = (listType == null) ? ListType.ALL : listType;
        setHasStableIds(true);
    }

    public void setListType(ListType type) {
        if (type == null) type = ListType.ALL;
        this.listType = type;

        forceResort();
        submitItems(getCurrentList());
    }

    public void setUserCookTools(@NonNull Collection<String> tools) {
        userCookTools.clear();
        for (String t : tools) {
            String n = normalizeToolCode(t);
            if (!TextUtils.isEmpty(n)) userCookTools.add(n);
        }
        forceResort();
        submitItems(getCurrentList());
    }

    public void forceResort() {
        lastSortSignature = Long.MIN_VALUE;
        lastSortedCache = null;
    }

    public void submitItems(List<RecipeResponse> items) {
        List<RecipeResponse> safe = (items == null) ? new ArrayList<>() : new ArrayList<>(items);

        long sig = computeSortSignature(safe);
        if (sig == lastSortSignature && lastSortedCache != null) {
            submitList(new ArrayList<>(lastSortedCache));
            return;
        }

        List<RecipeResponse> sorted = sortWithToolPriority(safe);
        lastSortSignature = sig;
        lastSortedCache = new ArrayList<>(sorted);

        submitList(sorted);
    }

    private List<RecipeResponse> sortWithToolPriority(List<RecipeResponse> list) {
        if (list == null) return new ArrayList<>();
        List<RecipeResponse> out = new ArrayList<>(list);

        if (listType == ListType.FRIDGE) {
            List<RecipeResponse> temp = new ArrayList<>();
            for (RecipeResponse r : out) {
                if (r == null) continue;
                int matchPercent = r.getMatchPercent();
                boolean hasUsed = r.getUsedIngredients() != null && !r.getUsedIngredients().isEmpty();
                if (matchPercent > 0 && hasUsed) temp.add(r);
            }

            Collections.sort(temp, new Comparator<RecipeResponse>() {
                @Override
                public int compare(RecipeResponse a, RecipeResponse b) {
                    int ma = (a == null) ? 0 : a.getMatchPercent();
                    int mb = (b == null) ? 0 : b.getMatchPercent();
                    if (mb != ma) return mb - ma;

                    int ta = cookToolMatchCount(a);
                    int tb = cookToolMatchCount(b);
                    if (tb != ta) return tb - ta;

                    String na = (a == null || a.getName() == null) ? "" : a.getName();
                    String nb = (b == null || b.getName() == null) ? "" : b.getName();
                    return na.compareTo(nb);
                }
            });
            return temp;

        } else {
            Collections.sort(out, new Comparator<RecipeResponse>() {
                @Override
                public int compare(RecipeResponse a, RecipeResponse b) {
                    int ta = cookToolMatchCount(a);
                    int tb = cookToolMatchCount(b);
                    if (tb != ta) return tb - ta;

                    int ma = (a == null) ? 0 : a.getMatchPercent();
                    int mb = (b == null) ? 0 : b.getMatchPercent();
                    if (mb != ma) return mb - ma;

                    String na = (a == null || a.getName() == null) ? "" : a.getName();
                    String nb = (b == null || b.getName() == null) ? "" : b.getName();
                    return na.compareTo(nb);
                }
            });
            return out;
        }
    }

    public void setCurrentFridgeIngredients(@NonNull Collection<String> fridgeIngredients) {
        currentFridgeNormalized.clear();
        for (String s : fridgeIngredients) {
            String n = normalizeIngredient(s);
            if (!TextUtils.isEmpty(n)) currentFridgeNormalized.add(n);
        }
        forceResort();
        submitItems(getCurrentList());
    }

    public void clearCurrentFridgeIngredients() {
        currentFridgeNormalized.clear();
        forceResort();
        submitItems(getCurrentList());
    }

    @Override
    public long getItemId(int position) {
        RecipeResponse item = getItem(position);
        if (item == null || item.getId() == null) return RecyclerView.NO_ID;
        return item.getId();
    }

    private SharedPreferences favPrefs() {
        return context.getSharedPreferences(PREFS_FAVORITE, Context.MODE_PRIVATE);
    }

    private boolean isFavorite(Long recipeId) {
        if (recipeId == null) return false;
        return favPrefs().getBoolean(KEY_FAV_PREFIX + recipeId, false);
    }

    private void setFavorite(Long recipeId, boolean value) {
        if (recipeId == null) return;
        favPrefs().edit().putBoolean(KEY_FAV_PREFIX + recipeId, value).apply();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecipeResponse item = getItem(position);
        if (item == null) return;

        holder.name.setText(item.getName() != null ? item.getName() : "");
        holder.summary.setText(item.getSummary() != null ? item.getSummary() : "");

        int matchPercent = item.getMatchPercent();
        holder.matchPercent.setVisibility(View.VISIBLE);
        holder.matchPercent.setText(matchPercent + "% 일치");

        String rawMain = item.getImageUrl();
        String rawThumb = item.getThumbnailUrl();

        String finalUrl = resolveImageUrl(rawMain);
        String thumbUrl = resolveImageUrl(rawThumb);
        if (TextUtils.isEmpty(thumbUrl)) thumbUrl = deriveThumbnailUrl(rawMain);

        Log.d(TAG_IMAGE, "recipe=" + item.getName());
        Log.d(TAG_IMAGE, "rawMain=" + rawMain);
        Log.d(TAG_IMAGE, "rawThumb=" + rawThumb);
        Log.d(TAG_IMAGE, "finalUrl=" + finalUrl);
        Log.d(TAG_IMAGE, "thumbUrl=" + thumbUrl);

        if (TextUtils.isEmpty(finalUrl)) {
            Log.w(TAG_IMAGE, "finalUrl is empty. placeholder used. recipe=" + item.getName());
            holder.image.setImageResource(R.drawable.bg_recipe_placeholder);
        } else {
            try {
                RequestOptions common = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .override(300, 300);

                RequestListener<Drawable> listener = new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<Drawable> target,
                            boolean isFirstResource
                    ) {
                        Log.e(TAG_IMAGE, "Glide load failed. model=" + model + ", recipe=" + item.getName(), e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            Drawable resource,
                            Object model,
                            Target<Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource
                    ) {
                        Log.d(TAG_IMAGE, "Glide load success. model=" + model + ", source=" + dataSource + ", recipe=" + item.getName());
                        return false;
                    }
                };

                if (!TextUtils.isEmpty(thumbUrl) && !finalUrl.equals(thumbUrl)) {
                    Glide.with(context)
                            .load(finalUrl)
                            .apply(common)
                            .listener(listener)
                            .thumbnail(
                                    Glide.with(context)
                                            .load(thumbUrl)
                                            .apply(common)
                                            .placeholder(R.drawable.bg_recipe_placeholder)
                                            .error(R.drawable.bg_recipe_placeholder)
                                            .centerCrop()
                                            .dontAnimate()
                            )
                            .placeholder(R.drawable.bg_recipe_placeholder)
                            .error(R.drawable.bg_recipe_placeholder)
                            .centerCrop()
                            .dontAnimate()
                            .into(holder.image);
                } else {
                    Glide.with(context)
                            .load(finalUrl)
                            .apply(common)
                            .listener(listener)
                            .placeholder(R.drawable.bg_recipe_placeholder)
                            .error(R.drawable.bg_recipe_placeholder)
                            .centerCrop()
                            .dontAnimate()
                            .into(holder.image);
                }
            } catch (Exception e) {
                Log.e(TAG_IMAGE, "Glide exception before request. recipe=" + item.getName() + ", url=" + finalUrl, e);
                holder.image.setImageResource(R.drawable.bg_recipe_placeholder);
            }
        }

        List<String> used = item.getUsedIngredients() != null ? item.getUsedIngredients() : new ArrayList<>();
        List<String> missing = item.getMissingIngredients() != null ? item.getMissingIngredients() : new ArrayList<>();

        List<String> usedFiltered = new ArrayList<>();
        List<String> missingFiltered = new ArrayList<>();

        boolean hasServerMatchData = !used.isEmpty() || !missing.isEmpty();

        if (hasServerMatchData) {
            usedFiltered.addAll(used);
            missingFiltered.addAll(missing);
        } else {
            List<String> allRequired = splitIngredientsToList(item.getIngredients());
            missingFiltered.addAll(allRequired);
        }

        holder.usedIngredients.setText(!usedFiltered.isEmpty()
                ? "사용 가능: " + TextUtils.join(", ", toIngredientNamesOnly(usedFiltered))
                : "사용 가능: -");

        holder.missingIngredients.setText(!missingFiltered.isEmpty()
                ? "부족: " + TextUtils.join(", ", toIngredientNamesOnly(missingFiltered))
                : "부족: -");
        float avgRating = item.getRatingAverage();
        int ratingCount = item.getRatingCount();
        if (ratingCount > 0) {
            holder.ratingBarCard.setRating(avgRating);
            holder.ratingText.setText(String.format(Locale.getDefault(), "%.1f점 (%d명)", avgRating, ratingCount));
        } else {
            holder.ratingBarCard.setRating(0f);
            holder.ratingText.setText("평점 없음");
        }

        bindAuthor(holder, item);

        bindCookTools(holder, item);

        Long id = item.getId();
        boolean fav = isFavorite(id);
        holder.favoriteStar.setImageResource(
                fav ? R.drawable.ic_star_filled_yellow : R.drawable.ic_star_outline_gray
        );

        holder.favoriteStar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            RecipeResponse current = getItem(pos);
            if (current == null) return;

            Long recipeId = current.getId();
            if (recipeId == null) return;

            boolean nowFav = !isFavorite(recipeId);
            setFavorite(recipeId, nowFav);

            holder.favoriteStar.setImageResource(
                    nowFav ? R.drawable.ic_star_filled_yellow : R.drawable.ic_star_outline_gray
            );

            if (favoriteChangedListener != null) favoriteChangedListener.onFavoriteChanged();
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) clickListener.onClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) deleteListener.onRequestDelete(item, pos);
            }
            return true;
        });
    }

    private void bindAuthor(@NonNull ViewHolder holder, @NonNull RecipeResponse item) {
        Long loginUserId = null;
        try {
            long uid = AuthPrefs.getUserId(context);
            if (uid > 0) loginUserId = uid;
        } catch (Exception ignore) {
        }

        Long authorId = item.getAuthorId();
        String authorNickname = item.getAuthorNickname();

        if (authorId != null && loginUserId != null && authorId.equals(loginUserId)) {
            holder.author.setText("작성자: 나 👑");
            holder.author.setVisibility(View.VISIBLE);
            return;
        }

        if (!TextUtils.isEmpty(authorNickname)) {
            holder.author.setText("작성자: " + authorNickname);
            holder.author.setVisibility(View.VISIBLE);
        } else {
            holder.author.setText("");
            holder.author.setVisibility(View.GONE);
        }
    }

    private int cookToolMatchCount(RecipeResponse item) {
        if (item == null) return 0;
        if (userCookTools.isEmpty()) return 0;

        List<String> recipeTools = extractCookTools(item);
        if (recipeTools == null || recipeTools.isEmpty()) return 0;

        int cnt = 0;
        for (String t : recipeTools) {
            String norm = normalizeToolCode(t);
            if (!TextUtils.isEmpty(norm) && userCookTools.contains(norm)) cnt++;
        }
        return cnt;
    }

    private void bindCookTools(@NonNull ViewHolder holder, @NonNull RecipeResponse item) {
        if (holder.cookToolsLayout == null) return;

        holder.cookToolsLayout.removeAllViews();

        if (DEBUG_TOOL_TEST_ICON) {
            holder.cookToolsLayout.setVisibility(View.VISIBLE);
            ImageView test = new ImageView(context);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dpToPx(22), dpToPx(22));
            test.setLayoutParams(tlp);
            test.setImageResource(R.drawable.ic_tool_air_fryer);
            test.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.cookToolsLayout.addView(test);
        }

        List<String> tools = extractCookTools(item);

        if (tools == null || tools.isEmpty()) {
            holder.cookToolsLayout.setVisibility(DEBUG_TOOL_TEST_ICON ? View.VISIBLE : View.GONE);
            return;
        }

        holder.cookToolsLayout.setVisibility(View.VISIBLE);

        final int sizePx = dpToPx(22);
        final int gapPx = dpToPx(6);

        for (int i = 0; i < tools.size(); i++) {
            String t = tools.get(i);
            int resId = mapToolToDrawable(t);
            if (resId == 0) continue;

            ImageView iv = new ImageView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.leftMargin = (i == 0) ? 0 : gapPx;
            iv.setLayoutParams(lp);
            iv.setImageResource(resId);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

            holder.cookToolsLayout.addView(iv);
        }

        if (holder.cookToolsLayout.getChildCount() == 0) {
            holder.cookToolsLayout.setVisibility(DEBUG_TOOL_TEST_ICON ? View.VISIBLE : View.GONE);
        }
    }

    private List<String> extractCookTools(@NonNull RecipeResponse item) {
        try {
            List<String> list = item.getCookTools();
            if (list != null && !list.isEmpty()) return normalizeToolList(list);
        } catch (Exception ignore) {}

        String text = null;
        try {
            text = item.getCookToolsText();
        } catch (Exception ignore) {}

        if (!TextUtils.isEmpty(text)) {
            String[] parts = text.split("[,\\n\\r\\t/|\\s]+");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                if (p == null) continue;
                String x = p.trim();
                if (!TextUtils.isEmpty(x)) out.add(x);
            }
            return normalizeToolList(out);
        }

        return normalizeToolList(extractCookToolsByReflection(item));
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCookToolsByReflection(@NonNull RecipeResponse item) {
        String[] candidates = new String[] {
                "getCookTools",
                "getCooktools",
                "getCookWare",
                "getCookware",
                "getTools",
                "getToolCodes",
                "getCookToolsText",
                "getCookwareText",
                "getToolsText"
        };

        for (String mName : candidates) {
            try {
                Method m = item.getClass().getMethod(mName);
                Object value = m.invoke(item);
                if (value == null) continue;

                if (value instanceof List) {
                    List<?> raw = (List<?>) value;
                    List<String> out = new ArrayList<>();
                    for (Object o : raw) {
                        if (o == null) continue;
                        String s = String.valueOf(o).trim();
                        if (!TextUtils.isEmpty(s)) out.add(s);
                    }
                    return out;
                }

                if (value instanceof String) {
                    String s = ((String) value).trim();
                    if (TextUtils.isEmpty(s)) continue;

                    String[] parts = s.split("[,\\n\\r\\t/|\\s]+");
                    List<String> out = new ArrayList<>();
                    for (String p : parts) {
                        if (p == null) continue;
                        String x = p.trim();
                        if (!TextUtils.isEmpty(x)) out.add(x);
                    }
                    return out;
                }
            } catch (Exception ignore) {
            }
        }
        return new ArrayList<>();
    }

    private List<String> normalizeToolList(@NonNull List<String> in) {
        List<String> out = new ArrayList<>();
        if (in == null) return out;
        for (String s : in) {
            String x = normalizeToolCode(s);
            if (!TextUtils.isEmpty(x) && !out.contains(x)) out.add(x);
        }
        return out;
    }

    private String normalizeToolCode(String raw) {
        if (raw == null) return "";
        String x = raw.trim();
        if (x.isEmpty()) return "";

        if (x.contains("에어프라이")) return "AIR_FRYER";
        if (x.contains("믹서") || x.contains("블렌더")) return "BLENDER";
        if (x.contains("가스레인지") || x.equals("가스")) return "GAS_RANGE";
        if (x.contains("인덕션") || x.equalsIgnoreCase("IH")) return "INDUCTION";
        if (x.contains("전자레인지") || x.contains("전자")) return "MICROWAVE";
        if (x.contains("오븐")) return "OVEN";
        if (x.contains("밥솥")) return "RICE_COOKER";
        if (x.contains("찜기")) return "STEAMER";

        x = x.replace("-", "_").replace(" ", "_").toUpperCase(Locale.getDefault());

        if (x.equals("GAS_STOVE")) x = "GAS_RANGE";
        if (x.equals("AIRFRYER")) x = "AIR_FRYER";
        if (x.equals("RICECOOKER")) x = "RICE_COOKER";
        return x;
    }

    private int mapToolToDrawable(String toolCode) {
        if (toolCode == null) return 0;
        String raw = toolCode.trim();
        if (TextUtils.isEmpty(raw)) return 0;

        if (raw.contains("에어프라이")) return R.drawable.ic_tool_air_fryer;
        if (raw.contains("믹서") || raw.contains("블렌더")) return R.drawable.ic_tool_blender;
        if (raw.contains("가스레인지") || raw.contains("가스")) return R.drawable.ic_tool_gas_range;
        if (raw.contains("인덕션") || raw.contains("IH")) return R.drawable.ic_tool_induction;
        if (raw.contains("전자레인지") || raw.contains("전자")) return R.drawable.ic_tool_microwave;
        if (raw.contains("오븐")) return R.drawable.ic_tool_oven;
        if (raw.contains("밥솥")) return R.drawable.ic_tool_rice_cooker;
        if (raw.contains("찜기")) return R.drawable.ic_tool_steamer;

        String t = normalizeToolCode(raw);

        switch (t) {
            case "AIR_FRYER":
                return R.drawable.ic_tool_air_fryer;
            case "BLENDER":
                return R.drawable.ic_tool_blender;
            case "GAS_RANGE":
                return R.drawable.ic_tool_gas_range;
            case "INDUCTION":
                return R.drawable.ic_tool_induction;
            case "MICROWAVE":
                return R.drawable.ic_tool_microwave;
            case "OVEN":
                return R.drawable.ic_tool_oven;
            case "RICE_COOKER":
                return R.drawable.ic_tool_rice_cooker;
            case "STEAMER":
                return R.drawable.ic_tool_steamer;
        }
        return 0;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    private String normalizeIngredient(String s) {
        if (s == null) return "";
        return s.trim().replace(" ", "").toLowerCase(Locale.getDefault());
    }
    private List<String> toIngredientNamesOnly(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;

        for (String s : list) {
            String name = extractIngredientNameOnly(s);

            if (!TextUtils.isEmpty(name) && !out.contains(name)) {
                out.add(name);
            }
        }

        return out;
    }

    private String extractIngredientNameOnly(String raw) {
        if (raw == null) return "";

        String s = raw.trim();

        s = s.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("|", "\n");

        if (s.contains("\n")) {
            String first = s.split("\\n")[0].trim();
            if (!first.isEmpty()) s = first;
        }

        s = s.replaceAll("\\s+\\d+/\\d+(개|장|컵|스푼|큰술|작은술|줄|꼬집|공기|알|봉|팩|g|ml)$", "");
        s = s.replaceAll("\\s+\\d+(\\.\\d+)?(개|장|컵|스푼|큰술|작은술|줄|꼬집|공기|알|봉|팩|g|ml)$", "");
        s = s.replaceAll("\\s+(한줌|약간|조금|적당히)$", "");

        return s.trim();
    }
    private String deriveThumbnailUrl(String rawMainUrl) {
        String main = resolveImageUrl(rawMainUrl);
        if (TextUtils.isEmpty(main)) return null;

        // ✅ Cloudinary 최적화 썸네일 생성
        if (main.startsWith("https://res.cloudinary.com/")) {

            // 이미 변환 옵션 있으면 그대로 사용
            if (main.contains("/upload/c_")) {
                return main;
            }

            return main.replace(
                    "/image/upload/",
                    "/image/upload/c_fill,w_300,h_300,q_auto,f_auto/"
            );
        }

        // 기존 로컬 방식
        if (main.contains("_thumb.webp")) return main;

        if (main.endsWith(".webp")) {
            return main.substring(0, main.length() - ".webp".length()) + "_thumb.webp";
        }

        return null;
    }
    private String resolveImageUrl(String raw) {
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
            return url;
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        TextView summary;
        TextView usedIngredients;
        TextView missingIngredients;
        TextView matchPercent;
        RatingBar ratingBarCard;
        TextView ratingText;
        TextView author;
        ImageView favoriteStar;
        LinearLayout cookToolsLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivRecipeImage);
            name = itemView.findViewById(R.id.tvRecipeName);
            summary = itemView.findViewById(R.id.tvRecipeSummary);
            usedIngredients = itemView.findViewById(R.id.tvUsedIngredients);
            missingIngredients = itemView.findViewById(R.id.tvMissingIngredients);
            matchPercent = itemView.findViewById(R.id.tvMatchPercent);
            ratingBarCard = itemView.findViewById(R.id.ratingBarCard);
            ratingText = itemView.findViewById(R.id.tvRatingCard);
            author = itemView.findViewById(R.id.tvAuthor);
            favoriteStar = itemView.findViewById(R.id.ivFavoriteStar);
            cookToolsLayout = itemView.findViewById(R.id.layoutCookTools);
        }
    }

    private static final DiffUtil.ItemCallback<RecipeResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RecipeResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull RecipeResponse oldItem, @NonNull RecipeResponse newItem) {
                    if (oldItem.getId() == null || newItem.getId() == null) return false;
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull RecipeResponse oldItem, @NonNull RecipeResponse newItem) {
                    if (!safeEq(oldItem.getName(), newItem.getName())) return false;
                    if (!safeEq(oldItem.getSummary(), newItem.getSummary())) return false;
                    if (!safeEq(oldItem.getAuthorNickname(), newItem.getAuthorNickname())) return false;

                    Long oldAuthorId = oldItem.getAuthorId();
                    Long newAuthorId = newItem.getAuthorId();
                    if (oldAuthorId == null && newAuthorId != null) return false;
                    if (oldAuthorId != null && !oldAuthorId.equals(newAuthorId)) return false;

                    if (!safeEq(oldItem.getImageUrl(), newItem.getImageUrl())) return false;
                    if (!safeEq(oldItem.getThumbnailUrl(), newItem.getThumbnailUrl())) return false;

                    if (oldItem.getMatchPercent() != newItem.getMatchPercent()) return false;

                    String ot = (oldItem.getCookToolsText() == null) ? "" : oldItem.getCookToolsText();
                    String nt = (newItem.getCookToolsText() == null) ? "" : newItem.getCookToolsText();
                    if (!ot.equals(nt)) return false;

                    if (oldItem.getRatingCount() != newItem.getRatingCount()) return false;
                    if (Float.compare(oldItem.getRatingAverage(), newItem.getRatingAverage()) != 0) return false;

                    return true;
                }

                private boolean safeEq(String a, String b) {
                    if (a == null) a = "";
                    if (b == null) b = "";
                    return a.equals(b);
                }
            };

    private long computeSortSignature(@NonNull List<RecipeResponse> items) {
        long h = 1469598103934665603L;
        h = fnv(h, listType == null ? 0 : listType.ordinal());

        for (String t : userCookTools) {
            h = fnv(h, t.hashCode());
        }

        for (RecipeResponse r : items) {
            if (r == null || r.getId() == null) continue;
            h = fnv(h, r.getId().hashCode());
            h = fnv(h, r.getMatchPercent());
            String ct = (r.getCookToolsText() == null) ? "" : r.getCookToolsText();
            h = fnv(h, ct.hashCode());
        }
        return h;
    }

    private long fnv(long h, int v) {
        h ^= (long) v;
        h *= 1099511628211L;
        return h;
    }

    private List<String> splitIngredientsToList(String ing) {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(ing)) return out;

        String normalized = ing
                .replace("\\r\\n", ",")
                .replace("\\n", ",")
                .replace("\r\n", ",")
                .replace("\n", ",")
                .replace("|", ",");

        String[] parts = normalized.split(",");

        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (!t.isEmpty() && !"-".equals(t)) out.add(t);
        }

        return out;
    }
}