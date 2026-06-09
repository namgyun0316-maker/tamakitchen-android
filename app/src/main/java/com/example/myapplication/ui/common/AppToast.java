    package com.namgyun.tamakitchen.ui.common;

    import android.app.Activity;
    import android.view.Gravity;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.widget.TextView;
    import android.widget.Toast;

    import com.namgyun.tamakitchen.R;

    public class AppToast {

        public static void show(Activity activity, String message) {

            LayoutInflater inflater = activity.getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);

            TextView text = layout.findViewById(R.id.toast_message);
            text.setText(message);

            Toast toast = new Toast(activity);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            // 화면 하단 위치
            toast.setGravity(Gravity.BOTTOM, 0, 180);

            toast.show();
        }
    }