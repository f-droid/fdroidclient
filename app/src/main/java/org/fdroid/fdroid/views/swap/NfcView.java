package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapService;

public class NfcView extends RelativeLayout implements SwapWorkflowActivity.InnerView {

    public NfcView(Context context) {
        super(context);
    }

    public NfcView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NfcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public NfcView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        CheckBox dontShowAgain = (CheckBox) findViewById(R.id.checkbox_dont_show);
        dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.get().setShowNfcDuringSwap(!isChecked);
            }
        });
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.swap_skip, menu);
        MenuItem next = menu.findItem(R.id.action_next);
        MenuItemCompat.setShowAsAction(next,
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        next.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getActivity().showWifiQr();
                return true;
            }
        });
        return true;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_SHOW_NFC;
    }

    @Override
    public int getPreviousStep() {
        return SwapService.STEP_JOIN_WIFI;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_nfc_title);
    }
}
