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

import java.time.LocalDate;
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

    public int weekCount(LocalDate date) {
        return summarize(date).days();
    }

    public int completedWeeks() {
        return summarize(LocalDate.MAX).weeks();
    }

    private Summary summarize(LocalDate date) {
        LocalDate start = null;
        LocalDate last = null;
        int days = 0;
        int weeks = 0;
        // Rebuild from saved dates so retries and restoration use exactly the same series boundaries.
        for (LocalDate day : new TreeSet<>(completed)) {
            if (day.isAfter(date)) {
                break;
            }
            if (start == null || days == GameConfig.DAILY_WEEK_GOAL
                    || day.toEpochDay() - start.toEpochDay() >= GameConfig.DAILY_WEEK_DAYS) {
                start = day;
                days = 0;
            }
            days++;
            last = day;
            if (days == GameConfig.DAILY_WEEK_GOAL) {
                weeks++;
            }
        }
        if (start != null && (days == GameConfig.DAILY_WEEK_GOAL ? date.isAfter(last)
                : date.toEpochDay() - start.toEpochDay() >= GameConfig.DAILY_WEEK_DAYS)) {
            days = 0;
        }
        return new Summary(days, weeks);
    }

    private record Summary(int days, int weeks) {}
}
