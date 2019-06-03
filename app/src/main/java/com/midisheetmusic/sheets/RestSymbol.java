/*
 * Copyright (c) 2007-2011 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package com.midisheetmusic.sheets;

import android.graphics.*;

import com.midisheetmusic.NoteDuration;
import com.midisheetmusic.SheetMusic;


/* @class RestSymbol
 * A Rest symbol represents a rest - whole, half, quarter, or eighth.
 * The Rest symbol has a starttime and a duration, just like a regular
 * note.
 */
public class RestSymbol implements MusicSymbol {
    private int starttime;          /** The starttime of the rest */
    private NoteDuration duration;  /** The rest duration (eighth, quarter, half, whole) */
    private int width;              /** The width in pixels */

    /** Create a new rest symbol with the given start time and duration */
    public RestSymbol(int start, NoteDuration dur) {
        starttime = start;
        duration = dur; 
        width = getMinWidth();
    }

    /** Get the time (in pulses) this symbol occurs at.
     * This is used to determine the measure this symbol belongs to.
     */
    public int getStartTime() { return starttime; }

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    public int getWidth() { return width; }
    public void setWidth(int value) { width = value; }

    /** Get the minimum width (in pixels) needed to draw this symbol */
    public int getMinWidth() { 
        return 2 * SheetMusic.NoteHeight + SheetMusic.NoteHeight/2;
    }

    /** Get the number of pixels this symbol extends above the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getAboveStaff() { return 0; }

    /** Get the number of pixels this symbol extends below the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getBelowStaff() { return 0; }

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public 
    void Draw(Canvas canvas, Paint paint, int ytop) {
        /* Align the rest symbol to the right */
        canvas.translate(getWidth() - getMinWidth(), 0);
        canvas.translate(SheetMusic.NoteHeight/2, 0);

        if (duration == NoteDuration.Whole) {
            DrawWhole(canvas, paint, ytop);
        }
        else if (duration == NoteDuration.Half) {
            DrawHalf(canvas, paint, ytop);
        }
        else if (duration == NoteDuration.Quarter) {
            DrawQuarter(canvas, paint, ytop);
        }
        else if (duration == NoteDuration.Eighth) {
            DrawEighth(canvas, paint, ytop);
        }
        canvas.translate(-SheetMusic.NoteHeight/2, 0);
        canvas.translate(-(getWidth() - getMinWidth()), 0);
    }


    /** Draw a whole rest symbol, a rectangle below a staff line.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public void DrawWhole(Canvas canvas, Paint paint, int ytop) {
        int y = ytop + SheetMusic.NoteHeight;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, y, SheetMusic.NoteWidth, y + SheetMusic.NoteHeight/2, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    /** Draw a half rest symbol, a rectangle above a staff line.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public void DrawHalf(Canvas canvas, Paint paint, int ytop) {
        int y = ytop + SheetMusic.NoteHeight + SheetMusic.NoteHeight/2;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, y, SheetMusic.NoteWidth, y + SheetMusic.NoteHeight/2, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    /** Draw a quarter rest symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public void DrawQuarter(Canvas canvas, Paint paint, int ytop) {
        paint.setStrokeCap(Paint.Cap.BUTT);

        int y = ytop + SheetMusic.NoteHeight/2;
        int x = 2;
        int xend = x + 2*SheetMusic.NoteHeight/3;
        paint.setStrokeWidth(1);
        canvas.drawLine(x, y, xend-1, y + SheetMusic.NoteHeight-1, paint);

        paint.setStrokeWidth(SheetMusic.LineSpace/2);
        y  = ytop + SheetMusic.NoteHeight + 1;
        canvas.drawLine(xend-2, y, x, y + SheetMusic.NoteHeight, paint);

        paint.setStrokeWidth(1);
        y = ytop + SheetMusic.NoteHeight*2 - 1;
        canvas.drawLine(0, y, xend+2, y + SheetMusic.NoteHeight, paint);

        paint.setStrokeWidth(SheetMusic.LineSpace/2);
        if (SheetMusic.NoteHeight == 6) {
            canvas.drawLine(xend, y + 1 + 3*SheetMusic.NoteHeight/4, 
                            x/2, y + 1 + 3*SheetMusic.NoteHeight/4, paint);
        }
        else {  /* NoteHeight == 8 */
            canvas.drawLine(xend, y + 3*SheetMusic.NoteHeight/4, 
                            x/2, y + 3*SheetMusic.NoteHeight/4, paint);
        }

        paint.setStrokeWidth(1);
        canvas.drawLine(0, y + 2*SheetMusic.NoteHeight/3 + 1, 
                        xend - 1, y + 3*SheetMusic.NoteHeight/2, paint);
    }

    /** Draw an eighth rest symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public void DrawEighth(Canvas canvas, Paint paint, int ytop) {
        int y = ytop + SheetMusic.NoteHeight - 1;
        RectF rect = new RectF(0, y+1, 
                               SheetMusic.LineSpace-1, y+1 + SheetMusic.LineSpace-1);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawOval(rect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawLine((SheetMusic.LineSpace-2)/2, y + SheetMusic.LineSpace-1, 
                        3*SheetMusic.LineSpace/2, y + SheetMusic.LineSpace/2, paint);
        canvas.drawLine(3*SheetMusic.LineSpace/2, y + SheetMusic.LineSpace/2, 
                        3*SheetMusic.LineSpace/4, y + SheetMusic.NoteHeight*2, paint);
    }

    public String toString() {
        return String.format("RestSymbol starttime=%1$s duration=%2$s width=%3$s",
                             starttime, duration, width);
    }

}


