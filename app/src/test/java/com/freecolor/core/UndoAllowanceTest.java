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

public class UndoAllowanceTest {
    @Test public void rewardRestoresOneUseWithoutExceedingLimit() {
        UndoAllowance allowance = new UndoAllowance(19, 0, () -> 1000);
        allowance.reward();
        allowance.reward();
        assertEquals(20, allowance.remaining());
        assertEquals(0, allowance.refillAt());
    }

    @Test public void rewardsGrowPastRefillAllowanceAndSurviveRestoration() {
        UndoAllowance allowance = new UndoAllowance(10, 0, () -> 1000);
        allowance.reward();
        assertEquals(11, allowance.remaining());
        allowance = new UndoAllowance(allowance.remaining(), allowance.refillAt(), () -> 1000);
        assertEquals(11, allowance.remaining());
        assertEquals(0, allowance.waitMillis());
    }

    @Test public void consumingRewardAllowanceStartsTimerOnlyAtZeroAndRefillsTen() {
        AtomicLong now = new AtomicLong(1000);
        UndoAllowance allowance = new UndoAllowance(20, 0, now::get);
        for (int i = 0; i < 19; i++) {
            allowance.consume();
            assertEquals(19 - i, allowance.remaining());
            assertEquals(0, allowance.refillAt());
        }
        allowance.consume();
        assertEquals(0, allowance.remaining());
        now.addAndGet(UndoAllowance.INTERVAL_MILLIS);
        assertEquals(10, allowance.remaining());
        assertEquals(0, allowance.refillAt());
    }

    @Test public void rewardShortensTimerAndRefillsWhenItReachesNow() {
        AtomicLong now = new AtomicLong(1000);
        UndoAllowance allowance = new UndoAllowance(0, 91000, now::get);
        allowance.reward();
        assertEquals(30000, allowance.waitMillis());
        assertEquals(0, allowance.remaining());
        allowance = new UndoAllowance(0, allowance.refillAt(), now::get);
        allowance.reward();
        assertEquals(10, allowance.remaining());
        assertEquals(0, allowance.refillAt());
    }

    @Test public void timerStartsOnlyAfterTenthUseAndRefillsAtBoundary() {
        AtomicLong now=new AtomicLong(1000);
        UndoAllowance allowance=new UndoAllowance(10,0,now::get);
        for(int i=0;i<9;i++) {
            allowance.consume();
            assertEquals(9-i,allowance.remaining());
            assertEquals(0,allowance.refillAt());
            now.addAndGet(900000);
        }
        assertEquals(1,allowance.remaining());
        allowance.consume();
        assertEquals(0,allowance.remaining());
        assertEquals(900000,allowance.waitMillis());
        long deadline=allowance.refillAt();
        allowance.consume();
        assertEquals(0,allowance.remaining());
        assertEquals(deadline,allowance.refillAt());
        allowance=new UndoAllowance(allowance.remaining(),allowance.refillAt(),now::get);
        now.addAndGet(899999);
        assertEquals(0,allowance.remaining());
        assertEquals(1,allowance.waitMillis());
        now.incrementAndGet();
        assertEquals(10,allowance.remaining());
        assertEquals(0,allowance.refillAt());
    }
    @Test public void partialAllowanceSurvivesRestorationWithoutTimer() {
        UndoAllowance allowance=new UndoAllowance(4,0,()->1000);
        assertEquals(4,allowance.remaining());
        assertEquals(0,allowance.waitMillis());
        allowance.consume();
        assertEquals(3,allowance.remaining());
    }
}
