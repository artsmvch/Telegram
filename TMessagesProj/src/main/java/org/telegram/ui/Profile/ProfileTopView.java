package org.telegram.ui.Profile;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ProfileTopView extends FrameLayout {

    public ProfileTopView(Context context) {
        super(context);
        init();
    }

    public ProfileTopView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProfileTopView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.GRAY);
        TextView profileName = new TextView(getContext());
        profileName.setTextSize(6f);
        profileName.setTextColor(Color.WHITE);
        LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        addView(profileName, lp);
        updateText();
    }

    private void updateText() {
        TextView tv = (TextView) getChildAt(0);
        tv.setText("T " + getTranslationX() + ":" + getTranslationY() +
                " S " + getScaleX() + ":" + getScaleY());
    }

    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        updateText();
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);
        updateText();
    }

    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
        updateText();
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        updateText();
    }

    void animateMorph(float progress) {

    }

    void setExpanded(boolean expanded) {

    }
}
