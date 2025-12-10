package com.app.trackd.common;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TwoFingerDoubleTapLayout extends FrameLayout {

    public interface OnTwoFingerDoubleTapListener {
        void onTwoFingerDoubleTap();
    }

    private OnTwoFingerDoubleTapListener listener;

    // timing thresholds
    private static final long DOUBLE_TAP_TIMEOUT = 250;

    // state
    private int lastPointerCount = 0;
    private long lastTapTime = 0;

    public TwoFingerDoubleTapLayout(Context context) {
        this(context, null);
    }

    public TwoFingerDoubleTapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnTwoFingerDoubleTapListener(OnTwoFingerDoubleTapListener listener) {
        this.listener = listener;
    }

    public void attachToActivity(Activity activity) {
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        ViewGroup content = (ViewGroup) decor.getChildAt(0);

        decor.removeViewAt(0);
        this.addView(content);
        decor.addView(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        detect(ev);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        detect(ev);
        return true;
    }

    private void detect(MotionEvent ev) {
        int pointerCount = ev.getPointerCount();

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                lastPointerCount = pointerCount;
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                if (lastPointerCount == 2) {
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                        // we got a two-finger double tap
                        if (listener != null) listener.onTwoFingerDoubleTap();
                    }
                    lastTapTime = now;
                }
                lastPointerCount = 0;
                break;
        }
    }
}
