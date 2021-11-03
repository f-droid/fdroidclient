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
                        ContextCompat.getDrawable(
                                getContext(),
                                antiFeatureIcon(
                                        holder.antiFeatureText.getContext(),
                                        antiFeatureName
                                )
                        )
                );
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
        if (antiFeatureName.equals(context.getString(R.string.antiads_key))) {
            return context.getString(R.string.antiadslist);
        } else if (antiFeatureName.equals(context.getString(R.string.antitrack_key))) {
            return context.getString(R.string.antitracklist);
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreenet_key))) {
            return context.getString(R.string.antinonfreenetlist);
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreead_key))) {
            return context.getString(R.string.antinonfreeadlist);
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreedep_key))) {
            return context.getString(R.string.antinonfreedeplist);
        } else if (antiFeatureName.equals(context.getString(R.string.antiupstreamnonfree_key))) {
            return context.getString(R.string.antiupstreamnonfreelist);
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreeassets_key))) {
            return context.getString(R.string.antinonfreeassetslist);
        } else if (antiFeatureName.equals(context.getString(R.string.antidisabledalgorithm_key))) {
            return context.getString(R.string.antidisabledalgorithmlist);
        } else if (antiFeatureName.equals(context.getString(R.string.antiknownvuln_key))) {
            return context.getString(R.string.antiknownvulnlist);
        } else if (antiFeatureName.equals(context.getString(R.string.antinosource_key))) {
            return context.getString(R.string.antinosourcesince);
        } else {
            return antiFeatureName;
        }
    }

    public static @DrawableRes int antiFeatureIcon(Context context, String antiFeatureName) {
        if (antiFeatureName.equals(context.getString(R.string.antiads_key))) {
            return R.drawable.ic_antifeature_ads;
        } else if (antiFeatureName.equals(context.getString(R.string.antitrack_key))) {
            return R.drawable.ic_antifeature_tracking;
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreenet_key))) {
            return R.drawable.ic_antifeature_nonfreenet;
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreead_key))) {
            return R.drawable.ic_antifeature_nonfreeadd;
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreedep_key))) {
            return R.drawable.ic_antifeature_nonfreedep;
        } else if (antiFeatureName.equals(context.getString(R.string.antiupstreamnonfree_key))) {
            return R.drawable.ic_antifeature_upstreamnonfree;
        } else if (antiFeatureName.equals(context.getString(R.string.antinonfreeassets_key))) {
            return R.drawable.ic_antifeature_nonfreeassets;
        } else if (antiFeatureName.equals(context.getString(R.string.antidisabledalgorithm_key))) {
            return R.drawable.ic_antifeature_disabledalgorithm;
        } else if (antiFeatureName.equals(context.getString(R.string.antiknownvuln_key))) {
            return R.drawable.ic_antifeature_knownvuln;
        } else if (antiFeatureName.equals(context.getString(R.string.antinosource_key))) {
            return R.drawable.ic_antifeature_nosourcesince;
        } else {
            return R.drawable.ic_cancel;
        }
    }
}