package com.namgyun.tamakitchen.ui.shopping;

import androidx.annotation.NonNull;

public class ReceiptLine {
    public enum Confidence { HIGH, MEDIUM, LOW }

    private final long id;
    private boolean checked;

    private String name;
    private double qty;
    private int price; // 원 단위 (라인 금액 or 단가? -> 여기서는 "라인 금액"으로 취급)
    private Confidence confidence;

    // 디버깅/확인용: 파서가 어떤 라인에서 만들었는지
    private String sourceLine;

    public ReceiptLine(long id,
                       String name,
                       double qty,
                       int price,
                       boolean checked,
                       Confidence confidence,
                       String sourceLine) {
        this.id = id;
        this.name = name;
        this.qty = qty;
        this.price = price;
        this.checked = checked;
        this.confidence = confidence;
        this.sourceLine = sourceLine;
    }

    public long getId() { return id; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQty() { return qty; }
    public void setQty(double qty) { this.qty = qty; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public Confidence getConfidence() { return confidence; }
    public void setConfidence(Confidence confidence) { this.confidence = confidence; }

    public String getSourceLine() { return sourceLine; }
    public void setSourceLine(String sourceLine) { this.sourceLine = sourceLine; }

    @NonNull
    @Override
    public String toString() {
        return "ReceiptLine{id=" + id + ", checked=" + checked + ", name=" + name +
                ", qty=" + qty + ", price=" + price + ", conf=" + confidence + "}";
    }
}