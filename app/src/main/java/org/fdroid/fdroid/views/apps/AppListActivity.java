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
import android.view.View;
import android.widget.EditText;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;

public class AppListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, CategoryTextWatcher.SearchTermsChangedListener {

    public static final String EXTRA_CATEGORY = "org.fdroid.fdroid.views.apps.AppListActivity.EXTRA_CATEGORY";
    private RecyclerView appView;
    private AppListAdapter appAdapter;
    private String category;
    private String searchTerms;
    private EditText searchInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_list);

        searchInput = (EditText) findViewById(R.id.search);
        searchInput.addTextChangedListener(new CategoryTextWatcher(this, searchInput, this));

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        category = intent.hasExtra(EXTRA_CATEGORY) ? intent.getStringExtra(EXTRA_CATEGORY) : null;

        searchInput.setText(getSearchText(category, null));
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
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        appAdapter.setAppCursor(cursor);
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
