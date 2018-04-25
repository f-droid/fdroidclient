package org.fdroid.fdroid.views.apps;

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

    private RecyclerView appView;
    private AppListAdapter appAdapter;
    private String category;
    private String searchTerms;
    private String sortClauseSelected = SortClause.LAST_UPDATED;
    private TextView emptyState;
    private EditText searchInput;
    private ImageView sortImage;

    private interface SortClause {
        String NAME = Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.NAME + " asc";
        String LAST_UPDATED = Schema.AppMetadataTable.NAME + "."
                + Schema.AppMetadataTable.Cols.LAST_UPDATED + " desc";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_list);

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

        sortImage = (ImageView) findViewById(R.id.sort);
        if (FDroidApp.isAppThemeLight()) {
            sortImage.setImageResource(R.drawable.ic_last_updated_black);
        } else {
            sortImage.setImageResource(R.drawable.ic_last_updated_white);
        }
        sortImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sortClauseSelected.equalsIgnoreCase(SortClause.LAST_UPDATED)) {
                    sortClauseSelected = SortClause.NAME;
                    if (FDroidApp.isAppThemeLight()) {
                        sortImage.setImageResource(R.drawable.ic_az_black);
                    } else {
                        sortImage.setImageResource(R.drawable.ic_az_white);
                    }
                } else {
                    sortClauseSelected = SortClause.LAST_UPDATED;
                    if (FDroidApp.isAppThemeLight()) {
                        sortImage.setImageResource(R.drawable.ic_last_updated_black);
                    } else {
                        sortImage.setImageResource(R.drawable.ic_last_updated_white);
                    }
                }
                getSupportLoaderManager().restartLoader(0, null, AppListActivity.this);
                appView.scrollToPosition(0);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                AppProvider.getSearchUri(searchTerms, category),
                Schema.AppMetadataTable.Cols.ALL,
                null,
                null,
                sortClauseSelected
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
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
    public void onLoaderReset(Loader<Cursor> loader) {
        appAdapter.setAppCursor(null);
    }

    @Override
    public void onSearchTermsChanged(@Nullable String category, @NonNull String searchTerms) {
        this.category = category;
        this.searchTerms = searchTerms;
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}
