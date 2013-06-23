package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import sawim.Options;
import sawim.cl.ContactList;
import protocol.Profile;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 18:23
 * To change this template use File | Settings | File Templates.
 */
public class AccountsAdapter extends BaseAdapter {

    private final Context baseContext;

    public AccountsAdapter(Context context) {
        baseContext = context;
    }

    @Override
    public int getCount() {
        return Options.getAccountCount();
    }

    @Override
    public Profile getItem(int position) {
        return Options.getAccount(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convView, ViewGroup viewGroup) {
        ItemWrapper wr;
        if (convView == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            convView = inf.inflate(R.layout.accounts_list_item, null);
            wr = new ItemWrapper(convView);
            convView.setTag(wr);
        } else {
            wr = (ItemWrapper) convView.getTag();
        }
        wr.populateFrom(position);
        return convView;
    }

    public class ItemWrapper {
        View item = null;
        private ImageView imageProtocol = null;
        private TextView textLogin = null;
        private ToggleButton toggleButton = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(final int position) {
            final Profile account = getItem(position);
            Protocol p = ContactList.getInstance().getProtocol(account);
            if (null != p) {
                ImageView icProtocol = getImageProtocol();
                Icon ic = p.getStatusInfo().getIcon((byte) 0);
                if (ic != null) {
                    icProtocol.setVisibility(ImageView.VISIBLE);
                    icProtocol.setImageBitmap(General.iconToBitmap(ic));
                } else {
                    icProtocol.setVisibility(ImageView.GONE);
                }
            }
            getTextLogin().setText(account.userId);
            ToggleButton tb = getToggleButton();
            tb.setChecked(account.isActive);
            tb.setFocusableInTouchMode(false);
            tb.setFocusable(false);

            tb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToggleButton t = (ToggleButton) v.findViewById(R.id.toggle_button);
                    account.isActive = t.isChecked();
                    Log.e("=======================", ""+t.isChecked()+" "+account.userId);
                    Options.saveAccount(account);
                }
            });

        }

        public ToggleButton getToggleButton() {
            if (toggleButton == null)
                toggleButton = (ToggleButton) item.findViewById(R.id.toggle_button);
            return toggleButton;
        }

        public TextView getTextLogin() {
            if (textLogin == null)
                textLogin = (TextView) item.findViewById(R.id.account_login);
            return textLogin;
        }

        public ImageView getImageProtocol() {
            if (imageProtocol == null)
                imageProtocol = (ImageView) item.findViewById(R.id.account_protocol);
            return imageProtocol;
        }
    }
}