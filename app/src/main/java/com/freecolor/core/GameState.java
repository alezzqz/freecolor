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

import java.io.Serializable;
import java.util.*;

/**
 * Immutable bottom-to-top liquid segments.
 */
public final class GameState implements Serializable {
    public static final int CAPACITY = GameConfig.CAPACITY;
    private final int[][] tubes;
    public final boolean extra;
    public final boolean hiddenColors;
    private final boolean[][] revealed;

    public GameState(int[][] tubes, boolean extra) {
        this(tubes, extra, false, null);
    }

    public GameState(int[][] tubes, boolean extra, boolean hiddenColors, boolean[][] revealed) {
        if (tubes.length < 1 || tubes.length > GameConfig.MAX_TUBES) {
            throw new IllegalArgumentException("Tube count");
        }

        this.tubes = new int[tubes.length][];
        this.extra = extra;
        this.hiddenColors = hiddenColors;
        if (revealed != null && revealed.length != tubes.length) {
            throw new IllegalArgumentException("Visibility tube count");
        }

        this.revealed = new boolean[tubes.length][];
        for (int i = 0; i < tubes.length; i++) {
            if (tubes[i].length > capacity(i)) {
                throw new IllegalArgumentException("Overflow");
            }

            this.tubes[i] = tubes[i].clone();
            if (revealed != null && revealed[i].length != tubes[i].length) {
                throw new IllegalArgumentException("Visibility segment count");
            }

            this.revealed[i] = revealed == null ? new boolean[tubes[i].length] : revealed[i].clone();
            if (!hiddenColors) {
                Arrays.fill(this.revealed[i], true);
            }
            for (int c : tubes[i]) {
                if (c < 0 || c >= GameConfig.MAX_COLORS) {
                    throw new IllegalArgumentException("Color");
                }
            }
            // Only the exposed top unit is revealed, even if lower colors match.
            if (tubes[i].length > 0) {
                this.revealed[i][tubes[i].length - 1] = true;
            }
        }
    }

    public GameState withHiddenColors(boolean hidden) {
        return new GameState(copy(), extra, hidden, null);
    }

    public boolean[] revealedTube(int index) {
        return revealed[index].clone();
    }

    public boolean[][] revealedCopy() {
        boolean[][] result = new boolean[count()][];
        for (int i = 0; i < count(); i++) {
            result[i] = revealedTube(i);
        }
        return result;
    }

    public int count() {
        return tubes.length;
    }

    public int capacity(int i) {
        return extra && i == tubes.length - 1 ? GameConfig.EXTRA_CAPACITY : CAPACITY;
    }

    public int[] tube(int i) {
        return tubes[i].clone();
    }

    public int size(int i) {
        return tubes[i].length;
    }

    public int top(int i) {
        return size(i) == 0 ? -1 : tubes[i][size(i) - 1];
    }

    public int[][] copy() {
        int[][] a = new int[count()][];
        for (int i = 0; i < count(); i++) {
            a[i] = tube(i);
        }
        return a;
    }

    public boolean solved() {
        for (int i = 0; i < count(); i++) {
            if (size(i) != 0 && !completeTube(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean completeTube(int index) {
        int[] tube = tubes[index];
        if (tube.length != CAPACITY) {
            return false;
        }
        for (int color : tube) {
            if (color != tube[0]) {
                return false;
            }
        }
        return true;
    }

    public GameState addTube() {
        if (extra) {
            throw new IllegalStateException("Already added");
        }
        int[][] a = Arrays.copyOf(copy(), count() + 1);
        a[count()] = new int[0];
        boolean[][] known = Arrays.copyOf(revealedCopy(), count() + 1);
        known[count()] = new boolean[0];
        return new GameState(a, true, hiddenColors, known);
    }

    public String canonical() {
        String[] a = new String[count()];
        for (int i = 0; i < count(); i++) {
            a[i] = capacity(i) + ":" + Arrays.toString(tubes[i]);
        }
        Arrays.sort(a);
        return Arrays.toString(a);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GameState s && extra == s.extra && hiddenColors == s.hiddenColors && Arrays.deepEquals(tubes, s.tubes) && Arrays.deepEquals(revealed, s.revealed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.deepHashCode(tubes), extra, hiddenColors, Arrays.deepHashCode(revealed));
    }
}

