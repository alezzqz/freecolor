/*
 * Copyright (C) 2026 TermoGRAF
 *
 * This file is part of FreeColor.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.freecolor.ui;

import android.content.Context;
import android.view.*;
import android.graphics.*;
import android.animation.*;

import androidx.annotation.NonNull;

import com.freecolor.core.*;

/**
 * Responsive glass rendering; authoritative state is committed before visual interpolation.
 */
final class GameView extends View {

    // Responsive tube layout (dp and width ratios).
    private static final int MIN_COLUMNS = 3;
    private static final int MAX_COLUMNS = 6;
    private static final float MAX_TUBE_WIDTH_DP = 130f;
    private static final float BOARD_VERTICAL_RESERVE_DP = 60f;
    private static final float COLUMN_WIDTH_RATIO = 1.65f;
    private static final float TUBE_HEIGHT_RATIO = 2.85f;
    private static final float TUBE_WIDTH_SCALE = 1.06f;
    private static final float TUBE_GAP_RATIO = .65f;
    private static final int COMPACT_LAYOUT_TUBE_COUNT = 8;
    private static final float COMPACT_TUBE_GAP_RATIO = .25f;
    private static final int DENSE_LAYOUT_TUBE_COUNT = 13;
    private static final float DENSE_TUBE_GAP_RATIO = .18f;
    private static final float ROW_GAP_RATIO = .8f;
    private static final int FEW_COLORS_MAX = 5;
    private static final float FEW_COLORS_EXTRA_GAP_RATIO = .30f;
    private static final float FEW_COLORS_ROW_GAP_RATIO = 1.25f;
    private static final int SMALL_PALETTE_MAX_COLORS = 6;
    private static final float SMALL_PALETTE_EXTRA_GAP_RATIO = .10f;
    private static final float SMALL_PALETTE_ROW_GAP_RATIO = 1f;
    private static final int LARGE_PALETTE_COLORS = 8;
    private static final float LARGE_PALETTE_TUBE_GAP_RATIO = .12f;
    private static final float LARGE_PALETTE_ROW_GAP_RATIO = .65f;
    private static final int VERY_LARGE_PALETTE_COLORS = 10;
    private static final float VERY_LARGE_PALETTE_ROW_GAP_RATIO = .55f;
    private static final float BOARD_TOP_OFFSET_RATIO = .4f;

    // Pour animation phases and geometry.
    private static final long POUR_DURATION_MILLIS = 900;
    private static final long SELECTION_DURATION_MILLIS = 240;
    private static final float POUR_APPROACH_END = .28f;
    private static final float POUR_RETURN_DURATION = .22f;
    private static final float POUR_TRANSFER_DURATION = .5f;
    private static final float POUR_STREAM_END = POUR_APPROACH_END + POUR_TRANSFER_DURATION;
    private static final float POUR_LIFT_HEIGHT_RATIO = .3f;
    private static final float POUR_PIVOT_RATIO = .2f;
    private static final float POUR_STREAM_WIDTH_DP = 5f;

    // Glass and liquid geometry; shading multipliers and stops.
    private static final float GLASS_INSET_RATIO = .1f;
    private static final float GLASS_BOTTOM_CURVE_RATIO = .4f;
    private static final float RIM_OVERHANG_DP = 2f;
    private static final float RIM_HALF_HEIGHT_DP = 3f;
    private static final float LIQUID_HEIGHT_RATIO = .88f;
    private static final float BOTTOM_LAYER_COMPENSATION_WIDTH_RATIO = .05f;
    private static final float LIQUID_LEFT_SHADE = .65f;
    private static final float LIQUID_HIGHLIGHT_SHADE = 1.16f;
    private static final float LIQUID_RIGHT_SHADE = .72f;
    private static final float LIQUID_SURFACE_SHADE = 1.20f;
    private static final float LIQUID_HIGHLIGHT_STOP = .24f;
    private static final float LIQUID_BASE_STOP = .58f;
    private static final float HIDDEN_LABEL_HEIGHT_RATIO = .52f;
    private static final float GLASS_STROKE_DP = 2f;
    private static final float SELECTED_GLASS_STROKE_DP = 3f;
    private static final float REFLECTION_X_RATIO = .23f;
    private static final float REFLECTION_TOP_OFFSET_DP = 10f;
    private static final float REFLECTION_BOTTOM_OFFSET_RATIO = .45f;
    private static final float EXTRA_REFLECTION_TOP_OFFSET_DP = 5f;
    private static final int COLOR_CHANNEL_MAX = 255;
    private static final int LIQUID_BOTTOM_OVERSCAN_PX = 1;

    // Extra tube button and countdown.
    private static final float EXTRA_BUTTON_RADIUS_DP = 11f;
    private static final float EXTRA_BUTTON_RADIUS_HEIGHT_RATIO = .34f;
    private static final float EXTRA_ICON_STROKE_DP = 2.5f;
    private static final float EXTRA_ICON_ARM_RATIO = .40f;
    private static final float CLOCK_HAND_X_RATIO = .75f;
    private static final float CLOCK_HAND_Y_RATIO = .45f;
    private static final float EXTRA_COUNTDOWN_TEXT_DP = 12f;
    private static final float EXTRA_COUNTDOWN_WIDTH_RATIO = .24f;
    private static final float EXTRA_COUNTDOWN_OFFSET_DP = 17f;

    // Selection and touch geometry.
    private static final int NO_SELECTION = -1;
    private static final float SELECTION_LIFT_DP = 28f;
    private static final float SELECTION_LIFT_WIDTH_RATIO = .6f;
    private static final float SELECTION_TOP_CLEARANCE_DP = 6f;
    private static final float TOUCH_HORIZONTAL_EXPANSION_DP = 8f;
    private static final float TOUCH_VERTICAL_EXPANSION_DP = 10f;
    private static final float MIN_TOUCH_HEIGHT_DP = 48f;

