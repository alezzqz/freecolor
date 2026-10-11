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

import java.util.*;

public final class GameEngine {
    private final GameState original;
    private GameState state;
    private final ArrayList<GameState> history;

    public GameEngine(GameState original) {
        this(original, original, List.of());
    }

    public GameEngine(GameState original, GameState current, List<GameState> history) {
        this.original = original;
        state = current;
        this.history = new ArrayList<>(history);
    }

    public GameState state() {
        return state;
    }

    public GameState original() {
        return original;
    }

    public List<GameState> history() {
        return List.copyOf(history);
    }

    public boolean move(Move m) {
        if (state.solved() || Rules.amount(state, m) == 0) {
            return false;
        }
        history.add(state);
        state = Rules.pour(state, m);
        if (state.solved()) {
            history.clear();
        }
        return true;
    }

    public boolean addTube() {
        if (state.extra || state.solved()) {
            return false;
        }
        history.add(state);
        state = state.addTube();
        return true;
    }

    public boolean undo() {
        if (history.isEmpty()) {
            return false;
        }

        GameState previous = history.remove(history.size() - 1);
        if (previous.hiddenColors) {
            boolean[][] known = previous.revealedCopy();
            // A pour only changes tube tops. Shared bottom prefixes represent the
            // same liquid units, so knowledge gained since the snapshot survives.
            for (int i = 0; i < Math.min(previous.count(), state.count()); i++) {
                int[] before = previous.tube(i), now = state.tube(i);
                boolean[] revealed = state.revealedTube(i);
                for (int j = 0; j < Math.min(before.length, now.length); j++) {
                    if (before[j] != now[j]) {
                        break;
                    }
                    known[i][j] |= revealed[j];
                }
                // Units returned to the source were visible during the pour.
                for (int j = now.length; j < before.length; j++) {
                    known[i][j] = true;
                }
            }
            previous = new GameState(previous.copy(), previous.extra, true, known);
        }
        state = previous;
        return true;
    }

    public void restart() {
        state = original;
        history.clear();
    }
}
