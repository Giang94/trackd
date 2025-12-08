package com.app.trackd.common;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class LongPressLayout extends FrameLayout {

    private static final long LONG_PRESS_DURATION = 450; // ms
    private float startX, startY;
    private boolean longPressTriggered = false;

    private Runnable longPressRunnable;
    private Handler handler = new Handler();

    private Activity activity;
    private OnLongPressListener listener;

    public interface OnLongPressListener {
        void onLongPress();
    }

    public LongPressLayout(Context context) {
        super(context);
    }

    public LongPressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnLongPressListener(OnLongPressListener l) {
        this.listener = l;
    }

    public void attachToActivity(Activity activity) {
        this.activity = activity;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        View content = decor.getChildAt(0);

        decor.removeViewAt(0);
        this.addView(content);
        decor.addView(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                longPressTriggered = false;

                longPressRunnable = () -> {
                    longPressTriggered = true;
                    if (listener != null) listener.onLongPress();
                };

                handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);

                if (dx > 20 || dy > 20) {
                    handler.removeCallbacks(longPressRunnable);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                break;
        }

        return longPressTriggered || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return longPressTriggered || super.onTouchEvent(ev);
    }
}
