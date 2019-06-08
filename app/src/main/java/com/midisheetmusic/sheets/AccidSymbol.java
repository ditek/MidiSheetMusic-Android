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


/** @class AccidSymbol
 * An accidental (accid) symbol represents a sharp, flat, or natural
 * accidental that is displayed at a specific position (note and clef).
 */
public class AccidSymbol implements MusicSymbol {
    private Accid accid;          /** The accidental (sharp, flat, natural) */
    private WhiteNote whitenote;  /** The white note where the symbol occurs */
    private Clef clef;            /** Which clef the symbols is in */
    private int width;            /** Width of symbol */

    /** 
     * Create a new AccidSymbol with the given accidental, that is
     * displayed at the given note in the given clef.
     */
    public AccidSymbol(Accid accid, WhiteNote note, Clef clef) {
        this.accid = accid;
        this.whitenote = note;
        this.clef = clef;
        width = getMinWidth();
    }

    /** Return the white note this accidental is displayed at */
    public WhiteNote getNote() { return whitenote; }

    /** Get the time (in pulses) this symbol occurs at.
     * Not used.  Instead, the StartTime of the ChordSymbol containing this
     * AccidSymbol is used.
     */
    public int getStartTime() { return -1; }  

    /** Get the minimum width (in pixels) needed to draw this symbol */
    public int getMinWidth() { return 3* SheetMusic.NoteHeight/2; }

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    public int getWidth() { return width; }
    public void setWidth(int value) { width = value; }

    /** Get the number of pixels this symbol extends above the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getAboveStaff() {
        int dist = WhiteNote.Top(clef).Dist(whitenote) * 
                   SheetMusic.NoteHeight/2;
        if (accid == Accid.Sharp || accid == Accid.Natural)
            dist -= SheetMusic.NoteHeight;
        else if (accid == Accid.Flat)
            dist -= 3*SheetMusic.NoteHeight/2;

        if (dist < 0)
            return -dist;
        else
            return 0;
    }

    /** Get the number of pixels this symbol extends below the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getBelowStaff() {
        int dist = WhiteNote.Bottom(clef).Dist(whitenote) * 
                   SheetMusic.NoteHeight/2 + 
                   SheetMusic.NoteHeight;
        if (accid == Accid.Sharp || accid == Accid.Natural) 
            dist += SheetMusic.NoteHeight;

        if (dist > 0)
            return dist;
        else 
            return 0;
    }

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public void Draw(Canvas canvas, Paint paint, int ytop) {
        /* Align the symbol to the right */
        canvas.translate(getWidth() - getMinWidth(), 0);

        /* Store the y-pixel value of the top of the whitenote in ynote. */
        int ynote = ytop + WhiteNote.Top(clef).Dist(whitenote) * 
                    SheetMusic.NoteHeight/2;

        if (accid == Accid.Sharp)
            DrawSharp(canvas, paint, ynote);
        else if (accid == Accid.Flat)
            DrawFlat(canvas, paint, ynote);
        else if (accid == Accid.Natural)
            DrawNatural(canvas, paint, ynote);

