package com.app.trackd.common;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class DoubleTapLayout extends FrameLayout {

    public interface OnDoubleTapListener {
        void onDoubleTap();
    }

    private OnDoubleTapListener listener;
    private GestureDetector gestureDetector;

    public DoubleTapLayout(Context context) {
        this(context, null);
    }

    public DoubleTapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (listener != null) listener.onDoubleTap();
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                });
    }

    public void setOnDoubleTapListener(OnDoubleTapListener listener) {
        this.listener = listener;
    }

    public void attachToActivity(Activity activity) {
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        ViewGroup content = (ViewGroup) decor.getChildAt(0);

        // Remove original content from decor
        decor.removeViewAt(0);

        // Add original content into this layout
        this.addView(content);

        // Add this layout back to decor
        decor.addView(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return gestureDetector.onTouchEvent(ev);
    }
}
