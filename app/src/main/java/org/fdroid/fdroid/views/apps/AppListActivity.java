/*
 * Copyright (C) 2016-17 Peter Serwylo,
 * Copyright (C) 2017-18 Hans-Christoph Steiner
 * Copyright (C) 2019 Michael Pöhn, michael.poehn@fsfe.org
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

package org.fdroid.fdroid.views.apps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.fdroid.fdroid.views.main.MainActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Provides scrollable listing of apps for search and category views.
 */
public class AppListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        CategoryTextWatcher.SearchTermsChangedListener {

    public static final String TAG = "AppListActivity";

    public static final String EXTRA_CATEGORY
            = "org.fdroid.fdroid.views.apps.AppListActivity.EXTRA_CATEGORY";
    public static final String EXTRA_SEARCH_TERMS
            = "org.fdroid.fdroid.views.apps.AppListActivity.EXTRA_SEARCH_TERMS";

    private static final String SEARCH_TERMS_KEY = "searchTerms";
    private static final String SORT_CLAUSE_KEY = "sortClauseSelected";
    private static SharedPreferences savedSearchSettings;

    private RecyclerView appView;
    private AppListAdapter appAdapter;
    private String category;
    private String searchTerms;
    private String sortClauseSelected;
    private TextView emptyState;
    private EditText searchInput;
    private ImageView sortImage;
    private View hiddenAppNotice;
    private Utils.KeyboardStateMonitor keyboardStateMonitor;

    private interface SortClause {
        String WORDS = Cols.NAME;
        String LAST_UPDATED = Cols.LAST_UPDATED;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_list);

        keyboardStateMonitor = new Utils.KeyboardStateMonitor(findViewById(R.id.app_list_root));

        savedSearchSettings = getSavedSearchSettings(this);
        searchTerms = savedSearchSettings.getString(SEARCH_TERMS_KEY, null);
        sortClauseSelected = savedSearchSettings.getString(SORT_CLAUSE_KEY, SortClause.LAST_UPDATED);

        searchInput = (EditText) findViewById(R.id.search);
        searchInput.setText(searchTerms);
        searchInput.addTextChangedListener(new CategoryTextWatcher(this, searchInput, this));
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Hide the keyboard (http://stackoverflow.com/a/1109108 (when pressing search)
                    InputMethodManager inputManager = ContextCompat.getSystemService(AppListActivity.this,
                            InputMethodManager.class);
                    inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);

