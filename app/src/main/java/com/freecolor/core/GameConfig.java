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

/** Product rules, global allowances, time units and bounded search budgets. */
public final class GameConfig {
    public static final int CAPACITY = 4;
    public static final int EXTRA_CAPACITY = 1;
    public static final int MIN_COLORS = 4;
    public static final int MAX_COLORS = 11;
    public static final int EMPTY_TUBES = 2;
    public static final int MAX_TUBES = MAX_COLORS + EMPTY_TUBES + 1;
    public static final int SEARCH_BUDGET = 100_000;
    public static final int CANDIDATE_BUDGET = 25_000;
    public static final int MAX_SEARCH_DEPTH = 180;
    // Level generation and move ordering.
    public static final int FIRST_LEVEL = 1;
    public static final int GENERATION_ATTEMPTS = 40;
    public static final int FALLBACK_ATTEMPTS = 20;
    public static final int SOLVER_MATCHING_DESTINATION_BONUS = 10;
    // Repeating five-level difficulty stages; preserve the overall 4–11 color range.
    public static final int DIFFICULTY_STAGE_LEVELS = 5;
    public static final int DIFFICULTY_CYCLE_LEVELS = DIFFICULTY_STAGE_LEVELS * 2;
    public static final int EASY_MAX_COLORS = 6;
    public static final int HARD_MIN_COLORS = EASY_MAX_COLORS + 1;
    // Persisted global cooldowns; independent of level and undo snapshots.
    public static final long MILLIS_PER_SECOND = 1000L;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int EXTRA_TUBE_COOLDOWN_MINUTES = 15;
    public static final int UNDO_REFILL_MINUTES = 15;
    public static final int UNDO_ALLOWANCE = 10;
    private GameConfig() {}
}
