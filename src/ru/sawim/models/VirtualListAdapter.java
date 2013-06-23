package ru.sawim.models;

import android.app.Activity;
import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.models.form.VirtualListItem;
import sawim.ui.base.Scheme;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:07
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListAdapter extends BaseAdapter {

    Context baseContext;
    List<VirtualListItem> items;

    public VirtualListAdapter(Context context, List<VirtualListItem> items) {
        this.baseContext = context;
        this.items = items;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public VirtualListItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        VirtualListItem element = getItem(i);
        if (convertView == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            convertView = inf.inflate(R.layout.virtual_list_item, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        LinearLayout descriptionLayout = (LinearLayout) convertView.findViewById(R.id.descriptionLayout);
        holder.labelView = (TextView) convertView.findViewById(R.id.label);
        holder.descView = (TextView) descriptionLayout.findViewById(R.id.description);
        holder.imageView = (ImageView) descriptionLayout.findViewById(R.id.imageView);

        holder.labelView.setTextColor(General.getColor(Scheme.THEME_TEXT));
        holder.descView.setTextColor(General.getColor(Scheme.THEME_TEXT));

        holder.labelView.setVisibility(TextView.GONE);
        holder.descView.setVisibility(TextView.GONE);
        holder.imageView.setVisibility(ImageView.GONE);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) descriptionLayout.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.setMargins(element.getMarginLeft(), 0, 0, 0);
            descriptionLayout.setLayoutParams(layoutParams);
        }
        if (element.getLabel() != null) {
            holder.labelView.setVisibility(TextView.VISIBLE);
            if (element.getThemeTextLabel() > -1) {
                holder.labelView.setTextColor(General.getColor(element.getThemeTextLabel()));
            }
            holder.labelView.setText(element.getLabel());
        }
        if (element.getDescStr() != null) {
            if (element.isTextSelectable()) {
                holder.descView.setTextIsSelectable(true);
                holder.descView.setAutoLinkMask(Linkify.ALL);
            }
            holder.descView.setVisibility(TextView.VISIBLE);
            if (element.getThemeTextDesc() > -1) {
                holder.descView.setTextColor(General.getColor(element.getThemeTextDesc()));
            }
            holder.descView.setText(element.getDescStr());
        } else {
            if (element.getDescSpan() != null) {
                if (element.isTextSelectable()) {
                    holder.descView.setTextIsSelectable(true);
                    holder.descView.setAutoLinkMask(Linkify.ALL);
                }
                holder.descView.setVisibility(TextView.VISIBLE);
                holder.descView.setText(element.getDescSpan());
            }
        }
        if (element.getImage() != null) {
            holder.imageView.setVisibility(ImageView.VISIBLE);
            holder.imageView.setImageBitmap(element.getImage());
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView labelView;
        TextView descView;
        ImageView imageView;

    }
}