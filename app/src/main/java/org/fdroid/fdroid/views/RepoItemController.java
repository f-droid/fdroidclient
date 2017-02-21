package org.fdroid.fdroid.views;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Repo;

/**
 * @see RepoAdapter
 */
public class RepoItemController extends RecyclerView.ViewHolder {

    @NonNull
    private final Activity activity;

    @NonNull
    private final RepoAdapter.EnabledListener enabledListener;

    @NonNull
    private final CompoundButton switchView;

    @NonNull
    private final TextView nameView;

    @NonNull
    private final View unsignedView;

    @NonNull
    private final View unverifiedView;

    @Nullable
    private Repo currentRepo;

    public RepoItemController(@NonNull Activity activity, View itemView, @NonNull RepoAdapter.EnabledListener listener) {
        super(itemView);
        this.activity = activity;
        enabledListener = listener;
        nameView = (TextView) itemView.findViewById(R.id.repo_name);
        switchView = (CompoundButton) itemView.findViewById(R.id.repo_switch);
        unsignedView = itemView.findViewById(R.id.repo_unsigned);
        unverifiedView = itemView.findViewById(R.id.repo_unverified);
        itemView.setOnClickListener(onRepoClicked);
    }

    public void bindRepo(@NonNull final Repo repo) {
        currentRepo = repo;

        // Remove old listener (because we are reusing this view, we don't want
        // to invoke the listener for the last repo to use it - particularly
        // because we are potentially about to change the checked status
        // which would in turn invoke this listener....
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(repo.inuse);

        // Add this listener *after* setting the checked status, so we don't
        // invoke the listener while setting up the view...
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enabledListener.onSetEnabled(repo, isChecked);
            }
        });

        nameView.setText(repo.getName());

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

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onRepoClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentRepo == null) {
                return;
            }

            Intent intent = new Intent(activity, RepoDetailsActivity.class);
            intent.putExtra(RepoDetailsActivity.ARG_REPO_ID, currentRepo.getId());
            activity.startActivityForResult(intent, RepoAdapter.SHOW_REPO_DETAILS);
        }
    };

}
