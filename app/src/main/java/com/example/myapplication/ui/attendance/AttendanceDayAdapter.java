package com.namgyun.tamakitchen.ui.attendance;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.namgyun.tamakitchen.R;

import java.util.List;

public class AttendanceDayAdapter extends RecyclerView.Adapter<AttendanceDayAdapter.VH> {

    private final Context context;
    private final List<AttendanceDayItem> items;
    private final int animatedStampDay;

    public AttendanceDayAdapter(Context context, List<AttendanceDayItem> items, int animatedStampDay) {
        this.context = context;
        this.items = items;
        this.animatedStampDay = animatedStampDay;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance_day, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AttendanceDayItem item = items.get(position);

        resetAnimations(holder);

        holder.tvDay.setText(item.getDay() + "일차");
        holder.tvReward.setText(String.valueOf(item.getReward()));
        holder.ivRewardCoin.setImageResource(R.drawable.ic_coin);

        if (item.isChecked()) {
            holder.ivStatusCenter.setVisibility(View.VISIBLE);
            holder.ivStatusCenter.setImageResource(R.drawable.ic_attendance_done);
            holder.root.setBackgroundResource(R.drawable.bg_attendance_day_checked);
            holder.contentWrap.setAlpha(0.25f);

            if (item.getDay() == animatedStampDay) {
                playStampAnimation(holder);
            }
        } else if (item.isTodayTarget()) {
            holder.ivStatusCenter.setVisibility(View.GONE);
            holder.root.setBackgroundResource(R.drawable.bg_attendance_day_today);
            holder.contentWrap.setAlpha(1f);
            playTodayGlow(holder);
        } else {
            holder.ivStatusCenter.setVisibility(View.GONE);
            holder.root.setBackgroundResource(R.drawable.bg_attendance_day_default);
            holder.contentWrap.setAlpha(1f);
            holder.vTodayGlow.setAlpha(0f);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    private void resetAnimations(@NonNull VH holder) {
        holder.root.animate().cancel();
        holder.vTodayGlow.animate().cancel();
        holder.ivStatusCenter.animate().cancel();

        holder.root.setScaleX(1f);
        holder.root.setScaleY(1f);

        holder.vTodayGlow.setAlpha(0f);

        holder.ivStatusCenter.setScaleX(1f);
        holder.ivStatusCenter.setScaleY(1f);
        holder.ivStatusCenter.setAlpha(1f);
        holder.ivStatusCenter.setRotation(0f);
    }

    private void playTodayGlow(@NonNull VH holder) {
        holder.vTodayGlow.setAlpha(0.10f);

        ObjectAnimator glow = ObjectAnimator.ofFloat(holder.vTodayGlow, View.ALPHA, 0.10f, 0.32f, 0.10f);
        glow.setDuration(1500);
        glow.setRepeatCount(ObjectAnimator.INFINITE);
        glow.setInterpolator(new AccelerateDecelerateInterpolator());
        glow.start();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(holder.root, View.SCALE_X, 1f, 1.02f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(holder.root, View.SCALE_Y, 1f, 1.02f, 1f);
        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    private void playStampAnimation(@NonNull VH holder) {
        holder.ivStatusCenter.setVisibility(View.VISIBLE);
        holder.ivStatusCenter.setAlpha(0f);
        holder.ivStatusCenter.setScaleX(1.8f);
        holder.ivStatusCenter.setScaleY(1.8f);
        holder.ivStatusCenter.setRotation(-10f);

        ObjectAnimator stampAlpha = ObjectAnimator.ofFloat(holder.ivStatusCenter, View.ALPHA, 0f, 1f);
        ObjectAnimator stampScaleX = ObjectAnimator.ofFloat(holder.ivStatusCenter, View.SCALE_X, 1.8f, 0.88f, 1f);
        ObjectAnimator stampScaleY = ObjectAnimator.ofFloat(holder.ivStatusCenter, View.SCALE_Y, 1.8f, 0.88f, 1f);
        ObjectAnimator stampRotate = ObjectAnimator.ofFloat(holder.ivStatusCenter, View.ROTATION, -10f, 3f, 0f);

        stampAlpha.setDuration(340);
        stampScaleX.setDuration(340);
        stampScaleY.setDuration(340);
        stampRotate.setDuration(340);

        AnimatorSet stampSet = new AnimatorSet();
        stampSet.playTogether(stampAlpha, stampScaleX, stampScaleY, stampRotate);
        stampSet.setInterpolator(new AccelerateDecelerateInterpolator());
        stampSet.start();

        ObjectAnimator cardPunchX = ObjectAnimator.ofFloat(holder.root, View.SCALE_X, 1f, 1.05f, 0.98f, 1f);
        ObjectAnimator cardPunchY = ObjectAnimator.ofFloat(holder.root, View.SCALE_Y, 1f, 1.05f, 0.98f, 1f);
        cardPunchX.setDuration(360);
        cardPunchY.setDuration(360);
        cardPunchX.setInterpolator(new AccelerateDecelerateInterpolator());
        cardPunchY.setInterpolator(new AccelerateDecelerateInterpolator());
        cardPunchX.start();
        cardPunchY.start();
    }

    static class VH extends RecyclerView.ViewHolder {
        View root;
        View contentWrap;
        View vTodayGlow;
        TextView tvDay;
        TextView tvReward;
        ImageView ivRewardCoin;
        ImageView ivStatusCenter;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.attendanceDayRoot);
            contentWrap = itemView.findViewById(R.id.contentWrap);
            vTodayGlow = itemView.findViewById(R.id.vTodayGlow);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvReward = itemView.findViewById(R.id.tvReward);
            ivRewardCoin = itemView.findViewById(R.id.ivRewardCoin);
            ivStatusCenter = itemView.findViewById(R.id.ivStatusCenter);
        }
    }
}