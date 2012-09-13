/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

package org.fdroid.fdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppFilter {

    boolean pref_antiAds;
    boolean pref_antiTracking;
    boolean pref_antiNonFreeAdd;
    boolean pref_antiNonFreeNet;
    boolean pref_antiNonFreeDep;
    boolean pref_rooted;

    public AppFilter(Context ctx) {

        // Read preferences and cache them so we can do quick lookups.
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        pref_antiAds = prefs.getBoolean("antiAds", false);
        pref_antiTracking = prefs.getBoolean("antiTracking", false);
        pref_antiNonFreeAdd = prefs.getBoolean("antiNonFreeAdd", false);
        pref_antiNonFreeNet = prefs.getBoolean("antiNonFreeNet", false);
        pref_antiNonFreeDep = prefs.getBoolean("antiNonFreeDep", false);
        pref_rooted = prefs.getBoolean("rooted", true);
    }

    // Return true if the given app should be filtered based on user
    // preferences, and false otherwise.
    public boolean filter(DB.App app) {
        boolean filtered = false;
        if (app.antiFeatures != null) {
            for (String af : app.antiFeatures) {
                if (af.equals("Ads") && !pref_antiAds)
                    filtered = true;
                else if (af.equals("Tracking") && !pref_antiTracking)
                    filtered = true;
                else if (af.equals("NonFreeNet") && !pref_antiNonFreeNet)
                    filtered = true;
                else if (af.equals("NonFreeAdd") && !pref_antiNonFreeAdd)
                    filtered = true;
                else if (af.equals("NonFreeDep") && !pref_antiNonFreeDep)
                    filtered = true;
            }
        }
        if (app.requirements != null) {
            for (String r : app.requirements) {
                if (r.equals("root") && !pref_rooted)
                    filtered = true;
            }
        }
        return filtered;
    }

}
