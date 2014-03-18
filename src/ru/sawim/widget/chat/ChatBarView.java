package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.widget.IcsLinearLayout;
import ru.sawim.widget.LabelView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:33
 * To change this template use File | Settings | File Templates.
 */
public class ChatBarView extends IcsLinearLayout {

    ImageView imageView;
    LabelView textView;

    public ChatBarView(Context context, View chatsImage) {
        super(context,
                com.viewpagerindicator.R.attr.vpiTabPageIndicatorStyle);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setOrientation(HORIZONTAL);
        if (SawimApplication.isManyPane())
            layoutParams.weight = 1;
        setLayoutParams(layoutParams);
        setDividerPadding(10);

        LinearLayout.LayoutParams labelLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        labelLayoutParams.weight = 1;
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(Gravity.CENTER);
        imageView = new ImageView(context);
        imageView.setPadding(0, 0, 10, 0);
        linearLayout.addView(imageView);
        textView = new LabelView(context);
        linearLayout.addView(textView);
        addViewInLayout(linearLayout, 0, labelLayoutParams);

        if (!SawimApplication.isManyPane()) {
            LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            chatsImageLP.gravity = Gravity.CENTER_VERTICAL;
            addViewInLayout(chatsImage, 1, chatsImageLP);
        }
    }

    public void update() {
        setDividerDrawable(SawimResources.listDivider);
    }

    public void setVisibilityChatsImage(int visibility) {
        if (getChildAt(1) != null)
            getChildAt(1).setVisibility(visibility);
    }

    public void setVisibilityLabelImage(int visibility) {
        imageView.setVisibility(visibility);
    }

    public void updateTextView(String text) {
        textView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        textView.setTextSize(SawimApplication.getFontSize());
        textView.setText(text);
    }

    public void updateLabelIcon(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }
}