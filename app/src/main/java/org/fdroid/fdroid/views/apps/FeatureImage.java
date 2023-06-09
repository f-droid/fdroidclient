package org.fdroid.fdroid.views.apps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;

import java.util.Random;

/**
 * A feature image can have a {@link android.graphics.drawable.Drawable} or a {@link Palette}. If
 * a Drawable is available, then it will draw that, otherwise it will attempt to fall back to the
 * Palette you gave it. If a Palette is given, it will draw a series of triangles like so:
 * <p>
 * +_----+----_+_----+----_+
 * | \_  |  _/ | \_  |  _/ |
 * |   \_|_/   |   \_|_/   |
 * +_----+----_+_----+----_+
 * | \_  |  _/ | \_  |  _/ |
 * |   \_|_/   |   \_|_/   |
 * +-----+-----+-----+-----+
 * <p>
 * where each triangle is filled with one of two variations of the {@link Palette#getDominantColor(int)}
 * that is chosen randomly. The randomness is first seeded with the colour that has been selected.
 * This is so that if this repaints itself in the future, it will have the same unique pattern rather
 * than picking a new random pattern each time.
 * <p>
 * It is suggested that you obtain the Palette from the icon of an app.
 */
@SuppressWarnings("LineLength")
public class FeatureImage extends AppCompatImageView {

    private static final int NUM_SQUARES_WIDE = 4;
    private static final int NUM_SQUARES_HIGH = 2;

    // Double, because there are two triangles per square.
    private final Path[] triangles = new Path[NUM_SQUARES_HIGH * NUM_SQUARES_WIDE * 2];

    @Nullable
    private Paint[] trianglePaints;

    /**
     * F-Droid blue is shown behind the animation which eases in the feature graphics. This is
     * preferable to showing white behind, which can be a bit harsh.
     */
    @ColorInt
    private int baseColour;

    public FeatureImage(Context context) {
        super(context);
        init(context);
    }

    public FeatureImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FeatureImage(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float[] hsv = new float[3];
        Color.colorToHSV(ContextCompat.getColor(context, R.color.fdroid_blue), hsv);
        hsv[1] *= 0.5f;
        hsv[2] *= 0.7f;
        baseColour = Color.HSVToColor(hsv);
    }

    /**
     * Takes the {@link Palette#getDominantColor(int)} from the palette, dims it substantially, and
     * then creates a second variation that is slightly dimmer still. These two colours are then
     * randomly allocated to each triangle which is expected to be rendered.
     */
    public void setColour(@ColorInt int colour) {
        if (colour == 0) {
            trianglePaints = null;
            return;
        }

        // It is easier to dull al colour in the HSV space, so convert to that then adjust the
        // saturation down and the colour value down.
        float[] hsv = new float[3];
        Color.colorToHSV(colour, hsv);
        hsv[1] *= 0.5f;
        hsv[2] *= 0.7f;
        int colourOne = Color.HSVToColor(hsv);

        hsv[2] *= 0.9f;
        int colourTwo = Color.HSVToColor(hsv);

        Paint paintOne = new Paint();
        paintOne.setColor(colourOne);
        paintOne.setAntiAlias(true);
        paintOne.setStrokeWidth(2);
        paintOne.setStyle(Paint.Style.FILL_AND_STROKE);

        Paint paintTwo = new Paint();
        paintTwo.setColor(colourTwo);
        paintTwo.setAntiAlias(true);
        paintTwo.setStrokeWidth(2);
        paintTwo.setStyle(Paint.Style.FILL_AND_STROKE);

        // Seed based on the colour, so that each time we try to render a feature image with the
        // same colour, it will give the same pattern.
        Random random = new Random(colourOne);
        trianglePaints = new Paint[triangles.length];
        for (int i = 0; i < trianglePaints.length; i++) {
            trianglePaints[i] = random.nextBoolean() ? paintOne : paintTwo;
        }
    }

    public void setColorAndAnimateChange(@ColorInt int colour) {
        setColour(colour);
        animateColourChange();
    }

