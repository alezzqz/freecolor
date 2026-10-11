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

public final class LevelGenerator {
    private final Random random;

    public LevelGenerator() {
        this(new Random());
    }

    public LevelGenerator(Random random) {
        this.random = random;
    }

    public static boolean isHardLevel(long level) {
        if (level < GameConfig.FIRST_LEVEL) {
            throw new IllegalArgumentException("Level must be positive");
        }
        return (level - GameConfig.FIRST_LEVEL) % GameConfig.DIFFICULTY_CYCLE_LEVELS
                >= GameConfig.DIFFICULTY_STAGE_LEVELS;
    }

    public GameState generateForLevel(long level) {
        boolean hard = isHardLevel(level);
        int minimum = hard ? GameConfig.HARD_MIN_COLORS : GameConfig.MIN_COLORS;
        int maximum = hard ? GameConfig.MAX_COLORS : GameConfig.EASY_MAX_COLORS;
        int colors = minimum + random.nextInt(maximum - minimum + 1);
        // Both stages shuffle individual units; fewer tubes make the easy stage more manageable.
        return generate(colors, hard);
    }

    public GameState generate() {
        return generate(GameConfig.MIN_COLORS + random.nextInt(GameConfig.MAX_COLORS - GameConfig.MIN_COLORS + 1));
    }

    public GameState generate(int colors) {
        return generate(colors, true);
    }

    private GameState generate(int colors, boolean allowHiddenColors) {
        if (colors < GameConfig.MIN_COLORS || colors > GameConfig.MAX_COLORS) {
            throw new IllegalArgumentException();
        }
        for (int attempt = 0; attempt < GameConfig.GENERATION_ATTEMPTS; attempt++) {
            ArrayList<Integer> units = new ArrayList<>();
            for (int c = 0; c < colors; c++) {
                for (int k = 0; k < GameConfig.CAPACITY; k++) {
                    units.add(c);
                }
            }
            Collections.shuffle(units, random);
            int[][] a = new int[colors + GameConfig.EMPTY_TUBES][];
            for (int i = 0; i < colors; i++) {
                a[i] = new int[GameConfig.CAPACITY];
                for (int j = 0; j < GameConfig.CAPACITY; j++) {
                    a[i][j] = units.get(i * GameConfig.CAPACITY + j);
                }
            }
            a[colors] = new int[0];
            a[colors + 1] = new int[0];
            GameState candidate = new GameState(a, false);
            if (LevelValidator.validInitial(candidate) && new LevelSolver(GameConfig.CANDIDATE_BUDGET).solve(candidate).solved()) {
                return candidate.withHiddenColors(allowHiddenColors && random.nextBoolean());
            }
        }
        // Retain a bounded, solver-checked fallback rather than weakening solvability.
        return generatePairs(colors, allowHiddenColors);
    }

    private GameState generatePairs(int colors, boolean allowHiddenColors) {
        // Bounded fallback: cyclic pairs retain randomness and admit a short solution, still solver checked.
        for (int attempt = 0; attempt < GameConfig.FALLBACK_ATTEMPTS; attempt++) {
            ArrayList<Integer> order = new ArrayList<>();
            for (int c = 0; c < colors; c++) {
                order.add(c);
            }
            Collections.shuffle(order, random);
            int[][] a = new int[colors + GameConfig.EMPTY_TUBES][];
            for (int i = 0; i < colors; i++) {
                a[i] = new int[]{
                        order.get(i), order.get(i), order.get((i + 1) % colors), order.get((i + 1) % colors)
                };
            }
            a[colors] = new int[0];
            a[colors + 1] = new int[0];
            GameState s = new GameState(a, false);
            if (LevelValidator.validInitial(s) && new LevelSolver().solve(s).solved()) {
                return s.withHiddenColors(allowHiddenColors && random.nextBoolean());
            }
        }
        throw new IllegalStateException("No verified candidate within generation budget");
    }
}

