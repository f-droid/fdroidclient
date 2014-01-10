package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.fdroid.fdroid.*;

import java.util.List;

public class RepoDetailsFragment extends Fragment {

    public static final String ARG_REPO_ID = "repo_id";

    /**
     * If the repo has been updated at least once, then we will show
     * all of this info, otherwise they will be hidden.
     */
    private static final int[] SHOW_IF_EXISTS = {
        R.id.label_repo_name,
        R.id.text_repo_name,
        R.id.label_description,
        R.id.text_description,
        R.id.label_num_apps,
        R.id.text_num_apps,
        R.id.label_last_update,
        R.id.text_last_update,
        R.id.label_signature,
        R.id.text_signature,
        R.id.text_signature_description
    };

    /**
     * If the repo has <em>not</em> been updated yet, then we only show
     * these, otherwise they are hidden.
     */
    private static final int[] HIDE_IF_EXISTS = {
        R.id.text_not_yet_updated,
        R.id.btn_update
    };

    private static final int DELETE = 0;
    private static final int UPDATE = 1;

    public void setRepoChangeListener(OnRepoChangeListener listener) {
        repoChangeListener = listener;
    }

    private OnRepoChangeListener repoChangeListener;

    public static interface OnRepoChangeListener {

        /**
         * This fragment is responsible for getting confirmation from the
         * user, so you should presume that the user has already consented
         * and confirmed to the deletion.
         */
        public void onDeleteRepo(DB.Repo repo);

        public void onRepoDetailsChanged(DB.Repo repo);

        public void onEnableRepo(DB.Repo repo);

        public void onDisableRepo(DB.Repo repo);

        public void onUpdatePerformed(DB.Repo repo);

    }

