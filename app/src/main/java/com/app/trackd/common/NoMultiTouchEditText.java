package com.app.trackd.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NoMultiTouchEditText extends androidx.appcompat.widget.AppCompatEditText {

    public NoMultiTouchEditText(Context context) {
        super(context);
    }

    public NoMultiTouchEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoMultiTouchEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            return false;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Extra safety: do not consume multi-touch
        if (event.getPointerCount() > 1) {
            return false;
        }
        return super.onTouchEvent(event);
    }
}
