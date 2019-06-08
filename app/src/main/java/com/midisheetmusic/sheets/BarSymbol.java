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

import com.midisheetmusic.SheetMusic;


/** @class BarSymbol
 * The BarSymbol represents the vertical bars which delimit measures.
 * The starttime of the symbol is the beginning of the new
 * measure.
 */
public class BarSymbol implements MusicSymbol {
    private int starttime;
    private int width;

    /** Create a BarSymbol. The starttime should be the beginning of a measure. */
    public BarSymbol(int starttime) {
        this.starttime = starttime;
        width = getMinWidth();
    }

    /** Get the time (in pulses) this symbol occurs at.
     * This is used to determine the measure this symbol belongs to.
     */
    public int getStartTime() { return starttime; }

    /** Get the minimum width (in pixels) needed to draw this symbol */
    public int getMinWidth() { return 2 * SheetMusic.LineSpace; }

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    public int getWidth() { return width; }
    public void setWidth(int value) { width = value; }

    /** Get the number of pixels this symbol extends above the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getAboveStaff() { return 0; } 

    /** Get the number of pixels this symbol extends below the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getBelowStaff() { return 0; }

    /** Draw a vertical bar.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public 
    void Draw(Canvas canvas, Paint paint, int ytop) {
        int y = ytop;
        int yend = y + SheetMusic.LineSpace*4 + SheetMusic.LineWidth*4;
        paint.setStrokeWidth(1);
        canvas.drawLine(SheetMusic.NoteWidth/2, y, SheetMusic.NoteWidth/2, yend, paint);

    }

    public String toString() {
        return String.format("BarSymbol starttime=%1$s width=%2$s", 
                             starttime, width);
    }
}