                    // Change focus from the search input to the app list.
                    appView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        sortImage = (ImageView) findViewById(R.id.sort);
        final Drawable lastUpdated = DrawableCompat.wrap(ContextCompat.getDrawable(this,
                R.drawable.ic_last_updated)).mutate();
        final Drawable words = DrawableCompat.wrap(ContextCompat.getDrawable(AppListActivity.this,
                R.drawable.ic_sort)).mutate();
        sortImage.setImageDrawable(SortClause.WORDS.equals(sortClauseSelected) ? words : lastUpdated);
        sortImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (sortClauseSelected) {
                    case SortClause.WORDS:
                        sortClauseSelected = SortClause.LAST_UPDATED;
                        DrawableCompat.setTint(lastUpdated, FDroidApp.isAppThemeLight() ? Color.BLACK : Color.WHITE);
                        sortImage.setImageDrawable(lastUpdated);
                        break;
                    case SortClause.LAST_UPDATED:
                        sortClauseSelected = SortClause.WORDS;
                        DrawableCompat.setTint(words, FDroidApp.isAppThemeLight() ? Color.BLACK : Color.WHITE);
                        sortImage.setImageDrawable(words);
                        break;
                }
                putSavedSearchSettings(getApplicationContext(), SORT_CLAUSE_KEY, sortClauseSelected);
                getSupportLoaderManager().restartLoader(0, null, AppListActivity.this);
                appView.scrollToPosition(0);
            }
        });

        hiddenAppNotice = findViewById(R.id.hiddenAppNotice);
        hiddenAppNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_VIEW_SETTINGS, true);
                getApplicationContext().startActivity(intent);
            }
        });
        emptyState = (TextView) findViewById(R.id.empty_state);

        View backButton = findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        View clearButton = findViewById(R.id.clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText("");
                searchInput.requestFocus();
                if (!keyboardStateMonitor.isKeyboardVisible()) {
                    InputMethodManager inputMethodManager =
                            ContextCompat.getSystemService(AppListActivity.this,
                                    InputMethodManager.class);
                    inputMethodManager.toggleSoftInputFromWindow(v.getApplicationWindowToken(),
                            InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        appAdapter = new AppListAdapter(this);

        appView = (RecyclerView) findViewById(R.id.app_list);
        appView.setHasFixedSize(true);
        appView.setLayoutManager(new LinearLayoutManager(this));
        appView.setAdapter(appAdapter);

        parseIntentForSearchQuery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Glide.with(this).applyDefaultRequestOptions(new RequestOptions()
                .onlyRetrieveFromCache(!Preferences.get().isBackgroundDownloadAllowed()));
    }

    private void parseIntentForSearchQuery() {
        Intent intent = getIntent();
        category = intent.hasExtra(EXTRA_CATEGORY) ? intent.getStringExtra(EXTRA_CATEGORY) : null;
        searchTerms = intent.hasExtra(EXTRA_SEARCH_TERMS) ? intent.getStringExtra(EXTRA_SEARCH_TERMS) : null;

        searchInput.setText(getSearchText(category, searchTerms));
        searchInput.setSelection(searchInput.getText().length());

        if (category != null) {
            // Do this so that the search input does not get focus by default. This allows for a user
            // experience where the user scrolls through the apps in the category.
            appView.requestFocus();
        }

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private CharSequence getSearchText(@Nullable String category, @Nullable String searchTerms) {
        StringBuilder string = new StringBuilder();
        if (category != null) {
            string.append(category).append(":");
        }

        if (searchTerms != null) {
            string.append(searchTerms);
        }

        return string.toString();
    }

    private void setShowHiddenAppsNotice(boolean show) {
        hiddenAppNotice.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                AppProvider.getSearchUri(searchTerms, category),
                AppMetadataTable.Cols.ALL,
                null,
                null,
                getSortOrder()
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        setShowHiddenAppsNotice(false);
        appAdapter.setHasHiddenAppsCallback(() -> setShowHiddenAppsNotice(true));
        appAdapter.setAppCursor(cursor);
        if (cursor.getCount() > 0) {
            emptyState.setVisibility(View.GONE);
            appView.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
            appView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        appAdapter.setAppCursor(null);
    }

    @Override
    public void onSearchTermsChanged(@Nullable String category, @NonNull String searchTerms) {
        this.category = category;
        this.searchTerms = searchTerms;
        appView.scrollToPosition(0);
        getSupportLoaderManager().restartLoader(0, null, this);
        if (TextUtils.isEmpty(searchTerms)) {
            removeSavedSearchSettings(this, SEARCH_TERMS_KEY);
        } else {
            putSavedSearchSettings(this, SEARCH_TERMS_KEY, searchTerms);
        }
    }

    private String getSortOrder() {
        final String table = AppMetadataTable.NAME;
        final String nameCol = table + "." + AppMetadataTable.Cols.NAME;
        final String summaryCol = table + "." + AppMetadataTable.Cols.SUMMARY;
        final String packageCol = Cols.Package.PACKAGE_NAME;

        if (sortClauseSelected.equals(SortClause.LAST_UPDATED)) {
            return table + "." + Cols.LAST_UPDATED + " DESC"
                    + ", " + table + "." + Cols.IS_LOCALIZED + " DESC"
                    + ", " + table + "." + Cols.ADDED + " ASC"
                    + ", " + table + "." + Cols.NAME + " IS NULL ASC"
                    + ", CASE WHEN " + table + "." + Cols.ICON + " IS NULL"
                    + "        AND " + table + "." + Cols.ICON_URL + " IS NULL"
                    + "        THEN 1 ELSE 0 END"
                    + ", " + table + "." + Cols.SUMMARY + " IS NULL ASC"
                    + ", " + table + "." + Cols.DESCRIPTION + " IS NULL ASC"
                    + ", " + table + "." + Cols.WHATSNEW + " IS NULL ASC"
                    + ", CASE WHEN " + table + "." + Cols.PHONE_SCREENSHOTS + " IS NULL"
                    + "        AND " + table + "." + Cols.SEVEN_INCH_SCREENSHOTS + " IS NULL"
                    + "        AND " + table + "." + Cols.TEN_INCH_SCREENSHOTS + " IS NULL"
                    + "        AND " + table + "." + Cols.TV_SCREENSHOTS + " IS NULL"
                    + "        AND " + table + "." + Cols.WEAR_SCREENSHOTS + " IS NULL"
                    + "        AND " + table + "." + Cols.FEATURE_GRAPHIC + " IS NULL"
                    + "        AND " + table + "." + Cols.PROMO_GRAPHIC + " IS NULL"
                    + "        AND " + table + "." + Cols.TV_BANNER + " IS NULL"
                    + "        THEN 1 ELSE 0 END";
        }

        // prevent SQL injection https://en.wikipedia.org/wiki/SQL_injection#Escaping
        final String[] terms = searchTerms.trim().replaceAll("[\\x1a\0\n\r\"';\\\\]+", " ").split("\\s+");
        if (terms.length == 0 || terms[0].equals("")) {
            return table + "." + Cols.NAME + " COLLATE LOCALIZED ";
        }

        boolean potentialPackageName = false;
        StringBuilder packageNameFirstCase = new StringBuilder();
        if (terms[0].length() > 2 && terms[0].substring(1, terms[0].length() - 1).contains(".")) {
            potentialPackageName = true;
            packageNameFirstCase.append(String.format("%s LIKE '%%%s%%' ",
                    packageCol, terms[0]));
        }
        StringBuilder titleCase = new StringBuilder(String.format("%s like '%%%s%%'", nameCol, terms[0]));
        StringBuilder summaryCase = new StringBuilder(String.format("%s like '%%%s%%'", summaryCol, terms[0]));
        StringBuilder packageNameCase = new StringBuilder(String.format("%s like '%%%s%%'", packageCol, terms[0]));
        for (int i = 1; i < terms.length; i++) {
            if (potentialPackageName) {
                packageNameCase.append(String.format(" and %s like '%%%s%%'", summaryCol, terms[i]));
            }
            titleCase.append(String.format(" and %s like '%%%s%%'", nameCol, terms[i]));
            summaryCase.append(String.format(" and %s like '%%%s%%'", summaryCol, terms[i]));
        }
        String sortOrder;
        if (packageNameCase.length() > 0) {
            sortOrder = String.format("CASE WHEN %s THEN 0 WHEN %s THEN 1 WHEN %s THEN 2 ELSE 3 END",
                    packageNameCase.toString(), titleCase.toString(), summaryCase.toString());
        } else {
            sortOrder = String.format("CASE WHEN %s THEN 1 WHEN %s THEN 2 ELSE 3 END",
                    titleCase.toString(), summaryCase.toString());
        }
        return sortOrder
                + ", " + table + "." + Cols.IS_LOCALIZED + " DESC"
                + ", " + table + "." + Cols.ADDED + " ASC"
                + ", " + table + "." + Cols.NAME + " IS NULL ASC"
                + ", CASE WHEN " + table + "." + Cols.ICON + " IS NULL"
                + "        AND " + table + "." + Cols.ICON_URL + " IS NULL"
                + "        THEN 1 ELSE 0 END"
                + ", " + table + "." + Cols.SUMMARY + " IS NULL ASC"
                + ", " + table + "." + Cols.DESCRIPTION + " IS NULL ASC"
                + ", " + table + "." + Cols.WHATSNEW + " IS NULL ASC"
                + ", CASE WHEN " + table + "." + Cols.PHONE_SCREENSHOTS + " IS NULL"
                + "        AND " + table + "." + Cols.SEVEN_INCH_SCREENSHOTS + " IS NULL"
                + "        AND " + table + "." + Cols.TEN_INCH_SCREENSHOTS + " IS NULL"
                + "        AND " + table + "." + Cols.TV_SCREENSHOTS + " IS NULL"
                + "        AND " + table + "." + Cols.WEAR_SCREENSHOTS + " IS NULL"
                + "        AND " + table + "." + Cols.FEATURE_GRAPHIC + " IS NULL"
                + "        AND " + table + "." + Cols.PROMO_GRAPHIC + " IS NULL"
                + "        AND " + table + "." + Cols.TV_BANNER + " IS NULL"
                + "        THEN 1 ELSE 0 END"
                + ", " + table + "." + Cols.LAST_UPDATED + " DESC";
    }

    public static void putSavedSearchSettings(Context context, String key, String searchTerms) {
        if (savedSearchSettings == null) {
            savedSearchSettings = getSavedSearchSettings(context);
        }
        savedSearchSettings.edit().putString(key, searchTerms).apply();
    }

    public static void removeSavedSearchSettings(Context context, String key) {
        if (savedSearchSettings == null) {
            savedSearchSettings = getSavedSearchSettings(context);
        }
        savedSearchSettings.edit().remove(key).apply();
    }

    private static SharedPreferences getSavedSearchSettings(Context context) {
        return context.getSharedPreferences("saved-search-settings", Context.MODE_PRIVATE);
    }
}
