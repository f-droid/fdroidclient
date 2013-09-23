package org.fdroid.fdroid.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.LayoutCompat;

import com.nostra13.universalimageloader.core.ImageLoader;

abstract public class AppListAdapter extends BaseAdapter {

    private List<DB.App> items = new ArrayList<DB.App>();
    private Context mContext;

    public AppListAdapter(Context context) {
        mContext = context;
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
        LinearLayout iconContainer = (LinearLayout)convertView.findViewById(R.id.status_icons);
        ImageView iconInstalled = (ImageView) convertView.findViewById(R.id.icon_status_installed);
        ImageView iconUpdates = (ImageView) convertView.findViewById(R.id.icon_status_has_updates);

        name.setText(app.name);
        summary.setText(app.summary);

        layoutSummary(summary);
        ImageLoader.getInstance().displayImage(app.repoAddress+"/icons/"+app.icon, icon);

        int visibleOnCompact = compact ? View.VISIBLE : View.GONE;
        int notVisibleOnCompact = compact ? View.GONE : View.VISIBLE;

        iconContainer.setVisibility(visibleOnCompact);
        status.setVisibility(notVisibleOnCompact);
        license.setVisibility(notVisibleOnCompact);

        if (!compact) {
            status.setText(getVersionInfo(app));
            license.setText(app.license);
        } else {
            status.setText("");
            license.setText("");

            iconInstalled.setImageResource(R.drawable.ic_cab_done_holo_dark);
            iconUpdates.setImageResource(R.drawable.ic_menu_refresh);

            if (app.hasUpdates && showStatusUpdate()) {
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

    /**
     * In compact view, the summary sites next to the icon, below the name.
     * In non-compact view, it sits under the icon, with some padding pushing
     * it away from the left margin.
     */
    private void layoutSummary(TextView summaryView) {

        if (Preferences.get().hasCompactLayout()) {

            RelativeLayout.LayoutParams summaryLayout =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            summaryLayout.addRule(RelativeLayout.BELOW, R.id.name);
            summaryLayout.addRule(LayoutCompat.RelativeLayout.END_OF, R.id.icon);
            summaryView.setLayoutParams(summaryLayout);
            summaryView.setPadding(0,0,0,0);

        } else {

            RelativeLayout.LayoutParams summaryLayout =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            summaryLayout.addRule(RelativeLayout.BELOW, R.id.icon);
            summaryView.setLayoutParams(summaryLayout);
            float padding = mContext.getResources().getDimension(R.dimen.applist_summary_padding);
            summaryView.setPadding((int)padding, 0, 0, 0);

        }
    }

    private String getVersionInfo(DB.App app) {
        StringBuilder version = new StringBuilder();
        if (app.installedVersion != null) {
            version.append(app.installedVersion);
            if (app.hasUpdates) {
                version.append(" -> ");
                version.append(app.updateVersion);
            }
        } else {
            int numav = app.apks.size();
            String numVersions;
            if (numav == 1)
                numVersions = mContext.getString(R.string.n_version_available);
            else
                numVersions = mContext.getString(R.string.n_versions_available);
            version.append(String.format(numVersions, numav));
        }
        return version.toString();
    }

}
