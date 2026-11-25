package com.lastmetro.view;

import com.lastmetro.model.GameWorld;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Locale;

/** Canvas ticket for a successful run. Coordinates use the shared logical viewport. */
public final class VictoryTicket {
    private static final int X = 370, Y = 142, W = 540, H = 456;
    public static final Rectangle2D RETRY = new Rectangle2D(441, 523, 196, 42);
    public static final Rectangle2D MENU = new Rectangle2D(654, 523, 196, 42);
    private static final Color PAPER = Color.web("#F3EED9");
    private static final Color RECESS = Color.web("#E6DEC3");
    private static final Color EDGE = Color.web("#C5B999");
    private static final Color INK = Color.web("#1E1B18");
    private static final Color MUTED = Color.web("#5C5346");
    private static final Color BORDER = Color.web("#2A241E");
    private static final Color SHADOW = Color.web("#0B101C");
    private static final Color PERFORATION = Color.web("#B8AC8C");
    private static final Color TEAL = Color.web("#007852");
    private static final double COUNT_START = .160, COUNT_END = .650;
    private static final double STAMP_START = .680, IMPACT = .760;

    private final ArcadeArt art;
    private double elapsed;
    private int tick;

    VictoryTicket(ArcadeArt art) { this.art = art; }

    void reset() { elapsed = 0; tick = 0; }

    /** Returns at most one audio cue in a frame. */
    String advance(double seconds, boolean reducedMotion) {
        double before = elapsed;
        elapsed = Math.min(1, elapsed + seconds);
        if (reducedMotion) return null;
        if (before < IMPACT && elapsed >= IMPACT) return "stamp";
        int nextTick = (int) Math.floor(clamp((elapsed - COUNT_START) / (COUNT_END - COUNT_START)) * 6);
        if (nextTick <= tick) return null;
        tick = nextTick;
        return "tick";
    }

    void draw(GraphicsContext gc, GameWorld world, String font, boolean reducedMotion,
              double pointerX, double pointerY, Rectangle2D pressed) {
        double t = reducedMotion ? 1 : elapsed;
        double lift = Math.rint(18 * Math.pow(1 - clamp(t / COUNT_START), 2));
        double shake = t >= IMPACT && t < IMPACT + .034 ? (t < IMPACT + .017 ? 3 : -3) : 0;
        double count = clamp((t - COUNT_START) / (COUNT_END - COUNT_START));
        gc.save();
        gc.setImageSmoothing(false);
        fill(gc, Color.rgb(11, 16, 28, .72), 0, 0, 1280, 720);
        gc.translate(shake, lift);
        paper(gc);
        header(gc, font);
        fill(gc, PERFORATION, 436, 255, 420, 92);
        fill(gc, RECESS, 438, 257, 416, 88);
        String points = String.format(Locale.US, "%,d", Math.round(world.getScore() * count));
        ink(gc, font, points, 648, 302, 34, EDGE, TextAlignment.CENTER);
        ink(gc, font, points, 646, 300, 34, INK, TextAlignment.CENTER);
        ink(gc, font, "TOTAL POINTS", 646, 326, 10, MUTED, TextAlignment.CENTER);

        art.prop(gc, 0, 437, 372, 16, 16);
        row(gc, font, "COINS COLLECTED", Long.toString(Math.round(world.getCoinsCollected() * count)), 370, 457);
        row(gc, font, "DISTANCE COVERED", Math.round(world.getDistanceMeters() * count) + " m", 392, 437);
        row(gc, font, "BEST STREAK", Long.toString(Math.round(world.getBestCombo() * count)), 414, 437);
        row(gc, font, "TIME REMAINING", String.format(Locale.US, "%.1fs", world.getTimeRemaining() * count), 436, 437);
        row(gc, font, "CLEAN SECTIONS", Long.toString(Math.round(world.getCleanSections() * count)), 458, 437);
        for (int x = 437; x < 854; x += 10) fill(gc, PERFORATION, x, 472, 6, 2);
        String microStats = Math.round(world.getCleanDodges() * count) + " CLEAN DODGES  •  "
                + Math.round(world.getPerfectLandings() * count) + " PERFECT LANDINGS";
        ink(gc, font, microStats, 646, 498, 10, MUTED, TextAlignment.CENTER);

        button(gc, font, RETRY, "TRY AGAIN", true, RETRY.contains(pointerX, pointerY), pressed == RETRY);
        button(gc, font, MENU, "MAIN MENU", false, MENU.contains(pointerX, pointerY), pressed == MENU);
        ink(gc, font, "ENTER / SPACE", RETRY.getMinX() + RETRY.getWidth() / 2, 581, 8, MUTED, TextAlignment.CENTER);
        ink(gc, font, "ESC", MENU.getMinX() + MENU.getWidth() / 2, 581, 8, MUTED, TextAlignment.CENTER);
        if (t >= STAMP_START) stamp(gc, font, world.getMedal(), t);
        gc.restore();
    }

