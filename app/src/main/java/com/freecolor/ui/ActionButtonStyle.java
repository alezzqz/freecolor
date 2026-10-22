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

import android.graphics.Typeface;
import android.widget.Button;
import com.freecolor.R;

/** Shared appearance for text actions on the game and result screens. */
final class ActionButtonStyle {
    static final int TEXT_COLOR = 0xff350c64;
    static final int SECONDARY_TEXT_COLOR = 0xff633c6b;

    private ActionButtonStyle() {}

    static void apply(Button button) {
        button.setAllCaps(false);
        button.setTextColor(TEXT_COLOR);
        button.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        button.setBackgroundResource(R.drawable.continue_background);
        button.setElevation(0);
        button.setPadding(0, 0, 0, 0);
    }
}

