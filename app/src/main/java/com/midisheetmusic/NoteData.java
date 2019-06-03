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


import com.midisheetmusic.sheets.Accid;
import com.midisheetmusic.sheets.WhiteNote;

/** @class NoteData
 *  Contains fields for displaying a single note in a chord.
 */
public class NoteData {
    public int number;             /** The Midi note number, used to determine the color */
    public WhiteNote whitenote;    /** The white note location to draw */
    public NoteDuration duration;  /** The duration of the note */
    public boolean leftside;       /** Whether to draw note to the left or right of the stem */
    public Accid accid;            /** Used to create the AccidSymbols for the chord */
}


