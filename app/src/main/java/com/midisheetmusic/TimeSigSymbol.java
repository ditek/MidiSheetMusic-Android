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

package com.midisheetmusic;

import android.graphics.*;
import android.content.*;
import android.content.res.*;

import com.midisheetmusic.sheets.MusicSymbol;


/** @class TimeSigSymbol
 * A TimeSigSymbol represents the time signature at the beginning
 * of the staff. We use pre-made images for the numbers, instead of
 * drawing strings.
 */

public class TimeSigSymbol implements MusicSymbol {
    private static Bitmap[] images; /** The images for each number */
    private int  numerator;         /** The numerator */
    private int  denominator;       /** The denominator */
    private int  width;             /** The width in pixels */
    private boolean candraw;        /** True if we can draw the time signature */

    /** Create a new TimeSigSymbol */
    public TimeSigSymbol(int numer, int denom) {
        numerator = numer;
        denominator = denom;
        candraw = numer >= 0 && numer < images.length && images[numer] != null &&
                denom >= 0 && denom < images.length && images[numer] != null;
        width = getMinWidth();
    }

    /** Load the images into memory. */
    public static void LoadImages(Context context) {
        if (images != null) {
            return;
        }
        images = new Bitmap[13];
        Resources res = context.getResources();
        images[2] = BitmapFactory.decodeResource(res, R.drawable.two);
        images[3] = BitmapFactory.decodeResource(res, R.drawable.three);
        images[4] = BitmapFactory.decodeResource(res, R.drawable.four);
        images[6] = BitmapFactory.decodeResource(res, R.drawable.six);
        images[8] = BitmapFactory.decodeResource(res, R.drawable.eight);
        images[9] = BitmapFactory.decodeResource(res, R.drawable.nine);
        images[12] = BitmapFactory.decodeResource(res, R.drawable.twelve);
    }

    /** Get the time (in pulses) this symbol occurs at. */
    public int getStartTime() { return -1; }

    /** Get the minimum width (in pixels) needed to draw this symbol */
    public int getMinWidth() { 
        if (candraw) 
            return images[2].getWidth() * SheetMusic.NoteHeight * 2 /images[2].getHeight();
        else
            return 0;
    }

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    public int getWidth()   { return width; }
    public void setWidth(int value) { width = value; }

    /** Get the number of pixels this symbol extends above the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getAboveStaff() {  return 0; }

    /** Get the number of pixels this symbol extends below the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getBelowStaff() { return 0; } 

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public 
    void Draw(Canvas canvas, Paint paint, int ytop) {
        if (!candraw)
            return;

        canvas.translate(getWidth() - getMinWidth(), 0);
        Bitmap numer = images[numerator];
        Bitmap denom = images[denominator];

        /* Scale the image width to match the height */
        int imgheight = SheetMusic.NoteHeight * 2;
        int imgwidth = numer.getWidth() * imgheight / numer.getHeight();
        Rect src = new Rect(0, 0, numer.getWidth(), numer.getHeight());
        Rect dest = new Rect(0, ytop, imgwidth, ytop + imgheight);
        canvas.drawBitmap(numer, src, dest, paint);

        src = new Rect(0, 0, denom.getWidth(), denom.getHeight());
        dest = new Rect(0, ytop + SheetMusic.NoteHeight*2, imgwidth, ytop + SheetMusic.NoteHeight*2 + imgheight);
        canvas.drawBitmap(denom, src, dest, paint);
        canvas.translate(-(getWidth() - getMinWidth()), 0);
    }

    public String toString() {
        return String.format("TimeSigSymbol numerator=%1$s denominator=%2$s",
                             numerator, denominator);
    }
}


