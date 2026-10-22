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
import java.util.concurrent.CancellationException;

/** Shortest paths for open-color daily boards with equal-capacity tubes and no extra buffer. */
public final class DailySolver {
    public record Solution(boolean solved, List<Move> moves, int visited) {}
    private record Node(int[] tubes, Node parent, Move move) {}

    private final int budget;

    public DailySolver() {
        this(GameConfig.DAILY_SEARCH_BUDGET);
    }

    public DailySolver(int budget) {
        if (budget < 1) {
            throw new IllegalArgumentException("Search budget must be positive");
        }
        this.budget = budget;
    }

    public Solution solve(GameState initial) {
        if (initial.extra || initial.hiddenColors || initial.count() > GameConfig.DAILY_COLORS + GameConfig.EMPTY_TUBES) {
            throw new IllegalArgumentException("Daily search requires open colors and ordinary tubes");
        }
        int[] tubes = new int[initial.count()];
        for (int i = 0; i < tubes.length; i++) {
            tubes[i] = 1;
            for (int color : initial.tube(i)) {
                if (color >= GameConfig.DAILY_COLORS) {
                    throw new IllegalArgumentException("Unsupported daily color");
                }
                tubes[i] = (tubes[i] << 3) | (color + 1);
            }
        }
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(new Node(tubes, null, null));
        visited.add(key(tubes));
        while (!queue.isEmpty()) {
            checkCancelled();
            Node current = queue.removeFirst();
            if (solved(current.tubes())) {
                ArrayList<Move> path = new ArrayList<>();
                while (current.parent() != null) {
                    path.add(current.move());
                    current = current.parent();
                }
                Collections.reverse(path);
                return new Solution(true, List.copyOf(path), visited.size());
            }
            for (int source = 0; source < tubes.length; source++) {
                for (int destination = 0; destination < tubes.length; destination++) {
                    int amount = amount(current.tubes(), source, destination);
                    if (amount == 0) {
                        continue;
                    }
                    int[] next = current.tubes().clone();
                    int color = next[source] & 7;
                    next[source] >>= 3 * amount;
                    for (int i = 0; i < amount; i++) {
                        next[destination] = (next[destination] << 3) | color;
                    }
                    String canonical = key(next);
                    if (visited.contains(canonical)) {
                        continue;
                    }
                    // A deterministic state limit keeps the selected puzzle independent of device speed.
                    if (visited.size() >= budget) {
                        return new Solution(false, List.of(), visited.size());
                    }
                    visited.add(canonical);
                    queue.addLast(new Node(next, current, new Move(source, destination)));
                }
            }
        }
        return new Solution(false, List.of(), visited.size());
    }

    static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Daily generation interrupted");
        }
    }

    private static int size(int tube) {
        return (31 - Integer.numberOfLeadingZeros(tube)) / 3;
    }

    private static int amount(int[] tubes, int source, int destination) {
        int from = tubes[source], to = tubes[destination];
        if (source == destination || from == 1 || size(to) == GameConfig.CAPACITY) {
            return 0;
        }
        int color = from & 7;
        if (to != 1 && (to & 7) != color) {
            return 0;
        }
        int group = 0;
        while (from != 1 && (from & 7) == color) {
            group++;
            from >>= 3;
        }
        return Math.min(group, GameConfig.CAPACITY - size(to));
    }

    private static boolean solved(int[] tubes) {
        for (int tube : tubes) {
            if (tube == 1) {
                continue;
            }
            if (size(tube) != GameConfig.CAPACITY) {
                return false;
            }
            int color = tube & 7;
            while (tube != 1) {
                if ((tube & 7) != color) {
                    return false;
                }
                tube >>= 3;
            }
        }
        return true;
    }

    private static String key(int[] tubes) {
        char[] canonical = new char[tubes.length];
        for (int i = 0; i < tubes.length; i++) {
            canonical[i] = (char) tubes[i];
        }
        Arrays.sort(canonical);
        return new String(canonical);
    }
}

