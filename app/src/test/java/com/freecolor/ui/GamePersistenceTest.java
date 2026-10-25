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

package com.freecolor.ui;

import com.freecolor.core.GameConfig;
import com.freecolor.core.GameState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class GamePersistenceTest {

    private static final int VERSION = 7;

    /** Five colors spread across five tubes plus two empty ones; a valid, unsolved initial board. */
    private static GameState sampleState() {
        return new GameState(new int[][]{
                {0, 1, 2, 3},
                {0, 1, 2, 3},
                {0, 1, 4, 4},
                {0, 1, 4, 4},
                {2, 2, 3, 3},
                {}, {}
        }, false);
    }

    private static String saveJson(int version, GameState original, GameState current,
            List<GameState> history, long level, long extraAvailableAt, int undoRemaining,
            long undoRefillAt, int undoRewardWins, int extraRewardWins, int pendingRewardEffects)
            throws JSONException {
        JSONArray historyArray = new JSONArray();
        for (GameState snapshot : history) {
            historyArray.put(GamePersistence.state(snapshot));
        }
        return new JSONObject()
                .put("version", version)
                .put("level", level)
                .put("original", GamePersistence.state(original))
                .put("current", GamePersistence.state(current))
                .put("history", historyArray)
                .put("extraAvailableAt", extraAvailableAt)
                .put("undoRemaining", undoRemaining)
                .put("undoRefillAt", undoRefillAt)
                .put("undoRewardWins", undoRewardWins)
                .put("extraRewardWins", extraRewardWins)
                .put("pendingRewardEffects", pendingRewardEffects)
                .toString();
    }

    private static String validJson() throws JSONException {
        GameState state = sampleState();
        return saveJson(VERSION, state, state, List.of(), 12, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0);
    }

    @Test public void decodeRestoresLevelEngineAndRewardFields() throws Exception {
        GameState state = sampleState();
        GamePersistence.Saved saved = GamePersistence.decode(saveJson(VERSION, state, state, List.of(),
                12, 5000, GameConfig.UNDO_ALLOWANCE, 0, 1, 2, 3));
        assertEquals(12L, saved.level());
        assertEquals(state, saved.engine().state());
        assertEquals(state, saved.engine().original());
        assertTrue(saved.engine().history().isEmpty());
        assertFalse(saved.rewound());
        assertEquals(5000L, saved.extraAvailableAt());
        assertEquals(GameConfig.UNDO_ALLOWANCE, saved.undoRemaining());
        assertEquals(0L, saved.undoRefillAt());
        assertEquals(1, saved.undoRewardWins());
        assertEquals(2, saved.extraRewardWins());
        assertEquals(3, saved.pendingRewardEffects());
    }

    @Test public void decodeRoundTripsAStateThroughSerialization() throws Exception {
        GameState state = sampleState();
        GamePersistence.Saved saved = GamePersistence.decode(validJson());
        assertEquals(state, saved.engine().state());
    }

    @Test public void decodeRejectsOutOfRangeVersion() {
        GameState state = sampleState();
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(0, state, state, List.of(), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(8, state, state, List.of(), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsLevelBelowFirst() {
        GameState state = sampleState();
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, state, List.of(), 0, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsInvalidInitialState() {
        GameState invalid = new GameState(new int[][]{{}, {}}, false);
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, invalid, invalid, List.of(), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsIncompatibleCurrent() {
        GameState state = sampleState();
        GameState different = new GameState(new int[][]{{}, {}, {}, {}, {}, {}, {}}, false);
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, different, List.of(), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsIncompatibleHistoryEntry() {
        GameState state = sampleState();
        GameState different = new GameState(new int[][]{{}, {}, {}, {}, {}, {}, {}}, false);
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, state, List.of(different), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsInvalidCooldownDeadline() {
        GameState state = sampleState();
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, state, List.of(), 1, -1, GameConfig.UNDO_ALLOWANCE, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsInvalidUndoAllowance() {
        GameState state = sampleState();
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, state, List.of(), 1, 0, 0, 0, 0, 0, 0)));
    }

    @Test public void decodeRejectsInvalidRewardProgress() {
        GameState state = sampleState();
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, state, List.of(), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 3, 0, 0)));
        assertThrows(IllegalStateException.class, () -> GamePersistence.decode(
                saveJson(VERSION, state, state, List.of(), 1, 0, GameConfig.UNDO_ALLOWANCE, 0, 0, 5, 0)));
    }
}
