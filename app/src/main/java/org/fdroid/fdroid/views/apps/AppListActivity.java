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
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;

/**
 * Provides scrollable listing of apps for search and category views.
 */
public class AppListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        CategoryTextWatcher.SearchTermsChangedListener {

    public static final String EXTRA_CATEGORY
            = "org.fdroid.fdroid.views.apps.AppListActivity.EXTRA_CATEGORY";
    public static final String EXTRA_SEARCH_TERMS
            = "org.fdroid.fdroid.views.apps.AppListActivity.EXTRA_SEARCH_TERMS";

    private static final String LAST_UPDATED = Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.LAST_UPDATED + " desc";
    private static final String NAME_COL = Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.NAME;
    private static final String SUMMARY_COL = Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.SUMMARY;

    private RecyclerView appView;
    private AppListAdapter appAdapter;
    private String category;
    private String searchTerms;
    private TextView emptyState;
    private EditText searchInput;
    private Utils.KeyboardStateMonitor keyboardStateMonitor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_list);

        keyboardStateMonitor = new Utils.KeyboardStateMonitor(findViewById(R.id.app_list_root));

        searchInput = (EditText) findViewById(R.id.search);
        searchInput.addTextChangedListener(new CategoryTextWatcher(this, searchInput, this));
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Hide the keyboard (http://stackoverflow.com/a/1109108 (when pressing search)
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);

                    // Change focus from the search input to the app list.
                    appView.requestFocus();
                    return true;
                }
                return false;
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
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
        appView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private final ImageLoader imageLoader = ImageLoader.getInstance();

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        imageLoader.pause();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        imageLoader.resume();
                        break;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        parseIntentForSearchQuery();
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

    private String getSortOrder() {
        final String[] terms = searchTerms.trim().split("\\s+");
        if (terms.length == 0 || terms[0].equals("")) {
            return LAST_UPDATED;
        }

        StringBuilder titleCase = new StringBuilder(String.format("%s like '%%%s%%'", NAME_COL, terms[0]));
        StringBuilder summaryCase = new StringBuilder(String.format("%s like '%%%s%%'", SUMMARY_COL, terms[0]));
        for (int i = 1; i < terms.length; i++) {
            titleCase.append(String.format(" and %s like '%%%s%%'", NAME_COL, terms[i]));
            summaryCase.append(String.format(" and %s like '%%%s%%'", SUMMARY_COL, terms[i]));
        }

        return String.format("case when %s then 1 when %s then 2 else 3 end, %s",
                titleCase.toString(), summaryCase.toString(), LAST_UPDATED);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                AppProvider.getSearchUri(searchTerms, category),
                Schema.AppMetadataTable.Cols.ALL,
                null,
                null,
                getSortOrder()
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
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
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}
