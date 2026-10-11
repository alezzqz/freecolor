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

import java.util.Arrays;

/**
 * Single authoritative implementation used by UI, generator verification and search.
 */
public final class Rules {
    private Rules() {
    }

    public static int amount(GameState s, Move m) {
        int a = m.source(), b = m.destination();
        if (a < 0 || b < 0 || a >= s.count() || b >= s.count() || a == b || s.size(a) == 0 || s.size(b) == s.capacity(b)) {
            return 0;
        }
        if (s.size(b) > 0 && s.top(a) != s.top(b)) {
            return 0;
        }
        int[] t = s.tube(a);
        int n = 1;
        while (n < t.length && t[t.length - n - 1] == s.top(a)) {
            n++;
        }
        return Math.min(n, s.capacity(b) - s.size(b));
    }

    public static GameState pour(GameState s, Move m) {
        int n = amount(s, m);
        if (n == 0) {
            throw new IllegalArgumentException("Illegal pour");
        }
        int[][] a = s.copy();
        int color = s.top(m.source()), len = a[m.destination()].length;
        a[m.source()] = Arrays.copyOf(a[m.source()], a[m.source()].length - n);
        a[m.destination()] = Arrays.copyOf(a[m.destination()], len + n);
        Arrays.fill(a[m.destination()], len, len + n, color);

        boolean[][] known = s.revealedCopy();
        known[m.source()] = Arrays.copyOf(known[m.source()], a[m.source()].length);
        known[m.destination()] = Arrays.copyOf(known[m.destination()], len + n);
        Arrays.fill(known[m.destination()], len, len + n, true);
        return new GameState(a, s.extra, s.hiddenColors, known);
    }
}

