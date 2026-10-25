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

import org.junit.Test;

import static org.junit.Assert.*;

public class TiltedLiquidGeometryTest {
    @Test public void horizontalSurfacesPreserveAreaAcrossTiltAndFillLevels() {
        TiltedLiquidGeometry geometry = new TiltedLiquidGeometry();
        geometry.configure(80, 285, 40);
        for (float fill : new float[]{0, .1f, 1, 35, 140, 250}) {
            geometry.rotate(0);
            double area = geometry.areaBelow(-fill);
            for (float angle : new float[]{0, .01f, 15, 45, 75, 89.99f, 90}) {
                geometry.rotate(angle);
                float surface = geometry.surfaceForArea(area);
                assertEquals(area, geometry.areaBelow(surface), .02);
                assertTrue(Float.isFinite(geometry.centerXAt(surface)));
            }
        }
    }

    @Test public void drainingIncreasesTiltAndKeepsSurfaceAtTheLip() {
        TiltedLiquidGeometry geometry = new TiltedLiquidGeometry();
        geometry.configure(80, 285, 40);
        float previousAngle = 0;
        for (float fill : new float[]{250, 200, 140, 70, 35, 1, .1f}) {
            geometry.rotate(0);
            double area = geometry.areaBelow(-fill);
            float angle = geometry.pourAngle(area);
            assertTrue(angle > previousAngle);
            assertTrue(angle < 90);
            geometry.rotate(angle);
            double radians = Math.toRadians(angle);
            double lip = 40 * Math.sin(radians) - 285 * Math.cos(radians);
            assertEquals(lip, geometry.surfaceForArea(area), .002);
            previousAngle = angle;
        }
        assertEquals(90, geometry.pourAngle(0), .001);
    }

    @Test public void roundBottomAreaMatchesTheGlassContour() {
        TiltedLiquidGeometry geometry = new TiltedLiquidGeometry();
        geometry.configure(80, 285, 40);
        // Two quadratic corners remove one sixth of their bounding rectangles.
        double expected = 80 * 285 - 80 * 40 / 6.0;
        assertEquals(expected, geometry.areaBelow(-285), 4);
    }
}
