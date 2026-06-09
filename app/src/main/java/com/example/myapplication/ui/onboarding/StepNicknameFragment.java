package com.namgyun.tamakitchen.ui.onboarding;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.namgyun.tamakitchen.R;
import com.google.android.material.textfield.TextInputLayout;

public class StepNicknameFragment extends Fragment {

    private static final int MAX_LENGTH = 8;

    private EditText etNickname;
    private TextView tvHint;
    private TextView tvCounter;
    private TextInputLayout tilNickname;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_onboarding_nickname, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        etNickname = view.findViewById(R.id.etNickname);
        tvHint = view.findViewById(R.id.tvNickHint);
        tvCounter = view.findViewById(R.id.tvNickCounter);
        tilNickname = view.findViewById(R.id.tilNickname);

        if (etNickname == null) return;

        // 1. 입력 길이 제한 (하드 제한)
        etNickname.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(MAX_LENGTH)
        });

        // 2. 저장된 닉네임 복원 (8자 초과 시 잘라냄)
        String saved = OnboardingPrefs.getNickname(requireContext());
        if (saved != null && !saved.trim().isEmpty()) {
            if (saved.length() > MAX_LENGTH) {
                saved = saved.substring(0, MAX_LENGTH);
            }
            etNickname.setText(saved);
            etNickname.setSelection(saved.length());

            OnboardingActivity act = (OnboardingActivity) getActivity();
            if (act != null) act.setNickname(saved);
        }

        updateCounter(etNickname.getText().toString());

        etNickname.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String raw = (s == null) ? "" : s.toString();
                String trimmed = raw.trim();

                // Activity 상태 반영
                OnboardingActivity activity = (OnboardingActivity) getActivity();
                if (activity != null) {
                    activity.setNickname(raw);
                }

                // prefs 즉시 저장
                OnboardingPrefs.saveNickname(requireContext(), raw);

                updateCounter(raw);

                if (tvHint == null) return;

                if (trimmed.isEmpty()) {
                    tvHint.setText("닉네임은 최대 8자까지 입력할 수 있습니다.");
                } else if (trimmed.length() < 2) {
                    tvHint.setText("2자 이상 입력을 권장합니다.");
                } else {
                    tvHint.setText("사용 가능한 닉네임입니다.");
                }
            }
        });
    }

    private void updateCounter(String text) {
        if (tvCounter == null) return;
        int length = (text == null) ? 0 : text.length();
        tvCounter.setText(length + " / " + MAX_LENGTH);
    }
}