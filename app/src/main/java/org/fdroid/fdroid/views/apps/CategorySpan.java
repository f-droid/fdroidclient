package org.fdroid.fdroid.views.apps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.categories.CategoryController;

/**
 * This draws a category "chip" in the search text view according to the material design specs
 * (https://material.google.com/components/chips.html#chips-specs). These contain a circle with an
 * icon representing "category" on the left, and the name of the category on the right. It also has
 * a background with curved corners behind the category text.
 */
public class CategorySpan extends ReplacementSpan {

    private static final int HEIGHT = 32;
    private static final int CORNER_RADIUS = 16;
    private static final int ICON_BACKGROUND_SIZE = 32;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = (ICON_BACKGROUND_SIZE - ICON_SIZE) / 2;
    private static final int TEXT_LEADING_PADDING = 8;
    private static final int TEXT_TRAILING_PADDING = 12;
    private static final int TEXT_BELOW_PADDING = 4;
    private static final int WHITE_SPACE_PADDING_AT_END = 4;
    private static final float DROP_SHADOW_HEIGHT = 1.5f;

    private final Context context;

    CategorySpan(Context context) {
        super();
        this.context = context;
    }

    @Nullable
    private static CharSequence getCategoryName(@Nullable CharSequence text, int start, int end) {
        if (text == null) {
            return null;
        }

        if (start + 1 >= end - 1) {
            // This can happen when the spell checker is trying to underline text within our category
            // name. It sometimes will ask for sub-lengths of this span.
            return null;
        }

        return text.subSequence(start, end - 1);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        CharSequence categoryName = getCategoryName(text, start, end);
        if (categoryName == null) {
            return 0;
        }

        float density = context.getResources().getDisplayMetrics().density;

        int iconBackgroundSize = (int) (ICON_BACKGROUND_SIZE * density);
        int textLeadingPadding = (int) (TEXT_LEADING_PADDING * density);
        int textWidth = (int) paint.measureText(categoryName.toString());
        int textTrailingPadding = (int) (TEXT_TRAILING_PADDING * density);
        int whiteSpacePadding = (int) (WHITE_SPACE_PADDING_AT_END * density);

        return iconBackgroundSize + textLeadingPadding + textWidth + textTrailingPadding + whiteSpacePadding;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        CharSequence categoryName = getCategoryName(text, start, end);
        if (categoryName == null) {
            return;
        }

        float density = context.getResources().getDisplayMetrics().density;

        int height = (int) (HEIGHT * density);
        int iconBackgroundSize = (int) (ICON_BACKGROUND_SIZE * density);
        int cornerRadius = (int) (CORNER_RADIUS * density);
        int iconSize = (int) (ICON_SIZE * density);
        int iconPadding = (int) (ICON_PADDING * density);
        int textWidth = (int) paint.measureText(categoryName.toString());
        int textLeadingPadding = (int) (TEXT_LEADING_PADDING * density);
        int textTrailingPadding = (int) (TEXT_TRAILING_PADDING * density);

        canvas.save();
        canvas.translate(x, bottom - height + TEXT_BELOW_PADDING * density);

        RectF backgroundRect = new RectF(0, 0, iconBackgroundSize + textLeadingPadding
                + textWidth + textTrailingPadding, height);

        int backgroundColour = CategoryController.getBackgroundColour(context, categoryName.toString());

        // The shadow below the entire category chip.
        canvas.save();
        canvas.translate(0, DROP_SHADOW_HEIGHT * density);
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(0x66000000);
        shadowPaint.setAntiAlias(true);
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, shadowPaint);
        canvas.restore();

        // The background which goes behind the text.
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColour);
        backgroundPaint.setAntiAlias(true);
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint);

        // The background behind the category icon.
        Paint iconBackgroundPaint = new Paint();
        iconBackgroundPaint.setColor(0xffd8d8d8);
        iconBackgroundPaint.setAntiAlias(true);
        RectF iconBackgroundRect = new RectF(0, 0, iconBackgroundSize, height);
        canvas.drawRoundRect(iconBackgroundRect, cornerRadius, cornerRadius, iconBackgroundPaint);

        // Category icon on top of the circular background which was just drawn.
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_categories);
        icon.setBounds(iconPadding, iconPadding, iconPadding + iconSize, iconPadding + iconSize);
        icon.draw(canvas);

        // Choose white or black text based on the perceived brightness.
        // Uses some arbitrary magic from https://stackoverflow.com/a/946734/2391921
        double grey = Color.red(backgroundColour) * 0.299 +
                Color.green(backgroundColour) * 0.587 +
                Color.blue(backgroundColour) * 0.114;

        // The category name drawn to the right of the category name.
        Paint textPaint = new Paint(paint);
        textPaint.setColor(grey < 186 ? Color.WHITE : Color.BLACK);
        canvas.drawText(categoryName.toString(), iconBackgroundSize + textLeadingPadding, bottom, textPaint);

        canvas.restore();
    }
}