        canvas.translate(-(getWidth() - getMinWidth()), 0);
    }

    /** Draw a sharp symbol. 
     * @param ynote The pixel location of the top of the accidental's note. 
     */
    public void DrawSharp(Canvas canvas, Paint paint, int ynote) {

        /* Draw the two vertical lines */
        int ystart = ynote - SheetMusic.NoteHeight;
        int yend = ynote + 2*SheetMusic.NoteHeight;
        int x = SheetMusic.NoteHeight/2;
        paint.setStrokeWidth(1);
        canvas.drawLine(x, ystart + 2, x, yend, paint);
        x += SheetMusic.NoteHeight/2;
        canvas.drawLine(x, ystart, x, yend - 2, paint);

        /* Draw the slightly upwards horizontal lines */
        int xstart = SheetMusic.NoteHeight/2 - SheetMusic.NoteHeight/4;
        int xend = SheetMusic.NoteHeight + SheetMusic.NoteHeight/4;
        ystart = ynote + SheetMusic.LineWidth;
        yend = ystart - SheetMusic.LineWidth - SheetMusic.LineSpace/4;
        paint.setStrokeWidth(SheetMusic.LineSpace/2);
        canvas.drawLine(xstart, ystart, xend, yend, paint);
        ystart += SheetMusic.LineSpace;
        yend += SheetMusic.LineSpace;
        canvas.drawLine(xstart, ystart, xend, yend, paint);
        paint.setStrokeWidth(1);
    }

    /** Draw a flat symbol.
     * @param ynote The pixel location of the top of the accidental's note.
     */
    public void DrawFlat(Canvas canvas, Paint paint, int ynote) {
        int x = SheetMusic.LineSpace/4;

        /* Draw the vertical line */
        paint.setStrokeWidth(1);
        canvas.drawLine(x, ynote - SheetMusic.NoteHeight - SheetMusic.NoteHeight/2, 
                        x, ynote + SheetMusic.NoteHeight, paint);

        /* Draw 3 bezier curves.
         * All 3 curves start and stop at the same points.
         * Each subsequent curve bulges more and more towards 
         * the topright corner, making the curve look thicker
         * towards the top-right.
         */
        Path bezierPath = new Path();
        bezierPath.moveTo(x, ynote + SheetMusic.LineSpace/4);
        bezierPath.cubicTo(x + SheetMusic.LineSpace/2, ynote - SheetMusic.LineSpace/2, 
                           x + SheetMusic.LineSpace, ynote + SheetMusic.LineSpace/3, 
                           x, ynote + SheetMusic.LineSpace + SheetMusic.LineWidth + 1);
        canvas.drawPath(bezierPath, paint);

        bezierPath = new Path();
        bezierPath.moveTo(x, ynote + SheetMusic.LineSpace/4);
        bezierPath.cubicTo(x + SheetMusic.LineSpace/2, ynote - SheetMusic.LineSpace/2, 
                           x + SheetMusic.LineSpace + SheetMusic.LineSpace/4, 
                           ynote + SheetMusic.LineSpace/3 - SheetMusic.LineSpace/4, 
                           x, ynote + SheetMusic.LineSpace + SheetMusic.LineWidth + 1);
        canvas.drawPath(bezierPath, paint);

        bezierPath = new Path();
        bezierPath.moveTo(x, ynote + SheetMusic.LineSpace/4);
        bezierPath.cubicTo(x + SheetMusic.LineSpace/2, ynote - SheetMusic.LineSpace/2, 
                           x + SheetMusic.LineSpace + SheetMusic.LineSpace/2, 
                           ynote + SheetMusic.LineSpace/3 - SheetMusic.LineSpace/2, 
                           x, ynote + SheetMusic.LineSpace + SheetMusic.LineWidth + 1);
        canvas.drawPath(bezierPath, paint);

    }

    /** Draw a natural symbol.
     * @param ynote The pixel location of the top of the accidental's note.
     */
    public void DrawNatural(Canvas canvas, Paint paint, int ynote) {

        /* Draw the two vertical lines */
        int ystart = ynote - SheetMusic.LineSpace - SheetMusic.LineWidth;
        int yend = ynote + SheetMusic.LineSpace + SheetMusic.LineWidth;
        int x = SheetMusic.LineSpace/2;
        paint.setStrokeWidth(1);
        canvas.drawLine(x, ystart, x, yend, paint);
        x += SheetMusic.LineSpace - SheetMusic.LineSpace/4;
        ystart = ynote - SheetMusic.LineSpace/4;
        yend = ynote + 2*SheetMusic.LineSpace + SheetMusic.LineWidth - 
                 SheetMusic.LineSpace/4;
        canvas.drawLine(x, ystart, x, yend, paint);

        /* Draw the slightly upwards horizontal lines */
        int xstart = SheetMusic.LineSpace/2;
        int xend = xstart + SheetMusic.LineSpace - SheetMusic.LineSpace/4;
        ystart = ynote + SheetMusic.LineWidth;
        yend = ystart - SheetMusic.LineWidth - SheetMusic.LineSpace/4;
        paint.setStrokeWidth(SheetMusic.LineSpace/2);
        canvas.drawLine(xstart, ystart, xend, yend, paint);
        ystart += SheetMusic.LineSpace;
        yend += SheetMusic.LineSpace;
        canvas.drawLine(xstart, ystart, xend, yend, paint);
        paint.setStrokeWidth(1);
    }


    public String toString() {
        return String.format(
          "AccidSymbol accid=%1$s whitenote=%2$s clef=%3$s width=%4$s",
          accid, whitenote, clef, width);
    }

}



