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
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** A separate atomic record preserves ordinary v1-v5 saves and commits a win with its weekly credit. */
final class DailyPersistence {
    record Saved(DailySession session, DailyProgress progress, boolean active) {}
    private final SharedPreferences preferences;

    DailyPersistence(Context context) {
        preferences = context.getSharedPreferences("progress", Context.MODE_PRIVATE);
    }

    boolean save(DailySession session, DailyProgress progress, boolean active) {
        try {
            JSONArray dates = new JSONArray();
            ArrayList<LocalDate> ordered = new ArrayList<>(progress.completedDates());
            Collections.sort(ordered);
            for (LocalDate date : ordered) {
                dates.put(date.toEpochDay());
            }
            JSONObject record = new JSONObject().put("version", 1).put("active", active).put("completed", dates);
            if (session != null) {
                GameEngine engine = session.engine();
                JSONArray history = new JSONArray();
                for (GameState snapshot : engine.history()) {
                    history.put(GamePersistence.state(snapshot));
                }
                DailyChallenge challenge = session.challenge();
                record.put("session", new JSONObject()
                        .put("date", challenge.date().toEpochDay()).put("generatorVersion", challenge.version())
                        .put("target", challenge.targetMoves()).put("moves", session.moves())
                        .put("best", session.bestMoves()).put("original", GamePersistence.state(engine.original()))
                        .put("current", GamePersistence.state(engine.state())).put("history", history));
            }
            return preferences.edit().putString("daily", record.toString()).commit();
        } catch (JSONException error) {
            return false;
        }
    }

    Saved load() {
        String raw = preferences.getString("daily", null);
        if (raw == null) {
            return new Saved(null, new DailyProgress(), false);
        }
        try {
            JSONObject record = new JSONObject(raw);
            if (record.getInt("version") != 1) {
                throw new JSONException("Daily save version");
            }
            JSONArray dates = record.getJSONArray("completed");
            Set<LocalDate> completed = new HashSet<>();
            for (int i = 0; i < dates.length(); i++) {
                completed.add(LocalDate.ofEpochDay(dates.getLong(i)));
            }
            DailyProgress progress = new DailyProgress(completed);
            DailySession session = null;
            if (record.has("session")) {
                JSONObject saved = record.getJSONObject("session");
                GameState original = GamePersistence.read(saved.getJSONArray("original"), false);
                GameState current = GamePersistence.read(saved.getJSONArray("current"), false);
                ArrayList<GameState> history = new ArrayList<>();
                JSONArray snapshots = saved.getJSONArray("history");
                for (int i = 0; i < snapshots.length(); i++) {
                    history.add(GamePersistence.read(snapshots.getJSONArray(i), false));
                }
                DailyChallenge challenge = new DailyChallenge(LocalDate.ofEpochDay(saved.getLong("date")),
                        saved.getInt("generatorVersion"), original, saved.getInt("target"));
                session = new DailySession(challenge, new GameEngine(original, current, history),
                        saved.getInt("moves"), saved.getInt("best"));
                if (session.bestMoves() > 0 && session.bestMoves() <= challenge.targetMoves()
                        && !progress.completed(challenge.date())) {
                    throw new JSONException("Daily completion missing");
                }
            }
            boolean active = record.getBoolean("active");
            if (active && session == null) {
                throw new JSONException("Active daily attempt missing");
            }
            return new Saved(session, progress, active);
        } catch (Exception error) {
            android.util.Log.e("FreeColor", "Cannot restore daily progress", error);
            throw new IllegalStateException("Daily progress is invalid; original record retained", error);
        }
    }
}

