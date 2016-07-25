package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;

public abstract class AppListAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private DisplayImageOptions displayImageOptions;
    private String upgradeFromTo;

    @SuppressWarnings("deprecation")
    public AppListAdapter(Context context, Cursor c) {
        super(context, c);
        init(context);
    }

    @Override
    public boolean isEmpty() {
        return mDataValid && super.isEmpty();
    }

    public AppListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        init(context);
    }

    public AppListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        init(context);
    }

    private void init(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        displayImageOptions = Utils.getImageLoadingOptions().build();
        upgradeFromTo = context.getResources().getString(R.string.upgrade_from_to);
    }

    protected abstract boolean showStatusUpdate();

    protected abstract boolean showStatusInstalled();

    private static class ViewHolder {
        TextView name;
        TextView summary;
        TextView status;
        TextView license;
        ImageView icon;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.applistitem, parent, false);

        ViewHolder holder = new ViewHolder();
        holder.name = (TextView) view.findViewById(R.id.name);
        holder.summary = (TextView) view.findViewById(R.id.summary);
        holder.status = (TextView) view.findViewById(R.id.status);
        holder.license = (TextView) view.findViewById(R.id.license);
        holder.icon = (ImageView) view.findViewById(R.id.icon);
        view.setTag(holder);

        setupView(view, cursor, holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        setupView(view, cursor, holder);
    }

    private void setupView(View view, Cursor cursor, ViewHolder holder) {
        final App app = new App(cursor);

        holder.name.setText(app.name);
        holder.summary.setText(app.summary);

        ImageLoader.getInstance().displayImage(app.iconUrl, holder.icon,
                displayImageOptions);

        holder.status.setText(getVersionInfo(app));
        holder.license.setText(app.license);

        // Disable it all if it isn't compatible...
        final View[] views = {
            view,
            holder.status,
            holder.summary,
            holder.license,
            holder.name,
        };

        for (View v : views) {
            v.setEnabled(app.compatible && !app.isFiltered());
        }
    }

    private String getVersionInfo(App app) {

        if (app.suggestedVersionCode <= 0) {
            return null;
        }

        if (!app.isInstalled()) {
            return app.getSuggestedVersionName();
        }

        final String installedVersionString = app.installedVersionName;

        if (app.canAndWantToUpdate(mContext) && showStatusUpdate()) {
            return String.format(upgradeFromTo,
                    installedVersionString, app.getSuggestedVersionName());
        }

        if (app.installedVersionCode > 0 && showStatusInstalled()) {
            return installedVersionString + " ✔";
        }

        return installedVersionString;
    }
}