    // Staggered particles surround the full tube, without drawing a border over the glass.
    private static final int COMPLETION_SPARK_COUNT = 28;
    private static final int SPARK_GOLD_GREEN = 220;
    private static final int SPARK_GOLD_BLUE = 95;
    private static final int SPARK_LIGHT_BLUE = 240;
    private static final float SPARK_ORIGIN_WIDTH_RATIO = .48f;
    private static final float SPARK_ORIGIN_HEIGHT_RATIO = .46f;
    private static final float SPARK_DELAY_FRACTION = .18f;
    private static final float SPARK_TRAVEL_MIN_DP = 16f;
    private static final float SPARK_TRAVEL_VARIATION_DP = 20f;
    private static final float SPARK_VERTICAL_TRAVEL_RATIO = .45f;
    private static final float SPARK_RISE_DP = 22f;
    private static final float SPARK_GRAVITY_DP = 14f;
    private static final float SPARK_ARM_DP = 2.8f;
    private static final float SPARK_SIZE_VARIATION_RATIO = .45f;
    private static final float SPARK_GLOW_SIZE_RATIO = 2.2f;
    private static final float SPARK_GLOW_ALPHA_RATIO = .14f;
    private static final float SPARK_STROKE_DP = 1.2f;
    private static final float SPARK_SHORT_ARM_RATIO = .65f;
    private static final float SPARK_FADE_IN_FRACTION = .12f;
    private static final float SPARK_TWINKLE_BASE = .75f;
    private static final float SPARK_TWINKLE_DEPTH = .25f;
    private static final float SPARK_TWINKLE_CYCLES = 3f;
    private static final float SPARK_ROTATION_DEGREES = 80f;
    private static final int SPARK_SEED_MODULUS = 17;
    private static final int SPARK_SEED_MULTIPLIER = 7;
    private static final int SPARK_STAR_INTERVAL = 3;

    // Rendering colors.
    private static final int GLASS_TINT = 0x183ed1ed;
    private static final int EXTRA_GLASS_TINT = 0x083ed1ed;
    private static final int GLASS_OUTLINE = 0xffc3d3e8;
    private static final int EXTRA_UPPER_OUTLINE = 0x48c3d3e8;
    private static final int SELECTED_OUTLINE = 0xffa0fff0;
    private static final int GLASS_REFLECTION = 0x30ffffff;
    private static final int EXTRA_UPPER_REFLECTION = 0x12ffffff;
    private static final int HIDDEN_LIQUID = 0xff15142e;
    private static final int HIDDEN_LABEL = 0xff8b7bbb;
    private static final int EXTRA_BUTTON_AVAILABLE = 0xffffd323;
    private static final int EXTRA_BUTTON_WAITING = 0xff61536e;
    private static final int EXTRA_PLUS = 0xff302047;
    private static final int EXTRA_CLOCK = 0xffffdb78;
    private static final int LIQUID_PINK = 0xfff773b1;
    private static final int LIQUID_GREEN = 0xff034f28;
    private static final int LIQUID_YELLOW = 0xfff7ac1c;
    private static final int LIQUID_PURPLE = 0xff7c1073;
    private static final int LIQUID_BLUE = 0xff054dcd;
    private static final int LIQUID_ORANGE = 0xffee6a17;
    private static final int LIQUID_OLIVE = 0xff709207;
    private static final int LIQUID_PEACH = 0xfffbc4b0;
    private static final int LIQUID_RED = 0xffe63348;
    private static final int LIQUID_CYAN = 0xff189fd7;
    private static final int LIQUID_BROWN = 0xff894a5b;

    interface Listener {
        void pour(Move m);

        void unlockExtra();
    }

