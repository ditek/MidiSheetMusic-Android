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

import java.util.*;

/** @class SymbolWidths
 * The SymbolWidths class is used to vertically align notes in different
 * tracks that occur at the same time (that have the same starttime).
 * This is done by the following:
 * - Store a list of all the start times.
 * - Store the width of symbols for each start time, for each track.
 * - Store the maximum width for each start time, across all tracks.
 * - Get the extra width needed for each track to match the maximum
 *   width for that start time.
 *
 * See method SheetMusic.AlignSymbols(), which uses this class.
 */

public class SymbolWidths {

    /** Array of maps (starttime -> symbol width), one per track */
    private DictInt[] widths;

    /** Map of starttime -> maximum symbol width */
    private DictInt maxwidths;

    /** An array of all the starttimes, in all tracks */
    private int[] starttimes;


    /** Initialize the symbol width maps, given all the symbols in
     * all the tracks.
     */
    public SymbolWidths(ArrayList<ArrayList<MusicSymbol>> tracks, ArrayList<ArrayList<LyricSymbol>> tracklyrics) {

        /* Get the symbol widths for all the tracks */
        widths = new DictInt[ tracks.size() ];
        for (int track = 0; track < tracks.size(); track++) {
            widths[track] = GetTrackWidths(tracks.get(track));
        }
        maxwidths = new DictInt();

        /* Calculate the maximum symbol widths */
        for (DictInt dict : widths) {
            for (int i = 0; i < dict.count(); i++) {
                int time = dict.getKey(i);
                if (!maxwidths.contains(time) ||
                    (maxwidths.get(time) < dict.get(time)) ) {

                    maxwidths.set(time, dict.get(time));
                }
            }
        }

        if (tracklyrics != null) {
            for (ArrayList<LyricSymbol> lyrics : tracklyrics) {
                if (lyrics == null) {
                    continue;
                }
                for (LyricSymbol lyric : lyrics) {
                    int width = lyric.getMinWidth();
                    int time = lyric.getStartTime();
                    if (!maxwidths.contains(time) ||
                        (maxwidths.get(time) < width) ) {

                        maxwidths.set(time, width);
                    }
                }
            }
        }

        /* Store all the start times to the starttime array */
        starttimes = new int[ maxwidths.count() ];
        for (int i = 0; i < maxwidths.count(); i++) {
            int key = maxwidths.getKey(i);
            starttimes[i] = key;
        }
        Arrays.sort(starttimes); 
    }

    /** Create a table of the symbol widths for each starttime in the track. */
    private static DictInt GetTrackWidths(ArrayList<MusicSymbol> symbols) {
        DictInt widths = new DictInt();

        for (MusicSymbol m : symbols) {
            int start = m.getStartTime();
            int w = m.getMinWidth();

            if (m instanceof BarSymbol) {
                continue;
            }
            else if (widths.contains(start)) {
                widths.set(start, widths.get(start) + w);
            }
            else {
                widths.set(start, w);
            }
        }
        return widths;
    }

    /** Given a track and a start time, return the extra width needed so that
     * the symbols for that start time align with the other tracks.
     */
    public int GetExtraWidth(int track, int start) {
        if (!widths[track].contains(start)) {
            return maxwidths.get(start);
        } else {
            return maxwidths.get(start) - widths[track].get(start);
        }
    }

    /** Return an array of all the start times in all the tracks */
    public int[] getStartTimes() { return starttimes; }
}



