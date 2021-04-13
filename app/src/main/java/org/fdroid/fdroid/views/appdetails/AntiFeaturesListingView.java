/*
 * Copyright (C) 2019 Michael Pöhn <michael.poehn@fsfe.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid.views.appdetails;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AntiFeaturesListingView extends RecyclerView {

    public AntiFeaturesListingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setApp(final App app) {

        setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);

        swapAdapter(new RecyclerView.Adapter<AntiFeatureItemViewHolder>() {

            @NonNull
            @Override
            public AntiFeatureItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                View view = inflater.inflate(R.layout.listitem_antifeaturelisting, null);
                return new AntiFeatureItemViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull AntiFeatureItemViewHolder holder, int position) {
                final String antiFeatureName = app.antiFeatures[position];
                holder.antiFeatureIcon.setBackgroundDrawable(
                        ContextCompat.getDrawable(getContext(), antiFeatureIcon(antiFeatureName)));
                holder.antiFeatureText.setText(
                        getAntiFeatureDescriptionText(holder.antiFeatureText.getContext(), antiFeatureName));
                holder.entireView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= 21) {
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        } else {
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        }
                        i.setData(Uri.parse("https://f-droid.org/docs/Anti-Features#" + antiFeatureName));
                        getContext().startActivity(i);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return app == null || app.antiFeatures == null ? 0 : app.antiFeatures.length;
            }
        }, false);
    }

    class AntiFeatureItemViewHolder extends RecyclerView.ViewHolder {

        private final View entireView;
        private final View antiFeatureIcon;
        private final TextView antiFeatureText;

        AntiFeatureItemViewHolder(View itemView) {
            super(itemView);
            entireView = itemView;
            antiFeatureIcon = itemView.findViewById(R.id.anti_feature_icon);
            antiFeatureText = itemView.findViewById(R.id.anti_feature_text);
        }

    }

    public static String getAntiFeatureDescriptionText(Context context, String antiFeatureName) {
        switch (antiFeatureName) {
            case "Ads":
                return context.getString(R.string.antiadslist);
            case "Tracking":
                return context.getString(R.string.antitracklist);
            case "NonFreeNet":
                return context.getString(R.string.antinonfreenetlist);
            case "NonFreeAdd":
                return context.getString(R.string.antinonfreeadlist);
            case "NonFreeDep":
                return context.getString(R.string.antinonfreedeplist);
            case "UpstreamNonFree":
                return context.getString(R.string.antiupstreamnonfreelist);
            case "NonFreeAssets":
                return context.getString(R.string.antinonfreeassetslist);
            case "DisabledAlgorithm":
                return context.getString(R.string.antidisabledalgorithmlist);
            case "KnownVuln":
                return context.getString(R.string.antiknownvulnlist);
            case "NoSourceSince":
                return context.getString(R.string.antinosourcesince);
            default:
                return antiFeatureName;
        }
    }

    public static @DrawableRes int antiFeatureIcon(String antiFeatureName) {
        switch (antiFeatureName) {
            case "Ads":
                return R.drawable.ic_antifeature_ads;
            case "Tracking":
                return R.drawable.ic_antifeature_tracking;
            case "NonFreeNet":
                return R.drawable.ic_antifeature_nonfreenet;
            case "NonFreeAdd":
                return R.drawable.ic_antifeature_nonfreeadd;
            case "NonFreeDep":
                return R.drawable.ic_antifeature_nonfreedep;
            case "UpstreamNonFree":
                return R.drawable.ic_antifeature_upstreamnonfree;
            case "NonFreeAssets":
                return R.drawable.ic_antifeature_nonfreeassets;
            case "DisabledAlgorithm":
                return R.drawable.ic_antifeature_disabledalgorithm;
            case "KnownVuln":
                return R.drawable.ic_antifeature_knownvuln;
            case "NoSourceSince":
                return R.drawable.ic_antifeature_nosourcesince;
            default:
                return R.drawable.ic_cancel;
        }
    }
}