    private void paper(GraphicsContext gc) {
        gc.save();
        gc.translate(4, 4);
        TicketPaper.path(gc, X, Y, W, H, false);
        gc.setFill(SHADOW);
        gc.fill();
        gc.restore();

        gc.save();
        TicketPaper.path(gc, X, Y, W, H, false);
        gc.setFill(PAPER);
        gc.fill();
        gc.clip();
        fill(gc, Color.web("#6B4423"), 384, Y, 36, H);
        fill(gc, Color.web("#4A2E16"), 388, Y, 26, H);
        fill(gc, Color.web("#8C5E35"), 386, Y, 2, H);
        fill(gc, BORDER, 419, Y, 1, H);
        gc.restore();

        gc.setStroke(BORDER);
        gc.setLineWidth(3);
        TicketPaper.path(gc, X, Y, W, H, false);
        gc.stroke();
    }

    private void header(GraphicsContext gc, String font) {
        gc.save();
        gc.translate(406, 373);
        gc.rotate(-90);
        ink(gc, font, "RATP • 75001 • VALIDÉ", 0, 0, 9, Color.web("#AF8255"), TextAlignment.CENTER);
        gc.restore();
        ink(gc, font, "RATP • RÉSEAU URBAIN", 437, 174, 10, MUTED, TextAlignment.LEFT);
        fill(gc, TEAL, 707, 155, 149, 27);
        ink(gc, font, "TICKET t+ VALIDÉ", 781.5, 173, 9, PAPER, TextAlignment.CENTER);
        TicketPaper.lineBadge(gc, font, 450, 210, "1", Color.web("#FFCE00"), INK);
        TicketPaper.lineBadge(gc, font, 482, 210, "4", Color.web("#A0006E"), Color.WHITE);
        ink(gc, font, "CHÂTELET — LEVEL COMPLETE!", 505, 216, 13, INK, TextAlignment.LEFT);
        ink(gc, font, "WELCOME ABOARD. PARIS CAN WAIT.", 437, 239, 10, MUTED, TextAlignment.LEFT);
    }

    private void row(GraphicsContext gc, String family, String label, String value, int y, int left) {
        int right = 854;
        Font font = Font.font(family, 11);
        ink(gc, family, label, left, y, 11, MUTED, TextAlignment.LEFT);
        ink(gc, family, value, right, y, 11, INK, TextAlignment.RIGHT);
        for (double x = Math.ceil(left + width(font, label) + 10); x < right - width(font, value) - 9; x += 6) {
            fill(gc, EDGE, x, y - 2, 2, 2);
        }
    }

    private void stamp(GraphicsContext gc, String family, String rank, double t) {
        double fall = clamp((t - STAMP_START) / (IMPACT - STAMP_START));
        double scale = 2.2 - 1.2 * fall;
        if (t >= IMPACT && t < IMPACT + .017) scale = .93;
        Color color = switch (rank) {
            case "GOLD" -> Color.web("#B83A2D");
            case "SILVER" -> Color.web("#2B5B84");
            default -> Color.web("#8C4A27");
        };
        gc.save();
        gc.translate(788, 337);
        gc.rotate(-7);
        gc.scale(scale, scale);
        gc.setGlobalAlpha(fall);
        gc.setStroke(color);
        gc.setLineWidth(3);
        gc.strokeRect(-79.5, -16.5, 159, 33);
        gc.setLineWidth(1);
        gc.strokeRect(-75.5, -12.5, 151, 25);
        fill(gc, RECESS, -59, -18, 5, 3);
        fill(gc, RECESS, 59, -13, 4, 2);
        fill(gc, PAPER, 50, 15, 5, 3);
        ink(gc, family, rank + " RANK", 0, 5, 14, color, TextAlignment.CENTER);
        gc.restore();
    }

    private void button(GraphicsContext gc, String font, Rectangle2D hit, String label,
                        boolean primary, boolean hover, boolean pressed) {
        double x = hit.getMinX(), y = hit.getMinY(), w = hit.getWidth(), h = hit.getHeight();
        Color face = Color.web(primary ? (hover ? "#E5AA37" : "#D99B26") : (hover ? "#31465F" : "#243447"));
        double down = pressed ? 2 : 0;
        fill(gc, INK, x, y + down, w, h - down);
        fill(gc, face, x + 2, y + 2 + down, w - 4, h - 4 - down);
        fill(gc, Color.web(primary ? "#F4C150" : "#3B526E"), x + 2, y + 2 + down, w - 4, 2);
        fill(gc, Color.web(primary ? "#8C5E12" : "#121B26"), x + 2, y + h - 6 + down, w - 4, 4 - down);
        ink(gc, font, label, x + w / 2, y + 26 + down, 13, primary ? INK : PAPER, TextAlignment.CENTER);
    }

    private static double width(Font font, String value) {
        Text text = new Text(value);
        text.setFont(font);
        return text.getLayoutBounds().getWidth();
    }

    private static void ink(GraphicsContext gc, String family, String value, double x, double y,
                            int size, Color color, TextAlignment align) {
        gc.setFont(Font.font(family, size));
        gc.setTextAlign(align);
        gc.setFill(color);
        gc.fillText(value, x, y);
    }

    private static void fill(GraphicsContext gc, Color color, double x, double y, double w, double h) {
        gc.setFill(color);
        gc.fillRect(x, y, w, h);
    }

    private static double clamp(double value) { return Math.clamp(value, 0, 1); }
}
