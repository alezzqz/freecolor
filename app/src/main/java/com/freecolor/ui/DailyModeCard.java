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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.freecolor.R;

/** Keeps mode navigation and weekly progress together, apart from the board controls. */
// Created only in Java with required callbacks and game data; never inflated from XML.
@SuppressLint("ViewConstructor")
final class DailyModeCard extends LinearLayout {
    private final ImageView icon;
    private final TextView title;
    private final TextView subtitle;
    private final TextView week;
    private final TextView dots;

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Text gets priority over ornament on narrow screens and with larger system fonts.
        boolean compact = MeasureSpec.getSize(widthSpec) < dp(350)
                || getResources().getConfiguration().fontScale > 1.3f;
        icon.setVisibility(compact ? View.GONE : View.VISIBLE);
        LayoutParams labels = (LayoutParams) ((View) title.getParent()).getLayoutParams();
        labels.leftMargin = compact ? 0 : dp(12);
        super.onMeasure(widthSpec, heightSpec);
    }

    DailyModeCard(Context context, Runnable action) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(16), dp(14), dp(16), dp(14));
        setMinimumHeight(dp(80));
        setBackgroundResource(R.drawable.daily_card_background);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(view -> action.run());

        icon = new ImageView(context);
        icon.setBackgroundResource(R.drawable.daily_icon_background);
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        addView(icon, new LayoutParams(dp(40), dp(40)));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(VERTICAL);
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        labelParams.leftMargin = dp(12);
        labelParams.rightMargin = dp(8);
        addView(labels, labelParams);
        title = label(16, 0xfff5efff);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labels.addView(title);
        subtitle = label(12, 0xffaaa7c5);
        subtitle.setPadding(0, dp(4), 0, 0);
        labels.addView(subtitle);

        LinearLayout progress = new LinearLayout(context);
        progress.setOrientation(VERTICAL);
        progress.setGravity(Gravity.END);
        addView(progress, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        week = label(11, 0xffaaa7c5);
        progress.addView(week);
        dots = label(14, 0xffffd775);
        dots.setPadding(0, dp(4), 0, 0);
        dots.setLetterSpacing(.10f);
        progress.addView(dots);
        // The entire card is one navigation target, with a combined spoken description.
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        labels.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    void update(boolean daily, boolean completed, int days, int goal, long level) {
        icon.setImageResource(daily ? R.drawable.ic_daily_back : R.drawable.ic_daily_challenge);
        title.setText(daily ? "Обычная игра" : "Задача дня");
        subtitle.setText(daily ? "Вернуться к уровню " + level
                : completed ? "На сегодня выполнено" : "Новая головоломка каждый день");
        week.setText(getResources().getString(R.string.daily_week_count, days, goal));
        StringBuilder indicators = new StringBuilder();
        for (int i = 0; i < goal; i++) {
            if (i > 0) {
                indicators.append(" ");
            }
            indicators.append(i < days ? "●" : "○");
        }
        dots.setText(indicators);
        setContentDescription(title.getText() + ". " + subtitle.getText()
                + ". На этой неделе решено " + days + " из " + goal);
        setAlpha(isEnabled() ? 1f : .5f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private TextView label(int size, int color) {
        TextView view = new TextView(getContext());
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }
}

