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

/**
 * Bounded depth-first graph search; canonical tube permutations eliminate equivalent states.
 */
public final class LevelSolver {
    public record Solution(boolean solved, List<Move> moves, int visited) {
    }

    private final int budget;
    private Set<String> visited;
    private ArrayList<Move> path;

    public LevelSolver() {
        this(GameConfig.SEARCH_BUDGET);
    }

    public LevelSolver(int budget) {
        this.budget = budget;
    }

    public Solution solve(GameState initial) {
        visited = new HashSet<>();
        path = new ArrayList<>();
        GameState search = initial.extra ? initial : initial.addTube();
        boolean ok = dfs(search, 0);
        return new Solution(ok, ok ? List.copyOf(path) : List.of(), visited.size());
    }

    private boolean dfs(GameState s, int depth) {
        if (s.solved()) {
            return true;
        }
        if (depth >= GameConfig.MAX_SEARCH_DEPTH || visited.size() >= budget || Thread.currentThread().isInterrupted()) {
            return false;
        }
        if (!visited.add(s.canonical())) {
            return false;
        }
        ArrayList<Move> moves = new ArrayList<>();
        for (int i = 0; i < s.count(); i++) {
            Set<Integer> emptyCapacities = new HashSet<>();
            for (int j = 0; j < s.count(); j++) {
                Move m = new Move(i, j);
                if (Rules.amount(s, m) == 0) {
                    continue;
                }
                if (s.size(j) == 0) {
                    if (!emptyCapacities.add(s.capacity(j))) {
                        continue;
                    }
                    int[] t = s.tube(i);
                    boolean mono = true;
                    for (int c : t) {
                        if (c != t[0]) {
                            mono = false;
                            break;
                        }
                    }
                    // Only moving an entire monochromatic tube to an equivalent empty
                    // tube is redundant. A one-unit buffer can split a top group.
                    if (mono && s.capacity(i) == s.capacity(j)) {
                        continue;
                    }
                }
                moves.add(m);
            }
        }
        moves.sort(Comparator.comparingInt((Move m) -> s.size(m.destination()) == 0 ? 0 : GameConfig.SOLVER_MATCHING_DESTINATION_BONUS + Rules.amount(s, m)).reversed());
        for (Move m : moves) {
            path.add(m);
            if (dfs(Rules.pour(s, m), depth + 1)) {
                return true;
            }
            path.remove(path.size() - 1);
            if (visited.size() >= budget) {
                return false;
            }
        }
        return false;
    }
}

