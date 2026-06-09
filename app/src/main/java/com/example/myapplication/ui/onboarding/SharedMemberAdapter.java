package com.namgyun.tamakitchen.ui.onboarding;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.network.SharedFridgeMemberDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SharedMemberAdapter extends RecyclerView.Adapter<SharedMemberAdapter.VH> {

    public interface Listener {
        void onClickMore(@NonNull SharedFridgeMemberDto member);
    }

    private final List<SharedFridgeMemberDto> items = new ArrayList<>();
    private Listener listener;

    private boolean iAmOwner = false;
    private long myUserId = 0;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setIAmOwner(boolean owner) {
        this.iAmOwner = owner;
        notifyDataSetChanged();
    }

    public void setMyUserId(long myUserId) {
        this.myUserId = myUserId;
        notifyDataSetChanged();
    }

    public void submit(List<SharedFridgeMemberDto> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    // ✅ 추가: 현재 멤버 수
    public int getMemberCount() {
        return items.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shared_member, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SharedFridgeMemberDto m = items.get(position);

        String nick = (m == null || m.nickname == null) ? "" : m.nickname.trim();
        String role = (m == null || m.role == null) ? "MEMBER" : m.role.trim().toUpperCase(Locale.ROOT);

        h.tvNick.setText(TextUtils.isEmpty(nick) ? "멤버" : nick);
        h.tvRole.setText(role);

        boolean isOwner = "OWNER".equalsIgnoreCase(role);
        h.ivCrown.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // ✅ OWNER(나)만 멤버 관리 가능 + 내 자신에게는 ⋮ 안 보여줌
        boolean showMore = iAmOwner && m != null && m.userId > 0 && m.userId != myUserId;

        // ✅ 자리 유지(OWNER/MEMBER 위치 동일)
        h.ivMore.setVisibility(showMore ? View.VISIBLE : View.INVISIBLE);

        h.ivMore.setOnClickListener(v -> {
            if (showMore && listener != null && m != null) {
                listener.onClickMore(m);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNick, tvRole;
        ImageView ivCrown, ivMore;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNick = itemView.findViewById(R.id.tvNick);
            tvRole = itemView.findViewById(R.id.tvRole);
            ivCrown = itemView.findViewById(R.id.ivCrown);
            ivMore = itemView.findViewById(R.id.ivMore);
        }
    }
}
