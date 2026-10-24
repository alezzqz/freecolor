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
import java.util.*;
public class CoreTest {
    private GameState s(int[]...t) {
        return new GameState(t,false);
    }
    @Test public void validationAndPour() {
        GameState a=s(new int[] {
            0,1,1
        }
        ,new int[] {
            1,1,1
        }
        ,new int[] {
            2
        }
        ,new int[] {
        }
        ,new int[] {
            0,0,0,0
        }
        );
        assertEquals(0,Rules.amount(a,new Move(3,0)));
        assertEquals(0,Rules.amount(a,new Move(0,4)));
        assertEquals(0,Rules.amount(a,new Move(0,2)));
        assertEquals(2,Rules.amount(a,new Move(0,3)));
        assertEquals(1,Rules.amount(a,new Move(0,1)));
        GameState b=Rules.pour(a,new Move(0,1));
        assertArrayEquals(new int[] {
            0,1
        }
        ,b.tube(0));
        assertArrayEquals(new int[] {
            1,1,1,1
        }
        ,b.tube(1));
        assertArrayEquals(new int[] {
            0,1,1
        }
        ,a.tube(0));
        assertEquals(0,Rules.amount(a,new Move(0,0)));
    }
    @Test public void wins() {
        assertTrue(s(new int[] {
            0,0,0,0
        }
        ,new int[] {
        }
        ).solved());
        assertFalse(s(new int[] {
            0
        }
        ).solved());
        assertFalse(s(new int[] {
            0,0,1,1
        }
        ).solved());
    }
    @Test public void historyAndRestart() {
        GameState a=s(new int[] {
            0,1
        }
        ,new int[] {
            1
        }
        ,new int[] {
        }
        );
        GameEngine e=new GameEngine(a);
        assertTrue(e.move(new Move(0,1)));
        GameState b=e.state();
        assertTrue(e.move(new Move(0,2)));
        assertTrue(e.undo());
        assertEquals(b,e.state());
        assertTrue(e.undo());
        assertEquals(a,e.state());
        assertFalse(e.undo());
        assertTrue(e.addTube());
        assertFalse(e.addTube());
        assertEquals(a.count()+1,e.state().count());
        assertTrue(e.undo());
        assertEquals(a,e.state());
        e.addTube();
        e.restart();
        assertEquals(a,e.state());
        assertTrue(e.history().isEmpty());
    }
    @Test public void completionClearsUndo() {
        GameEngine e=new GameEngine(s(new int[] {
            0,0
        }
        ,new int[] {
            0,0
        }
        ,new int[] {
        }
        ));
        assertTrue(e.move(new Move(0,1)));
        assertTrue(e.state().solved());
        assertFalse(e.undo());
    }
    @Test public void generationAndSolutionReplay() {
        LevelGenerator g=new LevelGenerator(new Random(73929));
        for (int n = 5; n <= 11; n++) {
            for(int trial=0;trial<100;trial++) {
                GameState a=g.generate(n);
                assertEquals(n+2,a.count());
                assertFalse(a.extra);
                assertFalse(a.solved());
                int[] counts=new int[n];
                for(int i=0;i<a.count();i++) {
                    assertEquals(i<n?4:0,a.size(i));
                    for(int c:a.tube(i)) {
                        counts[c]++;
                    }
                }
                for(int count:counts) {
                    assertEquals(4,count);
                }
                GameState snapshot=new GameState(a.copy(),a.extra,a.hiddenColors,a.revealedCopy());
                LevelSolver.Solution solution=new LevelSolver().solve(a);
                assertTrue(solution.solved());
                assertEquals(snapshot,a);
                GameEngine e=new GameEngine(a);
                e.addTube();
                for(Move m:solution.moves()) {
                    assertTrue(Rules.amount(e.state(),m)>0);
                    assertTrue(e.move(m));
                }
                assertTrue(e.state().solved());
            }
        }
    }
}

