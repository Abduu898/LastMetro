package com.lastmetro.view;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Locale;

/** Paris Métro kiosk drawn in logical game coordinates shared with pointer input. */
public final class TicketKiosk {
    private static final int X = 628, Y = 76, W = 560, H = 552, CENTER = X + W / 2;
    private static final int ROW_X = X + 36, ROW_W = W - 72, ROW_Y = 348, ROW_H = 44, STEP = ROW_H + 10;
    private static final String[] LABELS = {"CATCH THE LAST METRO", "ENDLESS COMMUTE", "HOW TO PLAY", "CONTROLS & SETTINGS"};
    private static final String[] KEYS = {"1", "2", "H", "O"};
    public static final int OPTIONS = 4;
    public static final Rectangle2D BACK = new Rectangle2D(ROW_X, 549, ROW_W, 46);
    public static final Rectangle2D JUMP = control(244);
    public static final Rectangle2D SLIDE = control(307);
    public static final Rectangle2D SOUND = control(370);
    public static final Rectangle2D MOTION = control(433);

    private static final Color PAPER = Color.web("#F3EED9"), INK = Color.web("#1E1B18");
    private static final Color MUTED = Color.web("#5C5346"), RECESS = Color.web("#E6DEC3");
    private static final Color RULE = Color.web("#B8AC8C"), JADE = Color.web("#007852");
    private static final Color GOLD = Color.web("#FFCE00"), MAGENTA = Color.web("#A0006E");
    private static final Color AMBER = Color.web("#D99B26"), NAVY = Color.web("#243447");
    private static final Color SHADOW = Color.web("#0B101C");

    private static Rectangle2D control(int y) { return new Rectangle2D(ROW_X, y, ROW_W, 48); }

    private static Rectangle2D row(int index) { return new Rectangle2D(ROW_X, ROW_Y + index * STEP, ROW_W, ROW_H); }

    public static int optionAt(double x, double y) {
        for (int i = 0; i < OPTIONS; i++) if (row(i).contains(x, y)) return i;
        return -1;
    }

    void draw(GraphicsContext g, String font, int selected, int pressed, double elapsed, boolean still) {
        g.save();
        double progress = Math.min(1, elapsed / .18);
        if (!still) g.translate(0, -Math.ceil(8 * Math.pow(1 - progress, 2)) * 2);
        frame(g, font);
        TicketPaper.lineBadge(g, font, CENTER - 107, 143, "1", GOLD, INK);
        TicketPaper.lineBadge(g, font, CENTER + 107, 143, "4", MAGENTA, Color.WHITE);
        center(g, font, "DERNIÈRE RAME", CENTER, 148, 12, MUTED);
        center(g, font, "LAST MÉTRO", CENTER + 3, 205, 43, AMBER);
        center(g, font, "LAST MÉTRO", CENTER, 202, 43, INK);
        center(g, font, "ONE CITY. ONE LAST TRAIN. RUN FOR IT.", CENTER, 226, 10, MUTED);
        perforation(g, 239);
        route(g, font, selected == 1);

        for (int i = 0; i < OPTIONS; i++) {
            Rectangle2D box = row(i);
            boolean active = i == selected, down = i == pressed;
            face(g, box, active, down);
            double y = box.getMinY() + (down ? 2 : 0);
            if (active && (still || (int) (elapsed * 4) % 2 == 0)) arrow(g, ROW_X + 11, y + 16, INK);
            fill(g, INK, ROW_X + 28, y + 8, 26, 26);
            fill(g, NAVY, ROW_X + 30, y + 10, 22, 22);
            center(g, font, KEYS[i], ROW_X + 41, y + 26, 13, PAPER);
            text(g, font, LABELS[i], ROW_X + 69, y + 27, 13, INK, TextAlignment.LEFT);
            if (active) text(g, font, "VALIDER ↵", ROW_X + ROW_W - 14, y + 26, 9, INK, TextAlignment.RIGHT);
        }
        perforation(g, 568);
        center(g, font, "CLICK TO PLAY  •  USE [1] [2] [H] [O] OR ARROWS + ENTER", CENTER, 590, 8, MUTED);
        g.restore();
    }

