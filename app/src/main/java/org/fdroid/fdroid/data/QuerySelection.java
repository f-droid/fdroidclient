package org.fdroid.fdroid.data;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class used by sublasses of ContentProvider to make the constraints
 * required for a given content URI (e.g. all apps that belong to a repo)
 * easily appendable to the constraints which are passed into, e.g. the query()
 * method in the content provider.
 */
public class QuerySelection {

    private final String[] args;
    private final String selection;

    public QuerySelection(String selection) {
        this.selection = selection;
        this.args = new String[] {};
    }

    public QuerySelection(String selection, String[] args) {
        this.args = args;
        this.selection = selection;
    }

    public QuerySelection(String selection, List<String> args) {
        this.args = new String[ args.size() ];
        args.toArray(this.args);
        this.selection = selection;
    }

    public String[] getArgs() {
        return args;
    }

    public String getSelection() {
        return selection;
    }

    private boolean hasSelection() {
        return !TextUtils.isEmpty(selection);
    }

    private boolean hasArgs() {
        return args != null && args.length > 0;
    }

    public QuerySelection add(String selection, String[] args) {
        return add(new QuerySelection(selection, args));
    }

    public QuerySelection add(QuerySelection query) {
        String s = null;
        if (this.hasSelection() && query.hasSelection()) {
            s = " (" + this.selection + ") AND (" + query.getSelection() + ") ";
        } else if (this.hasSelection()) {
            s = this.selection;
        } else if (query.hasSelection()) {
            s = query.selection;
        }

        int thisNumArgs  = this.hasArgs() ? this.args.length : 0;
        int queryNumArgs = query.hasArgs() ? query.args.length : 0;
        List<String> a = new ArrayList<>(thisNumArgs + queryNumArgs);

        if (this.hasArgs()) {
            Collections.addAll(a, this.args);
        }

        if (query.hasArgs()) {
            Collections.addAll(a, query.getArgs());
        }

        return new QuerySelection(s, a);
    }

}
