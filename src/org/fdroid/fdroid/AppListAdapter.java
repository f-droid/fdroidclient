package org.fdroid.fdroid;

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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppListAdapter extends BaseAdapter {

    private List<DB.App> items = new ArrayList<DB.App>();
    private Context mContext;

    private boolean pref_compact;

    public AppListAdapter(Context context) {
        mContext = context;
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        pref_compact = prefs.getBoolean("compactlayout", false);
    }

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

        String vs;
        if (app.hasUpdates)
            vs = app.installedVersion + " -> " + app.updateVersion;
        else if (app.installedVersion != null)
            vs = app.installedVersion;
        else {
            int numav = app.apks.size();
            if (numav == 1)
                vs = mContext.getString(R.string.n_version_available);
            else
                vs = mContext.getString(R.string.n_versions_available);
            vs = String.format(vs, numav);
        }

        TextView status = (TextView) v.findViewById(R.id.status);
        status.setText(vs);

        TextView license = (TextView) v.findViewById(R.id.license);
        license.setText(app.license);

        TextView summary = (TextView) v.findViewById(R.id.summary);
        if (pref_compact)
            summary.setVisibility(View.GONE);
        else
            summary.setText(app.summary);

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

}