    Listener listener;
    private static final int[] COLORS = {
            // Sampled from the liquid interiors of the supplied reference.
            LIQUID_PINK, LIQUID_GREEN, LIQUID_YELLOW, LIQUID_PURPLE, LIQUID_BLUE, LIQUID_ORANGE, LIQUID_OLIVE, LIQUID_PEACH, LIQUID_RED, LIQUID_CYAN, LIQUID_BROWN
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint hiddenTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics hiddenFontMetrics = new Paint.FontMetrics();
    private final RectF displayedBounds = new RectF();
    private final TiltedLiquidGeometry tiltedLiquid = new TiltedLiquidGeometry();
    private final RectF pourBounds = new RectF();
    private final RectF surfaceBounds = new RectF();
    private final Path glassPath = new Path();
    private final Path liquidPath = new Path();
    private final Path outlinePath = new Path();
    private final Matrix shaderPosition = new Matrix();
    private final LinearGradient[] liquidShaders = new LinearGradient[COLORS.length];
    private static final int[] EMPTY_UNITS = new int[0];
    private static final boolean[] EMPTY_VISIBILITY = new boolean[0];
    private static final float[] LIQUID_GRADIENT_STOPS = {0, LIQUID_HIGHLIGHT_STOP, LIQUID_BASE_STOP, 1};
    private float shaderWidth = -1;
    private int[][] currentUnits, previousUnits;
    private boolean[][] currentVisibility, previousVisibility;
    private int pourAmount;
    private RectF[] boxes = new RectF[0];
    private GameState state, before;
    private Move moving;
    private int selected = NO_SELECTION;
    boolean busy;
    boolean inputEnabled;
    private float progress;
    private ValueAnimator animator;
    private ValueAnimator selectionAnimator;
    private float[] selectionOffsets = new float[0];
    private float pourStartLift;
    private float selectionRowClearance;
    private long extraWaitMillis;
    private boolean extraAllowed = true;
    private long[] completionStarted = new long[0];
    private static final long COMPLETION_EFFECT_MILLIS = 1400;

    GameView(Context c) {
        super(c);
        setContentDescription("Игровое поле: выберите источник, затем пробирку назначения");
        setFocusable(true);
        hiddenTextPaint.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        hiddenTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setState(GameState s) {
        state = s;
        currentUnits = s.copy();
        currentVisibility = s.revealedCopy();
        clearSelection();
        layoutTubes();
        for (int i = 0; i < completionStarted.length; i++) {
            if (i >= state.count() || !state.completeTube(i)) {
                completionStarted[i] = 0;
            }
        }
        requestLayout();
        invalidate();
    }

    private float dp(float n) {
        return n * getResources().getDisplayMetrics().density;
    }

    private record Spacing(float gapRatio, float rowGapRatio, float extraGapRatio) {}

    private record ColumnLayout(int columns, float width) {}

    private void layoutTubes() {
        if (state == null) {
            return;
        }
        int n = visibleTubeCount();
        int colors = paletteSize();
        Spacing spacing = layoutSpacing(colors, n);
        ColumnLayout layout = bestColumnLayout(n, colors, spacing);
        placeTubes(n, layout, spacing);
    }

    private int paletteSize() {
        return state.count() - GameConfig.EMPTY_TUBES - (state.extra ? 1 : 0);
    }

    private static Spacing layoutSpacing(int colors, int n) {
        float gapRatio = n >= COMPACT_LAYOUT_TUBE_COUNT ? COMPACT_TUBE_GAP_RATIO : TUBE_GAP_RATIO;
        if (n >= DENSE_LAYOUT_TUBE_COUNT) {
            gapRatio = DENSE_TUBE_GAP_RATIO;
        }
        boolean largePalette = colors >= LARGE_PALETTE_COLORS;
        float rowGapRatio = largePalette ? LARGE_PALETTE_ROW_GAP_RATIO : ROW_GAP_RATIO;
        if (largePalette) {
            gapRatio = LARGE_PALETTE_TUBE_GAP_RATIO;
        }
        float extraGapRatio = colors <= FEW_COLORS_MAX
                ? FEW_COLORS_EXTRA_GAP_RATIO : SMALL_PALETTE_EXTRA_GAP_RATIO;
        if (colors <= SMALL_PALETTE_MAX_COLORS) {
            gapRatio += extraGapRatio;
            rowGapRatio = colors <= FEW_COLORS_MAX
                    ? FEW_COLORS_ROW_GAP_RATIO : SMALL_PALETTE_ROW_GAP_RATIO;
        } else if (colors >= VERY_LARGE_PALETTE_COLORS) {
            rowGapRatio = VERY_LARGE_PALETTE_ROW_GAP_RATIO;
        }
        return new Spacing(gapRatio, rowGapRatio, extraGapRatio);
    }

    private ColumnLayout bestColumnLayout(int n, int colors, Spacing spacing) {
        float rowWidthRatio = TUBE_HEIGHT_RATIO + spacing.rowGapRatio();
        float best = 0;
        int bestCols = MIN_COLUMNS;
        for (int cols = MIN_COLUMNS; cols <= MAX_COLUMNS; cols++) {
            int rows = (n + cols - 1) / cols;
            float w = Math.min(dp(MAX_TUBE_WIDTH_DP), Math.min(getWidth() / widthBudget(cols, n, colors, spacing),
                    (getHeight() - dp(BOARD_VERTICAL_RESERVE_DP)) / (rows * rowWidthRatio)));
            if (w > best) {
                best = w;
                bestCols = cols;
            }
        }
        return new ColumnLayout(bestCols, best);
    }

    private static float widthBudget(int cols, int n, int colors, Spacing spacing) {
        // Dense boards spend less width on gaps, including the two outer margins.
        if (n >= COMPACT_LAYOUT_TUBE_COUNT) {
            return cols * TUBE_WIDTH_SCALE + (cols + 1) * spacing.gapRatio();
        }
        return cols * (COLUMN_WIDTH_RATIO + (colors <= SMALL_PALETTE_MAX_COLORS ? spacing.extraGapRatio() : 0));
    }

    private void placeTubes(int n, ColumnLayout layout, Spacing spacing) {
        // Widen the glass independently of the original height and spacing budget.
        float w = layout.width() * TUBE_WIDTH_SCALE;
        float h = layout.width() * TUBE_HEIGHT_RATIO;
        float gap = layout.width() * spacing.gapRatio();
        float rowH = h + layout.width() * spacing.rowGapRatio();
        // Keep a raised tube clear of the row above even when the board is tightly packed.
        selectionRowClearance = Math.max(0, layout.width() * spacing.rowGapRatio() - dp(SELECTION_TOP_CLEARANCE_DP));
        int rows = (n + layout.columns() - 1) / layout.columns();
        float top = (getHeight() - rows * rowH) / 2 + layout.width() * BOARD_TOP_OFFSET_RATIO;
        boxes = new RectF[n];
        if (selectionOffsets.length != n) {
            selectionOffsets = new float[n];
        }
        if (completionStarted.length != n) {
            completionStarted = new long[n];
        }
        for (int i = 0; i < n; i++) {
            int row = i / layout.columns();
            int k = Math.min(layout.columns(), n - row * layout.columns());
            float x = (getWidth() - (k * w + (k - 1) * gap)) / 2 + (i % layout.columns()) * (w + gap);
            boxes[i] = new RectF(x, top + row * rowH, x + w, top + row * rowH + h);
        }
    }

    void setExtraAllowed(boolean allowed) {
        extraAllowed = allowed;
        layoutTubes();
        requestLayout();
        invalidate();
    }

    private int visibleTubeCount() {
        return state.count() + (state.extra || !extraAllowed ? 0 : 1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutTubes();
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        super.onDraw(c);
        if (state == null) {
            return;
        }
        if (boxes.length != visibleTubeCount()) {
            layoutTubes();
        }
        drawBoard(c);
        if (busy) {
            drawPourAnimation(c);
        }
        drawCompletionEffects(c);
    }

    private void drawBoard(Canvas c) {
        for (int i = 0; i < boxes.length; i++) {
            if (i == state.count()) {
                drawLockedExtra(c, boxes[i]);
                continue;
            }
            if (busy && i == moving.source()) {
                continue;
            }
            RectF b = displayedBox(i);
            int[] units = currentUnits[i];
            float level = units.length;
            if (busy && i == moving.destination()) {
                level = before.size(i) + (state.size(i) - before.size(i)) * transfer();
            }
            drawTube(c, b, units, currentVisibility[i], level, i == selected, state.capacity(i));
        }
    }

    private void drawPourAnimation(Canvas c) {
        RectF src = boxes[moving.source()], dst = boxes[moving.destination()];
        float travel = pourTravel();
        float fill = before.size(moving.source()) - pourAmount * transfer();
        int sourceCapacity = before.capacity(moving.source());
        float angle = pourAngle(src, fill, sourceCapacity);
        float rimOffset = src.width() * (.5f - GLASS_INSET_RATIO) + dp(RIM_OVERHANG_DP);

        RectF tube = pourTubeBounds(src, dst, angle, travel, rimOffset);
        drawPouringTube(c, tube, fill, sourceCapacity, angle, travel);
        drawPourStream(c, dst, tube, angle, travel, rimOffset);
    }

    private float pourTravel() {
        // Horizontal travel ramps up during approach and back down during the return.
        return Math.min(1, progress / POUR_APPROACH_END) * Math.min(1, (1 - progress) / POUR_RETURN_DURATION);
    }

    private RectF pourTubeBounds(RectF src, RectF dst, float angle, float travel, float rimOffset) {
        double fullAngle = Math.toRadians(angle);
        float lipOffset = rimOffset * (float) Math.cos(fullAngle)
                + src.width() * POUR_PIVOT_RATIO * (float) Math.sin(fullAngle);
        float x = src.left + (dst.centerX() - src.centerX() - lipOffset) * travel;
        float remainingLift = pourStartLift * (1 - Math.min(1, progress / POUR_APPROACH_END));
        float y = src.top + (dst.top - src.height() * POUR_LIFT_HEIGHT_RATIO - src.top) * travel - remainingLift;
        RectF b = pourBounds;
        b.set(x, y, x + src.width(), y + src.height());
        return b;
    }

    private void drawPouringTube(Canvas c, RectF tube, float fill, int sourceCapacity, float angle, float travel) {
        c.save();
        c.rotate(angle * travel, tube.centerX(), tube.top + tube.width() * POUR_PIVOT_RATIO);
        drawTube(c, tube, previousUnits[moving.source()], previousVisibility[moving.source()], fill, true, sourceCapacity, angle * travel);
        c.restore();
    }

    private void drawPourStream(Canvas c, RectF dst, RectF tube, float angle, float travel, float rimOffset) {
        if (progress > POUR_APPROACH_END && progress < POUR_STREAM_END) {
            paint.setColor(COLORS[before.top(moving.source())]);
            paint.setStrokeWidth(dp(POUR_STREAM_WIDTH_DP));
            int capacity = state.capacity(moving.destination());
            float surface = dst.bottom - liquidFillHeight(dst,
                    before.size(moving.destination()) + pourAmount * transfer(), capacity);
            double currentAngle = Math.toRadians(angle * travel);
            float lipX = tube.centerX() + rimOffset * (float) Math.cos(currentAngle)
                    + tube.width() * POUR_PIVOT_RATIO * (float) Math.sin(currentAngle);
            float lipY = tube.top + rimOffset * (float) Math.sin(currentAngle)
                    + tube.width() * POUR_PIVOT_RATIO * (1 - (float) Math.cos(currentAngle));
            c.drawLine(lipX, lipY, dst.centerX(), surface, paint);
        }
    }

    void setExtraWaitMillis(long remaining) {
        extraWaitMillis = Math.max(0, remaining);
        invalidate();
    }

    private void drawCompletionEffects(Canvas canvas) {
        long now = android.os.SystemClock.uptimeMillis();
        boolean active = false;
        for (int i = 0; i < completionStarted.length; i++) {
            if (completionStarted[i] == 0) {
                continue;
            }
            float fraction = (now - completionStarted[i]) / (float) COMPLETION_EFFECT_MILLIS;
            if (fraction >= 1 || i >= state.count() || !state.completeTube(i)) {
                completionStarted[i] = 0;
                continue;
            }
            active = true;
            drawCompletionSparks(canvas, displayedBox(i), fraction);
        }
        if (active) {
            postInvalidateOnAnimation();
        }
    }

    private void drawCompletionSparks(Canvas canvas, RectF bounds, float fraction) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(SPARK_STROKE_DP));
        for (int spark = 0; spark < COMPLETION_SPARK_COUNT; spark++) {
            drawSpark(canvas, bounds, spark, fraction);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSpark(Canvas canvas, RectF bounds, int spark, float fraction) {
        // Fixed seeds make trajectories stable across frames and avoid particle allocations.
        float seed = (spark * SPARK_SEED_MULTIPLIER % SPARK_SEED_MODULUS)
                / (float) SPARK_SEED_MODULUS;
        float age = (fraction - seed * SPARK_DELAY_FRACTION) / (1 - SPARK_DELAY_FRACTION);
        if (age <= 0 || age >= 1) {
            return;
        }
        double angle = spark * Math.PI * 2 / COMPLETION_SPARK_COUNT;
        float directionX = (float) Math.cos(angle);
        float directionY = (float) Math.sin(angle);
        float distance = dp(SPARK_TRAVEL_MIN_DP + seed * SPARK_TRAVEL_VARIATION_DP);
        float x = bounds.centerX() + directionX
                * (bounds.width() * SPARK_ORIGIN_WIDTH_RATIO + distance * age);
        float y = bounds.centerY() + directionY
                * (bounds.height() * SPARK_ORIGIN_HEIGHT_RATIO
                + distance * SPARK_VERTICAL_TRAVEL_RATIO * age)
                - dp(SPARK_RISE_DP) * age + dp(SPARK_GRAVITY_DP) * age * age;
        float fade = Math.min(1, age / SPARK_FADE_IN_FRACTION) * (1 - age) * (1 - age);
        float twinkle = SPARK_TWINKLE_BASE + SPARK_TWINKLE_DEPTH
                * (float) Math.sin((age * SPARK_TWINKLE_CYCLES + seed) * Math.PI * 2);
        int alpha = (int) (COLOR_CHANNEL_MAX * fade * twinkle);
        int green = spark % 2 == 0 ? SPARK_GOLD_GREEN : COLOR_CHANNEL_MAX;
        int blue = spark % 2 == 0 ? SPARK_GOLD_BLUE : SPARK_LIGHT_BLUE;
        float arm = dp(SPARK_ARM_DP) * (1 - age)
                * (1 + seed * SPARK_SIZE_VARIATION_RATIO);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (alpha * SPARK_GLOW_ALPHA_RATIO),
                COLOR_CHANNEL_MAX, green, blue));
        canvas.drawCircle(x, y, arm * SPARK_GLOW_SIZE_RATIO, paint);
        paint.setColor(Color.argb(alpha, COLOR_CHANNEL_MAX, green, blue));
        if (spark % SPARK_STAR_INTERVAL == 0) {
            canvas.save();
            canvas.rotate(seed * SPARK_ROTATION_DEGREES + age * SPARK_ROTATION_DEGREES, x, y);
            canvas.drawLine(x - arm, y, x + arm, y, paint);
            canvas.drawLine(x, y - arm * SPARK_SHORT_ARM_RATIO,
                    x, y + arm * SPARK_SHORT_ARM_RATIO, paint);
            canvas.restore();
        } else {
            canvas.drawCircle(x, y, arm * SPARK_SHORT_ARM_RATIO, paint);
        }
    }

    android.graphics.PointF extraRewardPosition() {
        if (boxes.length == 0) {
            return new android.graphics.PointF(getWidth() / 2f, getHeight() / 2f);
        }
        RectF bounds = boxes[boxes.length - 1];
        float centerY = (bounds.top + extraCompartmentTop(bounds)) / 2f;
        float radius = Math.min(dp(EXTRA_BUTTON_RADIUS_DP), bounds.height() * EXTRA_BUTTON_RADIUS_HEIGHT_RATIO);
        return new android.graphics.PointF(bounds.centerX(), centerY + radius + dp(EXTRA_COUNTDOWN_OFFSET_DP));
    }

    private void drawLockedExtra(Canvas canvas, RectF bounds) {
        drawTube(canvas, bounds, EMPTY_UNITS, EMPTY_VISIBILITY, 0, false, GameConfig.EXTRA_CAPACITY);
        float centerY = (bounds.top + extraCompartmentTop(bounds)) / 2f;
        float radius = Math.min(dp(EXTRA_BUTTON_RADIUS_DP), bounds.height() * EXTRA_BUTTON_RADIUS_HEIGHT_RATIO);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(extraWaitMillis == 0 ? EXTRA_BUTTON_AVAILABLE : EXTRA_BUTTON_WAITING);
        canvas.drawCircle(bounds.centerX(), centerY, radius, paint);
        paint.setColor(extraWaitMillis == 0 ? EXTRA_PLUS : EXTRA_CLOCK);
        paint.setStrokeWidth(dp(EXTRA_ICON_STROKE_DP));
        paint.setStrokeCap(Paint.Cap.ROUND);
        float arm = radius * EXTRA_ICON_ARM_RATIO;
        if (extraWaitMillis == 0) {
            canvas.drawLine(bounds.centerX() - arm, centerY, bounds.centerX() + arm, centerY, paint);
            canvas.drawLine(bounds.centerX(), centerY - arm, bounds.centerX(), centerY + arm, paint);
        } else {
            canvas.drawLine(bounds.centerX(), centerY, bounds.centerX(), centerY - arm, paint);
            canvas.drawLine(bounds.centerX(), centerY, bounds.centerX() + arm * CLOCK_HAND_X_RATIO, centerY + arm * CLOCK_HAND_Y_RATIO, paint);
            long seconds = (extraWaitMillis + GameConfig.MILLIS_PER_SECOND - 1) / GameConfig.MILLIS_PER_SECOND;
            String countdown = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / GameConfig.SECONDS_PER_MINUTE, seconds % GameConfig.SECONDS_PER_MINUTE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.min(dp(EXTRA_COUNTDOWN_TEXT_DP), bounds.width() * EXTRA_COUNTDOWN_WIDTH_RATIO));
            paint.setColor(EXTRA_CLOCK);
            canvas.drawText(countdown, bounds.centerX(), centerY + radius + dp(EXTRA_COUNTDOWN_OFFSET_DP), paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private float transfer() {
        return Math.max(0, Math.min(1, (progress - POUR_APPROACH_END) / POUR_TRANSFER_DURATION));
    }

    private float pourAngle(RectF bounds, float fill, int capacity) {
        configureLiquidGeometry(bounds);
        double area = tiltedLiquid.areaBelow(-liquidFillHeight(bounds, fill, capacity));
        return tiltedLiquid.pourAngle(area);
    }

    private void configureLiquidGeometry(RectF bounds) {
        tiltedLiquid.configure(bounds.width() * (1 - 2 * GLASS_INSET_RATIO),
                bounds.height(), bounds.width() * GLASS_BOTTOM_CURVE_RATIO);
    }

    private void drawTube(Canvas c, RectF b, int[] units, boolean[] revealed, float fill, boolean highlight, int capacity) {
        drawTube(c, b, units, revealed, fill, highlight, capacity, 0);
    }

    private void drawTube(Canvas c, RectF b, int[] units, boolean[] revealed, float fill,
            boolean highlight, int capacity, float angle) {
        drawLiquidContents(c, b, units, revealed, fill, capacity, angle);
        drawGlassOutline(c, b, capacity, highlight);
        drawGlassReflection(c, b, capacity);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLiquidContents(Canvas c, RectF b, int[] units, boolean[] revealed,
            float fill, int capacity, float angle) {
        float inset = b.width() * GLASS_INSET_RATIO;
        Path glass = glassPath;
        glass.reset();
        buildGlassContour(glass, b, inset);
        glass.close();
        c.save();
        c.clipPath(glass);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(capacity == GameConfig.EXTRA_CAPACITY ? EXTRA_GLASS_TINT : GLASS_TINT);
        c.drawRect(b, paint);
        if (angle > .01f) {
            drawTiltedLiquid(c, b, units, revealed, fill, capacity, angle);
        } else {
            drawUprightLiquid(c, b, units, revealed, fill, capacity);
        }
        paint.setAntiAlias(true);
        c.restore();
    }

    private void drawUprightLiquid(Canvas c, RectF b, int[] units, boolean[] revealed,
            float fill, int capacity) {
        // Matching units form one cylinder. Internal interfaces share curved
        // contours; only the exposed surface gets a lit elliptical cap.
        paint.setAntiAlias(true);
        for (int start = 0; start < units.length && start < fill; ) {
            int end = runEnd(units, revealed, start);
            drawUprightBand(c, b, units, revealed, fill, capacity, start, end);
            start = end;
        }
    }

    private void drawUprightBand(Canvas c, RectF b, int[] units, boolean[] revealed,
            float fill, int capacity, int start, int end) {
        float inset = b.width() * GLASS_INSET_RATIO;
        float bottom = b.bottom;
        float depth = liquidDepth(b, inset);
        float visibleEnd = Math.min(fill, end);
        paint.setColor(revealed[start] ? COLORS[units[start]] : HIDDEN_LIQUID);
        float top = Math.round(bottom - liquidFillHeight(b, visibleEnd, capacity));
        // Extend the lowest fill past the contour; the glass clip trims it exactly
        // to the rounded bottom, including fractional pixels during animation.
        float base = start == 0 ? (float) Math.ceil(bottom) + LIQUID_BOTTOM_OVERSCAN_PX
                : Math.round(bottom - liquidFillHeight(b, start, capacity));
        float left = b.left + inset, right = b.right - inset;
        boolean surface = visibleEnd >= fill;
        Path liquid = buildUprightBandPath(b, left, right, top, base, depth, surface, start == 0);
        if (revealed[start]) {
            int color = COLORS[units[start]];
            paint.setShader(liquidShader(units[start], left, right));
            c.drawPath(liquid, paint);
            paint.setShader(null);
            if (surface) {
                paint.setColor(shade(color, LIQUID_SURFACE_SHADE));
                c.drawOval(surfaceBounds, paint);
            }
        } else {
            c.drawPath(liquid, paint);
            drawHiddenLabel(c, b.centerX(), (top + base) / 2f,
                    liquidHeight(b, capacity) / capacity * HIDDEN_LABEL_HEIGHT_RATIO);
        }
    }

    private Path buildUprightBandPath(RectF b, float left, float right, float top, float base,
            float depth, boolean surface, boolean lowest) {
        Path liquid = liquidPath;
        liquid.reset();
        liquid.moveTo(left, surface ? top + depth : top);
        if (surface) {
            liquid.lineTo(right, top + depth);
        } else {
            liquid.quadTo(b.centerX(), top + depth * 2, right, top);
        }
        liquid.lineTo(right, base);
        if (lowest) {
            liquid.lineTo(left, base);
        } else {
            liquid.quadTo(b.centerX(), base + depth * 2, left, base);
        }
        liquid.close();
        surfaceBounds.set(left, top, right, top + depth * 2);
        if (surface) {
            liquid.addOval(surfaceBounds, Path.Direction.CW);
        }
        return liquid;
    }

    private float liquidDepth(RectF b, float inset) {
        float rimOverhang = dp(RIM_OVERHANG_DP);
        float rimHalfHeight = dp(RIM_HALF_HEIGHT_DP);
        float innerWidth = b.width() - inset * 2;
        return rimHalfHeight * innerWidth / (innerWidth + rimOverhang * 2);
    }

    private void drawGlassOutline(Canvas c, RectF b, int capacity, boolean highlight) {
        float inset = b.width() * GLASS_INSET_RATIO;
        float rimOverhang = dp(RIM_OVERHANG_DP);
        float rimHalfHeight = dp(RIM_HALF_HEIGHT_DP);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(highlight ? SELECTED_GLASS_STROKE_DP : GLASS_STROKE_DP));
        paint.setColor(capacity == GameConfig.EXTRA_CAPACITY ? EXTRA_UPPER_OUTLINE : highlight ? SELECTED_OUTLINE : GLASS_OUTLINE);
        Path outline = outlinePath;
        outline.reset();
        buildGlassContour(outline, b, inset);
        c.drawPath(outline, paint);
        surfaceBounds.set(b.left + inset - rimOverhang, b.top - rimHalfHeight,
                b.right - inset + rimOverhang, b.top + rimHalfHeight);
        c.drawOval(surfaceBounds, paint);
        if (capacity == GameConfig.EXTRA_CAPACITY) {
            // A pale upper silhouette surrounds a separate one-unit glass compartment.
            float divider = extraCompartmentTop(b);
            paint.setColor(highlight ? SELECTED_OUTLINE : GLASS_OUTLINE);
            c.save();
            c.clipRect(b.left, divider, b.right, b.bottom + dp(RIM_HALF_HEIGHT_DP));
            c.drawPath(outline, paint);
            c.restore();
            surfaceBounds.set(b.left + inset - rimOverhang, divider - rimHalfHeight,
                    b.right - inset + rimOverhang, divider + rimHalfHeight);
            c.drawOval(surfaceBounds, paint);
        }
    }

    private void drawGlassReflection(Canvas c, RectF b, int capacity) {
        paint.setColor(capacity == GameConfig.EXTRA_CAPACITY ? EXTRA_UPPER_REFLECTION : GLASS_REFLECTION);
        paint.setStrokeWidth(dp(GLASS_STROKE_DP));
        c.drawLine(b.left + b.width() * REFLECTION_X_RATIO, b.top + dp(REFLECTION_TOP_OFFSET_DP), b.left + b.width() * REFLECTION_X_RATIO, b.bottom - b.width() * REFLECTION_BOTTOM_OFFSET_RATIO, paint);
        if (capacity == GameConfig.EXTRA_CAPACITY) {
            paint.setColor(GLASS_REFLECTION);
            c.drawLine(b.left + b.width() * REFLECTION_X_RATIO, extraCompartmentTop(b) + dp(EXTRA_REFLECTION_TOP_OFFSET_DP),
                    b.left + b.width() * REFLECTION_X_RATIO, b.bottom - b.width() * REFLECTION_BOTTOM_OFFSET_RATIO, paint);
        }
    }

    private static void buildGlassContour(Path path, RectF b, float inset) {
        float bottomCurve = b.width() * GLASS_BOTTOM_CURVE_RATIO;
        path.moveTo(b.left + inset, b.top);
        path.lineTo(b.left + inset, b.bottom - bottomCurve);
        path.quadTo(b.left + inset, b.bottom, b.centerX(), b.bottom);
        path.quadTo(b.right - inset, b.bottom, b.right - inset, b.bottom - bottomCurve);
        path.lineTo(b.right - inset, b.top);
    }

    private void drawTiltedLiquid(Canvas canvas, RectF bounds, int[] units, boolean[] revealed,
            float fill, int capacity, float angle) {
        configureLiquidGeometry(bounds);
        // Undo only the drawing rotation after clipping the glass. Horizontal bands
        // now follow gravity while the existing glass contour contains every layer.
        canvas.save();
        canvas.rotate(-angle, bounds.centerX(), bounds.bottom);
        float base = 0;
        for (int start = 0; start < units.length && start < fill; ) {
            int end = runEnd(units, revealed, start);
            base = drawTiltedBand(canvas, bounds, units, revealed, fill, capacity, angle, start, end, base);
            start = end;
        }
        canvas.restore();
    }

    private float drawTiltedBand(Canvas canvas, RectF bounds, int[] units, boolean[] revealed,
            float fill, int capacity, float angle, int start, int end, float base) {
        float top = bandSurface(bounds, fill, capacity, angle, end);
        if (start == 0) {
            base = tiltedLiquid.bottom() + 1;
        }
        float left = bounds.centerX() + tiltedLiquid.left() - 1;
        float right = bounds.centerX() + tiltedLiquid.right() + 1;
        paint.setColor(revealed[start] ? COLORS[units[start]] : HIDDEN_LIQUID);
        if (revealed[start]) {
            paint.setShader(liquidShader(units[start], left, right));
        }
        canvas.drawRect(left, bounds.bottom + top, right, bounds.bottom + base, paint);
        paint.setShader(null);
        if (revealed[start] && end >= fill) {
            paint.setColor(shade(COLORS[units[start]], LIQUID_SURFACE_SHADE));
            canvas.drawRect(left, bounds.bottom + top, right, bounds.bottom + Math.min(base, top + dp(1)), paint);
        }
        if (!revealed[start]) {
            float centerY = (top + base) / 2;
            drawHiddenLabel(canvas, bounds.centerX() + tiltedLiquid.centerXAt(centerY),
                    bounds.bottom + centerY,
                    Math.min(liquidHeight(bounds, capacity) / capacity * HIDDEN_LABEL_HEIGHT_RATIO, (base - top) * .8f));
        }
        return top;
    }

    private float bandSurface(RectF bounds, float fill, int capacity, float angle, int end) {
        tiltedLiquid.rotate(0);
        double area = tiltedLiquid.areaBelow(-liquidFillHeight(bounds, Math.min(fill, end), capacity));
        tiltedLiquid.rotate(angle);
        return tiltedLiquid.surfaceForArea(area);
    }

    private static int runEnd(int[] units, boolean[] revealed, int start) {
        int end = start + 1;
        while (end < units.length && revealed[start] && revealed[end] && units[end] == units[start]) {
            end++;
        }
        return end;
    }

    private void drawHiddenLabel(Canvas canvas, float centerX, float centerY, float textSize) {
        hiddenTextPaint.setColor(HIDDEN_LABEL);
        hiddenTextPaint.setTextSize(textSize);
        hiddenTextPaint.getFontMetrics(hiddenFontMetrics);
        canvas.drawText("?", centerX, centerY - (hiddenFontMetrics.ascent + hiddenFontMetrics.descent) / 2f, hiddenTextPaint);
    }

    private static int shade(int color, float factor) {
        return Color.rgb(Math.min(COLOR_CHANNEL_MAX, Math.round(Color.red(color) * factor)),
                Math.min(COLOR_CHANNEL_MAX, Math.round(Color.green(color) * factor)),
                Math.min(COLOR_CHANNEL_MAX, Math.round(Color.blue(color) * factor)));
    }

    private LinearGradient liquidShader(int colorIndex, float left, float right) {
        float width = right - left;
        if (Float.compare(shaderWidth, width) != 0) {
            java.util.Arrays.fill(liquidShaders, null);
            shaderWidth = width;
        }
        LinearGradient shader = liquidShaders[colorIndex];
        if (shader == null) {
            int color = COLORS[colorIndex];
            shader = new LinearGradient(0, 0, width, 0,
                    new int[]{shade(color, LIQUID_LEFT_SHADE), shade(color, LIQUID_HIGHLIGHT_SHADE), color, shade(color, LIQUID_RIGHT_SHADE)},
                    LIQUID_GRADIENT_STOPS, Shader.TileMode.CLAMP);
            liquidShaders[colorIndex] = shader;
        }
        shaderPosition.setTranslate(left, 0);
        shader.setLocalMatrix(shaderPosition);
        return shader;
    }

    private float liquidHeight(RectF bounds, int capacity) {
        float height = bounds.height() * LIQUID_HEIGHT_RATIO;
        return capacity == GameConfig.EXTRA_CAPACITY
                ? height / GameConfig.CAPACITY + bounds.width() * BOTTOM_LAYER_COMPENSATION_WIDTH_RATIO
                : height;
    }

    private float liquidFillHeight(RectF bounds, float units, int capacity) {
        float height = liquidHeight(bounds, capacity) * units / capacity;
        if (capacity > GameConfig.EXTRA_CAPACITY) {
            // Rounded glass removes visible area at the bottom. Redistribute a small
            // amount of height to that layer while preserving the full-tube surface.
            float compensation = bounds.width() * BOTTOM_LAYER_COMPENSATION_WIDTH_RATIO;
            height += compensation * Math.min(units, 1)
                    - compensation * Math.max(0, units - 1) / (capacity - 1);
        }
        return height;
    }

    private float extraCompartmentTop(RectF bounds) {
        float ordinaryLiquidHeight = liquidHeight(bounds, GameConfig.CAPACITY);
        float headroom = bounds.height() - ordinaryLiquidHeight;
        return bounds.bottom - liquidHeight(bounds, GameConfig.EXTRA_CAPACITY) - headroom;
    }

    private float selectionLift(int index) {
        RectF box = boxes[index];
        float lift = Math.min(dp(SELECTION_LIFT_DP), box.width() * SELECTION_LIFT_WIDTH_RATIO);
        if (box.top > boxes[0].top) {
            lift = Math.min(lift, selectionRowClearance);
        }
        return Math.max(0, Math.min(lift, box.top - dp(SELECTION_TOP_CLEARANCE_DP)));
    }

    private RectF displayedBox(int index) {
        RectF box = displayedBounds;
        box.set(boxes[index]);
        box.offset(0, -selectionOffsets[index] * selectionLift(index));
        return box;
    }

    private void selectTube(int index) {
        if (selected == index) {
            return;
        }
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }
        selected = index;
        float[] from = selectionOffsets.clone();
        selectionAnimator = ValueAnimator.ofFloat(0, 1);
        selectionAnimator.setDuration(SELECTION_DURATION_MILLIS);
        selectionAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        selectionAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            for (int i = 0; i < selectionOffsets.length; i++) {
                float target = i == selected ? 1 : 0;
                selectionOffsets[i] = from[i] + (target - from[i]) * fraction;
            }
            invalidate();
        });
        selectionAnimator.start();
    }

