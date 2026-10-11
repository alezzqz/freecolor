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
 * Global allowance: refill starts only when all ten uses have been consumed.
 */
public final class UndoAllowance {
    public static final int LIMIT = GameConfig.UNDO_ALLOWANCE;
    public static final long INTERVAL_MILLIS = GameConfig.UNDO_REFILL_MINUTES * GameConfig.SECONDS_PER_MINUTE * GameConfig.MILLIS_PER_SECOND;
    private final LongSupplier clock;
    private int remaining;
    private long refillAt;

    public UndoAllowance(int remaining, long refillAt, LongSupplier clock) {
        if (remaining < 0 || remaining > LIMIT || refillAt < 0
                || (remaining == 0) != (refillAt > 0)) {
            throw new IllegalArgumentException("Invalid undo allowance");
        }
        this.remaining = remaining;
        this.refillAt = refillAt;
        this.clock = clock;
    }

    public int remaining() {
        if (refillAt > 0 && clock.getAsLong() >= refillAt) {
            remaining = LIMIT;
            refillAt = 0;
        }
        return remaining;
    }

    public void consume() {
        if (remaining() == 0) {
            return;
        }
        if (--remaining == 0) {
            refillAt = clock.getAsLong() + INTERVAL_MILLIS;
        }
    }

    public long refillAt() {
        remaining();
        return refillAt;
    }

    public long waitMillis() {
        remaining();
        return Math.max(0, refillAt - clock.getAsLong());
    }
}
