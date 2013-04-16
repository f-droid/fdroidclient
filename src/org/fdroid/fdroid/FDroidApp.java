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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Application;
import android.util.Log;

public class FDroidApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        File local_path = DB.getDataPath();
        Log.d("FDroid", "Data path is " + local_path.getPath());
        if (!local_path.exists())
            local_path.mkdir();

        File icon_path = DB.getIconsPath();
        Log.d("FDroid", "Icon path is " + icon_path.getPath());
        if (!icon_path.exists())
            icon_path.mkdir();

        apps = null;
        DB.initDB(getApplicationContext());
    
    }

    // Global list of all known applications.
    private List<DB.App> apps;

    // Set when something has changed (database or installed apps) so we know
    // we should invalidate the apps.
    private volatile boolean appsInvalid = false;
    private Semaphore appsInvalidLock = new Semaphore(1, false);

    // Set apps invalid. Call this when the database has been updated with
    // new app information, or when the installed packages have changed.
    public void invalidateApps() {
        try {
            appsInvalidLock.acquire();
            appsInvalid = true;
        } catch (InterruptedException e) {
            // Don't care
        } finally {
            appsInvalidLock.release();
        }
    }

    // Get a list of all known applications. Should not be called when the
    // database is locked (i.e. between DB.getDB() and db.releaseDB(). The
    // contents should never be modified, it's for reading only.
    public List<DB.App> getApps() {

        boolean invalid = false;
        try {
            appsInvalidLock.acquire();
            invalid = appsInvalid;
            if (invalid) {
                appsInvalid = false;
                Log.d("FDroid", "Dropping cached app data");
            }
        } catch (InterruptedException e) {
            // Don't care
        } finally {
            appsInvalidLock.release();
        }

        if (apps == null || invalid) {
            try {
                DB db = DB.getDB();
                apps = db.getApps(true);
            } finally {
                DB.releaseDB();
            }
        }
        if (apps == null)
            return new ArrayList<DB.App>();
        return apps;
    }

}
