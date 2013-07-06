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
import org.fdroid.fdroid.R;

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
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.applistitem, null);
        }
        DB.App app = items.get(position);

        TextView name = (TextView) v.findViewById(R.id.name);
        name.setText(app.name);

        TextView summary = (TextView) v.findViewById(R.id.summary);
        summary.setText(app.summary);

        TextView status = (TextView) v.findViewById(R.id.status);
        TextView license = (TextView) v.findViewById(R.id.license);

        ImageView iconUpdates   = (ImageView)v.findViewById(R.id.icon_status_has_updates);
        ImageView iconInstalled = (ImageView)v.findViewById(R.id.icon_status_installed);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (prefs.getBoolean("compactlayout", false)) {

            status.setVisibility(View.GONE);
            license.setVisibility(View.GONE);

            RelativeLayout.LayoutParams summaryLayout =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            summaryLayout.addRule(RelativeLayout.BELOW, R.id.name);
            summaryLayout.addRule(RelativeLayout.RIGHT_OF, R.id.icon);
            summary.setLayoutParams(summaryLayout);

            if (app.hasUpdates && showStatusUpdate()) {
                iconUpdates.setImageResource(R.drawable.ic_menu_refresh);
                iconUpdates.setVisibility(View.VISIBLE);
            } else {
                iconUpdates.setVisibility(View.GONE);
            }

            if (app.installedVerCode > 0 && showStatusInstalled()) {
                iconInstalled.setImageResource(R.drawable.ic_cab_done_holo_dark);
                iconInstalled.setVisibility(View.VISIBLE);
            } else {
                iconInstalled.setVisibility(View.GONE);
            }

        } else {

            status.setText(getVersionInfo(app));
            license.setText(app.license);

            iconUpdates.setVisibility(View.GONE);
            iconInstalled.setVisibility(View.GONE);
        }

        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        File icn = new File(DB.getIconsPath(), app.icon);
        if (icn.exists() && icn.length() > 0) {
            new Uri.Builder().build();
            icon.setImageURI(Uri.parse(icn.getPath()));
        } else {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Disable it all if it isn't compatible...
        View[] views = { v, status, summary, license, name };
        for (View view : views) {
            view.setEnabled(app.compatible);
        }

        return v;
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
