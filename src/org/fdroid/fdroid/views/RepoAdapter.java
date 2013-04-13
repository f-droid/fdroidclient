package org.fdroid.fdroid.views;

import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.ManageRepo;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.SwitchCompat;

import java.util.List;

public class RepoAdapter extends BaseAdapter {

    private List<DB.Repo> repositories;
    private final ManageRepo activity;

    public RepoAdapter(ManageRepo activity) {
        this.activity = activity;
        refresh();
    }

    public void refresh() {
        try {
            DB db = DB.getDB();
            repositories = db.getRepos();
        } finally {
            DB.releaseDB();
        }
        notifyDataSetChanged();
    }

    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return repositories.size();
    }

    @Override
    public Object getItem(int position) {
        return repositories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    private static final int SWITCH_ID = 10000;

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        final DB.Repo repository = repositories.get(position);

        CompoundButton switchView;
        if (view == null) {
            view = activity.getLayoutInflater().inflate(R.layout.repo_item,null);
            switchView = addSwitchToView(view);
        } else {
            switchView = (CompoundButton)view.findViewById(SWITCH_ID);
        }

        switchView.setChecked(repository.inuse);
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            activity.setRepoEnabled(repository, isChecked);
            }
        });

        int unsignedVisibility = repository.isSigned() ? View.GONE : View.VISIBLE;
        view.findViewById(R.id.repo_not_signed).setVisibility
                (unsignedVisibility);

        TextView nameView = (TextView)view.findViewById(R.id.repo_name);
        nameView.setText(repository.getName());
        RelativeLayout.LayoutParams nameViewLayout =
                (RelativeLayout.LayoutParams)nameView.getLayoutParams();
        nameViewLayout.addRule(RelativeLayout.LEFT_OF, switchView.getId());

        return view;
    }

    private CompoundButton addSwitchToView(View parent) {
        SwitchCompat switchBuilder = SwitchCompat.create(activity);
        CompoundButton switchView = switchBuilder.createSwitch();
        switchView.setId(SWITCH_ID);
        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layout.addRule(RelativeLayout.CENTER_VERTICAL);
        switchView.setLayoutParams(layout);
        ((RelativeLayout)parent).addView(switchView);
        return switchView;
    }
}
