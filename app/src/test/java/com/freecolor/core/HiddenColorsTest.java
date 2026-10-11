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

public class HiddenColorsTest {
    @Test public void initiallyShowsOnlyTopSegment() {
        GameState state=new GameState(new int[][]{{0,1,2,2},{1,0},{},{}},false).withHiddenColors(true);
        assertArrayEquals(new boolean[]{false,false,false,true},state.revealedTube(0));
        assertArrayEquals(new boolean[]{false,true},state.revealedTube(1));
    }

    @Test public void pourRevealsAndUndoKeepsKnowledge() {
        GameState initial=new GameState(new int[][]{{0,1,2,2},{1,0},{},{}},false).withHiddenColors(true);
        GameEngine engine=new GameEngine(initial);
        assertTrue(engine.move(new Move(0,2)));
        assertArrayEquals(new boolean[]{false,true},engine.state().revealedTube(0));
        assertArrayEquals(new boolean[]{true,true},engine.state().revealedTube(2));
        assertTrue(engine.undo());
        assertArrayEquals(initial.tube(0),engine.state().tube(0));
        assertArrayEquals(new boolean[]{false,true,true,true},engine.state().revealedTube(0));
        engine.move(new Move(0,2));
        engine.restart();
        assertEquals(initial,engine.state());
    }

    @Test public void multipleUndosKeepRevealedUnitsAfterRestore() {
        GameState initial=new GameState(new int[][]{{0,1,2,2},{1,0},{},{}},false).withHiddenColors(true);
        GameEngine engine=new GameEngine(initial);
        assertTrue(engine.move(new Move(0,2)));
        assertTrue(engine.move(new Move(0,3)));
        engine=new GameEngine(initial,engine.state(),engine.history());
        assertTrue(engine.undo());
        assertTrue(engine.undo());
        assertArrayEquals(initial.tube(0),engine.state().tube(0));
        assertArrayEquals(new boolean[]{true,true,true,true},engine.state().revealedTube(0));
        assertArrayEquals(initial.revealedTube(1),engine.state().revealedTube(1));
        assertTrue(engine.addTube());
        assertTrue(engine.undo());
        assertArrayEquals(new boolean[]{true,true,true,true},engine.state().revealedTube(0));
        engine.restart();
        assertEquals(initial,engine.state());
    }

    @Test public void knownUnitsRemainKnownWhenCoveredAndRestored() {
        GameState initial=new GameState(new int[][]{{0,1},{1},{},{}},false).withHiddenColors(true);
        GameState after=Rules.pour(initial,new Move(0,2));
        assertArrayEquals(new boolean[]{true},after.revealedTube(0));
        after=Rules.pour(after,new Move(1,2));
        assertArrayEquals(new boolean[]{true,true},after.revealedTube(2));
        GameState restored=new GameState(after.copy(),after.extra,after.hiddenColors,after.revealedCopy());
        assertEquals(after,restored);
        GameState extra=after.addTube();
        assertTrue(extra.hiddenColors);
        assertArrayEquals(after.revealedTube(2),extra.revealedTube(2));
        assertEquals(1,extra.capacity(extra.count()-1));
    }

    @Test public void generatorProducesBothModesAndKeepsThemSolvable() {
        LevelGenerator generator=new LevelGenerator(new Random(2299));
        boolean ordinary=false,hidden=false;
        for(int i=0;i<40;i++) {
            GameState state=generator.generate();
            ordinary|=!state.hiddenColors;hidden|=state.hiddenColors;
            LevelSolver.Solution solution=new LevelSolver().solve(state);
            assertTrue(solution.solved());
            GameEngine engine=new GameEngine(state);
            engine.addTube();
            for(Move move:solution.moves()) {
                assertTrue(engine.move(move));
            }
            assertTrue(engine.state().solved());
        }
        assertTrue(ordinary);assertTrue(hidden);
    }
}
