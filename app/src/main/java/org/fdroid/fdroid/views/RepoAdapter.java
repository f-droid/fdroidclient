package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Repo;

public class RepoAdapter extends CursorAdapter {

    public interface EnabledListener {
        void onSetEnabled(Repo repo, boolean isEnabled);
    }

    private final LayoutInflater inflater;

    private EnabledListener enabledListener;

    public static RepoAdapter create(Context context, Cursor cursor, int flags) {
        return new RepoAdapter(context, cursor, flags);
    }

    private RepoAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        inflater = LayoutInflater.from(context);
    }

    public RepoAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        inflater = LayoutInflater.from(context);
    }

    @SuppressWarnings("deprecation")
    private RepoAdapter(Context context, Cursor c) {
        super(context, c);
        inflater = LayoutInflater.from(context);
    }

    public void setEnabledListener(EnabledListener listener) {
        enabledListener = listener;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(R.layout.repo_item, parent, false);
        setupView(cursor, view, (CompoundButton) view.findViewById(R.id.repo_switch));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        CompoundButton switchView = (CompoundButton) view.findViewById(R.id.repo_switch);

        // Remove old listener (because we are reusing this view, we don't want
        // to invoke the listener for the last repo to use it - particularly
        // because we are potentially about to change the checked status
        // which would in turn invoke this listener....
        switchView.setOnCheckedChangeListener(null);
        setupView(cursor, view, switchView);
    }

    private void setupView(Cursor cursor, View view, CompoundButton switchView) {
        final Repo repo = new Repo(cursor);

        switchView.setChecked(repo.inuse);

        // Add this listener *after* setting the checked status, so we don't
        // invoke the listener while setting up the view...
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enabledListener != null) {
                    enabledListener.onSetEnabled(repo, isChecked);
                }
            }
        });

        TextView nameView = (TextView) view.findViewById(R.id.repo_name);
        nameView.setText(repo.getName());

        View unsignedView = view.findViewById(R.id.repo_unsigned);
        View unverifiedView = view.findViewById(R.id.repo_unverified);
        if (repo.isSigned()) {
            unsignedView.setVisibility(View.GONE);
            unverifiedView.setVisibility(View.GONE);
        } else if (repo.isSignedButUnverified()) {
            unsignedView.setVisibility(View.GONE);
            unverifiedView.setVisibility(View.VISIBLE);
        } else {
            unsignedView.setVisibility(View.VISIBLE);
            unverifiedView.setVisibility(View.GONE);
        }
    }
}
