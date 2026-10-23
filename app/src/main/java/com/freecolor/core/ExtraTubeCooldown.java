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

import java.util.function.LongSupplier;

/**
 * Global acquisition timer; intentionally independent of a level's undo history.
 */
public final class ExtraTubeCooldown {
    public static final long INTERVAL_MILLIS = GameConfig.EXTRA_TUBE_COOLDOWN_MINUTES * GameConfig.SECONDS_PER_MINUTE * GameConfig.MILLIS_PER_SECOND;
    private final LongSupplier clock;
    private long availableAt;

    public ExtraTubeCooldown(long availableAt, LongSupplier clock) {
        if (availableAt < 0) {
            throw new IllegalArgumentException("Negative deadline");
        }
        this.availableAt = availableAt;
        this.clock = clock;
    }

    public long remainingMillis() {
        return Math.max(0, availableAt - clock.getAsLong());
    }

    public boolean available() {
        return remainingMillis() == 0;
    }

    public void acquired() {
        if (!available()) {
            throw new IllegalStateException("Extra tube is cooling down");
        }
        availableAt = clock.getAsLong() + INTERVAL_MILLIS;
    }

    public void reward() {
        if (!available()) {
            availableAt = Math.max(clock.getAsLong(), availableAt
                    - GameConfig.SECONDS_PER_MINUTE * GameConfig.MILLIS_PER_SECOND);
        }
    }

    public long availableAt() {
        return availableAt;
    }
}
