package com.namgyun.tamakitchen.ui.onboarding;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SharedFridgePrefs {

    private static final String TAG = "SharedFridgePrefs";
    private static final String PREFS = "shared_fridge_prefs";

    private static final String KEY_FRIDGE_ID = "shared_fridge_id";
    private static final String KEY_FRIDGE_NAME = "shared_fridge_name";

    private static final String KEY_INVITE_CODE = "invite_code";
    private static final String KEY_INVITE_EXPIRES_AT = "invite_expires_at";

    private static final String KEY_MEMBERS_JSON = "members_json";

    private static final String KEY_PENDING_INVITE_CODE = "pending_invite_code";
    private static final String KEY_PENDING_FRIDGE_NAME = "pending_fridge_name";
    private static final String KEY_PENDING_EXPIRES_AT = "pending_expires_at";

    private static final long DEFAULT_EXPIRE_MS = 7L * 24L * 60L * 60L * 1000L;

    private SharedFridgePrefs() {}

    private static boolean isLikelyTimestampId(long id) {
        return id >= 1_000_000_000_000L;
    }

    private static void clearBadFridgeIdEverywhere(Context c, long badId) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().remove(KEY_FRIDGE_ID).apply();
        } catch (Exception e) {
            Log.w(TAG, "clear bad fridge id from shared prefs failed", e);
        }

        try {
            OnboardingPrefs.saveFridgeId(c, 0L);
        } catch (Exception e) {
            Log.w(TAG, "clear bad fridge id from onboarding prefs failed", e);
        }

        Log.w(TAG, "Cleared invalid sharedFridgeId=" + badId);
    }

    public static void saveFridgeId(Context c, long fridgeId) {
        if (c == null) return;

        if (fridgeId <= 0) {
            clearBadFridgeIdEverywhere(c, fridgeId);
            return;
        }

        if (isLikelyTimestampId(fridgeId)) {
            clearBadFridgeIdEverywhere(c, fridgeId);
            return;
        }

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putLong(KEY_FRIDGE_ID, fridgeId).apply();
            Log.d(TAG, "Saved sharedFridgeId=" + fridgeId);
        } catch (Exception e) {
            Log.w(TAG, "save fridge id failed", e);
        }
    }

    public static long getFridgeId(Context c) {
        if (c == null) return 0L;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long id = p.getLong(KEY_FRIDGE_ID, 0L);

            if (id > 0 && isLikelyTimestampId(id)) {
                clearBadFridgeIdEverywhere(c, id);
                return 0L;
            }

            return id;
        } catch (Exception e) {
            Log.w(TAG, "get fridge id failed", e);
            return 0L;
        }
    }

    public static void saveFridgeName(Context c, String name) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putString(KEY_FRIDGE_NAME, name == null ? "" : name.trim()).apply();
        } catch (Exception e) {
            Log.w(TAG, "save fridge name failed", e);
        }
    }

    public static String getFridgeName(Context c) {
        if (c == null) return "";

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return p.getString(KEY_FRIDGE_NAME, "");
        } catch (Exception e) {
            Log.w(TAG, "get fridge name failed", e);
            return "";
        }
    }

    public static void saveInvite(Context c, String code, long expiresAt) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit()
                    .putString(KEY_INVITE_CODE, code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                    .putLong(KEY_INVITE_EXPIRES_AT, expiresAt)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "save invite failed", e);
        }
    }

    public static String getInviteCode(Context c) {
        if (c == null) return "";

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return p.getString(KEY_INVITE_CODE, "");
        } catch (Exception e) {
            Log.w(TAG, "get invite code failed", e);
            return "";
        }
    }

    public static long getInviteExpiresAt(Context c) {
        if (c == null) return 0L;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return p.getLong(KEY_INVITE_EXPIRES_AT, 0L);
        } catch (Exception e) {
            Log.w(TAG, "get invite expires at failed", e);
            return 0L;
        }
    }

    public static InviteInfo regenerateInvite(Context c) {
        String code = generateInviteCode();
        long exp = System.currentTimeMillis() + DEFAULT_EXPIRE_MS;
        saveInvite(c, code, exp);
        return new InviteInfo(code, exp);
    }

    public static InviteInfo ensureInviteExists(Context c) {
        String code = getInviteCode(c);
        long exp = getInviteExpiresAt(c);

        if (code == null) code = "";
        code = code.trim().toUpperCase(Locale.ROOT);

        if (code.isEmpty() || exp <= 0L) {
            return regenerateInvite(c);
        }

        return new InviteInfo(code, exp);
    }

    private static String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public static void joinWithInvite(Context c, String fridgeName, String code, long expiresAt) {
        if (c == null) return;

        String name = fridgeName == null ? "" : fridgeName.trim();
        if (name.isEmpty()) name = "공동 냉장고";

        String invite = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (invite.isEmpty()) return;

        savePendingInvite(c, name, invite, expiresAt);
        OnboardingPrefs.saveFridgeType(c, "SHARED");
    }

    public static void savePendingInvite(Context c, String fridgeName, String inviteCode, long expiresAt) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit()
                    .putString(KEY_PENDING_FRIDGE_NAME, fridgeName == null ? "" : fridgeName.trim())
                    .putString(KEY_PENDING_INVITE_CODE, inviteCode == null ? "" : inviteCode.trim().toUpperCase(Locale.ROOT))
                    .putLong(KEY_PENDING_EXPIRES_AT, expiresAt)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "save pending invite failed", e);
        }
    }

    public static PendingInvite getPendingInvite(Context c) {
        if (c == null) return null;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String code = p.getString(KEY_PENDING_INVITE_CODE, "");
            String name = p.getString(KEY_PENDING_FRIDGE_NAME, "");
            long exp = p.getLong(KEY_PENDING_EXPIRES_AT, 0L);

            if (code == null) code = "";
            code = code.trim().toUpperCase(Locale.ROOT);

            if (code.isEmpty()) return null;

            return new PendingInvite(name, code, exp);

        } catch (Exception e) {
            Log.w(TAG, "get pending invite failed", e);
            return null;
        }
    }

    public static void clearPendingInvite(Context c) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit()
                    .remove(KEY_PENDING_INVITE_CODE)
                    .remove(KEY_PENDING_FRIDGE_NAME)
                    .remove(KEY_PENDING_EXPIRES_AT)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "clear pending invite failed", e);
        }
    }

    public static void applyJoinedFridgeFromServer(Context c,
                                                   long fridgeId,
                                                   String fridgeName,
                                                   String inviteCode,
                                                   long expiresAt,
                                                   String myNickname,
                                                   String myRole) {
        if (c == null) return;
        if (fridgeId <= 0) return;

        saveFridgeId(c, fridgeId);

        String name = fridgeName == null ? "" : fridgeName.trim();
        if (name.isEmpty()) name = "공동 냉장고";
        saveFridgeName(c, name);

        if (inviteCode != null && !inviteCode.trim().isEmpty()) {
            saveInvite(c, inviteCode.trim().toUpperCase(Locale.ROOT), expiresAt);
        }

        if (myNickname == null || myNickname.trim().isEmpty()) myNickname = "사용자";
        if (myRole == null || myRole.trim().isEmpty()) myRole = "MEMBER";

        resetMembersWithMeOnly(c, myNickname.trim(), myRole.trim().toUpperCase(Locale.ROOT));

        clearPendingInvite(c);
        OnboardingPrefs.saveFridgeType(c, "SHARED");
    }

    private static void resetMembersWithMeOnly(Context c, String myNick, String myRole) {
        List<Member> onlyMe = new ArrayList<>();
        onlyMe.add(new Member(myNick, myRole, System.currentTimeMillis()));
        setMembers(c, onlyMe);
        Log.d(TAG, "resetMembersWithMeOnly completed");
    }

    public static boolean isJoinedToServerSharedFridge(Context c) {
        return getFridgeId(c) > 0L;
    }

    public static List<Member> getMembers(Context c) {
        if (c == null) return new ArrayList<>();

        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = p.getString(KEY_MEMBERS_JSON, "[]");
        List<Member> out = new ArrayList<>();

        try {
            JSONArray arr = new JSONArray(raw);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                Member m = new Member(
                        o.optString("nickname", ""),
                        o.optString("role", "MEMBER"),
                        o.optLong("joinedAt", 0L)
                );

                out.add(m);
            }
        } catch (Exception e) {
            Log.w(TAG, "parse members failed", e);
        }

        return out;
    }

    public static void setMembers(Context c, List<Member> members) {
        if (c == null) return;

        JSONArray arr = new JSONArray();

        try {
            if (members != null) {
                for (Member m : members) {
                    if (m == null) continue;

                    JSONObject o = new JSONObject();
                    o.put("nickname", m.nickname == null ? "" : m.nickname);
                    o.put("role", m.role == null ? "MEMBER" : m.role);
                    o.put("joinedAt", m.joinedAt);
                    arr.put(o);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "build members json failed", e);
        }

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putString(KEY_MEMBERS_JSON, arr.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "save members failed", e);
        }
    }

    public static boolean isOwner(Context c, String nickname) {
        if (c == null) return false;
        if (nickname == null) return false;

        nickname = nickname.trim();
        if (nickname.isEmpty()) return false;

        List<Member> list = getMembers(c);

        for (Member m : list) {
            if (m == null) continue;

            if (nickname.equalsIgnoreCase(m.nickname)
                    && "OWNER".equalsIgnoreCase(m.role)) {
                return true;
            }
        }

        return false;
    }

    public static void leaveSharedFridge(Context c, String myNickname) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().clear().apply();
        } catch (Exception e) {
            Log.w(TAG, "clear shared fridge prefs failed", e);
        }

        try {
            OnboardingPrefs.saveFridgeType(c, "PERSONAL");
        } catch (Exception e) {
            Log.w(TAG, "save personal fridge type failed", e);
        }
    }

    public static void clearMembers(Context c) {
        if (c == null) return;

        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putString(KEY_MEMBERS_JSON, "[]").apply();
            Log.w(TAG, "Cleared local members list");
        } catch (Exception e) {
            Log.w(TAG, "clear members failed", e);
        }
    }

    public static class Member {
        public final String nickname;
        public final String role;
        public final long joinedAt;

        public Member(String nickname, String role, long joinedAt) {
            this.nickname = nickname;
            this.role = role;
            this.joinedAt = joinedAt;
        }
    }

    public static class InviteInfo {
        public final String code;
        public final long expiresAt;

        public InviteInfo(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    public static class PendingInvite {
        public final String fridgeName;
        public final String inviteCode;
        public final long expiresAt;

        public PendingInvite(String fridgeName, String inviteCode, long expiresAt) {
            this.fridgeName = fridgeName == null ? "" : fridgeName;
            this.inviteCode = inviteCode == null ? "" : inviteCode;
            this.expiresAt = expiresAt;
        }
    }
}