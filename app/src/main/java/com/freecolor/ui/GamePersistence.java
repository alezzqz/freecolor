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

import android.content.Context;
import android.content.SharedPreferences;

import com.freecolor.core.*;

import org.json.*;

import java.util.*;

/**
 * Versioned atomic preference record, including current-level undo snapshots.
 */
final class GamePersistence {

    // Save schema versions and state-array fields (preserve on-disk compatibility).
    private static final int MIN_SAVE_VERSION = 1;
    private static final int SAVE_VERSION = 5;
    private static final int LEGACY_EXTRA_TUBE_VERSION = 1;
    private static final int EXTRA_COOLDOWN_VERSION = 3;
    private static final int UNDO_ALLOWANCE_VERSION = 5;
    private static final int STATE_EXTRA_INDEX = 0;
    private static final int STATE_TUBES_INDEX = 1;
    private static final int STATE_HIDDEN_MODE_INDEX = 2;
    private static final int STATE_VISIBILITY_INDEX = 3;
    private static final int LEGACY_STATE_FIELDS = 2;

    record Saved(long level, GameEngine engine, boolean rewound, long extraAvailableAt,
                 int undoRemaining, long undoRefillAt) {
    }

    private final SharedPreferences preferences;

    GamePersistence(Context c) {
        preferences = c.getSharedPreferences("progress", Context.MODE_PRIVATE);
    }

    private JSONArray state(GameState s) throws JSONException {
        JSONArray tubes = new JSONArray();
        for (int[] t : s.copy()) {
            JSONArray a = new JSONArray();
            for (int c : t) {
                a.put(c);
            }
            tubes.put(a);
        }
        JSONArray known = new JSONArray();
        for (boolean[] mask : s.revealedCopy()) {
            JSONArray row = new JSONArray();
            for (boolean value : mask) {
                row.put(value);
            }
            known.put(row);
        }
        return new JSONArray().put(s.extra).put(tubes).put(s.hiddenColors).put(known);
    }

    private GameState read(JSONArray a, boolean legacy) throws JSONException {
        JSONArray tubes = a.getJSONArray(STATE_TUBES_INDEX);
        int[][] t = new int[tubes.length()][];
        for (int i = 0; i < t.length; i++) {
            JSONArray b = tubes.getJSONArray(i);
            t[i] = new int[b.length()];
            for (int j = 0; j < t[i].length; j++) {
                t[i][j] = b.getInt(j);
            }
        }
        boolean extra = a.getBoolean(STATE_EXTRA_INDEX);
        // An old four-unit buffer cannot be truncated without losing liquid.
        // Return to the latest compatible undo snapshot instead.
        if (legacy && extra && t[t.length - 1].length > GameConfig.EXTRA_CAPACITY) {
            return null;
        }
        if (a.length() == LEGACY_STATE_FIELDS) {
            return new GameState(t, extra);
        }
        boolean[][] known = new boolean[t.length][];
        JSONArray mask = a.getJSONArray(STATE_VISIBILITY_INDEX);
        if (mask.length() != t.length) {
            throw new JSONException("Visibility tube count");
        }
        for (int i = 0; i < t.length; i++) {
            JSONArray row = mask.getJSONArray(i);
            if (row.length() != t[i].length) {
                throw new JSONException("Visibility segment count");
            }
            known[i] = new boolean[row.length()];
            for (int j = 0; j < row.length(); j++) {
                known[i][j] = row.getBoolean(j);
            }
        }
        return new GameState(t, extra, a.getBoolean(STATE_HIDDEN_MODE_INDEX), known);
    }

    boolean save(long level, GameEngine e, long extraAvailableAt, int undoRemaining, long undoRefillAt) {
        try {
            JSONArray h = new JSONArray();
            for (GameState s : e.history()) {
                h.put(state(s));
            }
            JSONObject o = new JSONObject().put("version", SAVE_VERSION).put("level", level).put("original", state(e.original())).put("current", state(e.state())).put("history", h).put("extraAvailableAt", extraAvailableAt).put("undoRemaining", undoRemaining).put("undoRefillAt", undoRefillAt);
            return preferences.edit().putString("save", o.toString()).commit();
        } catch (JSONException ex) {
            return false;
        }
    }

    Saved load() {
        String raw = preferences.getString("save", null);
        if (raw == null) {
            return null;
        }
        return decode(raw);
    }

    private Saved decode(String raw) {
        try {
            JSONObject o = new JSONObject(raw);
            int version = o.getInt("version");
            if (version < MIN_SAVE_VERSION || version > SAVE_VERSION) {
                throw new JSONException("Version");
            }
            boolean legacy = version == LEGACY_EXTRA_TUBE_VERSION;
            GameState original = read(o.getJSONArray("original"), false), current = read(o.getJSONArray("current"), legacy);
            long level = o.getLong("level");
            if (level < GameConfig.FIRST_LEVEL || !LevelValidator.validInitial(Objects.requireNonNull(original)) || (current != null && !LevelValidator.compatible(original, current))) {
                throw new JSONException("Invalid save");
            }
            ArrayList<GameState> h = new ArrayList<>();
            JSONArray history = o.getJSONArray("history");
            for (int i = 0; i < history.length(); i++) {
                GameState entry = read(history.getJSONArray(i), legacy);
                if (entry == null) {
                    continue;
                }
                if (!LevelValidator.compatible(original, entry)) {
                    throw new JSONException("Invalid undo state");
                }
                h.add(entry);
            }

            boolean rewound = current == null;
            if (rewound) {
                current = h.isEmpty() ? original : h.remove(h.size() - 1);
            }
            long extraAvailableAt = version >= EXTRA_COOLDOWN_VERSION ? o.getLong("extraAvailableAt") : 0;
            if (extraAvailableAt < 0) {
                throw new JSONException("Invalid cooldown deadline");
            }
            int undoRemaining = version >= UNDO_ALLOWANCE_VERSION ? o.getInt("undoRemaining") : UndoAllowance.LIMIT;
            long undoRefillAt = version >= UNDO_ALLOWANCE_VERSION ? o.getLong("undoRefillAt") : 0;
            new UndoAllowance(undoRemaining, undoRefillAt, System::currentTimeMillis);
            return new Saved(level, new GameEngine(original, current, h), rewound, extraAvailableAt, undoRemaining, undoRefillAt);
        } catch (Exception ex) {
            android.util.Log.e("FreeColor", "Cannot restore saved progress", ex);
            throw new IllegalStateException("Saved progress is invalid; original record retained", ex);
        }
    }
}

