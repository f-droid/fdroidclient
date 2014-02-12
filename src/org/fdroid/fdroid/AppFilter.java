/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
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

package org.fdroid.fdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.fdroid.fdroid.data.App;

public class AppFilter {

    // Return true if the given app should be filtered out based on user
    // preferences, and false otherwise.
    public boolean filter(App app) {

        boolean dontFilterRequiringRoot = Preferences.get().filterAppsRequiringRoot();

        if (app.requirements == null || dontFilterRequiringRoot) return false;
        
        for (String r : app.requirements) {
            if (r.equals("root"))
                return true;
        }

        return false;

    }

}
