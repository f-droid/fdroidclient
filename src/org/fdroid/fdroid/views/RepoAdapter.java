package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.SwitchCompat;
import org.fdroid.fdroid.compat.LayoutCompat;
import org.fdroid.fdroid.data.Repo;

public class RepoAdapter extends CursorAdapter {

    public interface EnabledListener {
        public void onSetEnabled(Repo repo, boolean isEnabled);
    }

    private static final int SWITCH_ID = 10000;

    private final LayoutInflater inflater;

    private EnabledListener enabledListener;

    public RepoAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        inflater = LayoutInflater.from(context);
    }

    public RepoAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        inflater = LayoutInflater.from(context);
    }

    public RepoAdapter(Context context, Cursor c) {
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
        View view = inflater.inflate(R.layout.repo_item, null);
        CompoundButton switchView = addSwitchToView(view, context);
        setupView(cursor, view, switchView);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        CompoundButton switchView = (CompoundButton)view.findViewById(SWITCH_ID);

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

        TextView nameView = (TextView)view.findViewById(R.id.repo_name);
        nameView.setText(repo.getName());
        RelativeLayout.LayoutParams nameViewLayout =
                (RelativeLayout.LayoutParams)nameView.getLayoutParams();
        nameViewLayout.addRule(LayoutCompat.RelativeLayout.START_OF, switchView.getId());

        // If we set the signed view to GONE instead of INVISIBLE, then the
        // height of each list item varies.
        View signedView = view.findViewById(R.id.repo_unsigned);
        if (repo.isSigned()) {
            signedView.setVisibility(View.INVISIBLE);
        } else {
            signedView.setVisibility(View.VISIBLE);
        }
    }

    private CompoundButton addSwitchToView(View parent, Context context) {
        SwitchCompat switchBuilder = SwitchCompat.create(context);
        CompoundButton switchView = switchBuilder.createSwitch();
        switchView.setId(SWITCH_ID);
        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        layout.addRule(LayoutCompat.RelativeLayout.ALIGN_PARENT_END);
        layout.addRule(RelativeLayout.CENTER_VERTICAL);
        switchView.setLayoutParams(layout);
        ((RelativeLayout)parent).addView(switchView);
        return switchView;
    }
}
