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
import android.content.*;
import android.content.res.*;

import com.midisheetmusic.R;
import com.midisheetmusic.SheetMusic;


/** @class ClefSymbol 
 * A ClefSymbol represents either a Treble or Bass Clef image.
 * The clef can be either normal or small size.  Normal size is
 * used at the beginning of a new staff, on the left side.  The
 * small symbols are used to show clef changes within a staff.
 */

public class ClefSymbol implements MusicSymbol {
    public static Bitmap treble;  /** The treble clef image */
    private static Bitmap bass;    /** The bass clef image */

    private int starttime;        /** Start time of the symbol */
    private boolean smallsize;    /** True if this is a small clef, false otherwise */
    private Clef clef;            /** The clef, Treble or Bass */
    private int width;

    /** Create a new ClefSymbol, with the given clef, starttime, and size */
    public ClefSymbol(Clef clef, int starttime, boolean small) {
        this.clef = clef;
        this.starttime = starttime;
        smallsize = small;
        width = getMinWidth();
    }

    /** Set the Treble/Bass clef images into memory. */
    public static void LoadImages(Context context) {
        if (treble == null || bass == null) {
            Resources res = context.getResources();
            treble = BitmapFactory.decodeResource(res, R.drawable.treble);
            bass = BitmapFactory.decodeResource(res, R.drawable.bass);
        }
    }

    /** Get the time (in pulses) this symbol occurs at.
     * This is used to determine the measure this symbol belongs to.
     */
    public int getStartTime() { return starttime; }

    /** Get the minimum width (in pixels) needed to draw this symbol */
    public int getMinWidth() { 
        if (smallsize)
            return SheetMusic.NoteWidth * 2;
        else
            return SheetMusic.NoteWidth * 3;
    } 

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    public int getWidth() { return width; }
    public void setWidth(int value){ width = value; }

    /** Get the number of pixels this symbol extends above the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getAboveStaff() { 
        if (clef == Clef.Treble && !smallsize)
            return SheetMusic.NoteHeight * 2;
        else
            return 0;
    }

    /** Get the number of pixels this symbol extends below the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getBelowStaff() {
        if (clef == Clef.Treble && !smallsize)
            return SheetMusic.NoteHeight * 2;
        else if (clef == Clef.Treble && smallsize)
            return SheetMusic.NoteHeight;
        else
            return 0;
    }

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public 
    void Draw(Canvas canvas, Paint paint, int ytop) {
        canvas.translate(getWidth() - getMinWidth(), 0);
        int y = ytop;
        Bitmap image;
        int height;

        /* Get the image, height, and top y pixel, depending on the clef
         * and the image size.
         */
        if (clef == Clef.Treble) {
            image = treble;
            if (smallsize) {
                height = SheetMusic.StaffHeight + SheetMusic.StaffHeight/4;
            } else {
                height = 3 * SheetMusic.StaffHeight/2 + SheetMusic.NoteHeight/2;
                y = ytop - SheetMusic.NoteHeight;
            }
        }
        else {
            image = bass;
            if (smallsize) {
                height = SheetMusic.StaffHeight - 3*SheetMusic.NoteHeight/2;
            } else {
                height = SheetMusic.StaffHeight - SheetMusic.NoteHeight;
            }
        }

        /* Scale the image width to match the height */
        int imgwidth = image.getWidth() * height / image.getHeight();
        Rect src = new Rect(0, 0, image.getWidth(), image.getHeight());
        Rect dest = new Rect(0, y, 0 + imgwidth, y + height);
        canvas.drawBitmap(image, src, dest, paint);
        canvas.translate(-(getWidth() - getMinWidth()), 0);
    }

    public String toString() {
        return String.format("ClefSymbol clef=%1$s small=%2$s width=%3$s",
                             clef, smallsize, width);
    }
}


