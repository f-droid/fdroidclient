package org.fdroid.fdroid.views;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.database.Repository;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

import java.util.ArrayList;
import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {

    public interface RepoItemListener {
        void onClicked(Repository repo);

        void onSetEnabled(Repository repo, boolean isEnabled);
    }

    private final List<Repository> items = new ArrayList<>();
    private final RepoItemListener repoItemListener;

    RepoAdapter(RepoItemListener repoItemListener) {
        this.repoItemListener = repoItemListener;
    }

    @SuppressLint("NotifyDataSetChanged") // we could do better, but not really worth it at this point
    public void updateItems(List<Repository> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RepoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.repo_item, parent, false);
        return new RepoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RepoViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class RepoViewHolder extends RecyclerView.ViewHolder {
        private final View rootView;
        private final CompoundButton switchView;
        private final TextView nameView;
        private final View unsignedView;
        private final View unverifiedView;

        RepoViewHolder(@NonNull View view) {
            super(view);
            rootView = view;
            switchView = view.findViewById(R.id.repo_switch);
            nameView = view.findViewById(R.id.repo_name);
            unsignedView = view.findViewById(R.id.repo_unsigned);
            unverifiedView = view.findViewById(R.id.repo_unverified);
        }

        private void bind(Repository repo) {
            rootView.setOnClickListener(v -> repoItemListener.onClicked(repo));
            // Remove old listener (because we are reusing this view, we don't want
            // to invoke the listener for the last repo to use it - particularly
            // because we are potentially about to change the checked status
            // which would in turn invoke this listener....
            switchView.setOnCheckedChangeListener(null);
            switchView.setChecked(repo.getEnabled());

            // Add this listener *after* setting the checked status, so we don't
            // invoke the listener while setting up the view...
            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (repoItemListener != null) {
                    repoItemListener.onSetEnabled(repo, isChecked);
                }
            });
            nameView.setText(repo.getName(App.getLocales()));
            if (repo.getCertificate() != null) {
                unsignedView.setVisibility(View.GONE);
                unverifiedView.setVisibility(View.GONE);
            } else if (repo.getCertificate() == null) { // FIXME: Do we still need that unsignedView at all?
                unsignedView.setVisibility(View.GONE);
                unverifiedView.setVisibility(View.VISIBLE);
            } else {
                unsignedView.setVisibility(View.VISIBLE);
                unverifiedView.setVisibility(View.GONE);
            }
        }
    }
}
