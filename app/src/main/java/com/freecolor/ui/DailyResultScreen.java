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
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.*;
import com.freecolor.R;
import com.freecolor.core.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Scrollable result content also fits landscape windows and large accessibility fonts. */
// Created only in Java with required callbacks and game data; never inflated from XML.
@SuppressLint("ViewConstructor")
final class DailyResultScreen extends ScrollView {
    DailyResultScreen(Context context, DailySession session, DailyProgress progress,
                      Runnable retry, Runnable ordinary, Runnable today) {
        super(context);
        setFillViewport(true);
        setBackgroundColor(0xff171d35);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        addView(content, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        text(content, session.successful() ? "Задача дня решена!" : "Уровень пройден", 26, true);
        text(content, session.challenge().date().format(
                DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))), 16, false);
        ImageView kitten = new ImageView(context);
        kitten.setImageResource(R.drawable.victory_kitten);
        kitten.setScaleType(ImageView.ScaleType.FIT_CENTER);
        kitten.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        content.addView(kitten, new LinearLayout.LayoutParams(dp(170), dp(170)));
        text(content, "Ходов: " + session.moves() + " · цель: " + session.challenge().targetMoves() + " или меньше"
                + "\nЛучший результат: " + session.bestMoves(), 18, false);
        if (!session.successful()) {
            text(content, "Попробуйте ещё раз, чтобы уложиться в цель.", 16, false);
        }
        int days = progress.weekCount(LocalDate.now());
        boolean goalReached = days == GameConfig.DAILY_WEEK_GOAL;
        text(content, goalReached ? "Недельная цель выполнена!"
                : "Задач за неделю: " + days + " из " + GameConfig.DAILY_WEEK_GOAL, 18, true);
        if (retry != null) {
            button(content, session.successful() ? "Повторить" : "Попробовать снова", retry);
        }
        if (today != null) {
            button(content, "Задача на сегодня", today);
        }
        button(content, "Обычная игра", ordinary);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void text(LinearLayout parent, String value, int size, boolean bold) {
        TextView label = new TextView(getContext());
        label.setText(value);
        label.setTextColor(Color.WHITE);
        label.setTextSize(size);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(8), 0, dp(8));
        if (bold) {
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        parent.addView(label);
    }

    private void button(LinearLayout parent, String title, Runnable action) {
        Button button = new Button(getContext());
        button.setText(title);
        ActionButtonStyle.apply(button);
        button.setMaxLines(1);
        button.setPadding(dp(12), 0, dp(12), 0);
        // Match the continue action, shrinking longer labels to fit the available space.
        button.setAutoSizeTextTypeUniformWithConfiguration(18, 29, 1, TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(56));
        params.topMargin = dp(12);
        parent.addView(button, params);
        button.setOnClickListener(view -> {
            button.setEnabled(false);
            action.run();
        });
    }
}

