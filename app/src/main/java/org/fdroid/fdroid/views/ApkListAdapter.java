package org.fdroid.fdroid.views;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.RepoProvider;

import java.util.List;

public class ApkListAdapter extends ArrayAdapter<Apk> {

    private static final String TAG = "ApkListAdapter";

    private final LayoutInflater mInflater;
    private final App mApp;

    public ApkListAdapter(Context context, App app) {
        super(context, 0);
        mApp = app;
        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final List<Apk> apks = ApkProvider.Helper.findByPackageName(context, app.packageName);
        for (final Apk apk : apks) {
            if (apk.compatible || Preferences.get().showIncompatibleVersions()) {
                add(apk);
            }
        }
    }

    private String getInstalledStatus(final Apk apk) {
        // Definitely not installed.
        if (apk.versionCode != mApp.installedVersionCode) {
            return getContext().getString(R.string.app_not_installed);
        }
        // Definitely installed this version.
        if (apk.sig != null && apk.sig.equals(mApp.installedSig)) {
            return getContext().getString(R.string.app_installed);
        }
        // Installed the same version, but from someplace else.
        final String installerPkgName;
        try {
            installerPkgName = getContext().getPackageManager().getInstallerPackageName(mApp.packageName);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Application " + mApp.packageName + " is not installed anymore");
            return getContext().getString(R.string.app_not_installed);
        }
        if (TextUtils.isEmpty(installerPkgName)) {
            return getContext().getString(R.string.app_inst_unknown_source);
        }
        final String installerLabel = InstalledAppProvider
                .getApplicationLabel(getContext(), installerPkgName);
        return getContext().getString(R.string.app_inst_known_source, installerLabel);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        java.text.DateFormat df = DateFormat.getDateFormat(getContext());
        final Apk apk = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.apklistitem, parent, false);

            holder = new ViewHolder();
            holder.version = (TextView) convertView.findViewById(R.id.version);
            holder.status = (TextView) convertView.findViewById(R.id.status);
            holder.repository = (TextView) convertView.findViewById(R.id.repository);
            holder.size = (TextView) convertView.findViewById(R.id.size);
            holder.api = (TextView) convertView.findViewById(R.id.api);
            holder.incompatibleReasons = (TextView) convertView.findViewById(R.id.incompatible_reasons);
            holder.buildtype = (TextView) convertView.findViewById(R.id.buildtype);
            holder.added = (TextView) convertView.findViewById(R.id.added);
            holder.nativecode = (TextView) convertView.findViewById(R.id.nativecode);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.version.setText(getContext().getString(R.string.version)
                + " " + apk.versionName
                + (apk.versionCode == mApp.suggestedVersionCode ? "  ☆" : ""));

        holder.status.setText(getInstalledStatus(apk));

        holder.repository.setText(getContext().getString(R.string.repo_provider,
                RepoProvider.Helper.findById(getContext(), apk.repo).getName()));

        if (apk.size > 0) {
            holder.size.setText(Utils.getFriendlySize(apk.size));
            holder.size.setVisibility(View.VISIBLE);
        } else {
            holder.size.setVisibility(View.GONE);
        }

        if (!Preferences.get().expertMode()) {
            holder.api.setVisibility(View.GONE);
        } else if (apk.minSdkVersion > 0 && apk.maxSdkVersion < Apk.SDK_VERSION_MAX_VALUE) {
            holder.api.setText(getContext().getString(R.string.minsdk_up_to_maxsdk,
                    Utils.getAndroidVersionName(apk.minSdkVersion),
                    Utils.getAndroidVersionName(apk.maxSdkVersion)));
            holder.api.setVisibility(View.VISIBLE);
        } else if (apk.minSdkVersion > 0) {
            holder.api.setText(getContext().getString(R.string.minsdk_or_later,
                    Utils.getAndroidVersionName(apk.minSdkVersion)));
            holder.api.setVisibility(View.VISIBLE);
        } else if (apk.maxSdkVersion > 0) {
            holder.api.setText(getContext().getString(R.string.up_to_maxsdk,
                    Utils.getAndroidVersionName(apk.maxSdkVersion)));
            holder.api.setVisibility(View.VISIBLE);
        }

        if (apk.srcname != null) {
            holder.buildtype.setText("source");
        } else {
            holder.buildtype.setText("bin");
        }

        if (apk.added != null) {
            holder.added.setText(getContext().getString(R.string.added_on,
                    df.format(apk.added)));
            holder.added.setVisibility(View.VISIBLE);
        } else {
            holder.added.setVisibility(View.GONE);
        }

        if (Preferences.get().expertMode() && apk.nativecode != null) {
            holder.nativecode.setText(TextUtils.join(" ", apk.nativecode));
            holder.nativecode.setVisibility(View.VISIBLE);
        } else {
            holder.nativecode.setVisibility(View.GONE);
        }

        if (apk.incompatibleReasons != null) {
            holder.incompatibleReasons.setText(
                    getContext().getResources().getString(
                            R.string.requires_features,
                            TextUtils.join(", ", apk.incompatibleReasons)));
            holder.incompatibleReasons.setVisibility(View.VISIBLE);
        } else {
            holder.incompatibleReasons.setVisibility(View.GONE);
        }

        // Disable it all if it isn't compatible...
        final View[] views = {
                convertView,
                holder.version,
                holder.status,
                holder.repository,
                holder.size,
                holder.api,
                holder.buildtype,
                holder.added,
                holder.nativecode,
        };

        for (final View v : views) {
            v.setEnabled(apk.compatible);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView version;
        TextView status;
        TextView repository;
        TextView size;
        TextView api;
        TextView incompatibleReasons;
        TextView buildtype;
        TextView added;
        TextView nativecode;
    }
}