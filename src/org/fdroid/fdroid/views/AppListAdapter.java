package org.fdroid.fdroid.views;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

abstract public class AppListAdapter extends CursorAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private DisplayImageOptions displayImageOptions;

    public AppListAdapter(Context context, Cursor c) {
        super(context, c);
        init(context);
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

    private static class ViewHolder {
        TextView name;
        TextView summary;
        TextView status;
        TextView license;
        ImageView icon;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.applistitem, null);

        ViewHolder holder = new ViewHolder();
        holder.name = (TextView) view.findViewById(R.id.name);
        holder.summary = (TextView) view.findViewById(R.id.summary);
        holder.status = (TextView) view.findViewById(R.id.status);
        holder.license = (TextView) view.findViewById(R.id.license);
        holder.icon = (ImageView) view.findViewById(R.id.icon);
        view.setTag(holder);

        setupView(context, view, cursor, holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder)view.getTag();
        setupView(context, view, cursor, holder);
    }

    private void setupView(Context context, View view, Cursor cursor, ViewHolder holder) {
        final App app = new App(cursor);

        boolean compact = Preferences.get().hasCompactLayout();

        holder.name.setText(app.name);
        holder.summary.setText(app.summary);

        layoutIcon(holder.icon, compact);
        ImageLoader.getInstance().displayImage(app.iconUrl, holder.icon,
            displayImageOptions);

        holder.status.setText(getVersionInfo(app));
        holder.license.setText(app.license);

        // Disable it all if it isn't compatible...
        View[] views = {
            view,
            holder.status,
            holder.summary,
            holder.license,
            holder.name
        };

        for (View v : views) {
            v.setEnabled(app.compatible && !app.isFiltered());
        }
    }

     private String ellipsize(String input, int maxLength) {
        if (input == null || input.length() < maxLength+1) {
            return input;
        }
        return input.substring(0, maxLength) + "…";
    }

    private String getVersionInfo(App app) {

        if (app.curVercode <= 0) {
            return null;
        }

        PackageInfo installedInfo = app.getInstalledInfo(mContext);

        if (installedInfo == null) {
            return ellipsize(app.curVersion, 12);
        }

        String installedVersionString = installedInfo.versionName;
        int installedVersionCode = installedInfo.versionCode;

        if (app.canAndWantToUpdate(mContext) && showStatusUpdate()) {
            return ellipsize(installedVersionString, 8) +
                " → " + ellipsize(app.curVersion, 8);
        }

        if (installedVersionCode > 0 && showStatusInstalled()) {
            return ellipsize(installedVersionString, 12) + " ✔";
        }

        return installedVersionString;
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