    void help(GraphicsContext g, String font, String jump, String slide, double px, double py) {
        frame(g, font);
        title(g, font, "HOW TO PLAY");
        String[] labels = {upper(jump), upper(slide), "P / ESC", "COINS", "NAVIGO", "HEARTS", "MODES"};
        String[] descriptions = {
                "HOLD: HIGH JUMP / TAP: SHORT HOP",
                "SLIDE / FAST-FALL IN THE AIR",
                "PAUSE OR RESUME",
                "BUILD A STREAK FOR UP TO 4x POINTS",
                "FIVE-SECOND COIN MAGNET",
                "RESTORE A LIFE, UP TO THREE",
                "500 M IN 50S / ENDLESS: SURVIVE"
        };
        for (int i = 0; i < labels.length; i++) {
            int y = 225 + i * 43;
            fill(g, RECESS, ROW_X, y - 23, ROW_W, 34);
            text(g, font, labels[i], ROW_X + 12, y, 9, INK, TextAlignment.LEFT);
            text(g, font, descriptions[i], ROW_X + ROW_W - 12, y, 9, MUTED, TextAlignment.RIGHT);
        }
        back(g, font, px, py);
    }

    void settings(GraphicsContext g, String font, String jump, String slide, String binding,
                  boolean sound, boolean reduced, double px, double py) {
        frame(g, font);
        title(g, font, "CONTROLS & SETTINGS");
        center(g, font, "CLICK A CONTROL OR PRESS J / S TO REBIND", CENTER, 219, 10, MUTED);
        setting(g, font, JUMP, "J  JUMP", binding.equals("jump") ? "PRESS A KEY..." : upper(jump), binding.equals("jump"), px, py);
        setting(g, font, SLIDE, "S  SLIDE", binding.equals("slide") ? "PRESS A KEY..." : upper(slide), binding.equals("slide"), px, py);
        setting(g, font, SOUND, "F8  SOUND", sound ? "ON" : "OFF", false, px, py);
        setting(g, font, MOTION, "F9  REDUCED MOTION", reduced ? "ON" : "OFF", false, px, py);
        String note = switch (binding) {
            case "reserved" -> "THAT KEY IS RESERVED. CHOOSE ANOTHER.";
            case "unsaved" -> "SETTINGS COULD NOT BE SAVED.";
            case "jump", "slide" -> "ESC CANCELS KEY BINDING";
            default -> "P / ESC / ENTER / F8 / F9 ARE RESERVED";
        };
        center(g, font, note, CENTER, 512, 9, MUTED);
        back(g, font, px, py);
    }

    private void frame(GraphicsContext g, String font) {
        g.setImageSmoothing(false);
        TicketPaper.path(g, X + 5, Y + 5, W, H, true);
        g.setFill(SHADOW); g.fill();
        TicketPaper.path(g, X, Y, W, H, true);
        g.setFill(PAPER); g.fill();
        g.setStroke(INK); g.setLineWidth(3); g.stroke();
        fill(g, JADE, X + 3, Y + 8, W - 6, 36);
        fill(g, Color.web("#009968"), X + 3, Y + 8, W - 6, 2);
        fill(g, INK, X + 3, Y + 44, W - 6, 2);
        text(g, font, "RATP • BILLETTERIE AUTOMATIQUE", X + 19, Y + 31, 10, PAPER, TextAlignment.LEFT);
        fill(g, GOLD, X + W - 172, Y + 15, 154, 22);
        center(g, font, "PARIS • 01:14 AM", X + W - 95, Y + 30, 9, INK);
    }

