package org.fdroid.fdroid.views.swap;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.views.fragments.ThemeableListFragment;

import java.util.List;

public class BluetoothDeviceListFragment extends ThemeableListFragment {

    private Adapter adapter = null;

    private class Adapter extends ArrayAdapter<BluetoothDevice> {

        public Adapter(Context context, int resource) {
            super(context, resource);
        }

        public Adapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        public Adapter(Context context, int resource, BluetoothDevice[] objects) {
            super(context, resource, objects);
        }

        public Adapter(Context context, int resource, int textViewResourceId, BluetoothDevice[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public Adapter(Context context, int resource, List<BluetoothDevice> objects) {
            super(context, resource, objects);
        }

        public Adapter(Context context, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent);
            } else {
                view = convertView;
            }

            BluetoothDevice device = getItem(position);
            TextView nameView = (TextView)view.findViewById(android.R.id.text1);
            TextView descriptionView = (TextView)view.findViewById(android.R.id.text2);

            nameView.setText(device.getName());
            descriptionView.setText(device.getAddress());

            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No bluetooth devices found. Is the other device \"discoverable\"?");

        Adapter adapter = new Adapter(
            new ContextThemeWrapper(getActivity(), R.style.SwapTheme_BluetoothDeviceList_ListItem),
            R.layout.select_local_apps_list_item
        );

        setListAdapter(adapter);
        setListShown(false); // start out with a progress indicator
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor c = (Cursor) l.getAdapter().getItem(position);
        String packageName = c.getString(c.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
        if (FDroidApp.selectedApps.contains(packageName)) {
            FDroidApp.selectedApps.remove(packageName);
        } else {
            FDroidApp.selectedApps.add(packageName);
        }
    }

    @Override
    protected int getThemeStyle() {
        return R.style.SwapTheme_StartSwap;
    }

    @Override
    protected int getHeaderLayout() {
        return R.layout.swap_bluetooth_header;
    }
}
