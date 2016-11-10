package org.fdroid.fdroid;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.text.AllCapsTransformationMethod;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.LinearLayoutManagerSnapHelper;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class AppDetails2 extends AppCompatActivity {

    private static final String TAG = "AppDetails2";

    private App mApp;
    private RecyclerView mRecyclerView;
    private AppDetailsRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_details2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(""); // Nice and clean toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        App app = null;
        String packageName = getPackageNameFromIntent(getIntent());
        if (!TextUtils.isEmpty(packageName)) {
            app = AppProvider.Helper.findHighestPriorityMetadata(getContentResolver(), packageName);
        }
        setApp(app); // Will call finish if empty or unknown

        mRecyclerView = (RecyclerView) findViewById(R.id.rvDetails);
        mAdapter = new AppDetailsRecyclerViewAdapter(this);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        lm.setStackFromEnd(false);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);
    }

    private String getPackageNameFromIntent(Intent intent) {
        if (!intent.hasExtra(AppDetails.EXTRA_APPID)) {
            Log.e(TAG, "No package name found in the intent!");
            return null;
        }
        return intent.getStringExtra(AppDetails.EXTRA_APPID);
    }

    /**
     * If passed null, this will show a message to the user ("Could not find app ..." or something
     * like that) and then finish the activity.
     */
    private void setApp(App newApp) {
        if (newApp == null) {
            Toast.makeText(this, R.string.no_such_app, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mApp = newApp;
    }

    public class AppDetailsRecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final int VIEWTYPE_HEADER = 0;
        private final int VIEWTYPE_SCREENSHOTS = 1;
        private final int VIEWTYPE_WHATS_NEW = 2;

        private final Context mContext;
        private ArrayList<Integer> mItems;

        public AppDetailsRecyclerViewAdapter(Context context) {
            mContext = context;
            updateItems();
        }

        private void updateItems() {
            if (mItems == null)
                mItems = new ArrayList<>();
            else
                mItems.clear();
            mItems.add(Integer.valueOf(VIEWTYPE_HEADER));
            mItems.add(Integer.valueOf(VIEWTYPE_SCREENSHOTS));
            mItems.add(Integer.valueOf(VIEWTYPE_WHATS_NEW));
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEWTYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_header, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == VIEWTYPE_SCREENSHOTS) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_screenshots, parent, false);
                return new ScreenShotsViewHolder(view);
            } else if (viewType == VIEWTYPE_WHATS_NEW) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_whatsnew, parent, false);
                return new WhatsNewViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int viewType = mItems.get(position);
            if (viewType == VIEWTYPE_HEADER) {
                final HeaderViewHolder vh = (HeaderViewHolder) holder;
                ImageLoader.getInstance().displayImage(mApp.iconUrlLarge, vh.iconView, vh.displayImageOptions);
                vh.titleView.setText(mApp.name);
                if (!TextUtils.isEmpty(mApp.author)) {
                    vh.authorView.setText(getString(R.string.by_author) + " " + mApp.author);
                    vh.authorView.setVisibility(View.VISIBLE);
                } else {
                    vh.authorView.setVisibility(View.GONE);
                }
                vh.summaryView.setText(mApp.summary);
                final Spanned desc = Html.fromHtml(mApp.description, null, new Utils.HtmlTagHandler());
                vh.descriptionView.setMovementMethod(AppDetails2.SafeLinkMovementMethod.getInstance(mContext));
                vh.descriptionView.setText(trimNewlines(desc));
                vh.descriptionView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (vh.descriptionView.getLineCount() < HeaderViewHolder.MAX_LINES) {
                            vh.descriptionMoreView.setVisibility(View.GONE);
                        } else {
                            vh.descriptionMoreView.setVisibility(View.VISIBLE);
                        }
                    }
                });
                vh.buttonSecondaryView.setText(R.string.menu_uninstall);
                vh.buttonSecondaryView.setVisibility(mApp.isInstalled() ? View.VISIBLE : View.INVISIBLE);
                vh.buttonPrimaryView.setText(R.string.menu_install);
                vh.buttonPrimaryView.setVisibility(View.VISIBLE);

