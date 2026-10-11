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

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class CooldownTest {
    @Test
    public void boundaryAndPersistence() {
        AtomicLong now = new AtomicLong(1000);
        ExtraTubeCooldown timer = new ExtraTubeCooldown(0, now::get);
        assertTrue(timer.available());
        timer.acquired();
        assertEquals(900000, timer.remainingMillis());
        assertFalse(timer.available());
        ExtraTubeCooldown restored = new ExtraTubeCooldown(timer.availableAt(), now::get);
        now.addAndGet(899999);
        assertEquals(1, restored.remainingMillis());
        assertFalse(restored.available());
        now.incrementAndGet();
        assertTrue(restored.available());
        restored.acquired();
        assertEquals(900000, restored.remainingMillis());
    }

    @Test(expected = IllegalStateException.class)
    public void preventsRepeatedAcquisition() {
        ExtraTubeCooldown timer = new ExtraTubeCooldown(0, () -> 1000);
        timer.acquired();
        timer.acquired();
    }

    @Test
    public void completionRequiresFourMatchingUnits() {
        GameState state = new GameState(new int[][]{{0, 0, 0, 0}, {1, 1}, {0, 1, 0, 1}, {2}}, true);
        assertTrue(state.completeTube(0));
        assertFalse(state.completeTube(1));
        assertFalse(state.completeTube(2));
        assertFalse(state.completeTube(3));
    }
}
