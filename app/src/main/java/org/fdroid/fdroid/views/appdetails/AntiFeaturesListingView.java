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

import java.util.HashMap;

public class AntiFeaturesListingView extends RecyclerView {

    static HashMap<String, String> antiFeatureDescriptions;
    static HashMap<String, Integer> antiFeatureIcons;

    public AntiFeaturesListingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        if (antiFeatureDescriptions == null) {
            antiFeatureDescriptions = new HashMap<>();
            antiFeatureDescriptions.put(
                    context.getString(R.string.antiads_key),
                    context.getString(R.string.antiadslist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antitrack_key),
                    context.getString(R.string.antitracklist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antinonfreenet_key),
                    context.getString(R.string.antinonfreenetlist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antinonfreead_key),
                    context.getString(R.string.antinonfreeadlist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antinonfreedep_key),
                    context.getString(R.string.antinonfreedeplist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antiupstreamnonfree_key),
                    context.getString(R.string.antiupstreamnonfreelist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antinonfreeassets_key),
                    context.getString(R.string.antinonfreeassetslist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antidisabledalgorithm_key),
                    context.getString(R.string.antidisabledalgorithmlist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antiknownvuln_key),
                    context.getString(R.string.antiknownvulnlist)
            );
            antiFeatureDescriptions.put(
                    context.getString(R.string.antinosource_key),
                    context.getString(R.string.antinosourcesince)
            );
        }

        if (antiFeatureIcons == null) {
            antiFeatureIcons = new HashMap<>();
            antiFeatureIcons.put(
                    context.getString(R.string.antiads_key),
                    R.drawable.ic_antifeature_ads
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antitrack_key),
                    R.drawable.ic_antifeature_tracking
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antinonfreenet_key),
                    R.drawable.ic_antifeature_nonfreenet
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antinonfreead_key),
                    R.drawable.ic_antifeature_nonfreeadd
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antinonfreedep_key),
                    R.drawable.ic_antifeature_nonfreedep
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antiupstreamnonfree_key),
                    R.drawable.ic_antifeature_upstreamnonfree
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antinonfreeassets_key),
                    R.drawable.ic_antifeature_nonfreeassets
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antidisabledalgorithm_key),
                    R.drawable.ic_antifeature_disabledalgorithm
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antiknownvuln_key),
                    R.drawable.ic_antifeature_knownvuln
            );
            antiFeatureIcons.put(
                    context.getString(R.string.antinosource_key),
                    R.drawable.ic_antifeature_nosourcesince
            );
        }
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
                        getAntiFeatureDescriptionText(antiFeatureName));
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

    public static String getAntiFeatureDescriptionText(String antiFeatureName) {
        String description = antiFeatureDescriptions.get(antiFeatureName);
        if (description == null) {
            return antiFeatureName;
        }

        return description;
    }

    public static @DrawableRes int antiFeatureIcon(String antiFeatureName) {
        Integer icon = antiFeatureIcons.get(antiFeatureName);
        if (icon == null) {
            return R.drawable.ic_cancel;
        }

        return icon;
    }
}
