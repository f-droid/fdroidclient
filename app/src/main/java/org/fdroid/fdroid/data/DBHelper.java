/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2015-2016 Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2015 Christian Morgner
 * Copyright (C) 2014-2016 Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2013-2016 Peter Serwylo <peter@serwylo.com>
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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.fdroid.database.FDroidDatabase;
import org.fdroid.database.FDroidDatabaseHolder;
import org.fdroid.database.InitialRepository;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This is basically a singleton used to represent the database at the core
 * of all of the {@link android.content.ContentProvider}s used at the core
 * of this app.  {@link DBHelper} is not {@code private} so that it can be easily
 * used in test subclasses.
 */
@SuppressWarnings("LineLength")
public class DBHelper {

    private static final String TAG = "DBHelper";
    static final int REPO_XML_ITEM_COUNT = 7;

    public static FDroidDatabase getDb(Context context) {
        return FDroidDatabaseHolder.getDb(context, "fdroid_db", db -> prePopulateDb(context, db));
    }

    @WorkerThread
    @VisibleForTesting
    static void prePopulateDb(Context context, FDroidDatabase db) {
        List<String> initialRepos = DBHelper.loadInitialRepos(context);
        int weight = 1;
        for (int i = 0; i < initialRepos.size(); i += REPO_XML_ITEM_COUNT) {
            InitialRepository repo = new InitialRepository(
                    initialRepos.get(i), // name
                    initialRepos.get(i + 1), // address
                    initialRepos.get(i + 2), // description
                    initialRepos.get(i + 6),  // certificate
                    Integer.parseInt(initialRepos.get(i + 3)), // version
                    initialRepos.get(i + 4).equals("1"), // enabled
                    weight++ // weight
            );
            db.getRepositoryDao().insert(repo);
        }
        // Migrate repos from old content providers to new Room-based DB.
        // Added end of 2022 for alphas, can be removed after sufficient time has passed.
        ContentProviderMigrator migrator = new ContentProviderMigrator();
        if (migrator.needsMigration(context)) {
            Log.d(TAG, "Migrating DB...");
            migrator.runMigrations(context, db);
            migrator.removeOldDb(context);
            // force update on UiThread in case we need to show Toasts
            new Handler(Looper.getMainLooper()).post(() -> UpdateService.forceUpdateRepo(context));
        }
    }

    /**
     * Removes all index data related to apps from the DB.
     * Leaves repositories, their preferences as well as app preferences in place.
     */
    @AnyThread
    public static void resetTransient(Context context) {
        FDroidDatabase db = getDb(context);
        Utils.runOffUiThread(db::clearAllAppData);
    }

    @AnyThread
    public static void resetRepos(Context context) {
        FDroidDatabase db = getDb(context);
        Utils.runOffUiThread(() -> db.runInTransaction(() -> {
            db.getRepositoryDao().clearAll();
            prePopulateDb(context, db);
        }));
    }

    /**
     * Load Additional Repos first, then Default Repos. This way, Default
     * Repos will be shown after the OEM-added ones on the Manage Repos
     * screen.  This throws a hard {@code Exception} on parse errors since
     * Default Repos are built into the APK.  So it should fail as hard and fast
     * as possible so the developer catches the problem.
     * <p>
     * Additional Repos ({@code additional_repos.xml}) come from the ROM,
     * while Default Repos ({@code default_repos.xml} is built into the APK.
     * <p>
     * This also cleans up the whitespace in the description item, since the
     * XML parsing will include the linefeeds and indenting in the description.
     */
    static List<String> loadInitialRepos(Context context) throws IllegalArgumentException {
        String packageName = context.getPackageName();

        // get additional repos from OS (lowest priority/weight)
        List<String> additionalRepos = DBHelper.loadAdditionalRepos(packageName);
        if (additionalRepos.size() % REPO_XML_ITEM_COUNT != 0) {
            throw new IllegalArgumentException("additional_repos.xml has wrong item count: " +
                    additionalRepos.size() + " % REPO_XML_ARG_COUNT(" + REPO_XML_ITEM_COUNT + ") != 0");
        }

        // get our own default repos (higher priority/weight)
        List<String> defaultRepos = Arrays.asList(context.getResources().getStringArray(R.array.default_repos));
        if (defaultRepos.size() % REPO_XML_ITEM_COUNT != 0) {
            throw new IllegalArgumentException("default_repos.xml has wrong item count: " +
                    defaultRepos.size() + " % REPO_XML_ARG_COUNT(" + REPO_XML_ITEM_COUNT +
                    ") != 0, FYI the priority item was removed in v1.16");
        }

        List<String> repos = new ArrayList<>(additionalRepos.size() + defaultRepos.size());
        repos.addAll(additionalRepos);
        repos.addAll(defaultRepos);

        final int descriptionIndex = 2;
        for (int i = descriptionIndex; i < repos.size(); i += REPO_XML_ITEM_COUNT) {
            String description = repos.get(i);
            repos.set(i, description.replaceAll("\\s+", " "));
        }

        return repos;
    }

    /**
     * Look for additional, initial repositories from the device's filesystem.
     * These can be added as part of the ROM ({@code /system} or {@code /product}
     * or included later by vendors/OEMs ({@code /vendor}, {@code /odm}, {@code /oem}).
     * These are always added at a lower priority than the repos embedded in the APK via
     * {@code default_repos.xml}.
     * <p>
     * ROM (System) has the lowest priority, then Product, Vendor, ODM, and OEM.
     */
    private static List<String> loadAdditionalRepos(String packageName) {
        List<String> repoItems = new LinkedList<>();
        for (String root : Arrays.asList("/system", "/product", "/vendor", "/odm", "/oem")) {
            File additionalReposFile = new File(root + "/etc/" + packageName + "/additional_repos.xml");
            try {
                if (additionalReposFile.isFile()) {
                    repoItems.addAll(DBHelper.parseAdditionalReposXml(additionalReposFile));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading " + additionalReposFile + ": " + e.getMessage());
            }
        }

        return repoItems;
    }

    /**
     * Parse {@code additional_repos.xml} into a list of items. Walk through
     * all TEXT pieces of the xml file and put them into a single list of repo
     * elements.  Each repo is defined as eight elements in that list.
     * {@code additional_repos.xml} has seven elements per repo because it is
     * not allowed to set the priority since that would give it the power to
     * override {@code default_repos.xml}.
     */
    static List<String> parseAdditionalReposXml(File additionalReposFile)
            throws IOException, XmlPullParserException {
        List<String> repoItems = new LinkedList<>();
        InputStream xmlInputStream = new FileInputStream(additionalReposFile);
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(xmlInputStream, "UTF-8");

        int eventType = parser.getEventType();
        boolean isItem = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagname = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("item".equals(tagname)) {
                        isItem = true;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    isItem = false;
                    break;
                case XmlPullParser.TEXT:
                    if (isItem) {
                        repoItems.add(parser.getText());
                    }
                    break;
            }
            eventType = parser.next();
        }
        xmlInputStream.close();

        if (repoItems.size() % REPO_XML_ITEM_COUNT == 0) {
            return repoItems;
        }

        Log.e(TAG, "Ignoring " + additionalReposFile + ", wrong number of items: "
                + repoItems.size() + " % " + (REPO_XML_ITEM_COUNT - 1) + " != 0");
        return new LinkedList<>();
    }

}
