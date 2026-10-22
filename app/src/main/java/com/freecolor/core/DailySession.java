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

import java.util.Objects;

/** Attempt accounting is separate from undo snapshots; probing and undoing still costs moves. */
public final class DailySession {
    private final DailyChallenge challenge;
    private final GameEngine engine;
    private int moves;
    private int bestMoves;

    public DailySession(DailyChallenge challenge) {
        this(challenge, new GameEngine(challenge.initial()), 0, 0);
    }

    public DailySession(DailyChallenge challenge, GameEngine engine, int moves, int bestMoves) {
        this.challenge = Objects.requireNonNull(challenge);
        this.engine = Objects.requireNonNull(engine);
        if (!engine.original().equals(challenge.initial()) || moves < 0 || bestMoves < 0
                || moves < engine.history().size() || !compatible(engine.state())
                || (engine.state().solved() && (moves == 0 || bestMoves == 0 || bestMoves > moves))) {
            throw new IllegalArgumentException("Invalid daily attempt");
        }
        for (GameState snapshot : engine.history()) {
            if (!compatible(snapshot)) {
                throw new IllegalArgumentException("Invalid daily undo state");
            }
        }
        this.moves = moves;
        this.bestMoves = bestMoves;
    }

    private boolean compatible(GameState state) {
        return !state.extra && LevelValidator.compatible(challenge.initial(), state);
    }

    public DailyChallenge challenge() {
        return challenge;
    }

    public GameEngine engine() {
        return engine;
    }

    public int moves() {
        return moves;
    }

    public int bestMoves() {
        return bestMoves;
    }

    public boolean move(Move move) {
        if (moves == Integer.MAX_VALUE || !engine.move(move)) {
            return false;
        }
        moves++;
        if (engine.state().solved() && (bestMoves == 0 || moves < bestMoves)) {
            bestMoves = moves;
        }
        return true;
    }

    public boolean undo() {
        return engine.undo();
    }

    public void restart() {
        engine.restart();
        moves = 0;
    }

    public boolean successful() {
        return engine.state().solved() && moves <= challenge.targetMoves();
    }
}

