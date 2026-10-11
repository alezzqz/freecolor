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
import static org.junit.Assert.*;

public class ExtraTubeTest {
    @Test public void extraAcceptsOnlyOneUnitAndUndoRestoresIt() {
        GameState original=new GameState(new int[][]{{0,1,1},{1},{}},false);
        GameEngine engine=new GameEngine(original);
        engine.addTube();
        int extra=engine.state().count()-1;
        assertEquals(1,engine.state().capacity(extra));
        assertEquals(1,Rules.amount(engine.state(),new Move(0,extra)));
        assertTrue(engine.move(new Move(0,extra)));
        assertArrayEquals(new int[]{1},engine.state().tube(extra));
        assertArrayEquals(new int[]{0,1},engine.state().tube(0));
        assertEquals(0,Rules.amount(engine.state(),new Move(0,extra)));
        assertTrue(engine.move(new Move(extra,1)));
        assertEquals(0,engine.state().size(extra));
        assertTrue(engine.undo());
        assertArrayEquals(new int[]{1},engine.state().tube(extra));
        engine.restart();
        assertEquals(original,engine.state());
    }

    @Test public void canonicalKeyDistinguishesDifferentCapacities() {
        GameState a=new GameState(new int[][]{{0},{},{}},true);
        GameState b=new GameState(new int[][]{{},{},{0}},true);
        assertNotEquals(a.canonical(),b.canonical());
    }

    @Test public void filledBufferDoesNotCountAsCompletedColor() {
        assertFalse(new GameState(new int[][]{{0,0,0},{},{0}},true).solved());
        assertTrue(new GameState(new int[][]{{0,0,0,0},{},{}},true).solved());
    }

    @Test(expected=IllegalArgumentException.class) public void rejectsBufferOverflow() {
        new GameState(new int[][]{{},{0,0}},true);
    }
}
