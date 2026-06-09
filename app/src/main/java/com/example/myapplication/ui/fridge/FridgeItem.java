package com.namgyun.tamakitchen.ui.fridge;

import android.text.TextUtils;

import com.namgyun.tamakitchen.ui.shopping.IconCatalog;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FridgeItem {

    @Expose
    private Long id;

    @Expose
    private String name;

    @Expose
    private String storage;

    @SerializedName("quantity")
    @Expose
    private double quantity;

    @Expose(serialize = false, deserialize = false)
    private boolean isChecked;

    @Expose(serialize = false, deserialize = false)
    private int iconResId;

    @SerializedName("iconName")
    @Expose
    private String iconName;

    @Expose
    private String expiryDate;

    @Expose(serialize = false, deserialize = false)
    private String registrationDate;

    @SerializedName("unit")
    @Expose
    private String unit;

    @SerializedName("addedByNickname")
    @Expose
    private String addedByNickname;

    @Expose(serialize = false, deserialize = false)
    private boolean expiryManuallySet;

    public String getUnit() {
        return TextUtils.isEmpty(unit) ? "개" : unit.trim();
    }

    public void setUnit(String unit) {
        this.unit = (unit == null) ? "개" : unit.trim();
        if (TextUtils.isEmpty(this.unit)) this.unit = "개";
    }

    public String getAddedByNickname() {
        return (addedByNickname == null) ? "" : addedByNickname.trim();
    }

    public void setAddedByNickname(String addedByNickname) {
        this.addedByNickname = (addedByNickname == null) ? "" : addedByNickname.trim();
    }

    public boolean isExpiryManuallySet() {
        return expiryManuallySet;
    }

    public void setExpiryManuallySet(boolean expiryManuallySet) {
        this.expiryManuallySet = expiryManuallySet;
    }

    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public FridgeItem() {
        this.registrationDate = getCurrentDate();
        if (TextUtils.isEmpty(this.unit)) this.unit = "개";
        if (this.addedByNickname == null) this.addedByNickname = "";
        this.expiryManuallySet = false;
    }

    public FridgeItem(String name,
                      String storage,
                      double quantity,
                      boolean isChecked,
                      int iconResId,
                      String iconName,
                      String expiryDate,
                      String registrationDate,
                      String unit) {

        this.name = name;
        this.storage = storage;
        this.quantity = quantity;
        this.isChecked = isChecked;
        this.iconResId = iconResId;

        this.iconName = (iconName == null) ? "" : iconName.trim();
        this.expiryDate = (expiryDate == null) ? "" : expiryDate.trim();
        this.registrationDate = TextUtils.isEmpty(registrationDate) ? getCurrentDate() : registrationDate.trim();
        this.unit = (unit == null) ? "개" : unit.trim();
        if (TextUtils.isEmpty(this.unit)) this.unit = "개";

        if (this.addedByNickname == null) this.addedByNickname = "";
        this.expiryManuallySet = false;

        if (this.iconResId != 0) {
            String rn = ResourceNameHelper.getNameFromResId(this.iconResId);
            if (!TextUtils.isEmpty(rn)) {
                this.iconName = rn;
            }
        } else {
            recalcIconResIdFromIconName();
        }
    }

    public FridgeItem(String name, String storage, double quantity, boolean isChecked, int iconResId, String expiryDate) {
        this(name, storage, quantity, isChecked, iconResId, "", expiryDate, getCurrentDate(), "개");
    }

    public FridgeItem(String name, String storage, double quantity, boolean isChecked, int iconResId) {
        this(name, storage, quantity, isChecked, iconResId, "", "", getCurrentDate(), "개");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStorage() { return storage; }
    public double getQuantity() { return quantity; }
    public String getExpiryDate() { return expiryDate; }
    public String getExpireDate() { return expiryDate; }
    public String getIconName() { return iconName; }
    public boolean isChecked() { return isChecked; }
    public String getRegistrationDate() { return registrationDate; }

    public int getIconResId() {
        if (iconResId != 0) return iconResId;
        if (TextUtils.isEmpty(iconName)) return 0;

        recalcIconResIdFromIconName();
        return iconResId;
    }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStorage(String storage) { this.storage = storage; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public void setExpireDate(String expireDate) { this.expiryDate = expireDate; }

    public void setChecked(boolean checked) { isChecked = checked; }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = TextUtils.isEmpty(registrationDate) ? getCurrentDate() : registrationDate.trim();
    }

    public void setIconName(String iconName) {
        this.iconName = (iconName == null) ? "" : iconName.trim();

        if (TextUtils.isEmpty(this.iconName)) {
            this.iconResId = 0;
            return;
        }
        recalcIconResIdFromIconName();
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;

        if (iconResId != 0) {
            String rn = ResourceNameHelper.getNameFromResId(iconResId);
            if (!TextUtils.isEmpty(rn)) {
                this.iconName = rn;
            } else if (this.iconName == null) {
                this.iconName = "";
            }
        }
    }

    private void recalcIconResIdFromIconName() {
        if (TextUtils.isEmpty(iconName)) {
            iconResId = 0;
            return;
        }

        int res = ResourceNameHelper.getResIdFromName(iconName);
        if (res == 0) {
            res = IconCatalog.findResIdByRawKey(iconName);
        }
        iconResId = res;
    }

    public boolean isNearExpire() {
        if (TextUtils.isEmpty(expiryDate)) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date today = sdf.parse(sdf.format(new Date()));
            Date end = sdf.parse(expiryDate);
            if (today == null || end == null) return false;

            long diff = end.getTime() - today.getTime();
            long days = diff / (24L * 60L * 60L * 1000L);
            return days >= 0 && days <= 7;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isExpired() {
        if (TextUtils.isEmpty(expiryDate)) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date today = sdf.parse(sdf.format(new Date()));
            Date end = sdf.parse(expiryDate);
            if (today == null || end == null) return false;
            return end.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    public String getDDayString() {
        if (TextUtils.isEmpty(expiryDate)) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date today = sdf.parse(sdf.format(new Date()));
            Date end = sdf.parse(expiryDate);
            if (today == null || end == null) return "";

            long diff = end.getTime() - today.getTime();
            long days = diff / (24L * 60L * 60L * 1000L);

            if (days > 0) return "D-" + days;
            if (days == 0) return "D-Day";
            return "유통기한 지남";
        } catch (Exception e) {
            return "";
        }
    }
}