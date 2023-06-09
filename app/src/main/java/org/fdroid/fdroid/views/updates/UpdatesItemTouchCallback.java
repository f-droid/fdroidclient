package org.fdroid.fdroid.views.updates;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.fdroid.views.apps.AppListItemController;
import org.fdroid.fdroid.views.updates.items.AppStatusListItemController;
import org.fdroid.fdroid.views.updates.items.KnownVulnAppListItemController;
import org.fdroid.fdroid.views.updates.items.UpdateableAppListItemController;

/**
 * Certain views within the {@link UpdatesAdapter} can be swiped to dismiss. Depending on which item is swiped, there
 * is a different behaviour, but all of it revolves around dismissing the item.
 *
 * <ul>
 *   <li>
 *     {@link KnownVulnAppListItemController}: Will be marked as "Ignored" and won't warn the user in the future.
 *   </li>
 *   <li>
 *     {@link UpdateableAppListItemController}: Will get marked as "Ignore this update".
 *   </li>
 *   <li>
 *     {@link AppStatusListItemController}:
 *     <ul>
 *         <li>If downloading or queued to download, cancel the download.</li>
 *         <li>If downloaded waiting to install, forget that we downloaded it.</li>
 *         <li>If installed ready to run, stop prompting the user to run the app.</li>
 *     </ul>
 *   <li>
 * </ul>
 */
public class UpdatesItemTouchCallback extends ItemTouchHelper.Callback {

    private final UpdatesAdapter adapter;

    UpdatesItemTouchCallback(UpdatesAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = 0;
        if (viewHolder instanceof AppListItemController) {
            AppListItemController controller = (AppListItemController) viewHolder;
            if (controller.canDismiss()) {
                swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            }
        }
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        AppListItemController controller = (AppListItemController) viewHolder;
        controller.onDismiss(adapter);
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

}
