package org.fdroid.fdroid.views.categories;

import org.fdroid.database.Category;

public class CategoryItem {

    public final Category category;
    final int numApps;

    public CategoryItem(Category category, int numApps) {
        this.category = category;
        this.numApps = numApps;
    }
}
