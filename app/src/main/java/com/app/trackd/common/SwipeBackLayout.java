package com.app.trackd.common;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class SwipeBackLayout extends FrameLayout {

    private float downX;
    private boolean isSwiping = false;
    private static final int SWIPE_THRESHOLD = 150; // px

    private Activity activity;

    public SwipeBackLayout(Context context) {
        super(context);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
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
                downX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float diff = ev.getX() - downX;
                if (diff > 20) { // start intercepting
                    isSwiping = true;
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (isSwiping) {
                    setTranslationX(x - downX); // move the whole layout
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isSwiping) {
                    if (x - downX > SWIPE_THRESHOLD) {
                        activity.finish();
                        activity.overridePendingTransition(0, android.R.anim.slide_out_right);
                    } else {
                        animate().translationX(0).setDuration(200).start();
                    }
                }
                isSwiping = false;
                break;
        }
        return true;
    }
}