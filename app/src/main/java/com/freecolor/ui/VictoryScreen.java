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
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.*;
import android.view.Gravity;

import androidx.annotation.NonNull;

/**
 * Original vector-like scenery inspired by the supplied completion-screen composition.
 */
final class VictoryScreen extends FrameLayout {
    // Layout dimensions (dp/sp), responsive ratios and original scenery palette.
    private static final float MAX_BUTTON_WIDTH_DP = 360f;
    private static final float MAX_BUTTON_HEIGHT_DP = 72f;
    private static final float HEADING_HEIGHT_DP = 100f;
    private static final float BUTTON_WIDTH_RATIO = .72f;
    private static final float BUTTON_HEIGHT_RATIO = .09f;
    private static final float MAX_HEADING_TEXT_SP = 64f;
    private static final float HEADING_TEXT_WIDTH_RATIO = .15f;
    private static final float MAX_BUTTON_TEXT_SP = 29f;
    private static final float BUTTON_TEXT_WIDTH_RATIO = .10f;
    private static final float HEADING_CENTER_HEIGHT_RATIO = .27f;
    private static final float BUTTON_CENTER_HEIGHT_RATIO = .82f;
    private static final float MOON_RADIUS_DP = 27f;
    private static final float MOON_RIGHT_OFFSET_DP = 18f;
    private static final float MOON_BOTTOM_OFFSET_DP = 51f;
    private static final float MOON_HALO_RADIUS_RATIO = 1.6f;
    private static final float MOON_HALO_MIDDLE_STOP = .6f;
    private static final float MOON_CRATER_LEFT_OFFSET_DP = 26f;
    private static final float MOON_CRATER_TOP_OFFSET_DP = 56f;
    private static final float MOON_SMALL_CRATER_RADIUS_DP = 7f;
    private static final float MOON_CRATER_RIGHT_OFFSET_DP = 9f;
    private static final float MOON_CRATER_BOTTOM_OFFSET_DP = 42f;
    private static final float MOON_LARGE_CRATER_RADIUS_DP = 9f;
    private static final float CLOUD_RIGHT_OFFSET_DP = 7f;
    private static final float CLOUD_BOTTOM_OFFSET_DP = 2f;
    private static final float CLOUD_GRADIENT_TOP_DP = 28f;
    private static final float CLOUD_GRADIENT_BOTTOM_DP = 10f;
    private static final float CLOUD_LEFT_LOBE_OFFSET_DP = 21f;
    private static final float CLOUD_LOBE_RADIUS_DP = 14f;
    private static final float CLOUD_CENTER_LOBE_OFFSET_DP = 9f;
    private static final float CLOUD_CENTER_RADIUS_DP = 23f;
    private static final float CLOUD_RIGHT_LOBE_OFFSET_DP = 24f;
    private static final float CLOUD_BASE_LEFT_OFFSET_DP = 33f;
    private static final float CLOUD_BASE_RIGHT_OFFSET_DP = 36f;
    private static final float CLOUD_BASE_HEIGHT_DP = 12f;
    private static final float CLOUD_BASE_CORNER_DP = 10f;
    private static final float LANTERN_LARGE_X_OFFSET_DP = 5f;
    private static final float LANTERN_LARGE_Y_OFFSET_DP = 4f;
    private static final float LANTERN_LARGE_RADIUS_DP = 14f;
    private static final float LANTERN_SMALL_X_OFFSET_DP = 27f;
    private static final float LANTERN_SMALL_Y_OFFSET_DP = 13f;
    private static final float LANTERN_SMALL_RADIUS_DP = 9f;
    private static final float LANTERN_SHOULDER_RATIO = 1.7f;
    private static final float LANTERN_CROWN_RATIO = 3f;
    private static final float LANTERN_BASE_WIDTH_RATIO = .5f;
    private static final float LANTERN_BASE_CURVE_RATIO = 1.4f;
    private static final float LANTERN_GRADIENT_HEIGHT_RATIO = 2f;
    private static final float LANTERN_RIM_TOP_RATIO = .75f;
    private static final float LANTERN_RIM_BOTTOM_RATIO = 1.1f;
    private static final int MOON_HALO_CENTER = 0xfffff7b7;
    private static final int MOON_HALO_MIDDLE = 0xffffe579;
    private static final int MOON_HALO_EDGE = 0x00ffdf75;
    private static final int MOON_COLOR = 0xffffee95;
    private static final int MOON_CRATER_COLOR = 0xffffd26f;
    private static final int CLOUD_TOP_COLOR = 0xfff1e7ff;
    private static final int CLOUD_BOTTOM_COLOR = 0xff8d60df;
    private static final int LANTERN_TOP_COLOR = 0xffff741d;
    private static final int LANTERN_BOTTOM_COLOR = 0xffffe36a;
    private static final int LANTERN_RIM_COLOR = 0xfffff6c0;
    private static final int BACKGROUND_TOP_COLOR = 0xff12132f;
    private static final int BACKGROUND_BOTTOM_COLOR = 0xff10112b;
    private static final int RIBBON_START_COLOR = 0xff351065;
    private static final int RIBBON_END_COLOR = 0xff241047;
    private static final int SWIRL_CENTER_COLOR = 0xff700ba4;
    private static final int SWIRL_MIDDLE_COLOR = 0xff421077;
    private static final int SWIRL_EDGE_COLOR = 0xff291055;
    private static final float RIBBON_START_HEIGHT_RATIO = .07f;
    private static final float RIBBON_GRADIENT_HEIGHT_RATIO = .2f;
    private static final float SWIRL_START_HEIGHT_RATIO = .25f;
    private static final float SWIRL_RIGHT_HEIGHT_RATIO = .75f;
    private static final float SWIRL_GLOW_X_RATIO = .54f;
    private static final float SWIRL_GLOW_Y_RATIO = .62f;
    private static final float SWIRL_GLOW_RADIUS_RATIO = .55f;
    private static final float[] RIBBON_UPPER_CURVE = {.20f, .04f, .42f, .08f, .51f, .12f};
    private static final float[] RIBBON_LOWER_CURVE = {.17f, .10f, .05f, .14f, 0, .19f};
    private static final float[] SWIRL_UPPER_CURVE = {.27f, .11f, .70f, .08f, 1, .23f};
    private static final float[] SWIRL_LOWER_CURVE = {1, .93f, .63f, .96f, .35f, .86f};
    private static final float[] SWIRL_LEFT_CURVE = {.08f, .79f, -.08f, .79f, -.10f, .60f};


