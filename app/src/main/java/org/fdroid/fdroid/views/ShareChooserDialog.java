package org.fdroid.fdroid.views;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.List;

public class ShareChooserDialog extends BottomSheetDialogFragment {
    private static final String ARG_WIDTH = "width";
    private static final String ARG_INTENT = "intent";
    private static final String ARG_SHOW_NEARBY = "showNearby";

    private static final int VIEWTYPE_SWAP = 1;
    private static final int VIEWTYPE_INTENT = 0;

    private RecyclerView recyclerView;
    private ArrayList<ResolveInfo> targets;
    private int parentWidth;
    private Intent shareIntent;
    private boolean showNearby;

    public interface ShareChooserDialogListener {

        void onNearby();

        void onResolvedShareIntent(Intent shareIntent);

    }

    private ShareChooserDialogListener listener;

    private void setListener(ShareChooserDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentWidth = getArguments().getInt(ARG_WIDTH, 640);
        shareIntent = getArguments().getParcelable(ARG_INTENT);
        showNearby = getArguments().getBoolean(ARG_SHOW_NEARBY, false);
        targets = new ArrayList<>();
        List<ResolveInfo> resInfo = getContext().getPackageManager().queryIntentActivities(shareIntent, 0);
        if (resInfo != null && resInfo.size() > 0) {
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (!packageName.equals(BuildConfig.APPLICATION_ID)) { // Remove ourselves
                    targets.add(resolveInfo);
                }
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        parentWidth - Utils.dpToPx(0, getContext()), // Set margins here!
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.share_chooser, container, false);
        setupView(v);
        return v;
    }

    private void setupView(View v) {
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_apps);

        // Figure out how many columns that fit in the given parent width. Give them 100dp.
        int appWidth = Utils.dpToPx(80, getContext());
        final int nCols = (parentWidth - /* padding */ Utils.dpToPx(8, getContext())) / appWidth;
        GridLayoutManager glm = new GridLayoutManager(getContext(), nCols);

        // Ensure that if available, the "Nearby Swap" item spans the entire width.
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (recyclerView.getAdapter() != null) {
                    if (recyclerView.getAdapter().getItemViewType(position) == VIEWTYPE_SWAP) {
                        return nCols;
                    }
                    return 1;
                }
                return 0;
            }
        });
        recyclerView.setLayoutManager(glm);

        class VH extends RecyclerView.ViewHolder {
            public final ImageView icon;
            public final TextView label;

            VH(View itemView) {
                super(itemView);
                icon = (ImageView) itemView.findViewById(R.id.ivShare);
                label = (TextView) itemView.findViewById(R.id.tvShare);
            }
        }

        recyclerView.setAdapter(new RecyclerView.Adapter<VH>() {

            private ArrayList<ResolveInfo> intents;

            RecyclerView.Adapter init(List<ResolveInfo> targetedShareIntents) {
                intents = new ArrayList<>();
                if (showNearby) {
                    intents.add(null);
                }
                for (ResolveInfo ri : targetedShareIntents) {
                    intents.add(ri);
                }
                return this;
            }

            @Override
            public int getItemViewType(int position) {
                if (intents.get(position) == null) {
                    return VIEWTYPE_SWAP;
                }
                return VIEWTYPE_INTENT;
            }

            @Override
            public VH onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate((viewType == 1)
                        ? R.layout.share_header_item
                        : R.layout.share_item, parent, false);
                return new VH(view);
            }

            @Override
            public void onBindViewHolder(VH holder, int position) {
                if (getItemViewType(position) == VIEWTYPE_SWAP) {
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) {
                                listener.onNearby();
                            }
                            dismiss();
                        }
                    });
                    return;
                }
                final ResolveInfo ri = intents.get(position);
                holder.icon.setImageDrawable(ri.loadIcon(getContext().getPackageManager()));
                holder.label.setText(ri.loadLabel(getContext().getPackageManager()));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) {
                            Intent intent = new Intent(shareIntent);
                            ComponentName name = new ComponentName(ri.activityInfo.applicationInfo.packageName,
                                    ri.activityInfo.name);
                            intent.setComponent(name);
                            listener.onResolvedShareIntent(intent);
                        }
                        dismiss();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return intents.size();
            }
        }.init(targets));

    }

    public static void createChooser(CoordinatorLayout rootView,
                                     ShareChooserDialog.ShareChooserDialogListener listener,
                                     final AppCompatActivity parent,
                                     final Intent shareIntent,
                                     boolean showNearbyItem) {
        ShareChooserDialog d = new ShareChooserDialog();
        d.setListener(listener);
        Bundle args = new Bundle();
        args.putInt(ARG_WIDTH, rootView.getWidth());
        args.putParcelable(ARG_INTENT, shareIntent);
        args.putBoolean(ARG_SHOW_NEARBY, showNearbyItem);
        d.setArguments(args);
        d.show(parent.getSupportFragmentManager(), "Share");
    }
}