    private int currentAlpha = 255;
    private ValueAnimator alphaAnimator = null;

    private void animateColourChange() {
        if (alphaAnimator == null) {
            alphaAnimator = ValueAnimator.ofInt(0, 255);
        } else {
            alphaAnimator.cancel();
        }

        alphaAnimator = ValueAnimator.ofInt(0, 255).setDuration(150);
        alphaAnimator.addUpdateListener(animation -> {
            currentAlpha = (int) animation.getAnimatedValue();
            invalidate();
        });

        currentAlpha = 0;
        invalidate();
        alphaAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int triangleWidth = w / NUM_SQUARES_WIDE;
        int triangleHeight = h / NUM_SQUARES_HIGH;

        for (int x = 0; x < NUM_SQUARES_WIDE; x++) {
            for (int y = 0; y < NUM_SQUARES_HIGH; y++) {
                int startX = x * triangleWidth;
                int startY = y * triangleHeight;
                int endX = startX + triangleWidth;
                int endY = startY + triangleHeight;

                // Note that the order of these points need to go in a clockwise direction, or else
                // the fill will not be applied properly.
                Path firstTriangle;
                Path secondTriangle;

                // Alternate between two different ways to split a square into two triangles. This
                // results in a nicer geometric pattern (see doc comments at top of class for more
                // ASCII art of the expected outcome).
                if (x % 2 == 0) {
                    // +_----+
                    // | \_ 1|
                    // |2  \_|
                    // +-----+
                    firstTriangle = createTriangle(new Point(startX, startY), new Point(endX, startY), new Point(endX, endY));
                    secondTriangle = createTriangle(new Point(startX, startY), new Point(endX, endY), new Point(startX, endY));
                } else {
                    // +----_+
                    // |1 _/ |
                    // |_/  2|
                    // +-----+
                    firstTriangle = createTriangle(new Point(startX, startY), new Point(endX, startY), new Point(startX, endY));
                    secondTriangle = createTriangle(new Point(startX, endY), new Point(endX, startY), new Point(endX, endY));
                }

                triangles[y * (NUM_SQUARES_WIDE * 2) + (x * 2)] = firstTriangle;
                triangles[y * (NUM_SQUARES_WIDE * 2) + (x * 2) + 1] = secondTriangle;
            }
        }

    }

    /**
     * First try to draw whatever image was given to this view. If that doesn't exist, try to draw
     * a geometric pattern based on the palette that was given to us. If we haven't had a colour
     * assigned to us (using {@link #setColour(int)}) then clear the view by filling it with white.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null) {
            super.onDraw(canvas);
        } else if (trianglePaints != null) {
            for (Paint paint : trianglePaints) {
                paint.setAlpha(currentAlpha);
            }

            canvas.drawColor(baseColour);
            for (int i = 0; i < triangles.length; i++) {
                canvas.drawPath(triangles[i], trianglePaints[i]);
            }
        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }

    }

    /**
     * This requires the three points to be in a sequence that traces out a triangle in clockwise
     * fashion. This is required for the triangle to be filled correctly when drawing, otherwise
     * it will end up black.
     */
    private static Path createTriangle(Point start, Point middle, Point end) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(start.x, start.y);
        path.lineTo(middle.x, middle.y);
        path.lineTo(end.x, end.y);
        path.close();

        return path;
    }

    public void loadImageAndDisplay(App app) {
        setColour(ContextCompat.getColor(getContext(), R.color.fdroid_blue));

        if (app.featureGraphic == null) {
            loadImageAndExtractColour(app.loadWithGlide(getContext(), app.iconFile));
        } else {
            Utils.loadWithGlide(getContext(), app.repoId, app.featureGraphic, this);
        }
    }

    private void loadImageAndExtractColour(RequestBuilder<Drawable> request) {
        request.listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object o, Target<Drawable> target, boolean b) {
                setColorAndAnimateChange(Color.LTGRAY);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable loadedImage, Object o, Target<Drawable> target, DataSource dataSource, boolean b) {
                return false;
            }
        }).into(this);

    }
}
