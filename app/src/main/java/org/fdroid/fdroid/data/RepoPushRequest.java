/*
 * Copyright (C) 2016 Blue Jay Wireless
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.data;

import android.support.annotation.Nullable;
import org.fdroid.fdroid.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Represents action requests embedded in the index XML received from a repo.
 * When {@link #versionCode} is {@code null}, that means that the
 * {@code versionCode} was not specified by the server, and F-Droid should
 * install the best available version.
 */
public class RepoPushRequest {
    public static final String TAG = "RepoPushRequest";

    public static final String INSTALL = "install";
    public static final String UNINSTALL = "uninstall";
    public static final List<String> VALID_REQUESTS = Arrays.asList(INSTALL, UNINSTALL);

    public final String request;
    public final String packageName;
    @Nullable
    public final Integer versionCode;

    /**
     * Create a new instance.  {@code request} is validated against the list of
     * valid install requests.  {@code packageName} has a safety validation to
     * make sure that only valid Android/Java Package Name characters are included.
     * If validation fails, the the values are set to {@code null}, which are
     * handled in {@link org.fdroid.fdroid.IndexV1Updater#processRepoPushRequests(List)}
     * or {@link org.fdroid.fdroid.IndexUpdater#processRepoPushRequests(List)}
     */
    public RepoPushRequest(String request, String packageName, @Nullable String versionCode) {
        if (VALID_REQUESTS.contains(request)) {
            this.request = request;
        } else {
            this.request = null;
        }

        if (Utils.isSafePackageName(packageName)) {
            this.packageName = packageName;
        } else {
            this.packageName = null;
        }

        Integer i;
        try {
            i = Integer.parseInt(versionCode);
        } catch (NumberFormatException e) {
            i = null;
        }
        this.versionCode = i;
    }

    @Override
    public String toString() {
        return request + " " + packageName + " " + versionCode;
    }
}