    private void clearSelection() {
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }
        selected = NO_SELECTION;
        java.util.Arrays.fill(selectionOffsets, 0);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        performClick();
        if (busy || !inputEnabled || state == null || state.solved()) {
            return true;
        }
        for (int i = 0; i < boxes.length; i++) {
            RectF touch = touchBounds(i);
            if (touch.contains(e.getX(), e.getY())) {
                handleTubeTap(i);
                invalidate();
                break;
            }
        }
        return true;
    }

    private RectF touchBounds(int index) {
        RectF touch = displayedBox(index);
        touch.union(boxes[index]);
        touch.inset(-dp(TOUCH_HORIZONTAL_EXPANSION_DP), -dp(TOUCH_VERTICAL_EXPANSION_DP));
        if (index == boxes.length - 1 && touch.height() < dp(MIN_TOUCH_HEIGHT_DP)) {
            float expand = (dp(MIN_TOUCH_HEIGHT_DP) - touch.height()) / 2;
            touch.inset(0, -expand);
        }
        return touch;
    }

    private void handleTubeTap(int index) {
        if (index == state.count()) {
            selectTube(NO_SELECTION);
            if (extraWaitMillis == 0) {
                listener.unlockExtra();
            }
        } else if (selected == index) {
            selectTube(NO_SELECTION);
        } else if (selected >= 0 && Rules.amount(state, new Move(selected, index)) > 0) {
            listener.pour(new Move(selected, index));
            selectTube(NO_SELECTION);
        } else {
            selectTube(state.size(index) > 0 ? index : NO_SELECTION);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    void animatePour(GameState old, GameState next, Move m, Runnable done) {
        pourStartLift = selectionOffsets[m.source()] * selectionLift(m.source());
        clearSelection();
        progress = 0;
        before = old;
        state = next;
        currentUnits = next.copy();
        currentVisibility = next.revealedCopy();
        previousUnits = old.copy();
        previousVisibility = old.revealedCopy();
        pourAmount = Rules.amount(old, m);
        moving = m;
        busy = true;
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(POUR_DURATION_MILLIS);
        animator.addUpdateListener(a -> {
                    progress = (float) a.getAnimatedValue();
                    invalidate();
                }
        );
        animator.addListener(new AnimatorListenerAdapter() {
                                 @Override
                                 public void onAnimationEnd(@NonNull Animator a) {
                                     busy = false;
                                     before = null;
                                     if (next.completeTube(m.destination()) && !old.completeTube(m.destination())) {
                                         completionStarted[m.destination()] = android.os.SystemClock.uptimeMillis();
                                     }
                                     invalidate();
                                     done.run();
                                 }
                             }
        );
        animator.start();
    }

    void stopAnimation() {
        clearSelection();
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
        busy = false;
    }
}
