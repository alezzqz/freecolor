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
import java.util.Objects;

/** Persist the complete definition so an update cannot replace an already started daily board. */
public record DailyChallenge(LocalDate date, int version, GameState initial, int targetMoves) {
    public DailyChallenge {
        Objects.requireNonNull(date);
        Objects.requireNonNull(initial);
        if (version < 1 || targetMoves < 1 || initial.hiddenColors || initial.extra
                || initial.count() != GameConfig.DAILY_COLORS + GameConfig.EMPTY_TUBES
                || !LevelValidator.validInitial(initial)) {
            throw new IllegalArgumentException("Invalid daily challenge");
        }
    }
}

