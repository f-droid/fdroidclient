package org.fdroid.fdroid.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.LayoutCompat;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

abstract public class AppListAdapter extends BaseAdapter {

    private List<DB.App> items = new ArrayList<DB.App>();
    private Context mContext;
    private DisplayImageOptions displayImageOptions;

    public AppListAdapter(Context context) {
        mContext = context;

        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        builder.imageScaleType(ImageScaleType.NONE); // let android scale
        builder.resetViewBeforeLoading(true); // required for multiple loading
        builder.cacheInMemory(true); // default even if doc says otherwise
        displayImageOptions = builder.build();
    }

    abstract protected boolean showStatusUpdate();

    abstract protected boolean showStatusInstalled();

    public void addItem(DB.App app) {
        items.add(app);
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        boolean compact = Preferences.get().hasCompactLayout();
        DB.App app = items.get(position);

        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.applistitem, null);
        }

        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView summary = (TextView) convertView.findViewById(R.id.summary);
        TextView status = (TextView) convertView.findViewById(R.id.status);
        TextView license = (TextView) convertView.findViewById(R.id.license);
        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

        name.setText(app.name);
        summary.setText(app.summary);

        int visibleOnCompact = compact ? View.VISIBLE : View.GONE;
        int notVisibleOnCompact = compact ? View.GONE : View.VISIBLE;

        LinearLayout iconContainer = (LinearLayout)convertView.findViewById(R.id.status_icons);

        iconContainer.setVisibility(visibleOnCompact);
        status.setVisibility(notVisibleOnCompact);
        license.setVisibility(notVisibleOnCompact);

        layoutIcon(icon, compact);
        ImageLoader.getInstance().displayImage(app.iconUrl, icon,
            displayImageOptions);

        if (!compact) {
            status.setText(getVersionInfo(app));
            license.setText(app.license);
        } else {
            ImageView iconInstalled = (ImageView) convertView.findViewById(R.id.icon_status_installed);
            ImageView iconUpdates = (ImageView) convertView.findViewById(R.id.icon_status_has_updates);

            iconInstalled.setImageResource(R.drawable.ic_cab_done_holo_dark);
            iconUpdates.setImageResource(R.drawable.ic_menu_refresh);

            if (app.toUpdate && showStatusUpdate()) {
                iconUpdates.setVisibility(View.VISIBLE);
            } else {
                iconUpdates.setVisibility(View.GONE);
            }

            if (app.installedVerCode > 0 && showStatusInstalled()) {
                iconInstalled.setVisibility(View.VISIBLE);
            } else {
                iconInstalled.setVisibility(View.GONE);
            }
        }

        // Disable it all if it isn't compatible...
        View[] views = { convertView, status, summary, license, name };
        for (View view : views) {
            view.setEnabled(app.compatible && !app.filtered);
        }

        return convertView;
    }

    private String getVersionInfo(DB.App app) {
        if (app.installedVersion != null) {
            if (app.toUpdate) {
                return app.installedVersion + " -> " + app.curApk.version;
            }
            return app.installedVersion;
        } else {
            int numav = app.apks.size();
            if (numav == 1) {
                return mContext.getString(R.string.n_version_available, numav);
            }
            return mContext.getString(R.string.n_versions_available, numav);
        }
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
