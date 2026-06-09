package com.namgyun.tamakitchen.ui.menu;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.namgyun.tamakitchen.ui.onboarding.OnboardingPrefs;
import com.google.android.material.card.MaterialCardView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ToolsSettingActivity extends AppCompatActivity {

    private final Set<String> selectedTools = new HashSet<>();

    private static class ToolDef {
        final String key;
        final String label;
        final int iconRes;
        final int viewId;

        ToolDef(String key, String label, int iconRes, int viewId) {
            this.key = key;
            this.label = label;
            this.iconRes = iconRes;
            this.viewId = viewId;
        }
    }

    private final List<ToolDef> TOOL_DEFS = Arrays.asList(
            new ToolDef("air_fryer", "에어프라이어", R.drawable.ic_tool_air_fryer, R.id.toolAirFryer),
            new ToolDef("oven", "오븐", R.drawable.ic_tool_oven, R.id.toolOven),
            new ToolDef("microwave", "전자레인지", R.drawable.ic_tool_microwave, R.id.toolMicrowave),
            new ToolDef("blender", "블렌더", R.drawable.ic_tool_blender, R.id.toolBlender),
            new ToolDef("rice_cooker", "전기밥솥", R.drawable.ic_tool_rice_cooker, R.id.toolRiceCooker),
            new ToolDef("steamer", "찜기", R.drawable.ic_tool_steamer, R.id.toolSteamer),
            new ToolDef("gas_range", "가스레인지", R.drawable.ic_tool_gas_range, R.id.toolGasRange),
            new ToolDef("induction", "인덕션", R.drawable.ic_tool_induction, R.id.toolInduction)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools_setting);

        selectedTools.clear();
        selectedTools.addAll(OnboardingPrefs.getTools(this));

        bindHeader();
        bindTools();
        bindSave();
    }

    private void bindHeader() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("조리도구 설정");
    }

    private void bindTools() {
        for (ToolDef def : TOOL_DEFS) {
            android.view.View toolRoot = findViewById(def.viewId);
            if (toolRoot == null) continue;

            MaterialCardView card = resolveCard(toolRoot);
            if (card == null) continue;

            android.widget.ImageView iv = card.findViewById(R.id.ivIcon);
            android.widget.TextView tv = card.findViewById(R.id.tvName);

            if (iv != null) iv.setImageResource(def.iconRes);
            if (tv != null) tv.setText(def.label);

            boolean selected = selectedTools.contains(def.key);
            applyCardStyle(card, selected);

            android.view.View.OnClickListener toggle = v -> {
                boolean nowSelected = toggleKey(def.key);
                applyCardStyle(card, nowSelected);
            };

            card.setOnClickListener(toggle);
            toolRoot.setOnClickListener(toggle);
        }
    }

    private void bindSave() {
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            OnboardingPrefs.saveTools(this, selectedTools);
            AppToast.show(this, "조리도구가 저장됐어요");
            finish();
        });
    }

    private boolean toggleKey(String key) {
        if (selectedTools.contains(key)) {
            selectedTools.remove(key);
            return false;
        }
        selectedTools.add(key);
        return true;
    }

    private MaterialCardView resolveCard(android.view.View toolRoot) {
        if (toolRoot instanceof MaterialCardView) return (MaterialCardView) toolRoot;
        android.view.View found = toolRoot.findViewById(R.id.cardTool);
        if (found instanceof MaterialCardView) return (MaterialCardView) found;
        return null;
    }

    private void applyCardStyle(MaterialCardView card, boolean selected) {
        int strokeSelected = ContextCompat.getColor(this, R.color.pascal_blue_light);
        int strokeNormal = ContextCompat.getColor(this, R.color.gray_200);

        card.setStrokeColor(selected ? strokeSelected : strokeNormal);
        card.setStrokeWidth(dp(selected ? 3 : 1));
        card.setScaleX(selected ? 1.03f : 1f);
        card.setScaleY(selected ? 1.03f : 1f);
        card.setCardElevation(selected ? dpF(4) : 0f);
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }

    private float dpF(int v) {
        float d = getResources().getDisplayMetrics().density;
        return v * d;
    }
}