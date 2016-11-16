package org.fdroid.fdroid.views;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
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
    private RecyclerView mRecyclerView;
    private ArrayList<ResolveInfo> mTargets;
    private int mParentWidth;
    private Intent mShareIntent;
    private boolean mShowNearby;

    public interface ShareChooserDialogListener {
        void onNearby();
        void onResolvedShareIntent(Intent shareIntent);
    }
    private ShareChooserDialogListener mListener;

    public ShareChooserDialog() {
        super();
    }

    private void setListener(ShareChooserDialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParentWidth = getArguments().getInt("width", 640);
        mShareIntent = getArguments().getParcelable("intent");
        mShowNearby = getArguments().getBoolean("showNearby", false);
        mTargets = new ArrayList<>();
        List<ResolveInfo> resInfo = getContext().getPackageManager().queryIntentActivities(mShareIntent, 0);
        if (resInfo != null && resInfo.size() > 0) {
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (!packageName.equals(BuildConfig.APPLICATION_ID)) // Remove ourselves
                {
                    mTargets.add(resolveInfo);
                }
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogINterface) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        mParentWidth - Utils.dpToPx(0, getContext()), // Set margins here!
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
        mRecyclerView = (RecyclerView)v.findViewById(R.id.recycler_view_apps);

        // Figure out how many columns that fit in the given parent width. Give them 100dp.
        int appWidth = Utils.dpToPx(80, getContext());
        final int nCols = (mParentWidth - /* padding */ Utils.dpToPx(8, getContext())) / appWidth;
        GridLayoutManager glm = new GridLayoutManager(getContext(), nCols);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mRecyclerView.getAdapter() != null) {
                    if (mRecyclerView.getAdapter().getItemViewType(position) == 1) {
                        return nCols;
                    }
                    return 1;
                }
                return 0;
            }
        });
        mRecyclerView.setLayoutManager(glm);


        class VH extends RecyclerView.ViewHolder {
            public final ImageView icon;
            public final TextView label;

            public VH(View itemView) {
                super(itemView);
                icon = (ImageView) itemView.findViewById(R.id.ivShare);
                label = (TextView) itemView.findViewById(R.id.tvShare);
            }
        }
        mRecyclerView.setAdapter(new RecyclerView.Adapter<VH>() {

            private ArrayList<Object> mIntents;

            RecyclerView.Adapter init(List<ResolveInfo> targetedShareIntents) {
                mIntents = new ArrayList<>();
               if (mShowNearby) {
                   mIntents.add("Nearby (string contents do not matter!)");
               }
                for (ResolveInfo ri : targetedShareIntents) {
                    mIntents.add(ri);
                }
                return this;
            }

            @Override
            public int getItemViewType(int position) {
                if (mIntents.get(position) instanceof String)
                    return 1;
                return 0;
            }

            @Override
            public VH onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate((viewType == 1) ? R.layout.share_header_item : R.layout.share_item, parent, false);
                return new VH(view);
            }

            @Override
            public void onBindViewHolder(VH holder, int position) {
                if (getItemViewType(position) == 1) {
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mListener != null) {
                                mListener.onNearby();
                            }
                            dismiss();
                        }
                    });
                    return;
                }
                final ResolveInfo ri = (ResolveInfo) mIntents.get(position);
                holder.icon.setImageDrawable(ri.loadIcon(getContext().getPackageManager()));
                holder.label.setText(ri.loadLabel(getContext().getPackageManager()));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            Intent intent = new Intent(mShareIntent);
                            ComponentName name = new ComponentName(ri.activityInfo.applicationInfo.packageName,
                                    ri.activityInfo.name);
                            intent.setComponent(name);
                            mListener.onResolvedShareIntent(intent);
                        }
                        dismiss();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return mIntents.size();
            }
        }.init(mTargets));

    }

    public static void createChooser(CoordinatorLayout rootView, ShareChooserDialog.ShareChooserDialogListener listener, final AppCompatActivity parent, final Intent shareIntent, boolean showNearbyItem) {
        ShareChooserDialog d = new ShareChooserDialog();
        d.setListener(listener);
        Bundle args = new Bundle();
        args.putInt("width", rootView.getWidth());
        args.putParcelable("intent", shareIntent);
        args.putBoolean("showNearby", showNearbyItem);
        d.setArguments(args);
        d.show(parent.getSupportFragmentManager(), "Share");
    }
}