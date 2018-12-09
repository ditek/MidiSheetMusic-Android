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


/** Enumeration of the notes in a scale (A, A#, ... G#) */
public class NoteScale {
    public static final int A      = 0;
    public static final int Asharp = 1;
    public static final int Bflat  = 1;
    public static final int B      = 2;
    public static final int C      = 3;
    public static final int Csharp = 4;
    public static final int Dflat  = 4;
    public static final int D      = 5;
    public static final int Dsharp = 6;
    public static final int Eflat  = 6;
    public static final int E      = 7;
    public static final int F      = 8;
    public static final int Fsharp = 9;
    public static final int Gflat  = 9;
    public static final int G      = 10;
    public static final int Gsharp = 11;
    public static final int Aflat  = 11;

    /** Convert a note (A, A#, B, etc) and octave into a
     * Midi Note number.
     */
    public static int ToNumber(int notescale, int octave) {
        return 9 + notescale + octave * 12;
    }

    /** Convert a Midi note number into a notescale (A, A#, B) */
    public static int FromNumber(int number) {
        return (number + 3) % 12;
    }

    /** Return true if this notescale number is a black key */
    public static boolean IsBlackKey(int notescale) {
        if (notescale == Asharp ||
            notescale == Csharp ||
            notescale == Dsharp ||
            notescale == Fsharp ||
            notescale == Gsharp) {

            return true;
        }
        else {
            return false;
        }
    }
}


