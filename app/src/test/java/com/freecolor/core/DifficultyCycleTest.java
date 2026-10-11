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
import java.util.Random;
import static org.junit.Assert.*;

public final class DifficultyCycleTest {
    @Test
    public void difficultyChangesAtFiveAndRepeatsEveryTenLevels() {
        for (long level = 1; level <= 40; level++) {
            assertEquals((level - 1) % 10 >= 5, LevelGenerator.isHardLevel(level));
        }
        assertFalse(LevelGenerator.isHardLevel(5));
        assertTrue(LevelGenerator.isHardLevel(6));
        assertTrue(LevelGenerator.isHardLevel(10));
        assertFalse(LevelGenerator.isHardLevel(11));
        assertTrue(LevelGenerator.isHardLevel(Long.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidLevelNumber() {
        new LevelGenerator(new Random(1)).generateForLevel(0);
    }

    @Test
    public void bothStagesProduceValidPuzzlesWithExecutableSolutions() {
        LevelGenerator generator = new LevelGenerator(new Random(91827));
        boolean hiddenHardLevel = false;
        int easyLevels = 0;
        int mixedEasyLevels = 0;
        for (int level = 1; level <= 80; level++) {
            GameState initial = generator.generateForLevel(level);
            boolean hard = LevelGenerator.isHardLevel(level);
            int colors = initial.count() - GameConfig.EMPTY_TUBES;
            assertTrue(LevelValidator.validInitial(initial));
            if (hard) {
                assertTrue(colors >= GameConfig.HARD_MIN_COLORS && colors <= GameConfig.MAX_COLORS);
                hiddenHardLevel |= initial.hiddenColors;
            } else {
                assertTrue(colors >= GameConfig.MIN_COLORS && colors <= GameConfig.EASY_MAX_COLORS);
                assertFalse(initial.hiddenColors);
                easyLevels++;
                boolean individuallyMixed = false;
                for (int tube = 0; tube < colors; tube++) {
                    int[] units = initial.tube(tube);
                    individuallyMixed |= units[0] != units[1] || units[2] != units[3];
                }
                if (individuallyMixed) {
                    mixedEasyLevels++;
                }
            }
            GameState snapshot = new GameState(initial.copy(), initial.extra,
                    initial.hiddenColors, initial.revealedCopy());
            LevelSolver.Solution solution = new LevelSolver().solve(initial);
            assertTrue(solution.solved());
            assertEquals(snapshot, initial);
            GameEngine engine = new GameEngine(initial);
            assertTrue(engine.addTube());
            for (Move move : solution.moves()) {
                assertTrue(Rules.amount(engine.state(), move) > 0);
                assertTrue(engine.move(move));
            }
            assertTrue(engine.state().solved());
        }
        assertTrue(hiddenHardLevel);
        // A regression to paired construction must not pass the structural/solver checks alone.
        assertTrue(mixedEasyLevels > easyLevels / 2);
    }
}
