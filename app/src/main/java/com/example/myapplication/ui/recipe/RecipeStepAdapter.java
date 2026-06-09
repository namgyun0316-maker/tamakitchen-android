package com.namgyun.tamakitchen.ui.recipe;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder; // ✅ 여기! (Glide 패키지)
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.namgyun.tamakitchen.R;

import java.util.ArrayList;
import java.util.List;

public class RecipeStepAdapter extends RecyclerView.Adapter<RecipeStepAdapter.StepViewHolder> {

    public interface OnStepImageClickListener {
        void onSelectImage(int stepIndex);
    }

    private final List<RecipeStepForm> steps = new ArrayList<>();
    private final OnStepImageClickListener imageClickListener;

    private int currentIndex = 0;

    /**
     * ✅ 서버가 "/uploads/..." 상대경로를 내려줄 때 붙일 baseUrl
     * 예) http://192.168.45.126:8080
     *
     * - 서버가 이미 풀URL을 내려주면 이 값은 무시됨
     * - 필요할 때 Fragment/Activity에서 setServerBaseUrl(...)로 세팅
     */
    private String serverBaseUrl = "";

    public RecipeStepAdapter(OnStepImageClickListener listener) {
        this.imageClickListener = listener;
        steps.add(new RecipeStepForm());
    }

    public void setServerBaseUrl(String baseUrl) {
        this.serverBaseUrl = safeTrim(baseUrl);
        notifyItemChanged(0);
    }

    public void setSteps(List<RecipeStepForm> newSteps) {
        steps.clear();
        if (newSteps != null && !newSteps.isEmpty()) {
            steps.addAll(newSteps);
        } else {
            steps.add(new RecipeStepForm());
        }
        currentIndex = 0;
        notifyDataSetChanged();
    }

    public void goToNextStepOrAdd() {
        if (currentIndex == steps.size() - 1) {
            steps.add(new RecipeStepForm());
        }
        currentIndex++;
        notifyItemChanged(0);
    }

    public void goToPrevStep() {
        if (currentIndex > 0) {
            currentIndex--;
            notifyItemChanged(0);
        }
    }

    public int getCurrentStepNumber() {
        return currentIndex + 1;
    }

    public List<RecipeStepForm> getSteps() {
        return steps;
    }

    public void setStepImage(int stepIndex, String uriString) {
        if (stepIndex >= 0 && stepIndex < steps.size()) {
            steps.get(stepIndex).setImageUrl(uriString);
            // 편집모드에서는 썸네일이 따로 없으니 비워둠
            // (RecipeStepForm에 thumbnailUrl 필드/세터가 있어야 함)
            steps.get(stepIndex).setThumbnailUrl("");
            if (stepIndex == currentIndex) notifyItemChanged(0);
        }
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_step_edit, parent, false);
        return new StepViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        if (steps.isEmpty()) return;

        RecipeStepForm item = steps.get(currentIndex);
        holder.bind(item, currentIndex + 1);

