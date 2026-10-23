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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.widget.*;
import android.graphics.Typeface;

import com.freecolor.R;
import com.freecolor.core.*;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import android.app.AlertDialog;
import java.util.concurrent.*;

public final class MainActivity extends Activity {

    // Screen layout (dp) and typography (sp).
    private static final int SCREEN_PADDING_DP = 16;
    private static final int LEVEL_TEXT_SP = 20;
    private static final int STATUS_TEXT_SP = 15;
    private static final int CONTROL_SIZE_DP = 56;
    private static final int CONTROL_MARGIN_DP = 6;
    private static final int CONTROL_PADDING_DP = 14;
    private static final int UNDO_ICON_SIDE_PADDING_DP = 21;
    private static final int UNDO_ICON_TOP_PADDING_DP = 8;
    private static final int UNDO_ICON_BOTTOM_PADDING_DP = 28;
    private static final int UNDO_COUNT_TEXT_SP = 20;
    private static final int UNDO_TIMER_TEXT_SP = 13;
    private static final int UNDO_LABEL_HEIGHT_DP = 24;
    private static final int UNDO_LABEL_BOTTOM_MARGIN_DP = 6;
    private static final int LEVEL_RIBBON_WIDTH_DP = 200;
    private static final int LEVEL_RIBBON_HEIGHT_DP = 40;
    private static final int LEVEL_RIBBON_TOP_MARGIN_DP = 24;
    private static final int LEVEL_RIBBON_BOTTOM_MARGIN_DP = 10;
    private static final int SCREEN_BACKGROUND = 0xff171d35;
    private static final int STATUS_TEXT_COLOR = 0xffb1bcd5;
    private static final float DISABLED_CONTROL_ALPHA = .35f;

