package com.namgyun.tamakitchen.ui.menu;

public class InquiryRequest {

    private String subject;
    private String content;

    private Long userId;
    private String nickname;
    private String email;

    private String appVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private String osVersion;
    private Integer sdkInt;

    public InquiryRequest() {}

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getDeviceManufacturer() { return deviceManufacturer; }
    public void setDeviceManufacturer(String deviceManufacturer) { this.deviceManufacturer = deviceManufacturer; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public Integer getSdkInt() { return sdkInt; }
    public void setSdkInt(Integer sdkInt) { this.sdkInt = sdkInt; }
}
