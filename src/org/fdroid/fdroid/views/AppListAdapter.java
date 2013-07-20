package org.fdroid.fdroid.views;

import org.fdroid.fdroid.Utils;

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
import org.fdroid.fdroid.compat.LayoutCompat;

abstract public class AppListAdapter extends BaseAdapter {

    private List<DB.App> items = new ArrayList<DB.App>();
    private Context mContext;
    Boolean pref_compactlayout = null;

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
        boolean init = false;

        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.applistitem, null);
            init = true;
        }

        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView summary = (TextView) convertView.findViewById(R.id.summary);
        TextView status = (TextView) convertView.findViewById(R.id.status);
        TextView license = (TextView) convertView.findViewById(R.id.license);

        DB.App app = items.get(position);

        status.setText(getVersionInfo(app));
        license.setText(app.license);

        name.setText(app.name);
        summary.setText(app.summary);

        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        File icn = new File(DB.getIconsPath(mContext), app.icon);
        if (icn.exists() && icn.length() > 0) {
            new Uri.Builder().build();
            icon.setImageURI(Uri.parse(icn.getPath()));
        } else {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        ImageView iconInstalled = (ImageView) convertView.findViewById(R.id.icon_status_installed);
        ImageView iconUpdates = (ImageView) convertView.findViewById(R.id.icon_status_has_updates);

        if (init) {

            if (pref_compactlayout == null) {
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(mContext);
                pref_compactlayout = prefs.getBoolean("compactlayout", false);
            }

            if (pref_compactlayout == true) {

                iconInstalled.setImageResource(R.drawable.ic_cab_done_holo_dark);
                iconUpdates.setImageResource(R.drawable.ic_menu_refresh);

                status.setVisibility(View.GONE);
                license.setVisibility(View.GONE);

                RelativeLayout.LayoutParams summaryLayout =
                    new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                summaryLayout.addRule(RelativeLayout.BELOW, R.id.name);
                summaryLayout.addRule(LayoutCompat.RelativeLayout.END_OF, R.id.icon);
                summary.setLayoutParams(summaryLayout);
                summary.setPadding(0,0,0,0);

            }
        }

        if (pref_compactlayout == true) {

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
            view.setEnabled(app.compatible);
        }

        return convertView;
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