    // UI scheduling.
    private static final long COOLDOWN_TICK_MILLIS = 1000;
    private static final long VICTORY_DELAY_MILLIS = 900;
    private static final long GENERATION_RETRY_MILLIS = 2000;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private GameEngine engine;
    private GameEngine ordinaryEngine;
    private DailySession dailySession;
    private DailyProgress dailyProgress = new DailyProgress();
    private DailyPersistence dailyPersistence;
    private boolean dailyMode, storageReady, preparingDaily, saveFailed;
    private int generationId;
    private DailyModeCard modeCard;
    private Button todayButton;
    private TextView dailyInfo;
    private DailyResultScreen dailyResult;
    private LocalDate displayedDate = LocalDate.now();
    private long level = GameConfig.FIRST_LEVEL;
    private GamePersistence persistence;
    private GameView board;
    private TextView title, status;
    private ImageButton undo, restart;
    private boolean generating, destroyed;
    private FrameLayout screen;
    private VictoryScreen victoryScreen;
    private ExtraTubeCooldown extraCooldown = new ExtraTubeCooldown(0, System::currentTimeMillis);
    private UndoAllowance undoAllowance = new UndoAllowance(UndoAllowance.LIMIT, 0, System::currentTimeMillis);
    private TextView undoLabel;
    private int undoRewardWins;
    private int extraRewardWins;
    private int pendingRewardEffects;
    private final Runnable cooldownTick = new Runnable() {
        @Override
        public void run() {
            LocalDate today = LocalDate.now();
            if (!displayedDate.equals(today)) {
                displayedDate = today;
                if (dailyResult != null) {
                    screen.removeView(dailyResult);
                    dailyResult = null;
                    showVictory();
                }
            }
            refreshControls();
            board.postDelayed(this, COOLDOWN_TICK_MILLIS);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        persistence = new GamePersistence(this);
        dailyPersistence = new DailyPersistence(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(SCREEN_PADDING_DP), dp(SCREEN_PADDING_DP), dp(SCREEN_PADDING_DP), dp(SCREEN_PADDING_DP));
        root.setBackgroundColor(SCREEN_BACKGROUND);
        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(LEVEL_TEXT_SP);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setBackgroundResource(R.drawable.level_ribbon);
        status = new TextView(this);
        status.setTextColor(STATUS_TEXT_COLOR);
        status.setTextSize(STATUS_TEXT_SP);
        status.setGravity(Gravity.CENTER);
        board = new GameView(this);
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        restart = button("Заново", R.drawable.ic_restart, controls, () -> {
                    if (dailyMode) {
                        restartDaily();
                    } else {
                        engine.restart();
                        save();
                        refresh();
                    }
                }
        );
        undo = button("Отмена", R.drawable.ic_undo, controls, () -> {
                    if (dailyMode) {
                        dailySession.undo();
                    } else if (undoAllowance.remaining() > 0 && engine.undo()) {
                        undoAllowance.consume();
                    }
                    save();
                    refresh();
                }
        );
        FrameLayout undoFrame = new FrameLayout(this);
        android.view.ViewGroup.LayoutParams undoParams = undo.getLayoutParams();
        controls.removeView(undo);
        controls.addView(undoFrame, undoParams);
        undoFrame.addView(undo, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        undo.setPadding(dp(UNDO_ICON_SIDE_PADDING_DP), dp(UNDO_ICON_TOP_PADDING_DP), dp(UNDO_ICON_SIDE_PADDING_DP), dp(UNDO_ICON_BOTTOM_PADDING_DP));
        undoLabel = new TextView(this);
        undoLabel.setTextColor(Color.WHITE);
        undoLabel.setTextSize(UNDO_COUNT_TEXT_SP);
        undoLabel.setIncludeFontPadding(false);
        undoLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        undoLabel.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(MATCH_PARENT, dp(UNDO_LABEL_HEIGHT_DP), Gravity.BOTTOM);
        labelParams.bottomMargin = dp(UNDO_LABEL_BOTTOM_MARGIN_DP);
        undoFrame.addView(undoLabel, labelParams);
        modeCard = new DailyModeCard(this, () -> {
            if (dailyMode) {
                switchOrdinary();
            } else {
                openDaily();
            }
        });
        root.addView(controls, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams ribbonParams = new LinearLayout.LayoutParams(dp(LEVEL_RIBBON_WIDTH_DP), dp(LEVEL_RIBBON_HEIGHT_DP));
        ribbonParams.gravity = Gravity.CENTER_HORIZONTAL;
        ribbonParams.topMargin = dp(LEVEL_RIBBON_TOP_MARGIN_DP);
        ribbonParams.bottomMargin = dp(LEVEL_RIBBON_BOTTOM_MARGIN_DP);
        root.addView(title, ribbonParams);
        dailyInfo = new TextView(this);
        dailyInfo.setTextColor(STATUS_TEXT_COLOR);
        dailyInfo.setTextSize(13);
        dailyInfo.setGravity(Gravity.CENTER);
        root.addView(dailyInfo, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        statusParams.gravity = Gravity.CENTER_HORIZONTAL;
        statusParams.topMargin = dp(10);
        root.addView(status, statusParams);
        root.addView(board, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
        todayButton = new Button(this);
        todayButton.setText("Задача на сегодня");
        ActionButtonStyle.apply(todayButton);
        todayButton.setOnClickListener(view -> openDaily());
        LinearLayout.LayoutParams todayParams = new LinearLayout.LayoutParams(MATCH_PARENT, dp(48));
        todayParams.bottomMargin = dp(8);
        root.addView(todayButton, todayParams);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.topMargin = dp(16);
        root.addView(modeCard, cardParams);
        screen = new FrameLayout(this);
        screen.addView(root, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        setContentView(screen);
        board.listener = new GameView.Listener() {
            public void unlockExtra() {
                if (!dailyMode && !generating && !board.busy && engine != null && extraCooldown.available() && engine.addTube()) {
                    extraCooldown.acquired();
                    save();
                    refresh();
                }
            }

            public void pour(Move m) {
                GameState before = engine.state();
                if (dailyMode ? dailySession.move(m) : engine.move(m)) {
                    if (dailyMode) {
                        dailyProgress.record(dailySession);
                    } else if (engine.state().solved()) {
                        level++;
                        resetUnavailableRewards();
                        if (undoAllowance.remaining() < UndoAllowance.LIMIT && ++undoRewardWins == 3) {
                            undoRewardWins = 0;
                            pendingRewardEffects |= 1;
                        }
                        if (!extraCooldown.available() && ++extraRewardWins == 5) {
                            extraRewardWins = 0;
                            pendingRewardEffects |= 2;
                        }
                    }
                    save();
                    board.animatePour(before, engine.state(), m, () -> {
                                if (engine.state().solved()) {
                                    complete();
                                } else {
                                    refresh();
                                }
                            }
                    );
                    refreshControls();
                }
            }
        }
        ;
        GamePersistence.Saved saved;
        try {
            saved = persistence.load();
            DailyPersistence.Saved daily = dailyPersistence.load();
            dailySession = daily.session();
            dailyProgress = daily.progress();
            dailyMode = daily.active();
            storageReady = true;
        } catch (RuntimeException error) {
            title.setText("FreeColor");
            status.setText("Не удалось восстановить прогресс. Исходные данные сохранены.");
            refreshControls();
            return;
        }
        if (saved != null) {
            level = saved.level();
            ordinaryEngine = saved.engine();
            engine = dailyMode ? dailySession.engine() : ordinaryEngine;
            extraCooldown = new ExtraTubeCooldown(saved.extraAvailableAt(), System::currentTimeMillis);
            undoAllowance = new UndoAllowance(saved.undoRemaining(), saved.undoRefillAt(), System::currentTimeMillis);
            undoRewardWins = saved.undoRewardWins();
            extraRewardWins = saved.extraRewardWins();
            pendingRewardEffects = saved.pendingRewardEffects();
            refresh();
            if (saved.rewound() && !dailyMode) {
                status.setText("Прогресс возвращён до заполнения большой дополнительной пробирки");
            }
            if (engine.state().solved()) {
                showVictory();
            }
        } else if (dailyMode) {
            engine = dailySession.engine();
            refresh();
            if (engine.state().solved()) {
                showVictory();
            }
        } else {
            generate();
        }
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density);
    }

    private ImageButton button(String text, int icon, LinearLayout parent, Runnable r) {
        ImageButton b = new ImageButton(this);
        b.setContentDescription(text);
        b.setTooltipText(text);
        b.setImageResource(icon);
        b.setBackgroundResource(R.drawable.control_background);
        b.setPadding(dp(CONTROL_PADDING_DP), dp(CONTROL_PADDING_DP), dp(CONTROL_PADDING_DP), dp(CONTROL_PADDING_DP));
        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(CONTROL_SIZE_DP), dp(CONTROL_SIZE_DP));
        params.leftMargin = dp(CONTROL_MARGIN_DP);
        params.rightMargin = dp(CONTROL_MARGIN_DP);
        parent.addView(b, params);
        b.setOnClickListener(v -> {
                    if (!generating && !board.busy && engine != null) {
                        r.run();
                    }
                }
        );
        return b;
    }

    private void save() {
        if (!storageReady) {
            return;
        }
        boolean saved = true;
        if (ordinaryEngine != null) {
            saved = persistence.save(level, ordinaryEngine, extraCooldown.availableAt(),
                    undoAllowance.remaining(), undoAllowance.refillAt(), undoRewardWins, extraRewardWins,
                    pendingRewardEffects);
        }
        saveFailed = !dailyPersistence.save(dailySession, dailyProgress, dailyMode) || !saved;
        if (saveFailed) {
            status.setText("Не удалось сохранить прогресс");
        }
    }

    private void refreshControls() {
        resetUnavailableRewards();
        // Animation guards live in the click handlers so controls keep a stable appearance.
        boolean active = engine != null && !generating && !engine.state().solved();
        restart.setEnabled(active && (!dailyMode || dailySession.challenge().date().equals(LocalDate.now())));
        int remaining = undoAllowance.remaining();
        undo.setEnabled(active && !engine.history().isEmpty() && (dailyMode || remaining > 0));
        long seconds = (undoAllowance.waitMillis() + GameConfig.MILLIS_PER_SECOND - 1) / GameConfig.MILLIS_PER_SECOND;
        if (undoLabel != null) {
            undoLabel.setTextSize(dailyMode || remaining > 0 ? UNDO_COUNT_TEXT_SP : UNDO_TIMER_TEXT_SP);
            undoLabel.setText(dailyMode ? "∞" : remaining > 0 ? Integer.toString(remaining)
                    : String.format(Locale.ROOT, "%02d:%02d", seconds / GameConfig.SECONDS_PER_MINUTE,
                    seconds % GameConfig.SECONDS_PER_MINUTE));
        }
        undo.setContentDescription(dailyMode ? "Отмена без ограничений; потраченные ходы сохраняются"
                : remaining > 0 ? "Отмена: осталось " + remaining : "Отмена доступна через " + seconds + " секунд");
        board.inputEnabled = active && !board.busy;
        board.setExtraWaitMillis(extraCooldown.remainingMillis());
        boolean canSwitch = storageReady && !generating && engine != null;
        modeCard.setEnabled(canSwitch);
        todayButton.setEnabled(canSwitch);
        LocalDate today = LocalDate.now();
        int days = Math.min(GameConfig.DAILY_WEEK_GOAL, dailyProgress.weekCount(today));
        modeCard.update(dailyMode, dailyProgress.completed(today), days, GameConfig.DAILY_WEEK_GOAL, level);
        boolean previousDay = dailyMode && dailySession != null && !dailySession.challenge().date().equals(today);
        todayButton.setVisibility(previousDay ? android.view.View.VISIBLE : android.view.View.GONE);
        dailyInfo.setVisibility(dailyMode ? android.view.View.VISIBLE : android.view.View.GONE);
        if (dailyMode && dailySession != null) {
            int completedWeeks = dailyProgress.completedWeeks();
            String date = dailySession.challenge().date().format(
                    DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")));
            dailyInfo.setText(completedWeeks > 0
                    ? getString(R.string.daily_date_with_weeks, date, completedWeeks) : date);
        }
        if (dailyMode && dailySession != null && !generating) {
            status.setText(dailySession.bestMoves() > 0
                    ? getString(R.string.daily_moves_with_best, dailySession.moves(),
                            dailySession.challenge().targetMoves(), dailySession.bestMoves())
                    : getString(R.string.daily_moves, dailySession.moves(), dailySession.challenge().targetMoves()));
        }
        if (saveFailed) {
            status.setText("Не удалось сохранить прогресс");
        }
        status.setVisibility(status.getText().length() > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        undo.setTooltipText(dailyMode ? "Отмена не возвращает потраченные ходы" : "Отмена");
        restart.setAlpha(restart.isEnabled() ? 1f : DISABLED_CONTROL_ALPHA);
        undo.setAlpha(undo.isEnabled() ? 1f : DISABLED_CONTROL_ALPHA);
    }

    private void refresh() {
        title.setText(dailyMode ? "Задача дня" : MessageFormat.format("Уровень {0}", level));
        status.setText(generating ? preparingDaily ? "Готовим задачу дня…" : "Готовим новый уровень…" : "");
        status.setBackgroundResource(dailyMode ? R.drawable.daily_counter_background : 0);
        status.setPadding(dailyMode ? dp(16) : 0, dailyMode ? dp(8) : 0,
                dailyMode ? dp(16) : 0, dailyMode ? dp(8) : 0);
        status.setTextColor(dailyMode ? 0xffe1d9f6 : STATUS_TEXT_COLOR);
        board.setExtraAllowed(!dailyMode);
        if (engine != null) {
            board.setState(engine.state());
        }
        refreshControls();
    }

    private void resetUnavailableRewards() {
        if (undoAllowance.remaining() >= UndoAllowance.LIMIT) {
            undoRewardWins = 0;
        }
        if (extraCooldown.available()) {
            extraRewardWins = 0;
        }
    }

    private void showUndoReward() {
        showReward(undoLabel, undoLabel.getWidth() / 2f, undoLabel.getHeight() / 2f);
    }

    private void showPendingRewardEffects() {
        if (destroyed || dailyMode || generating || engine == null || engine.state().solved()) {
            return;
        }
        int effects = pendingRewardEffects;
        pendingRewardEffects = 0;
        if ((effects & 1) != 0) {
            showUndoReward();
        }
        if ((effects & 2) != 0) {
            android.graphics.PointF position = board.extraRewardPosition();
            showReward(board, position.x, position.y);
        }
        if (effects != 0) {
            save();
        }
    }

    private void showReward(android.view.View anchor, float anchorX, float anchorY) {
        int[] labelPosition = new int[2];
        int[] screenPosition = new int[2];
        anchor.getLocationInWindow(labelPosition);
        screen.getLocationInWindow(screenPosition);
        float centerX = labelPosition[0] - screenPosition[0] + anchorX;
        float centerY = labelPosition[1] - screenPosition[1] + anchorY;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        android.view.View sparks = new android.view.View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            @Override protected void onDraw(android.graphics.Canvas canvas) {
                float progress = (float) animator.getAnimatedValue();
                paint.setStrokeWidth(dp(1));
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                for (int index = 0; index < 18; index++) {
                    double angle = index * Math.PI * 2 / 18;
                    float distance = dp(10) + dp(26 + index % 3 * 5) * progress;
                    float sparkX = centerX + (float) Math.cos(angle) * distance;
                    float sparkY = centerY + (float) Math.sin(angle) * distance - dp(10) * progress;
                    float arm = dp(3) * (1 - progress);
                    int alpha = (int) (255 * (1 - progress));
                    paint.setColor(Color.argb(alpha / 4, 255, 220, 130));
                    canvas.drawCircle(sparkX, sparkY, arm * 2, paint);
                    paint.setColor(Color.argb(alpha, 255, index % 2 == 0 ? 215 : 250, 150));
                    canvas.drawLine(sparkX - arm, sparkY, sparkX + arm, sparkY, paint);
                    canvas.drawLine(sparkX, sparkY - arm, sparkX, sparkY + arm, paint);
                }
            }
        };
        sparks.layout(0, 0, screen.getWidth(), screen.getHeight());
        screen.getOverlay().add(sparks);
        animator.setDuration(900);
        animator.addUpdateListener(animation -> sparks.invalidate());
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                screen.getOverlay().remove(sparks);
            }
        });
        animator.start();
    }

    private void complete() {
        save();
        refreshControls();
        // Let the last tube's completion sparkle finish before the full-screen result.
        GameEngine finished = engine;
        board.postDelayed(() -> {
            if (!destroyed && engine == finished && engine.state().solved() && !generating) {
                showVictory();
            }
        }, VICTORY_DELAY_MILLIS);
    }

    private void showVictory() {
        if (dailyMode) {
            if (dailyResult == null) {
                boolean current = dailySession.challenge().date().equals(LocalDate.now());
                dailyResult = new DailyResultScreen(this, dailySession, dailyProgress,
                        current ? this::restartDaily : null, this::switchOrdinary,
                        current ? null : this::openDaily);
                screen.addView(dailyResult, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            }
            return;
        }
        if (victoryScreen != null) {
            return;
        }
        victoryScreen = new VictoryScreen(this, () -> {
            if (!generating) {
                generate();
            }
        });
        screen.addView(victoryScreen, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    private void generate() {
        generating = true;
        preparingDaily = false;
        final int request = ++generationId;
        refresh();
        final long requestedLevel = level;
        worker.execute(() -> {
                    try {
                        GameState next = new LevelGenerator().generateForLevel(requestedLevel);
                        runOnUiThread(() -> {
                                    if (destroyed || request != generationId) {
                                        return;
                                    }
                                    // Commit rewards only when the next level is ready to replace the completed one.
                                    if ((pendingRewardEffects & 1) != 0) {
                                        undoAllowance.reward();
                                    }
                                    if ((pendingRewardEffects & 2) != 0) {
                                        extraCooldown.reward();
                                    }
                                    engine = new GameEngine(next);
                                    ordinaryEngine = engine;
                                    generating = false;
                                    save();
                                    refresh();
                                    if (victoryScreen != null) {
                                        screen.removeView(victoryScreen);
                                        victoryScreen = null;
                                    }
                                    board.postDelayed(this::showPendingRewardEffects, 180);
                                }
                        );
                    } catch (Exception e) {
                        android.util.Log.e("FreeColor", "Generation failed", e);
                        runOnUiThread(() -> {
                                    if (destroyed || request != generationId) {
                                        return;
                                    }
                                    status.setText("Повторяем проверку уровня…");
                                    if (victoryScreen != null) {
                                        generating = false;
                                        victoryScreen.allowRetry();
                                        return;
                                    }
                                    board.postDelayed(() -> {
                                                if (!destroyed && request == generationId) {
                                                    generate();
                                                }
                                            }
                                            , GENERATION_RETRY_MILLIS);
                                }
                        );
                    }
                }
        );
    }

    private void clearResults() {
        if (victoryScreen != null) {
            screen.removeView(victoryScreen);
            victoryScreen = null;
        }
        if (dailyResult != null) {
            screen.removeView(dailyResult);
            dailyResult = null;
        }
    }

    private boolean canChangeMode() {
        return storageReady && !generating && !board.busy;
    }

    private void switchOrdinary() {
        if (!canChangeMode()) {
            return;
        }
        clearResults();
        dailyMode = false;
        engine = ordinaryEngine;
        save();
        if (engine == null) {
            generate();
        } else {
            refresh();
            if (engine.state().solved()) {
                showVictory();
            }
        }
    }

    private void activateDaily() {
        clearResults();
        dailyMode = true;
        engine = dailySession.engine();
        save();
        refresh();
        if (engine.state().solved()) {
            showVictory();
        }
    }

    private void openDaily() {
        if (!canChangeMode()) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (dailySession != null && dailySession.challenge().date().equals(today)) {
            activateDaily();
        } else if (dailySession != null && !dailySession.engine().state().solved() && dailySession.moves() > 0) {
            new AlertDialog.Builder(this).setTitle("Есть незаконченная задача")
                    .setMessage("Можно закончить прежнюю задачу или заменить её задачей на сегодня.")
                    .setPositiveButton("Продолжить прежнюю", (dialog, which) -> activateDaily())
                    .setNegativeButton("Новая задача", (dialog, which) -> generateDaily(LocalDate.now()))
                    .show();
        } else {
            generateDaily(today);
        }
    }

    private void restartDaily() {
        if (!canChangeMode()) {
            return;
        }
        if (!dailySession.challenge().date().equals(LocalDate.now())) {
            openDaily();
            return;
        }
        clearResults();
        dailySession.restart();
        save();
        refresh();
    }

    private void generateDaily(LocalDate date) {
        if (!canChangeMode()) {
            return;
        }
        clearResults();
        generating = true;
        preparingDaily = true;
        final int request = ++generationId;
        refresh();
        worker.execute(() -> {
            try {
                DailyChallenge challenge = new DailyGenerator().generate(date);
                runOnUiThread(() -> {
                    if (destroyed || request != generationId) {
                        return;
                    }
                    generating = false;
                    preparingDaily = false;
                    dailySession = new DailySession(challenge);
                    activateDaily();
                });
            } catch (CancellationException cancelled) {
                // Activity destruction interrupts bounded search without selecting a different board.
            } catch (Exception error) {
                android.util.Log.e("FreeColor", "Daily generation failed", error);
                runOnUiThread(() -> {
                    if (destroyed || request != generationId) {
                        return;
                    }
                    generating = false;
                    preparingDaily = false;
                    refreshControls();
                    status.setText("Не удалось подготовить задачу дня. Попробуйте снова.");
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        board.removeCallbacks(cooldownTick);
        cooldownTick.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        board.removeCallbacks(cooldownTick);
        save();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        board.stopAnimation();
        worker.shutdownNow();
        super.onDestroy();
    }
}
