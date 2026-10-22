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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Dates make completion idempotent across retries, process restarts and clock changes. */
public final class DailyProgress {
    private final Set<LocalDate> completed;

    public DailyProgress() {
        this(Set.of());
    }

    public DailyProgress(Collection<LocalDate> completed) {
        this.completed = new HashSet<>(completed);
        for (LocalDate date : this.completed) {
            Objects.requireNonNull(date);
        }
    }

    public Set<LocalDate> completedDates() {
        return Set.copyOf(completed);
    }

    public boolean completed(LocalDate date) {
        return completed.contains(date);
    }

    public boolean record(DailySession session) {
        return session.successful() && completed.add(session.challenge().date());
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public int weekCount(LocalDate date) {
        LocalDate start = weekStart(date);
        int count = 0;
        for (LocalDate day : completed) {
            if (!day.isBefore(start) && day.isBefore(start.plusWeeks(1))) {
                count++;
            }
        }
        return count;
    }

    public int completedWeeks() {
        Map<LocalDate, Integer> weeks = new HashMap<>();
        for (LocalDate day : completed) {
            weeks.merge(weekStart(day), 1, Integer::sum);
        }
        int count = 0;
        for (int days : weeks.values()) {
            if (days >= GameConfig.DAILY_WEEK_GOAL) {
                count++;
            }
        }
        return count;
    }
}

