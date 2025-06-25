package org.fdroid.fdroid.views.apps;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.TtsSpan;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.fdroid.fdroid.R;

/**
 * The search input treats text before the first colon as a category name. Text after this colon
 * (or all text if there is no colon) is the free text search terms.
 * The behaviour of this search input is:
 * * Replacing anything before the first colon with a {@link FilterSpan} that renders a "Chip"
 * including an icon representing "category" and the name of the category.
 * * Removing the trailing ":" from a category chip will cause it to remove the entire category
 * from the input.
 */
public class FilterTextWatcher implements TextWatcher {

    interface SearchTermsChangedListener {
        void onSearchTermsChanged(@Nullable String category, @NonNull String searchTerms);
    }

    /**
     * The character that separates the filter from the search terms.
     * This is the non-printable {@code NUL} character which is unlikely to be entered by the user.
     */
    public static final char FILTER_SEPARATOR = 0;

    private final Context context;
    private final EditText widget;
    private final SearchTermsChangedListener listener;

    private AppListActivity.FilterType filterType;

    private int removeTo = -1;
    private boolean requiresSpanRecalculation = false;

    FilterTextWatcher(final Context context, final EditText widget,
                      final SearchTermsChangedListener listener) {
        this.context = context;
        this.widget = widget;
        this.listener = listener;
    }

    public void setFilterType(AppListActivity.FilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * If the user removed the first colon in the search text, then request for the entire
     * block of text representing the category text to be removed when able.
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        removeTo = -1;

        boolean removingOrReplacing = count > 0;

        // Don't bother working out if we need to recalculate spans if we are removing text
        // right to the start. This could be if we are removing everything (in which case
        // there is no text to span), or we are removing somewhere from after the category
        // back to the start (in which case we've removed the category anyway and don't need
        // to explicitly request it to be removed.
        if (start == 0 && removingOrReplacing) {
            return;
        }

        String string = s.toString();
        boolean removingColon = removingOrReplacing
                && string.indexOf(FILTER_SEPARATOR, start) < (start + count);
        boolean removingFirstColon = removingColon && string.indexOf(FILTER_SEPARATOR) >= start;
        if (removingFirstColon) {
            removeTo = start + count - 1;
        }
    }

    /**
     * If the user added a colon, and there was not previously a colon before the newly added
     * one, then request for a {@link FilterSpan} to be added when able.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean addingOrReplacing = count > 0;
        boolean addingColon = addingOrReplacing
                && s.subSequence(start, start + count).toString().indexOf(FILTER_SEPARATOR) >= 0;
        boolean addingFirstColon = addingColon
                && s.subSequence(0, start).toString().indexOf(FILTER_SEPARATOR) == -1;
        if (addingFirstColon) {
            requiresSpanRecalculation = true;
        }
    }

    /**
     * If it was decided that we were removing a category, then ensure that the relevant
     * characters are removed. If it was deemed we were adding a new category, then ensure
     * that the relevant {@link FilterSpan} is added to {@param searchText}.
     */
    @Override
    public void afterTextChanged(Editable searchText) {
        if (removeTo >= 0) {
            removeLeadingCharacters(searchText, removeTo);
            removeTo = -1;
        } else if (requiresSpanRecalculation) {
            prepareSpans(searchText);
            requiresSpanRecalculation = false;
        }

        int colonIndex = searchText.toString().indexOf(FILTER_SEPARATOR);
        String category = colonIndex == -1 ? null : searchText.subSequence(0, colonIndex).toString();
        String searchTerms = searchText.subSequence(colonIndex == -1 ? 0 : colonIndex + 1,
                searchText.length()).toString();
        listener.onSearchTermsChanged(category, searchTerms);
    }

    /**
     * Removes all characters from {@param searchText} up until {@param end}.
     * Will do so without triggering a further set of callbacks on this {@link TextWatcher},
     * though if any other {@link TextWatcher}s have been added, they will be notified.
     */
    private void removeLeadingCharacters(Editable searchText, int end) {
        widget.removeTextChangedListener(this);
        if (searchText.length() < end) end = searchText.length();
        searchText.replace(0, end, "");
        widget.addTextChangedListener(this);
    }

    /**
     * Ensures that a {@link FilterSpan} is in {@param textToSpannify} if required.
     * Will firstly remove all existing category spans, and then add back one if necessary.
     * In addition, also adds a {@link TtsSpan} to indicate to screen readers that the category
     * span has semantic meaning representing a category.
     */
    private void prepareSpans(Editable textToSpannify) {
        if (textToSpannify == null) {
            return;
        }

        removeSpans(textToSpannify, FilterSpan.class);
        removeSpans(textToSpannify, TtsSpan.class);

        int colonIndex = textToSpannify.toString().indexOf(FILTER_SEPARATOR);
        if (colonIndex > 0) {
            FilterSpan span = new FilterSpan(context, filterType);
            textToSpannify.setSpan(span, 0, colonIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // For accessibility reasons, make this more clear to screen readers that the
            // span we just added semantically represents a specific filter.
            CharSequence filterName = textToSpannify.subSequence(0, colonIndex);

            @StringRes int ttsFilterDescription = switch (filterType) {
                case CATEGORY -> R.string.tts_category_name;
                case REPO -> R.string.tts_repo_name;
                case AUTHOR -> R.string.tts_author_name;
            };

            TtsSpan ttsSpan = new TtsSpan.TextBuilder(context.getString(ttsFilterDescription,
                    filterName)).build();
            textToSpannify.setSpan(ttsSpan, 0, 0, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Helper function to remove all spans of a certain type from an {@link Editable}.
     */
    private <T> void removeSpans(Editable text, Class<T> clazz) {
        T[] spans = text.getSpans(0, text.length(), clazz);
        for (T span : spans) {
            text.removeSpan(span);
        }
    }
}
