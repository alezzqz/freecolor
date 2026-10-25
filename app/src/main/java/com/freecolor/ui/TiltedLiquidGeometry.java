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

/** Area-preserving horizontal surfaces inside a rotated, round-bottomed tube. */
final class TiltedLiquidGeometry {
    private static final int CURVE_STEPS = 24;
    private static final int VERTICES = CURVE_STEPS * 2 + 3;
    private final double[] x = new double[VERTICES];
    private final double[] y = new double[VERTICES];
    private final double[] rotatedX = new double[VERTICES];
    private final double[] rotatedY = new double[VERTICES];
    private final double[] clippedX = new double[VERTICES + 2];
    private final double[] clippedY = new double[VERTICES + 2];
    private double minY;
    private double maxY;
    private double left;
    private double right;

    void configure(float width, float height, float curve) {
        double half = width / 2.0;
        x[0] = -half;
        y[0] = -height;
        x[1] = -half;
        y[1] = -curve;
        for (int i = 1; i <= CURVE_STEPS; i++) {
            double t = i / (double) CURVE_STEPS;
            x[1 + i] = -half * (1 - t * t);
            y[1 + i] = -curve * (1 - t) * (1 - t);
            x[1 + CURVE_STEPS + i] = half * (2 * t - t * t);
            y[1 + CURVE_STEPS + i] = -curve * t * t;
        }
        x[VERTICES - 1] = half;
        y[VERTICES - 1] = -height;
        rotate(0);
    }

    void rotate(float degrees) {
        double angle = Math.toRadians(degrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        minY = Double.POSITIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
        left = Double.POSITIVE_INFINITY;
        right = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < VERTICES; i++) {
            rotatedX[i] = x[i] * cos - y[i] * sin;
            rotatedY[i] = x[i] * sin + y[i] * cos;
            minY = Math.min(minY, rotatedY[i]);
            maxY = Math.max(maxY, rotatedY[i]);
            left = Math.min(left, rotatedX[i]);
            right = Math.max(right, rotatedX[i]);
        }
    }

    double areaBelow(double surface) {
        int count = 0;
        int previous = VERTICES - 1;
        for (int current = 0; current < VERTICES; current++) {
            boolean previousInside = rotatedY[previous] >= surface;
            boolean currentInside = rotatedY[current] >= surface;
            if (previousInside != currentInside) {
                double fraction = (surface - rotatedY[previous]) / (rotatedY[current] - rotatedY[previous]);
                clippedX[count] = rotatedX[previous] + fraction * (rotatedX[current] - rotatedX[previous]);
                clippedY[count++] = surface;
            }
            if (currentInside) {
                clippedX[count] = rotatedX[current];
                clippedY[count++] = rotatedY[current];
            }
            previous = current;
        }
        double twiceArea = 0;
        previous = count - 1;
        for (int current = 0; current < count; current++) {
            twiceArea += clippedX[previous] * clippedY[current] - clippedX[current] * clippedY[previous];
            previous = current;
        }
        return Math.abs(twiceArea) / 2;
    }

    float surfaceForArea(double area) {
        double low = minY;
        double high = maxY;
        // A bounded search avoids frame allocations and stays well below pixel precision.
        for (int i = 0; i < 24; i++) {
            double middle = (low + high) / 2;
            if (areaBelow(middle) > area) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (float) ((low + high) / 2);
    }

    float pourAngle(double area) {
        float low = 0;
        float high = 90;
        for (int i = 0; i < 24; i++) {
            float middle = (low + high) / 2;
            rotate(middle);
            if (areaBelow(rotatedY[VERTICES - 1]) > area) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) / 2;
    }

    float centerXAt(float surface) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int previous = VERTICES - 1;
        for (int current = 0; current < VERTICES; current++) {
            if ((rotatedY[previous] >= surface) != (rotatedY[current] >= surface)) {
                double fraction = (surface - rotatedY[previous]) / (rotatedY[current] - rotatedY[previous]);
                double intersection = rotatedX[previous] + fraction * (rotatedX[current] - rotatedX[previous]);
                min = Math.min(min, intersection);
                max = Math.max(max, intersection);
            }
            previous = current;
        }
        return Double.isFinite(min) ? (float) ((min + max) / 2) : 0;
    }

    float left() {
        return (float) left;
    }

    float right() {
        return (float) right;
    }

    float bottom() {
        return (float) maxY;
    }
}