    // TODO: Currently initialised in onCreateView. Not sure if that is the
    // best way to go about this...
    private DB.Repo repo;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    /**
     * After, for example, a repo update, the details will have changed in the
     * database. However, or local reference to the DB.Repo object will not
     * have been updated. The safest way to deal with this is to reload the
     * repo object directly from the database.
     */
    private void reloadRepoDetails() {
        try {
            DB db = DB.getDB();
            repo = db.getRepo(repo.id);
        } finally {
            DB.releaseDB();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int repoId = getArguments().getInt(ARG_REPO_ID);
        DB db = DB.getDB();
        repo = db.getRepo(repoId);
        DB.releaseDB();

        if (repo == null) {
            Log.e("FDroid", "Error showing details for repo '" + repoId + "'");
            return new LinearLayout(container.getContext());
        }

        ViewGroup repoView = (ViewGroup)inflater.inflate(R.layout.repodetails, null);
        updateView(repoView);

        // Setup listeners here, rather than in updateView(...),
        // because otherwise we will end up adding multiple listeners with
        // subsequent calls to updateView().
        EditText inputUrl = (EditText)repoView.findViewById(R.id.input_repo_url);
        inputUrl.addTextChangedListener(new UrlWatcher());

        Button update = (Button)repoView.findViewById(R.id.btn_update);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performUpdate();
            }
        });

        return repoView;
    }

    /**
     * Populates relevant views with properties from the current repository.
     * Decides which views to show and hide depending on the state of the
     * repository.
     */
    private void updateView(ViewGroup repoView) {

        EditText inputUrl = (EditText)repoView.findViewById(R.id.input_repo_url);
        inputUrl.setText(repo.address);

        if (repo.hasBeenUpdated()) {
            updateViewForExistingRepo(repoView);
        } else {
            updateViewForNewRepo(repoView);
        }

    }

    /**
     * Help function to make switching between two view states easier.
     * Perhaps there is a better way to do this. I recall that using Adobe
     * Flex, there was a thing called "ViewStates" for exactly this. Wonder if
     * that exists in  Android?
     */
    private static void setMultipleViewVisibility(ViewGroup parent,
                                                  int[] viewIds,
                                                  int visibility) {
        for (int viewId : viewIds) {
            parent.findViewById(viewId).setVisibility(visibility);
        }
    }

    private void updateViewForNewRepo(ViewGroup repoView) {
        setMultipleViewVisibility(repoView, HIDE_IF_EXISTS, View.VISIBLE);
        setMultipleViewVisibility(repoView, SHOW_IF_EXISTS, View.GONE);
    }

    private void updateViewForExistingRepo(ViewGroup repoView) {
        setMultipleViewVisibility(repoView, SHOW_IF_EXISTS, View.VISIBLE);
        setMultipleViewVisibility(repoView, HIDE_IF_EXISTS, View.GONE);

        TextView name          = (TextView)repoView.findViewById(R.id.text_repo_name);
        TextView numApps       = (TextView)repoView.findViewById(R.id.text_num_apps);
        TextView lastUpdated   = (TextView)repoView.findViewById(R.id.text_last_update);

        name.setText(repo.getName());
        numApps.setText(Integer.toString(repo.getNumberOfApps()));

        setupDescription(repoView, repo);
        setupSignature(repoView, repo);

        // Repos that existed before this feature was supported will have an
        // "Unknown" last update until next time they update...
        String lastUpdate = repo.lastUpdated != null
                ? repo.lastUpdated.toString() : getString(R.string.unknown);
        lastUpdated.setText(lastUpdate);
    }

    private void setupDescription(ViewGroup parent, DB.Repo repo) {

        TextView descriptionLabel = (TextView)parent.findViewById(R.id.label_description);
        TextView description      = (TextView)parent.findViewById(R.id.text_description);

        if (repo.description == null || repo.description.length() == 0) {
            descriptionLabel.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
        } else {
            descriptionLabel.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
        }

        String descriptionText = repo.description == null
                ? "" : repo.description.replaceAll("\n", " ");
        description.setText(descriptionText);

    }

    /**
     * When an update is performed, notify the listener so that the repo
     * list can be updated. We will perform the update ourselves though.
     */
    private void performUpdate() {
        repo.enable((FDroidApp)getActivity().getApplication());
        UpdateService.updateNow(getActivity()).setListener(new ProgressListener() {
            @Override
            public void onProgress(Event event) {
                if (event.type == UpdateService.STATUS_COMPLETE_AND_SAME ||
                        event.type == UpdateService.STATUS_COMPLETE_WITH_CHANGES) {
                    reloadRepoDetails();
                    updateView((ViewGroup)getView());
                }
            }
        });
        if (repoChangeListener != null) {
            repoChangeListener.onUpdatePerformed(repo);
        }
    }

    /**
     * When the URL is changed, notify the repoChangeListener.
     */
    class UrlWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!repo.address.equals(s.toString())) {
                repo.address = s.toString();
                try {
                    DB db = DB.getDB();
                    db.updateRepo(repo);
                } finally {
                    DB.releaseDB();
                }
                if (repoChangeListener != null) {
                    repoChangeListener.onRepoDetailsChanged(repo);
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        MenuItem update = menu.add(Menu.NONE, UPDATE, 0, R.string.repo_update);
        update.setIcon(R.drawable.ic_menu_refresh);
        MenuItemCompat.setShowAsAction(update,
            MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
            MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT );

        MenuItem delete = menu.add(Menu.NONE, DELETE, 0, R.string.delete);
        delete.setIcon(android.R.drawable.ic_menu_delete);
        MenuItemCompat.setShowAsAction(delete,
            MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
            MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == DELETE) {
            promptForDelete();
            return true;
        } else if (item.getItemId() == UPDATE) {
            performUpdate();
            return true;
        }

        return false;
    }

    private void promptForDelete() {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.repo_confirm_delete_title)
            .setIcon(android.R.drawable.ic_menu_delete)
            .setMessage(R.string.repo_confirm_delete_body)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (repoChangeListener != null) {
                    DB.Repo repo = RepoDetailsFragment.this.repo;
                    repoChangeListener.onDeleteRepo(repo);
                }
            }
        }).setNegativeButton(android.R.string.cancel,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing...
                }
            }
        ).show();
    }

    private void setupSignature(ViewGroup parent, DB.Repo repo) {
        TextView signatureView     = (TextView)parent.findViewById(R.id.text_signature);
        TextView signatureDescView = (TextView)parent.findViewById(R.id.text_signature_description);

        String signature;
        int signatureColour;

        if (repo.pubkey != null && repo.pubkey.length() > 0) {
            signature       = Utils.formatFingerprint(repo.pubkey);
            signatureColour = getResources().getColor(R.color.signed);
            signatureDescView.setVisibility(View.GONE);
        } else {
            signature       = getResources().getString(R.string.unsigned);
            signatureColour = getResources().getColor(R.color.unsigned);
            signatureDescView.setVisibility(View.VISIBLE);
            signatureDescView.setText(getResources().getString(R.string.unsigned_description));
        }

        signatureView.setText(signature);
        signatureView.setTextColor(signatureColour);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}
