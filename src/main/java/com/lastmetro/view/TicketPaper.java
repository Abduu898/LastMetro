package com.lastmetro.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/** Pixel ticket shapes shared by the kiosk and the validated receipt. */
final class TicketPaper {
    private TicketPaper() {}

    static void path(GraphicsContext gc, int left, int top, int width, int height, boolean bothPunches) {
        int right = left + width, bottom = top + height, punch = top + height / 2;
        gc.beginPath();
        gc.moveTo(left, top);
        for (int x = left + 8; x + 6 <= right - 8; x += 16) {
            gc.lineTo(x, top); gc.lineTo(x, top + 4);
            gc.lineTo(x + 6, top + 4); gc.lineTo(x + 6, top);
        }
        gc.lineTo(right, top);
        punch(gc, right, punch, 1);
        gc.lineTo(right, bottom);
        for (int x = right - 8; x - 6 >= left + 8; x -= 16) {
            gc.lineTo(x, bottom); gc.lineTo(x, bottom - 4);
            gc.lineTo(x - 6, bottom - 4); gc.lineTo(x - 6, bottom);
        }
        gc.lineTo(left, bottom);
        if (bothPunches) punch(gc, left, punch, -1);
        gc.closePath();
    }

    private static void punch(GraphicsContext gc, int x, int y, int direction) {
        int[] inset = {0, 6, 6, 12, 12, 16, 16, 12, 12, 6, 6, 0};
        int[] steps = {-16, -16, -12, -12, -7, -7, 7, 7, 12, 12, 16, 16};
        for (int i = 0; i < steps.length; i++)
            gc.lineTo(x - direction * inset[i], y + direction * steps[i]);
    }

    static void lineBadge(GraphicsContext gc, String font, int x, int y, String line, Color face, Color text) {
        gc.setFill(Color.web("#1E1B18"));
        gc.fillPolygon(new double[]{x-8,x+8,x+14,x+14,x+8,x-8,x-14,x-14},
                new double[]{y-14,y-14,y-8,y+8,y+14,y+14,y+8,y-8},8);
        gc.setFill(face);
        gc.fillPolygon(new double[]{x-7,x+7,x+12,x+12,x+7,x-7,x-12,x-12},
                new double[]{y-12,y-12,y-7,y+7,y+12,y+12,y+7,y-7},8);
        gc.setFont(Font.font(font, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(text);
        gc.fillText(line, x, y + 5);
    }

}
