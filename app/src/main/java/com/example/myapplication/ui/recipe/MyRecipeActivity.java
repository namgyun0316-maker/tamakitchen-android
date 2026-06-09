package com.namgyun.tamakitchen.ui.recipe;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.RecipeApiService;
import com.namgyun.tamakitchen.network.RetrofitClient;
import com.namgyun.tamakitchen.ui.auth.AuthPrefs;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.common.NetworkErrorUtil;
import com.namgyun.tamakitchen.ui.recipe.adapter.RecipeAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyRecipeActivity extends AppCompatActivity {

    private TextView tvEmpty;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddRecipe;

    private RecipeAdapter adapter;
    private RecipeApiService service;

    private long loginUserId = -1L;
    private boolean isLoading = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipe);

        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerMyRecipe);
        fabAddRecipe = findViewById(R.id.fabAddRecipe);

        service = RetrofitClient.getRecipeApi();
        loginUserId = AuthPrefs.getUserId(this);

        adapter = new RecipeAdapter(this, RecipeAdapter.ListType.ALL);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        fabAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecipeWriteActivity.class);
            startActivity(intent);
        });

        adapter.setOnRecipeClickListener(item -> {
            if (item == null) return;

            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, item);
            intent.putStringArrayListExtra(
                    RecipeDetailActivity.EXTRA_USED_INGREDIENTS,
                    toArrayList(item.getUsedIngredients())
            );
            intent.putStringArrayListExtra(
                    RecipeDetailActivity.EXTRA_MISSING_INGREDIENTS,
                    toArrayList(item.getMissingIngredients())
            );
            intent.putExtra(RecipeDetailActivity.EXTRA_MATCH_PERCENT, item.getMatchPercent());
            intent.putExtra(RecipeDetailActivity.EXTRA_LIST_TYPE, RecipeAdapter.ListType.ALL.name());

            startActivity(intent);
        });

        adapter.setOnRecipeDeleteListener((item, position) -> {
            if (item == null || item.getId() == null) {
                AppToast.show(this, "레시피 정보를 확인할 수 없습니다.");
                return;
            }

            Long authorId = item.getAuthorId();
            if (loginUserId <= 0 || authorId == null || !authorId.equals(loginUserId)) {
                AppToast.show(this, "본인이 작성한 레시피만 삭제할 수 있습니다.");
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("레시피 삭제")
                    .setMessage("'" + item.getName() + "' 레시피를 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) -> deleteRecipe(item.getId()))
                    .setNegativeButton("취소", null)
                    .show();
        });

        adapter.setOnFavoriteChangedListener(this::loadMyRecipes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyRecipes();
    }

    private void loadMyRecipes() {
        if (isLoading) return;

        if (loginUserId <= 0) {
            showEmpty("로그인 후 내가 등록한 레시피를 확인할 수 있어요.");
            return;
        }

        isLoading = true;

        service.getMyRecipes(loginUserId).enqueue(new Callback<List<RecipeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecipeResponse>> call,
                                   @NonNull Response<List<RecipeResponse>> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    List<RecipeResponse> list = response.body();

                    if (list.isEmpty()) {
                        showEmpty("아직 등록한 레시피가 없어요.\n오른쪽 아래 + 버튼으로 추가해보세요.");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        adapter.forceResort();
                        adapter.submitItems(list);
                    }
                } else {
                    showEmpty("내 레시피를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RecipeResponse>> call,
                                  @NonNull Throwable t) {
                isLoading = false;
                showEmpty(NetworkErrorUtil.getUserMessage(t));
            }
        });
    }

    private void deleteRecipe(Long recipeId) {
        if (recipeId == null || loginUserId <= 0) return;

        service.deleteRecipe(recipeId, loginUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    AppToast.show(MyRecipeActivity.this, "삭제되었습니다.");
                    loadMyRecipes();
                } else {
                    AppToast.show(
                            MyRecipeActivity.this,
                            "삭제에 실패했어요. 잠시 후 다시 시도해주세요."
                    );
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call,
                                  @NonNull Throwable t) {
                AppToast.show(
                        MyRecipeActivity.this,
                        NetworkErrorUtil.getUserMessage(t)
                );
            }
        });
    }

    private void showEmpty(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        adapter.submitItems(new ArrayList<>());
    }

    private ArrayList<String> toArrayList(List<String> list) {
        ArrayList<String> out = new ArrayList<>();
        if (list == null) return out;

        for (String s : list) {
            if (s == null) continue;

            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }

        return out;
    }
}