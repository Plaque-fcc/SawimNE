package ru.sawim;

import DrawControls.icons.ImageList;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by admin on 07.01.14.
 */
public class SawimResources {

    public static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");
    public static final ImageList groupDownIcon = ImageList.createImageList("/control_down.png");
    public static final ImageList groupRightIcons = ImageList.createImageList("/control_right.png");
    public static BitmapDrawable usersIcon;
    public static BitmapDrawable typingIcon;

    public static BitmapDrawable messageIconCheck = (BitmapDrawable) General.getResources(SawimApplication.getContext()).getDrawable(R.drawable.msg_check);
    public static BitmapDrawable authGrantIcon = (BitmapDrawable) General.getResources(SawimApplication.getContext()).
            getDrawable(R.drawable.ic_auth_grant);
    public static BitmapDrawable authReqIcon = (BitmapDrawable) General.getResources(SawimApplication.getContext()).
            getDrawable(R.drawable.ic_auth_req);
    public static BitmapDrawable messageIcon = (BitmapDrawable) General.getResources(SawimApplication.getContext()).
            getDrawable(R.drawable.ic_new_message);
    public static BitmapDrawable personalMessageIcon = (BitmapDrawable) General.getResources(SawimApplication.getContext()).
            getDrawable(R.drawable.ic_new_personal_message);

    public static void initIcons() {
        usersIcon = (BitmapDrawable) General.getResources(SawimApplication.getContext()).
                getDrawable(Scheme.isBlack() ? R.drawable.ic_participants_dark : R.drawable.ic_participants_light);
        typingIcon = (BitmapDrawable) General.getResources(SawimApplication.getContext()).
                getDrawable(Scheme.isBlack() ? R.drawable.ic_typing_dark : R.drawable.ic_typing_light);

    }
}