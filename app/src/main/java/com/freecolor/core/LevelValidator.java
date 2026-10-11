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

package com.freecolor.core;

/**
 * Structural validation before a candidate may enter solver search.
 */
public final class LevelValidator {
    private LevelValidator() {
    }

    public static boolean validInitial(GameState state) {
        int colors = state.count() - GameConfig.EMPTY_TUBES;
        if (state.extra || colors < GameConfig.MIN_COLORS || colors > GameConfig.MAX_COLORS || state.solved()) {
            return false;
        }
        int[] counts = new int[colors];
        for (int i = 0; i < state.count(); i++) {
            if (state.size(i) != (i < colors ? GameState.CAPACITY : 0)) {
                return false;
            }
            for (int color : state.tube(i)) {
                if (color >= colors) {
                    return false;
                }
                counts[color]++;
            }
        }
        for (int count : counts) {
            if (count != GameState.CAPACITY) {
                return false;
            }
        }
        return true;
    }

    public static boolean compatible(GameState original, GameState current) {
        if (original.hiddenColors != current.hiddenColors) {
            return false;
        }
        if (current.count() != original.count() + (current.extra ? 1 : 0)) {
            return false;
        }
        int[] expected = new int[GameConfig.MAX_COLORS], actual = new int[GameConfig.MAX_COLORS];
        for (int[] tube : original.copy()) {
            for (int color : tube) {
                expected[color]++;
            }
        }
        for (int[] tube : current.copy()) {
            for (int color : tube) {
                actual[color]++;
            }
        }
        return java.util.Arrays.equals(expected, actual);
    }
}

