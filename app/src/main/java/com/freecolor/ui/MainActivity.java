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
    private static final long VICTORY_DELAY_MILLIS = 650;
    private static final long GENERATION_RETRY_MILLIS = 2000;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private GameEngine engine;
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
    private final Runnable cooldownTick = new Runnable() {
        @Override
        public void run() {
            board.setExtraWaitMillis(extraCooldown.remainingMillis());
            refreshControls();
            board.postDelayed(this, COOLDOWN_TICK_MILLIS);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        persistence = new GamePersistence(this);
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
                    engine.restart();
                    save();
                    refresh();
                }
        );
        undo = button("Отмена", R.drawable.ic_undo, controls, () -> {
                    if (undoAllowance.remaining() > 0 && engine.undo()) {
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
        root.addView(controls, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams ribbonParams = new LinearLayout.LayoutParams(dp(LEVEL_RIBBON_WIDTH_DP), dp(LEVEL_RIBBON_HEIGHT_DP));
        ribbonParams.gravity = Gravity.CENTER_HORIZONTAL;
        ribbonParams.topMargin = dp(LEVEL_RIBBON_TOP_MARGIN_DP);
        ribbonParams.bottomMargin = dp(LEVEL_RIBBON_BOTTOM_MARGIN_DP);
        root.addView(title, ribbonParams);
        root.addView(status);
        root.addView(board, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
        screen = new FrameLayout(this);
        screen.addView(root, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        setContentView(screen);
        board.listener = new GameView.Listener() {
            public void unlockExtra() {
                if (!generating && !board.busy && engine != null && extraCooldown.available() && engine.addTube()) {
                    extraCooldown.acquired();
                    save();
                    refresh();
                }
            }

            public void pour(Move m) {
                GameState before = engine.state();
                if (engine.move(m)) {
                    if (engine.state().solved()) {
                        level++;
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
        } catch (RuntimeException error) {
            title.setText("FreeColor");
            status.setText("Не удалось восстановить прогресс. Исходные данные сохранены.");
            refreshControls();
            return;
        }
        if (saved != null) {
            level = saved.level();
            engine = saved.engine();
            extraCooldown = new ExtraTubeCooldown(saved.extraAvailableAt(), System::currentTimeMillis);
            undoAllowance = new UndoAllowance(saved.undoRemaining(), saved.undoRefillAt(), System::currentTimeMillis);
            refresh();
            if (saved.rewound()) {
                status.setText("Прогресс возвращён до заполнения большой дополнительной пробирки");
            }
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
        if (engine != null && !persistence.save(level, engine, extraCooldown.availableAt(), undoAllowance.remaining(), undoAllowance.refillAt())) {
            status.setText("Не удалось сохранить прогресс");
        }
    }

    private void refreshControls() {
        boolean active = engine != null && !generating && !board.busy && !engine.state().solved();
        restart.setEnabled(active);
        int remaining = undoAllowance.remaining();
        undo.setEnabled(active && !engine.history().isEmpty() && remaining > 0);
        long seconds = (undoAllowance.waitMillis() + GameConfig.MILLIS_PER_SECOND - 1) / GameConfig.MILLIS_PER_SECOND;
        if (undoLabel != null) {
            undoLabel.setTextSize(remaining > 0 ? UNDO_COUNT_TEXT_SP : UNDO_TIMER_TEXT_SP);
            undoLabel.setText(remaining > 0 ? Integer.toString(remaining) : String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / GameConfig.SECONDS_PER_MINUTE, seconds % GameConfig.SECONDS_PER_MINUTE));
        }
        undo.setContentDescription(remaining > 0 ? "Отмена: осталось " + remaining : "Отмена доступна через " + seconds + " секунд");
        board.inputEnabled = active;
        board.setExtraWaitMillis(extraCooldown.remainingMillis());
        restart.setAlpha(restart.isEnabled() ? 1f : DISABLED_CONTROL_ALPHA);
        undo.setAlpha(undo.isEnabled() ? 1f : DISABLED_CONTROL_ALPHA);
    }

    private void refresh() {
        title.setText(MessageFormat.format("Уровень {0}", level));
        status.setText(generating ? "Готовим новый уровень…" : "");
        if (engine != null) {
            board.setState(engine.state());
        }
        refreshControls();
    }

    private void complete() {
        save();
        refreshControls();
        // Let the last tube's completion sparkle finish before the full-screen result.
        board.postDelayed(() -> {
            if (!destroyed) {
                showVictory();
            }
        }, VICTORY_DELAY_MILLIS);
    }

    private void showVictory() {
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
        refresh();
        final long requestedLevel = level;
        worker.execute(() -> {
                    try {
                        GameState next = new LevelGenerator().generateForLevel(requestedLevel);
                        runOnUiThread(() -> {
                                    if (destroyed) {
                                        return;
                                    }
                                    engine = new GameEngine(next);
                                    generating = false;
                                    save();
                                    refresh();
                                    if (victoryScreen != null) {
                                        screen.removeView(victoryScreen);
                                        victoryScreen = null;
                                    }
                                }
                        );
                    } catch (Exception e) {
                        android.util.Log.e("FreeColor", "Generation failed", e);
                        runOnUiThread(() -> {
                                    if (destroyed) {
                                        return;
                                    }
                                    status.setText("Повторяем проверку уровня…");
                                    if (victoryScreen != null) {
                                        generating = false;
                                        victoryScreen.allowRetry();
                                        return;
                                    }
                                    board.postDelayed(() -> {
                                                if (!destroyed) {
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