    private final TextView heading;
    private static final float MAX_KITTEN_SIZE_DP = 340f;
    private static final float KITTEN_WIDTH_RATIO = .76f;
    private static final float KITTEN_HEIGHT_RATIO = .38f;
    private static final float KITTEN_CENTER_HEIGHT_RATIO = .55f;
    private static final float KITTEN_GLOW_WIDTH_RATIO = .43f;
    private static final float KITTEN_GLOW_HEIGHT_RATIO = .23f;
    private static final int KITTEN_GLOW_CENTER = 0xaac8a6ee;
    private static final int KITTEN_GLOW_MIDDLE = 0x607f52b5;
    private static final int KITTEN_GLOW_EDGE = 0x007f52b5;
    private final ImageView kitten;
    private final Button continueButton;
    private final View decorations;

    /** Constructor for layout tools; gameplay supplies the Continue handler below. */
    public VictoryScreen(Context context) {
        this(context, () -> { });
        continueButton.setEnabled(false);
    }

    VictoryScreen(Context context, Runnable onContinue) {
        super(context);
        setBackground(new Scenery());
        heading = new TextView(context);
        heading.setText("Отлично!");
        heading.setTextColor(Color.WHITE);
        heading.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        heading.setGravity(Gravity.CENTER);
        addView(heading);
        kitten = new ImageView(context);
        kitten.setImageResource(com.freecolor.R.drawable.victory_kitten);
        kitten.setScaleType(ImageView.ScaleType.FIT_CENTER);
        kitten.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(kitten);
        continueButton = new Button(context);
        continueButton.setText("Продолжить");
        ActionButtonStyle.apply(continueButton);
        continueButton.setOnClickListener(v -> {
            continueButton.setEnabled(false);
            continueButton.setText("Подождите…");
            onContinue.run();
        });
        addView(continueButton);
        decorations = new View(context) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(@NonNull Canvas canvas) {
                super.onDraw(canvas);
                drawDecorations(canvas, paint);
            }
        };
        decorations.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(decorations);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        int width = getMeasuredWidth(), height = getMeasuredHeight();
        int buttonWidth = (int) Math.min(dp(MAX_BUTTON_WIDTH_DP), width * BUTTON_WIDTH_RATIO);
        int buttonHeight = (int) Math.min(dp(MAX_BUTTON_HEIGHT_DP), height * BUTTON_HEIGHT_RATIO);
        int kittenSize = (int) Math.min(dp(MAX_KITTEN_SIZE_DP),
                Math.min(width * KITTEN_WIDTH_RATIO, height * KITTEN_HEIGHT_RATIO));
        kitten.measure(MeasureSpec.makeMeasureSpec(kittenSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(kittenSize, MeasureSpec.EXACTLY));
        heading.setTextSize(Math.min(MAX_HEADING_TEXT_SP, width / getResources().getDisplayMetrics().density * HEADING_TEXT_WIDTH_RATIO));
        continueButton.setTextSize(Math.min(MAX_BUTTON_TEXT_SP, buttonWidth / getResources().getDisplayMetrics().density * BUTTON_TEXT_WIDTH_RATIO));
        heading.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) dp(HEADING_HEIGHT_DP), MeasureSpec.EXACTLY));
        continueButton.measure(MeasureSpec.makeMeasureSpec(buttonWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(buttonHeight, MeasureSpec.EXACTLY));
        decorations.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = getWidth(), height = getHeight();
        int headingTop = (int) (height * HEADING_CENTER_HEIGHT_RATIO - heading.getMeasuredHeight() / 2f);
        heading.layout(0, headingTop, width, headingTop + heading.getMeasuredHeight());
        int kittenLeft = (width - kitten.getMeasuredWidth()) / 2;
        int kittenTop = (int) (height * KITTEN_CENTER_HEIGHT_RATIO - kitten.getMeasuredHeight() / 2f);
        kitten.layout(kittenLeft, kittenTop, kittenLeft + kitten.getMeasuredWidth(),
                kittenTop + kitten.getMeasuredHeight());
        int buttonLeft = (width - continueButton.getMeasuredWidth()) / 2;
        int buttonTop = (int) (height * BUTTON_CENTER_HEIGHT_RATIO - continueButton.getMeasuredHeight() / 2f);
        continueButton.layout(buttonLeft, buttonTop, buttonLeft + continueButton.getMeasuredWidth(), buttonTop + continueButton.getMeasuredHeight());
        decorations.layout(0, 0, width, height);
    }

    void allowRetry() {
        continueButton.setEnabled(true);
        continueButton.setText("Продолжить");
    }

    private void drawDecorations(Canvas canvas, Paint p) {
        if (continueButton.getWidth() == 0) {
            return;
        }
        float x = continueButton.getRight(), y = continueButton.getBottom();
        float radius = dp(MOON_RADIUS_DP);
        p.setShader(new RadialGradient(x - dp(MOON_RIGHT_OFFSET_DP), y - dp(MOON_BOTTOM_OFFSET_DP), radius * MOON_HALO_RADIUS_RATIO, new int[]{MOON_HALO_CENTER, MOON_HALO_MIDDLE, MOON_HALO_EDGE}, new float[]{0, MOON_HALO_MIDDLE_STOP, 1}, Shader.TileMode.CLAMP));
        canvas.drawCircle(x - dp(MOON_RIGHT_OFFSET_DP), y - dp(MOON_BOTTOM_OFFSET_DP), radius * MOON_HALO_RADIUS_RATIO, p);
        p.setShader(null);
        p.setColor(MOON_COLOR);
        canvas.drawCircle(x - dp(MOON_RIGHT_OFFSET_DP), y - dp(MOON_BOTTOM_OFFSET_DP), radius, p);
        p.setColor(MOON_CRATER_COLOR);
        canvas.drawCircle(x - dp(MOON_CRATER_LEFT_OFFSET_DP), y - dp(MOON_CRATER_TOP_OFFSET_DP), dp(MOON_SMALL_CRATER_RADIUS_DP), p);
        canvas.drawCircle(x - dp(MOON_CRATER_RIGHT_OFFSET_DP), y - dp(MOON_CRATER_BOTTOM_OFFSET_DP), dp(MOON_LARGE_CRATER_RADIUS_DP), p);
        float cloudX = x - dp(CLOUD_RIGHT_OFFSET_DP), cloudY = y + dp(CLOUD_BOTTOM_OFFSET_DP);
        p.setShader(new LinearGradient(0, cloudY - dp(CLOUD_GRADIENT_TOP_DP), 0, cloudY + dp(CLOUD_GRADIENT_BOTTOM_DP), CLOUD_TOP_COLOR, CLOUD_BOTTOM_COLOR, Shader.TileMode.CLAMP));
        canvas.drawCircle(cloudX - dp(CLOUD_LEFT_LOBE_OFFSET_DP), cloudY, dp(CLOUD_LOBE_RADIUS_DP), p);
        canvas.drawCircle(cloudX, cloudY - dp(CLOUD_CENTER_LOBE_OFFSET_DP), dp(CLOUD_CENTER_RADIUS_DP), p);
        canvas.drawCircle(cloudX + dp(CLOUD_RIGHT_LOBE_OFFSET_DP), cloudY, dp(CLOUD_LOBE_RADIUS_DP), p);
        canvas.drawRoundRect(cloudX - dp(CLOUD_BASE_LEFT_OFFSET_DP), cloudY, cloudX + dp(CLOUD_BASE_RIGHT_OFFSET_DP), cloudY + dp(CLOUD_BASE_HEIGHT_DP), dp(CLOUD_BASE_CORNER_DP), dp(CLOUD_BASE_CORNER_DP), p);
        p.setShader(null);
        // Warm paper lanterns alongside the button, without currencies or rewards.
        lantern(canvas, p, continueButton.getLeft() + dp(LANTERN_LARGE_X_OFFSET_DP), continueButton.getTop() + dp(LANTERN_LARGE_Y_OFFSET_DP), dp(LANTERN_LARGE_RADIUS_DP));
        lantern(canvas, p, continueButton.getLeft() + dp(LANTERN_SMALL_X_OFFSET_DP), continueButton.getTop() - dp(LANTERN_SMALL_Y_OFFSET_DP), dp(LANTERN_SMALL_RADIUS_DP));
    }

    private void lantern(Canvas c, Paint p, float x, float y, float radius) {
        Path shape = new Path();
        shape.moveTo(x - radius, y - radius);
        shape.cubicTo(x - radius * LANTERN_SHOULDER_RATIO, y - radius * LANTERN_CROWN_RATIO, x + radius * LANTERN_SHOULDER_RATIO, y - radius * LANTERN_CROWN_RATIO, x + radius, y - radius);
        shape.lineTo(x + radius * LANTERN_BASE_WIDTH_RATIO, y + radius);
        shape.quadTo(x, y + radius * LANTERN_BASE_CURVE_RATIO, x - radius * LANTERN_BASE_WIDTH_RATIO, y + radius);
        shape.close();
        p.setShader(new LinearGradient(x, y - radius * LANTERN_GRADIENT_HEIGHT_RATIO, x, y + radius, LANTERN_TOP_COLOR, LANTERN_BOTTOM_COLOR, Shader.TileMode.CLAMP));
        c.drawPath(shape, p);
        p.setShader(null);
        p.setColor(LANTERN_RIM_COLOR);
        c.drawOval(x - radius * LANTERN_BASE_WIDTH_RATIO, y + radius * LANTERN_RIM_TOP_RATIO, x + radius * LANTERN_BASE_WIDTH_RATIO, y + radius * LANTERN_RIM_BOTTOM_RATIO, p);
    }

    /**
     * Normalized x/y control points and endpoint of one cubic artwork contour.
     */
    private static void normalizedCurve(Path path, float width, float height, float[] curve) {
        path.cubicTo(width * curve[0], height * curve[1], width * curve[2], height * curve[3], width * curve[4], height * curve[5]);
    }

    private static final class Scenery extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width(), h = b.height();
            paint.setShader(new LinearGradient(0, 0, 0, h, BACKGROUND_TOP_COLOR, BACKGROUND_BOTTOM_COLOR, Shader.TileMode.CLAMP));
            canvas.drawRect(b, paint);
            Path ribbon = new Path();
            ribbon.moveTo(0, h * RIBBON_START_HEIGHT_RATIO);
            normalizedCurve(ribbon, w, h, RIBBON_UPPER_CURVE);
            normalizedCurve(ribbon, w, h, RIBBON_LOWER_CURVE);
            ribbon.close();
            paint.setShader(new LinearGradient(0, 0, w, h * RIBBON_GRADIENT_HEIGHT_RATIO, RIBBON_START_COLOR, RIBBON_END_COLOR, Shader.TileMode.CLAMP));
            canvas.drawPath(ribbon, paint);
            Path swirl = new Path();
            swirl.moveTo(0, h * SWIRL_START_HEIGHT_RATIO);
            normalizedCurve(swirl, w, h, SWIRL_UPPER_CURVE);
            swirl.lineTo(w, h * SWIRL_RIGHT_HEIGHT_RATIO);
            normalizedCurve(swirl, w, h, SWIRL_LOWER_CURVE);
            normalizedCurve(swirl, w, h, SWIRL_LEFT_CURVE);
            swirl.close();
            paint.setShader(new RadialGradient(w * SWIRL_GLOW_X_RATIO, h * SWIRL_GLOW_Y_RATIO, h * SWIRL_GLOW_RADIUS_RATIO, new int[]{SWIRL_CENTER_COLOR, SWIRL_MIDDLE_COLOR, SWIRL_EDGE_COLOR}, null, Shader.TileMode.CLAMP));
            canvas.drawPath(swirl, paint);
            paint.setShader(null);
            // A soft violet spotlight separates the black kitten from the existing scenery.
            float glowRadius = Math.min(w * KITTEN_GLOW_WIDTH_RATIO, h * KITTEN_GLOW_HEIGHT_RATIO);
            paint.setShader(new RadialGradient(w / 2f, h * KITTEN_CENTER_HEIGHT_RATIO, glowRadius,
                    new int[]{KITTEN_GLOW_CENTER, KITTEN_GLOW_MIDDLE, KITTEN_GLOW_EDGE}, null,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(w / 2f, h * KITTEN_CENTER_HEIGHT_RATIO, glowRadius, paint);
            paint.setShader(null);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
        }

        // Drawable still requires this legacy override on the supported SDK.
        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}
