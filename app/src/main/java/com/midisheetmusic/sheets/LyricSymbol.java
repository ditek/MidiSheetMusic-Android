/*
 * Copyright (c) 2007-2012 Madhav Vaidyanathan
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

import java.util.*;


/** @class LyricSymbol
 *  A lyric contains the lyric to display, the start time the lyric occurs at,
 *  the the x-coordinate where it will be displayed.
 */
public class LyricSymbol {
    private int starttime;   /** The start time, in pulses */
    private String text;     /** The lyric text */
    private int x;           /** The x (horizontal) position within the staff */

    public LyricSymbol(int starttime, String text) {
        this.starttime = starttime; 
        this.text = text;
    }
     
    public int getStartTime() { return starttime; }
    public void setStartTime(int value) { starttime = value; }

    public String getText() { return text; }
    public void setText(String value) { text = value; }

    public int getX() { return x; }
    public void setX(int value) { x = value; }

    /* Return the minimum width in pixels needed to display this lyric.
     * This is an estimation, not exact.
     */
    public int getMinWidth() { 
        float widthPerChar = 10.0f * 2.0f/3.0f;
        float width = text.length() * widthPerChar;
        if (text.contains("i")) {
            width -= widthPerChar/2.0f;
        }
        if (text.contains("j")) {
            width -= widthPerChar/2.0f;
        }
        if (text.contains("l")) {
            width -= widthPerChar/2.0f;
        }
        return (int)width;
    }

    @Override
    public String toString() {
        return String.format("Lyric start=%1$s x=%2$s text=%3$s",
                             starttime, x, text);
    }

}