/*                if (appDetails.activeDownloadUrlString != null) {
                    btMain.setText(R.string.downloading);
                    btMain.setEnabled(false);
                } else if (!app.isInstalled() && app.suggestedVersionCode > 0 &&
                        appDetails.adapter.getCount() > 0) {
                    // Check count > 0 due to incompatible apps resulting in an empty list.
                    // If App isn't installed
                    installed = false;
                    statusView.setText(R.string.details_notinstalled);
                    NfcHelper.disableAndroidBeam(appDetails);
                    // Set Install button and hide second button
                    btMain.setText(R.string.menu_install);
                    btMain.setOnClickListener(mOnClickListener);
                    btMain.setEnabled(true);
                } else if (app.isInstalled()) {
                    // If App is installed
                    installed = true;
                    statusView.setText(getString(R.string.details_installed, app.installedVersionName));
                    NfcHelper.setAndroidBeam(appDetails, app.packageName);
                    if (app.canAndWantToUpdate(appDetails)) {
                        updateWanted = true;
                        btMain.setText(R.string.menu_upgrade);
                    } else {
                        updateWanted = false;
                        if (appDetails.packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                            btMain.setText(R.string.menu_launch);
                        } else {
                            btMain.setText(R.string.menu_uninstall);
                        }
                    }
                    btMain.setOnClickListener(mOnClickListener);
                    btMain.setEnabled(true);
                }

                TextView currentVersion = (TextView) view.findViewById(R.id.current_version);
                if (!appDetails.getApks().isEmpty()) {
                    currentVersion.setText(appDetails.getApks().getItem(0).versionName + " (" + app.license + ")");
                } else {
                    currentVersion.setVisibility(View.GONE);
                    btMain.setVisibility(View.GONE);
                }*/

            } else if (viewType == VIEWTYPE_SCREENSHOTS) {
                ScreenShotsViewHolder vh = (ScreenShotsViewHolder) holder;
                LinearLayoutManager lm = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
                vh.recyclerView.setLayoutManager(lm);
                ScreenShotsRecyclerViewAdapter adapter = new ScreenShotsRecyclerViewAdapter(mApp);
                vh.recyclerView.setAdapter(adapter);
                vh.recyclerView.setHasFixedSize(true);
                vh.recyclerView.setNestedScrollingEnabled(false);
                LinearLayoutManagerSnapHelper helper = new LinearLayoutManagerSnapHelper(lm);
                helper.setLinearSnapHelperListener(adapter);
                helper.attachToRecyclerView(vh.recyclerView);
            } else if (viewType == VIEWTYPE_WHATS_NEW) {
                WhatsNewViewHolder vh = (WhatsNewViewHolder) holder;
                vh.textView.setText("WHATS NEW GOES HERE");
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position);
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            private static final int MAX_LINES = 5;

            final ImageView iconView;
            final TextView titleView;
            final TextView authorView;
            final TextView summaryView;
            final TextView descriptionView;
            final TextView descriptionMoreView;
            final Button buttonPrimaryView;
            final Button buttonSecondaryView;
            final DisplayImageOptions displayImageOptions;

            HeaderViewHolder(View view) {
                super(view);
                iconView = (ImageView) view.findViewById(R.id.icon);
                titleView = (TextView) view.findViewById(R.id.title);
                authorView = (TextView) view.findViewById(R.id.author);
                summaryView = (TextView) view.findViewById(R.id.summary);
                descriptionView = (TextView) view.findViewById(R.id.description);
                descriptionMoreView = (TextView) view.findViewById(R.id.description_more);
                buttonPrimaryView = (Button) view.findViewById(R.id.primaryButtonView);
                buttonSecondaryView = (Button) view.findViewById(R.id.secondaryButtonView);
                displayImageOptions = new DisplayImageOptions.Builder()
                        .cacheInMemory(true)
                        .cacheOnDisk(true)
                        .imageScaleType(ImageScaleType.NONE)
                        .showImageOnLoading(R.drawable.ic_repo_app_default)
                        .showImageForEmptyUri(R.drawable.ic_repo_app_default)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .build();
                descriptionView.setMaxLines(MAX_LINES);
                descriptionView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                descriptionMoreView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Remember current scroll position so that we can restore it
                        LinearLayoutManager lm = (LinearLayoutManager)mRecyclerView.getLayoutManager();
                        int pos = lm.findFirstVisibleItemPosition();
                        int posOffset = 0;
                        if (pos != NO_POSITION) {
                            View view = lm.findViewByPosition(pos);
                            if (view != null)
                                posOffset = lm.getDecoratedTop(view);
                        }
                        if (TextViewCompat.getMaxLines(descriptionView) != MAX_LINES) {
                            descriptionView.setMaxLines(MAX_LINES);
                            descriptionMoreView.setText(R.string.more);
                        } else {
                            descriptionView.setMaxLines(Integer.MAX_VALUE);
                            descriptionMoreView.setText(R.string.less);
                        }
                        if (pos != NO_POSITION) {
                            // Restore scroll position
                            lm.scrollToPositionWithOffset(pos, posOffset);
                        }
                    }
                });
                // Set ALL caps (in a way compatible with SDK 10)
                AllCapsTransformationMethod allCapsTransformation = new AllCapsTransformationMethod(view.getContext());
                buttonPrimaryView.setTransformationMethod(allCapsTransformation);
                buttonSecondaryView.setTransformationMethod(allCapsTransformation);
                descriptionMoreView.setTransformationMethod(allCapsTransformation);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + titleView.getText() + "'";
            }
        }

        public class ScreenShotsViewHolder extends RecyclerView.ViewHolder {
            final RecyclerView recyclerView;

            ScreenShotsViewHolder(View view) {
                super(view);
                recyclerView = (RecyclerView) view.findViewById(R.id.screenshots);
            }

            @Override
            public String toString() {
                return super.toString() + " screenshots";
            }
        }

        public class WhatsNewViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            WhatsNewViewHolder(View view) {
                super(view);
                textView = (TextView) view.findViewById(R.id.text);
            }

            @Override
            public String toString() {
                return super.toString() + " " + textView.getText();
            }
        }

        class ScreenShotsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements LinearLayoutManagerSnapHelper.LinearSnapHelperListener {
            private final App app;
            private final DisplayImageOptions displayImageOptions;
            private View selectedView;
            private int selectedPosition;
            private int selectedItemElevation;
            private int unselectedItemMargin;

            public ScreenShotsRecyclerViewAdapter(App app) {
                super();
                this.app = app;
                selectedPosition = 0;
                selectedItemElevation = getResources().getDimensionPixelSize(R.dimen.details_screenshot_selected_elevation);
                unselectedItemMargin = getResources().getDimensionPixelSize(R.dimen.details_screenshot_margin);
                displayImageOptions = new DisplayImageOptions.Builder()
                        .cacheInMemory(true)
                        .cacheOnDisk(true)
                        .imageScaleType(ImageScaleType.NONE)
                        .showImageOnLoading(R.drawable.ic_repo_app_default)
                        .showImageForEmptyUri(R.drawable.ic_repo_app_default)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .build();
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ScreenShotViewHolder vh = (ScreenShotViewHolder) holder;
                setViewSelected(vh.itemView, position == selectedPosition);
                if (position == selectedPosition)
                    this.selectedView = vh.itemView;
                ImageLoader.getInstance().displayImage(mApp.iconUrlLarge, vh.image, displayImageOptions);
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_screenshot_item, parent, false);
                return new ScreenShotViewHolder(view);
            }

            @Override
            public int getItemCount() {
                return 7;
            }

            @Override
            public void onSnappedToView(View view, int snappedPosition) {
                setViewSelected(selectedView, false);
                selectedView = view;
                selectedPosition = snappedPosition;
                setViewSelected(selectedView, true);
            }

            private void setViewSelected(View view, boolean selected) {
                if (view != null) {
                    RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)view.getLayoutParams();
                    if (selected)
                        lp.setMargins(0,selectedItemElevation,0,selectedItemElevation);
                    else
                        lp.setMargins(0,unselectedItemMargin,0,unselectedItemMargin);
                    ViewCompat.setElevation(view, selected ? selectedItemElevation : 0);
                    view.setLayoutParams(lp);
                }
            }

            public class ScreenShotViewHolder extends RecyclerView.ViewHolder {
                final ImageView image;

                ScreenShotViewHolder(View view) {
                    super(view);
                    image = (ImageView) view.findViewById(R.id.image);
                }

                @Override
                public String toString() {
                    return super.toString() + " screenshot";
                }
            }
        }
    }

    // The HTML formatter adds "\n\n" at the end of every paragraph. This
    // is desired between paragraphs, but not at the end of the whole
    // string as it adds unwanted spacing at the end of the TextView.
    // Remove all trailing newlines.
    // Use this function instead of a trim() as that would require
    // converting to String and thus losing formatting (e.g. bold).
    private static CharSequence trimNewlines(CharSequence s) {
        if (s == null || s.length() < 1) {
            return s;
        }
        int i;
        for (i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) != '\n') {
                break;
            }
        }
        if (i == s.length() - 1) {
            return s;
        }
        return s.subSequence(0, i + 1);
    }

    private static final class SafeLinkMovementMethod extends LinkMovementMethod {

        private static AppDetails2.SafeLinkMovementMethod instance;

        private final Context ctx;

        private SafeLinkMovementMethod(Context ctx) {
            this.ctx = ctx;
        }

        public static AppDetails2.SafeLinkMovementMethod getInstance(Context ctx) {
            if (instance == null) {
                instance = new AppDetails2.SafeLinkMovementMethod(ctx);
            }
            return instance;
        }

        private static CharSequence getLink(TextView widget, Spannable buffer,
                                            MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();
            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            final int off = layout.getOffsetForHorizontal(line, x);
            final ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);

            if (links.length > 0) {
                final ClickableSpan link = links[0];
                final Spanned s = (Spanned) widget.getText();
                return s.subSequence(s.getSpanStart(link), s.getSpanEnd(link));
            }
            return "null";
        }

        @Override
        public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer,
                                    @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (ActivityNotFoundException ex) {
                Selection.removeSelection(buffer);
                final CharSequence link = getLink(widget, buffer, event);
                Toast.makeText(ctx,
                        ctx.getString(R.string.no_handler_app, link),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        }

    }
}
