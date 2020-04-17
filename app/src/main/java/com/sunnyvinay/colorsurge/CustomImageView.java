package com.sunnyvinay.colorsurge;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

public class CustomImageView extends AppCompatImageView {
    public CustomImageView(Context context) {
    super(context);
}

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;

            case MotionEvent.ACTION_UP:
                performClick();
                return true;
        }
        return false;
    }

    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    @Override
    public boolean performClick() {
        super.performClick();
        doSomething();
        return true;
    }

    private void doSomething() {
        Toast.makeText(getContext(), "did something", Toast.LENGTH_SHORT).show();
    }
}