        holder.btnSelectImage.setOnClickListener(v -> {
            if (imageClickListener != null) {
                imageClickListener.onSelectImage(currentIndex);
            }
        });
    }

    @Override
    public int getItemCount() {
        return steps.isEmpty() ? 0 : 1;
    }

    @Override
    public void onViewRecycled(@NonNull StepViewHolder holder) {
        super.onViewRecycled(holder);
        try {
            Glide.with(holder.itemView.getContext()).clear(holder.ivStepImage);
        } catch (Exception ignore) {}
        holder.ivStepImage.setImageDrawable(null);
    }

    class StepViewHolder extends RecyclerView.ViewHolder {

        TextView tvStepTitle;
        EditText etStepText;
        ImageView ivStepImage;
        Button btnSelectImage;

        private TextWatcher textWatcher;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepTitle = itemView.findViewById(R.id.tvStepTitle);
            etStepText = itemView.findViewById(R.id.etStepText);
            ivStepImage = itemView.findViewById(R.id.ivStepImage);
            btnSelectImage = itemView.findViewById(R.id.btnSelectImage);
        }

        void bind(RecipeStepForm item, int stepNumber) {
            tvStepTitle.setText(stepNumber + "단계");

            if (textWatcher != null) {
                etStepText.removeTextChangedListener(textWatcher);
            }

            etStepText.setText(item.getText());
            textWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    item.setText(s.toString());
                }
            };
            etStepText.addTextChangedListener(textWatcher);

            // ✅ 썸네일 우선 로드
            bindStepImage(item.getThumbnailUrl(), item.getImageUrl());
        }

        /**
         * ✅ thumb 우선 → 실패하면 원본(imageUrl) 로드
         * - thumb가 비었으면 원본만 로드
         */
        private void bindStepImage(String thumbUrlOrUri, String imageUrlOrUri) {

            try {
                Glide.with(itemView.getContext()).clear(ivStepImage);
            } catch (Exception ignore) {}

            if (TextUtils.isEmpty(thumbUrlOrUri) && TextUtils.isEmpty(imageUrlOrUri)) {
                ivStepImage.setImageDrawable(null);
                return;
            }

            // 최종 model (상대경로면 baseUrl 붙임)
            Object mainModel = normalizeToGlideModel(resolveIfRelative(imageUrlOrUri));
            Object thumbModel = normalizeToGlideModel(resolveIfRelative(thumbUrlOrUri));

            RequestOptions opts = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .priority(Priority.LOW)
                    .centerCrop()
                    .dontAnimate();

            // ✅ 원본 요청
            RequestBuilder<Drawable> mainReq = Glide.with(itemView.getContext())
                    .load(mainModel)
                    .apply(opts)
                    .placeholder(R.drawable.bg_recipe_placeholder)
                    .error(R.drawable.bg_recipe_placeholder);

            // ✅ 썸네일이 있으면: thumbnail을 "실제 썸네일 요청"으로 붙인다
            if (!TextUtils.isEmpty(thumbUrlOrUri)) {
                RequestBuilder<Drawable> thumbReq = Glide.with(itemView.getContext())
                        .load(thumbModel)
                        .apply(opts);

                // thumbReq가 실패해도 mainReq가 최종으로 남음
                mainReq.thumbnail(thumbReq).into(ivStepImage);
                return;
            }

            // 썸네일이 없으면 원본만
            mainReq.into(ivStepImage);
        }

        /**
         * raw가 "/uploads/..." 또는 "uploads/..."이면 baseUrl 붙여서 풀URL로 만든다.
         * raw가 이미 http(s) 또는 file/content/android.resource면 그대로 둔다.
         */
        private String resolveIfRelative(String raw) {
            String s = safeTrim(raw);
            if (s.isEmpty()) return "";

            // 로컬 uri는 그대로
            if (s.startsWith("content://") || s.startsWith("file://") || s.startsWith("android.resource://")) {
                return s;
            }

            // 이미 풀 URL이면 그대로
            if (s.startsWith("http://") || s.startsWith("https://")) {
                return s;
            }

            // 상대경로 처리: "/uploads/..." or "uploads/..."
            if (s.startsWith("/uploads/") || s.startsWith("uploads/")) {
                String base = safeTrim(serverBaseUrl);
                if (base.isEmpty()) {
                    // baseUrl이 없으면 그냥 원본 반환 (앱에서 로드 실패 가능)
                    return s;
                }
                while (base.endsWith("/")) base = base.substring(0, base.length() - 1);

                if (!s.startsWith("/")) s = "/" + s;
                return base + s;
            }

            return s;
        }

        private Object normalizeToGlideModel(String raw) {
            String s = safeTrim(raw);
            if (s.isEmpty()) return "";

            if (s.startsWith("content://") || s.startsWith("file://") || s.startsWith("android.resource://")) {
                try { return Uri.parse(s); } catch (Exception e) { return s; }
            }

            if (s.startsWith("http://") || s.startsWith("https://")) {
                return s;
            }

            try { return Uri.parse(s); }
            catch (Exception e) { return s; }
        }
    }

    private String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}