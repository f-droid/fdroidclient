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

import org.fdroid.database.AppListItem;
import org.fdroid.database.AppListSortOrder;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.fdroid.fdroid.views.main.MainActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Provides scrollable listing of apps for search and category views.
 */
public class AppListActivity extends AppCompatActivity implements CategoryTextWatcher.SearchTermsChangedListener {

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
    private FDroidDatabase db;
    private Utils.KeyboardStateMonitor keyboardStateMonitor;
    private LiveData<List<AppListItem>> itemsLiveData;

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

        db = DBHelper.getDb(this.getApplicationContext());
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
        sortImage.setImageResource(
                SortClause.WORDS.equals(sortClauseSelected) ? R.drawable.ic_sort : R.drawable.ic_last_updated
        );
        sortImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (sortClauseSelected) {
                    case SortClause.WORDS:
                        sortClauseSelected = SortClause.LAST_UPDATED;
                        sortImage.setImageResource(R.drawable.ic_last_updated);
                        break;
                    case SortClause.LAST_UPDATED:
                        sortClauseSelected = SortClause.WORDS;
                        sortImage.setImageResource(R.drawable.ic_sort);
                        break;
                }
                putSavedSearchSettings(getApplicationContext(), SORT_CLAUSE_KEY, sortClauseSelected);
                loadItems();
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
        loadItems();
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
    }

    private void loadItems() {
        if (itemsLiveData != null) {
            itemsLiveData.removeObserver(this::onAppsLoaded);
        }
        AppListSortOrder sortOrder =
                SortClause.WORDS.equals(sortClauseSelected) ? AppListSortOrder.NAME : AppListSortOrder.LAST_UPDATED;
        if (category == null) {
            itemsLiveData = db.getAppDao().getAppListItems(getPackageManager(), searchTerms,
                    sortOrder);
        } else {
            itemsLiveData = db.getAppDao().getAppListItems(getPackageManager(), category,
                    searchTerms, sortOrder);
        }
        itemsLiveData.observe(this, this::onAppsLoaded);
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

    private void onAppsLoaded(List<AppListItem> items) {
        setShowHiddenAppsNotice(false);
        appAdapter.setHasHiddenAppsCallback(() -> setShowHiddenAppsNotice(true));
        appAdapter.setItems(items);
        if (items.size() > 0) {
            emptyState.setVisibility(View.GONE);
            appView.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
            appView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSearchTermsChanged(@Nullable String category, @NonNull String searchTerms) {
        this.category = category;
        this.searchTerms = searchTerms;
        appView.scrollToPosition(0);
        loadItems();
        if (TextUtils.isEmpty(searchTerms)) {
            removeSavedSearchSettings(this, SEARCH_TERMS_KEY);
        } else {
            putSavedSearchSettings(this, SEARCH_TERMS_KEY, searchTerms);
        }
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