    private void route(GraphicsContext g, String font, boolean endless) {
        fill(g, RULE, ROW_X, 250, ROW_W, 83);
        fill(g, RECESS, ROW_X + 2, 252, ROW_W - 4, 79);
        text(g, font, endless ? "ENDLESS COMMUTE" : "THE NIGHT RUN", ROW_X + 12, 269, 10, INK, TextAlignment.LEFT);
        text(g, font, endless ? "∞ METRES  •  3♥" : "500M  •  50S  •  3♥",
                ROW_X + ROW_W - 12, 269, 10, INK, TextAlignment.RIGHT);
        text(g, font, "QUAI", ROW_X + 16, 295, 9, MUTED, TextAlignment.LEFT);
        text(g, font, endless ? "SURVIVAL RUN" : "CHÂTELET", ROW_X + ROW_W - 16, 295, 9, MUTED, TextAlignment.RIGHT);
        int start = ROW_X + 74;
        fill(g, GOLD, start, 287, 168, 7);
        fill(g, MAGENTA, start + 168, 287, endless ? 96 : 119, 7);
        for (int stop : new int[] {start, start + 89, start + 178}) {
            fill(g, INK, stop - 5, 285, 10, 11);
            if (stop != start) fill(g, PAPER, stop - 3, 287, 6, 7);
        }
        if (endless) {
            arrow(g, start + 255, 284, INK);
            arrow(g, start + 265, 284, INK);
        } else {
            fill(g, INK, start + 278, 283, 15, 15);
            fill(g, PAPER, start + 281, 286, 9, 9);
            fill(g, JADE, start + 283, 288, 5, 5);
        }
        center(g, font, endless ? "PACE BUILDS WITH DISTANCE  •  CHASE HIGH SCORE"
                : "COLLECT COINS  •  DODGE BARRIERS  •  CATCH THE TRAIN", CENTER, 317, 8, MUTED);
    }

    private void title(GraphicsContext g, String font, String title) {
        center(g, font, title, CENTER + 2, 176, 23, AMBER);
        center(g, font, title, CENTER, 174, 23, INK);
        perforation(g, 191);
    }

    private void setting(GraphicsContext g, String font, Rectangle2D box, String label, String value,
                         boolean active, double px, double py) {
        face(g, box, active || box.contains(px, py), false);
        text(g, font, label, box.getMinX() + 14, box.getMinY() + 29, 12, INK, TextAlignment.LEFT);
        text(g, font, value, box.getMaxX() - 14, box.getMinY() + 29, 11, INK, TextAlignment.RIGHT);
    }

    private void back(GraphicsContext g, String font, double px, double py) {
        perforation(g, 534);
        fill(g, INK, BACK.getMinX(), BACK.getMinY(), BACK.getWidth(), BACK.getHeight());
        fill(g, BACK.contains(px, py) ? Color.web("#31465F") : NAVY,
                BACK.getMinX() + 2, BACK.getMinY() + 2, BACK.getWidth() - 4, BACK.getHeight() - 4);
        fill(g, Color.web("#3B526E"), BACK.getMinX() + 2, BACK.getMinY() + 2, BACK.getWidth() - 4, 2);
        fill(g, SHADOW, BACK.getMinX() + 2, BACK.getMaxY() - 6, BACK.getWidth() - 4, 4);
        center(g, font, "[ESC] BACK TO KIOSK", CENTER, 578, 12, PAPER);
    }

    private void face(GraphicsContext g, Rectangle2D box, boolean active, boolean down) {
        double x = box.getMinX(), y = box.getMinY() + (down ? 2 : 0), w = box.getWidth();
        double h = box.getMaxY() - y;
        fill(g, active ? INK : RULE, x, y, w, h);
        fill(g, active ? AMBER : RECESS, x + 2, y + 2, w - 4, h - 4);
        if (active) {
            fill(g, Color.web("#F4C150"), x + 2, y + 2, w - 4, 2);
            fill(g, Color.web("#8C5E12"), x + 2, y + h - (down ? 3 : 5), w - 4, down ? 1 : 3);
        }
    }

    private void perforation(GraphicsContext g, int y) {
        for (int x = ROW_X; x + 6 <= ROW_X + ROW_W; x += 10) fill(g, RULE, x, y, 6, 2);
    }

    private static String upper(String value) { return value.toUpperCase(Locale.ROOT); }

    private static void arrow(GraphicsContext g, double x, double y, Color color) {
        for (int i = 0; i < 3; i++) fill(g, color, x + i * 3, y + i * 2, 3, 12 - i * 4);
    }

    private static void center(GraphicsContext g, String font, String value, double x, double y, int size, Color color) {
        text(g, font, value, x, y, size, color, TextAlignment.CENTER);
    }

    private static void text(GraphicsContext g, String font, String value, double x, double y, int size,
                             Color color, TextAlignment alignment) {
        g.setFont(Font.font(font, size));
        g.setTextAlign(alignment);
        g.setFill(color);
        g.fillText(value, x, y);
    }

    private static void fill(GraphicsContext g, Color color, double x, double y, double w, double h) {
        g.setFill(color);
        g.fillRect(x, y, w, h);
    }
}
