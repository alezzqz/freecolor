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
import java.util.concurrent.CancellationException;
import org.junit.Test;
import static org.junit.Assert.*;

public final class DailyChallengeTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    private DailySession session(LocalDate date, int target) {
        GameState initial = new GameState(new int[][]{
                {0, 0, 0, 1}, {1, 1, 1, 0}, {2, 2, 2, 2}, {3, 3, 3, 3},
                {4, 4, 4, 4}, {5, 5, 5, 5}, {}, {}
        }, false);
        return new DailySession(new DailyChallenge(date, 1, initial, target));
    }

    private void finish(DailySession session) {
        assertTrue(session.move(new Move(0, 6)));
        assertTrue(session.move(new Move(1, 0)));
        assertTrue(session.move(new Move(6, 1)));
        assertTrue(session.engine().state().solved());
    }

    @Test
    public void undoCostsMovesAndRestartRetainsTheBestResult() {
        DailySession session = session(MONDAY, 3);
        assertFalse(session.move(new Move(0, 1)));
        assertEquals(0, session.moves());
        assertTrue(session.move(new Move(0, 6)));
        assertTrue(session.undo());
        assertEquals(1, session.moves());
        finish(session);
        assertEquals(4, session.moves());
        assertEquals(4, session.bestMoves());
        assertFalse(session.successful());
        assertFalse(session.move(new Move(1, 6)));
        assertEquals(4, session.moves());
        session.restart();
        assertEquals(0, session.moves());
        assertEquals(4, session.bestMoves());
        finish(session);
        assertEquals(3, session.bestMoves());
        assertTrue(session.successful());
        assertFalse(session.undo());
        session.restart();
        assertEquals(3, session.bestMoves());
    }

    @Test
    public void unfinishedAttemptRestoresMovesAndUndoHistory() {
        DailySession original = session(MONDAY, 3);
        original.move(new Move(0, 6));
        DailySession restored = new DailySession(original.challenge(),
                new GameEngine(original.engine().original(), original.engine().state(), original.engine().history()),
                original.moves(), original.bestMoves());
        assertTrue(restored.undo());
        assertEquals(1, restored.moves());
        assertEquals(original.challenge().initial(), restored.engine().state());
        assertEquals(1, original.engine().history().size());
    }

    @Test
    public void onlySuccessfulDistinctDatesCountAndReplayCannotRemoveCredit() {
        DailyProgress progress = new DailyProgress();
        DailySession failed = session(MONDAY, 2);
        finish(failed);
        assertFalse(progress.record(failed));
        assertEquals(0, progress.weekCount(MONDAY));
        DailySession successful = session(MONDAY, 3);
        finish(successful);
        assertTrue(progress.record(successful));
        assertFalse(progress.record(successful));
        successful.restart();
        assertTrue(progress.completed(MONDAY));
        assertFalse(progress.record(successful));
        assertEquals(1, progress.weekCount(MONDAY));
        assertEquals(progress.completedDates(), new DailyProgress(progress.completedDates()).completedDates());
    }

    @Test
    public void sixthSuccessRemainsVisibleUntilTheNextDayAndReplayCannotRestartSeries() {
        DailyProgress progress = new DailyProgress();
        LocalDate start = MONDAY.plusDays(2);
        for (int day = 0; day < 6; day++) {
            DailySession session = session(start.plusDays(day), 3);
            finish(session);
            assertTrue(progress.record(session));
            assertEquals(day + 1, progress.weekCount(start.plusDays(day)));
            assertEquals(day == 5 ? 1 : 0, progress.completedWeeks());
            assertFalse(progress.record(session));
        }
        DailyProgress restored = new DailyProgress(progress.completedDates());
        assertEquals(6, restored.weekCount(start.plusDays(5)));
        assertEquals(0, restored.weekCount(start.plusDays(6)));
        assertEquals(1, restored.completedWeeks());
        DailySession next = session(start.plusDays(6), 3);
        finish(next);
        restored.record(next);
        assertEquals(1, restored.weekCount(start.plusDays(6)));
        assertEquals(1, restored.completedWeeks());
    }

    @Test
    public void oneMissedDayStillAllowsSuccessOnTheSeventhDay() {
        LocalDate start = MONDAY.plusDays(4);
        DailyProgress progress = new DailyProgress(List.of(start, start.plusDays(1), start.plusDays(3),
                start.plusDays(4), start.plusDays(5), start.plusDays(6)));
        assertEquals(5, progress.weekCount(start.plusDays(5)));
        assertEquals(6, progress.weekCount(start.plusDays(6)));
        assertEquals(0, progress.weekCount(start.plusDays(7)));
        assertEquals(1, progress.completedWeeks());
    }

    @Test
    public void incompleteSeriesExpiresOnDayEightAndNextSuccessStartsANewWindow() {
        DailyProgress progress = new DailyProgress(List.of(MONDAY, MONDAY.plusDays(1), MONDAY.plusDays(3),
                MONDAY.plusDays(4), MONDAY.plusDays(6)));
        assertEquals(5, progress.weekCount(MONDAY.plusDays(6)));
        assertEquals(0, progress.weekCount(MONDAY.plusDays(7)));
        assertEquals(0, progress.completedWeeks());
        DailySession next = session(MONDAY.plusDays(9), 3);
        finish(next);
        progress.record(next);
        DailyProgress restored = new DailyProgress(progress.completedDates());
        assertEquals(1, restored.weekCount(MONDAY.plusDays(9)));
        assertEquals(1, restored.weekCount(MONDAY.plusDays(15)));
        assertEquals(0, restored.weekCount(MONDAY.plusDays(16)));
        assertEquals(0, restored.completedWeeks());
        assertEquals(6, restored.completedDates().size());
    }

    @Test
    public void multipleSeriesAndLongAbsencesRetainOnlyCompletedAchievements() {
        Set<LocalDate> dates = new HashSet<>();
        for (int day = 0; day < 12; day++) {
            dates.add(MONDAY.plusDays(day));
        }
        dates.add(MONDAY.plusDays(40));
        DailyProgress progress = new DailyProgress(dates);
        assertEquals(6, progress.weekCount(MONDAY.plusDays(11)));
        assertEquals(0, progress.weekCount(MONDAY.plusDays(12)));
        assertEquals(1, progress.weekCount(MONDAY.plusDays(40)));
        assertEquals(0, progress.weekCount(MONDAY.plusDays(100)));
        assertEquals(2, progress.completedWeeks());
    }

    @Test
    public void seriesCrossYearAndLeapDayBoundariesWithoutCalendarWeekResets() {
        for (LocalDate start : List.of(LocalDate.of(2026, 12, 29), LocalDate.of(2028, 2, 27))) {
            Set<LocalDate> dates = new HashSet<>();
            for (int day = 0; day < 6; day++) {
                dates.add(start.plusDays(day));
            }
            DailyProgress progress = new DailyProgress(dates);
            assertEquals(0, progress.weekCount(start.minusDays(1)));
            assertEquals(6, progress.weekCount(start.plusDays(5)));
            assertEquals(0, progress.weekCount(start.plusDays(6)));
            assertEquals(1, progress.completedWeeks());
        }
    }

    @Test
    public void lateCompletionUsesChallengeDateAndDoesNotCountAsTodaysTask() {
        DailyProgress progress = new DailyProgress(List.of(MONDAY, MONDAY.plusDays(1), MONDAY.plusDays(2),
                MONDAY.plusDays(3), MONDAY.plusDays(4)));
        DailySession old = session(MONDAY.plusDays(6), 3);
        finish(old);
        progress.record(old);
        assertEquals(6, progress.weekCount(MONDAY.plusDays(6)));
        assertEquals(0, progress.weekCount(MONDAY.plusDays(7)));
        assertFalse(progress.completed(MONDAY.plusDays(7)));
        assertEquals(1, progress.completedWeeks());
    }

    @Test
    public void yearOfDailyBoardsIsDeterministicAndEveryTargetHasAReplayableSolution() {
        DailyGenerator generator = new DailyGenerator();
        Set<String> layouts = new HashSet<>();
        for (int day = 0; day < 365; day++) {
            LocalDate date = MONDAY.plusDays(day);
            DailyChallenge challenge = generator.generate(date);
            DailyChallenge repeated = generator.generate(date);
            assertEquals(challenge, repeated);
            assertEquals(date, challenge.date());
            assertFalse(challenge.initial().hiddenColors);
            assertFalse(challenge.initial().extra);
            assertEquals(8, challenge.initial().count());
            DailySolver.Solution solution = new DailySolver().solve(challenge.initial());
            assertTrue(solution.solved());
            assertEquals(solution.moves().size() + GameConfig.DAILY_MOVE_MARGIN, challenge.targetMoves());
            DailySession session = new DailySession(challenge);
            for (Move move : solution.moves()) {
                assertTrue(session.move(move));
            }
            assertTrue(session.successful());
            layouts.add(challenge.initial().canonical());
        }
        assertTrue(layouts.size() > 350);
    }

    @Test
    public void budgetExhaustionUsesAnIndependentlyVerifiedReserve() {
        DailyChallenge reserve = new DailyGenerator(1).generate(MONDAY);
        assertEquals(22, reserve.targetMoves());
        DailySolver.Solution solution = new DailySolver().solve(reserve.initial());
        assertTrue(solution.solved());
        assertEquals(19, solution.moves().size());
        GameState state = reserve.initial();
        for (Move move : solution.moves()) {
            state = Rules.pour(state, move);
        }
        assertTrue(state.solved());
        assertEquals(1, new DailySolver(1).solve(reserve.initial()).visited());
        assertFalse(new DailySolver(1).solve(reserve.initial()).solved());
    }

    @Test
    public void compactShortestPathsAgreeWithIndependentProductionRuleBreadthFirstSearch() {
        for (int seed = 0; seed < 12; seed++) {
            GameState initial = new LevelGenerator(new Random(seed)).generate(5).withHiddenColors(false);
            DailySolver.Solution compact = new DailySolver().solve(initial);
            assertTrue(compact.solved());
            assertEquals(referenceShortest(initial), compact.moves().size());
        }
    }

    private int referenceShortest(GameState initial) {
        record Entry(GameState state, int depth) {}
        ArrayDeque<Entry> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(new Entry(initial, 0));
        visited.add(initial.canonical());
        while (!queue.isEmpty()) {
            Entry entry = queue.removeFirst();
            if (entry.state().solved()) {
                return entry.depth();
            }
            for (int i = 0; i < entry.state().count(); i++) {
                for (int j = 0; j < entry.state().count(); j++) {
                    Move move = new Move(i, j);
                    if (Rules.amount(entry.state(), move) == 0) {
                        continue;
                    }
                    GameState next = Rules.pour(entry.state(), move);
                    if (visited.add(next.canonical())) {
                        queue.addLast(new Entry(next, entry.depth() + 1));
                    }
                }
            }
        }
        throw new AssertionError("Reference search exhausted");
    }

    @Test
    public void interruptedGenerationDoesNotReturnADifferentReserveBoard() {
        Thread.currentThread().interrupt();
        try {
            new DailyGenerator().generate(MONDAY);
            fail("Expected cancellation");
        } catch (CancellationException expected) {
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void dailySearchRejectsAnExtraTube() {
        new DailySolver().solve(session(MONDAY, 3).engine().state().addTube());
    }

    @Test(expected = IllegalArgumentException.class)
    public void dailyRestoreRejectsAnExtraTubeInCurrentState() {
        DailySession session = session(MONDAY, 3);
        GameEngine incompatible = new GameEngine(session.challenge().initial());
        incompatible.addTube();
        new DailySession(session.challenge(), incompatible, 1, 0);
    }
}

