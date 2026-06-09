package com.namgyun.tamakitchen.ui.recipe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SubstituteItem implements Serializable {

    private String original;          // 대표재료
    private List<String> alternatives; // 대체재료들

    public SubstituteItem(String original, List<String> alternatives) {
        this.original = original;
        this.alternatives = alternatives != null ? alternatives : new ArrayList<>();
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<String> alternatives) {
        this.alternatives = alternatives;
    }
}
