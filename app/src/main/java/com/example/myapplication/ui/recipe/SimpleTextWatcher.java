package com.namgyun.tamakitchen.ui.recipe;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

public abstract class SimpleTextWatcher implements TextWatcher {

    public static SimpleTextWatcher after(Consumer<Editable> consumer) {
        return new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                consumer.accept(s);
            }
        };
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    @Override public void afterTextChanged(Editable s) {}
}
