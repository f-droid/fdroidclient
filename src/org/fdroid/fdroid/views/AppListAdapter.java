package org.fdroid.fdroid.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.graphics.Bitmap;

import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.LayoutCompat;

import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

abstract public class AppListAdapter extends BaseAdapter {

    private List<DB.App> items = new ArrayList<DB.App>();
    private Context mContext;
    private LayoutInflater mInflater;
    private DisplayImageOptions displayImageOptions;

    public AppListAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        displayImageOptions = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .imageScaleType(ImageScaleType.NONE)
            .resetViewBeforeLoading(true)
            .showImageOnLoading(R.drawable.ic_repo_app_default)
            .showImageForEmptyUri(R.drawable.ic_repo_app_default)
            .displayer(new FadeInBitmapDisplayer(200, true, true, false))
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();

    }

    abstract protected boolean showStatusUpdate();

    abstract protected boolean showStatusInstalled();

    public void addItem(DB.App app) {
        items.add(app);
    }

    public void addItems(List<DB.App> apps) {
        items.addAll(apps);
    }

    public void clear() {
        items.clear();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView name;
        TextView summary;
        TextView status;
        TextView license;
        ImageView icon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        boolean compact = Preferences.get().hasCompactLayout();
        DB.App app = items.get(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.applistitem, null);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.summary = (TextView) convertView.findViewById(R.id.summary);
            holder.status = (TextView) convertView.findViewById(R.id.status);
            holder.license = (TextView) convertView.findViewById(R.id.license);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.name.setText(app.name);
        holder.summary.setText(app.summary);

        layoutIcon(holder.icon, compact);
        ImageLoader.getInstance().displayImage(app.iconUrl, holder.icon,
            displayImageOptions);

        holder.status.setText(getVersionInfo(app));
        holder.license.setText(app.license);

        // Disable it all if it isn't compatible...
        View[] views = {
            convertView,
            holder.status,
            holder.summary,
            holder.license,
            holder.name
        };

        for (View view : views) {
            view.setEnabled(app.compatible && !app.filtered);
        }

        return convertView;
    }

    private String ellipsize(String input, int maxLength) {
        if (input == null || input.length() < maxLength+1) {
            return input;
        }
        return input.substring(0, maxLength) + "…";
    }

    private String getVersionInfo(DB.App app) {

        if (app.curApk == null) {
            return null;
        }

        if (app.installedVersion == null) {
            return ellipsize(app.curApk.version, 12);
        }

        if (app.toUpdate && showStatusUpdate()) {
            return ellipsize(app.installedVersion, 8) +
                " → " + ellipsize(app.curApk.version, 8);
        }

        if (app.installedVerCode > 0 && showStatusInstalled()) {
            return ellipsize(app.installedVersion, 12) + " ✔";
        }

        return app.installedVersion;
    }

    private void layoutIcon(ImageView icon, boolean compact) {
        int size = (int)mContext.getResources().getDimension((compact
            ? R.dimen.applist_icon_compact_size
            : R.dimen.applist_icon_normal_size));

        RelativeLayout.LayoutParams params =
            (RelativeLayout.LayoutParams)icon.getLayoutParams();

        params.height = size;
        params.width = size;

        icon.setLayoutParams(params);
    }

}
