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
    public void fifthDayCompletesWeekAndLateSundayWinBelongsToPreviousWeek() {
        DailyProgress progress = new DailyProgress();
        for (int day = 0; day < 4; day++) {
            DailySession session = session(MONDAY.plusDays(day), 3);
            finish(session);
            progress.record(session);
        }
        assertEquals(0, progress.completedWeeks());
        DailySession sunday = session(MONDAY.plusDays(6), 3);
        finish(sunday);
        progress.record(sunday);
        assertEquals(5, progress.weekCount(MONDAY));
        assertEquals(0, progress.weekCount(MONDAY.plusWeeks(1)));
        assertEquals(1, progress.completedWeeks());
        assertEquals(MONDAY, DailyProgress.weekStart(sunday.challenge().date()));
        DailySession nextMonday = session(MONDAY.plusWeeks(1), 3);
        finish(nextMonday);
        progress.record(nextMonday);
        assertEquals(1, progress.weekCount(MONDAY.plusWeeks(1)));
        assertEquals(1, progress.completedWeeks());
    }

    @Test
    public void weeksUseMondayAcrossYearAndLeapDayBoundaries() {
        assertEquals(LocalDate.of(2026, 12, 28), DailyProgress.weekStart(LocalDate.of(2027, 1, 3)));
        assertEquals(LocalDate.of(2027, 1, 4), DailyProgress.weekStart(LocalDate.of(2027, 1, 4)));
        DailyProgress progress = new DailyProgress(List.of(LocalDate.of(2028, 2, 29)));
        assertEquals(1, progress.weekCount(LocalDate.of(2028, 3, 1)));
        assertEquals(0, progress.weekCount(LocalDate.of(2028, 3, 6)));
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

