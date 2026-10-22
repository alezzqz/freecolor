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

import java.time.LocalDate;
import java.util.*;

/** Version 1 uses its own shuffle sequence; ordinary-level generator changes cannot alter daily boards. */
public final class DailyGenerator {
    private static final long SEED_SALT = 0x46524545434f4c31L;
    private final DailySolver solver;

    public DailyGenerator() {
        this(GameConfig.DAILY_SEARCH_BUDGET);
    }

    public DailyGenerator(int budget) {
        solver = new DailySolver(budget);
    }

    public DailyChallenge generate(LocalDate date) {
        Objects.requireNonNull(date);
        Random random = new Random(date.toEpochDay() ^ SEED_SALT);
        for (int attempt = 0; attempt < GameConfig.DAILY_GENERATION_ATTEMPTS; attempt++) {
            DailySolver.checkCancelled();
            ArrayList<Integer> units = new ArrayList<>();
            for (int color = 0; color < GameConfig.DAILY_COLORS; color++) {
                for (int i = 0; i < GameConfig.CAPACITY; i++) {
                    units.add(color);
                }
            }
            Collections.shuffle(units, random);
            int[][] tubes = new int[GameConfig.DAILY_COLORS + GameConfig.EMPTY_TUBES][];
            for (int i = 0; i < GameConfig.DAILY_COLORS; i++) {
                tubes[i] = new int[GameConfig.CAPACITY];
                for (int j = 0; j < GameConfig.CAPACITY; j++) {
                    tubes[i][j] = units.get(i * GameConfig.CAPACITY + j);
                }
            }
            tubes[GameConfig.DAILY_COLORS] = new int[0];
            tubes[GameConfig.DAILY_COLORS + 1] = new int[0];
            GameState initial = new GameState(tubes, false);
            if (!LevelValidator.validInitial(initial)) {
                continue;
            }
            DailySolver.Solution solution = solver.solve(initial);
            if (solution.solved()) {
                return new DailyChallenge(date, GameConfig.DAILY_VERSION, initial,
                        solution.moves().size() + GameConfig.DAILY_MOVE_MARGIN);
            }
        }
        DailySolver.checkCancelled();
        // Independently verified reserve: optimum 19, unaffected by the runtime search budget.
        GameState reserve = new GameState(new int[][]{
                {5, 4, 3, 2}, {2, 2, 5, 0}, {5, 1, 0, 1}, {1, 3, 4, 0},
                {3, 0, 4, 1}, {2, 5, 3, 4}, {}, {}
        }, false);
        return new DailyChallenge(date, GameConfig.DAILY_VERSION, reserve, 19 + GameConfig.DAILY_MOVE_MARGIN);
    }
}

