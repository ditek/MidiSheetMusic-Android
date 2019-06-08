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

import com.midisheetmusic.MidiNote;

import java.util.*;

/** @class ClefMeasures
 * The ClefMeasures class is used to report what Clef (Treble or Bass) a
 * given measure uses.
 */
public class ClefMeasures {
    private ArrayList<Clef> clefs;  /** The clefs used for each measure (for a single track) */
    private int measure;       /** The length of a measure, in pulses */

 
    /** Given the notes in a track, calculate the appropriate Clef to use
     * for each measure.  Store the result in the clefs list.
     * @param notes  The midi notes
     * @param measurelen The length of a measure, in pulses
     */
    public ClefMeasures(ArrayList<MidiNote> notes, int measurelen) {
        measure = measurelen;
        Clef mainclef = MainClef(notes);
        int nextmeasure = measurelen;
        int pos = 0;
        Clef clef = mainclef;

        clefs = new ArrayList<Clef>();

        while (pos < notes.size()) {
            /* Sum all the notes in the current measure */
            int sumnotes = 0;
            int notecount = 0;
            while (pos < notes.size() && notes.get(pos).getStartTime() < nextmeasure) {
                sumnotes += notes.get(pos).getNumber();
                notecount++;
                pos++;
            }
            if (notecount == 0)
                notecount = 1;

            /* Calculate the "average" note in the measure */
            int avgnote = sumnotes / notecount;
            if (avgnote == 0) {
                /* This measure doesn't contain any notes.
                 * Keep the previous clef.
                 */
            }
            else if (avgnote >= WhiteNote.BottomTreble.getNumber()) {
                clef = Clef.Treble;
            }
            else if (avgnote <= WhiteNote.TopBass.getNumber()) {
                clef = Clef.Bass;
            }
            else {
                /* The average note is between G3 and F4. We can use either
                 * the treble or bass clef.  Use the "main" clef, the clef
                 * that appears most for this track.
                 */
                clef = mainclef;
            }

            clefs.add(clef);
            nextmeasure += measurelen;
        }
        clefs.add(clef);
    }

    /** Given a time (in pulses), return the clef used for that measure. */
    public Clef GetClef(int starttime) {

        /* If the time exceeds the last measure, return the last measure */
        if (starttime / measure >= clefs.size()) {
            return clefs.get(clefs.size()-1);
        }
        else {
            return clefs.get(starttime / measure);
        }
    }

    /** Calculate the best clef to use for the given notes.  If the
     * average note is below Middle C, use a bass clef.  Else, use a treble
     * clef.
     */
    private static Clef MainClef(ArrayList<MidiNote> notes) {
        int middleC = WhiteNote.MiddleC.getNumber();
        int total = 0;
        for (MidiNote m : notes) {
            total += m.getNumber();
        }
        if (notes.size() == 0) {
            return Clef.Treble;
        }
        else if (total/notes.size() >= middleC) {
            return Clef.Treble;
        }
        else {
            return Clef.Bass;
        }
    }
}



