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
import com.midisheetmusic.sheets.AccidSymbol;
import com.midisheetmusic.sheets.Clef;
import com.midisheetmusic.sheets.WhiteNote;

/** @class KeySignature
 * The KeySignature class represents a key signature, like G Major
 * or B-flat Major.  For sheet music, we only care about the number
 * of sharps or flats in the key signature, not whether it is major
 * or minor.
 *
 * The main operations of this class are:
 * - Guessing the key signature, given the notes in a song.
 * - Generating the accidental symbols for the key signature.
 * - Determining whether a particular note requires an accidental
 *   or not.
 *
 */

public class KeySignature {
    /** The number of sharps in each key signature */
    public static final int C = 0;
    public static final int G = 1;
    public static final int D = 2;
    public static final int A = 3;
    public static final int E = 4;
    public static final int B = 5;

    /** The number of flats in each key signature */
    public static final int F = 1;
    public static final int Bflat = 2;
    public static final int Eflat = 3;
    public static final int Aflat = 4;
    public static final int Dflat = 5;
    public static final int Gflat = 6;

    /** The two arrays below are key maps.  They take a major key
     * (like G major, B-flat major) and a note in the scale, and
     * return the Accidental required to display that note in the
     * given key.  In a nutshel, the map is
     *
     *   map[Key][NoteScale] -> Accidental
     */
    private static Accid[][] sharpkeys;
    private static Accid[][] flatkeys;

    private int num_flats;   /** The number of sharps in the key, 0 thru 6 */
    private int num_sharps;  /** The number of flats in the key, 0 thru 6 */

    /** The accidental symbols that denote this key, in a treble clef */
    private AccidSymbol[] treble;

    /** The accidental symbols that denote this key, in a bass clef */
    private AccidSymbol[] bass;

    /** The key map for this key signature:
     *   keymap[notenumber] -> Accidental
     */
    private Accid[] keymap;

    /** The measure used in the previous call to GetAccidental() */
    private int prevmeasure; 


    /** Create new key signature, with the given number of
     * sharps and flats.  One of the two must be 0, you can't
     * have both sharps and flats in the key signature.
     */
    public KeySignature(int num_sharps, int num_flats) {
        if (!(num_sharps == 0 || num_flats == 0)) {
            throw new IllegalArgumentException();
        }
        this.num_sharps = num_sharps;
        this.num_flats = num_flats;

        CreateAccidentalMaps();
        keymap = new Accid[160];
        ResetKeyMap();
        CreateSymbols();
    }

    /** Create new key signature, with the given notescale.
     */
    public KeySignature(int notescale) {
        num_sharps = num_flats = 0;
        switch (notescale) {
            case NoteScale.A:     num_sharps = 3; break;
            case NoteScale.Bflat: num_flats = 2;  break;
            case NoteScale.B:     num_sharps = 5; break;
            case NoteScale.C:     break;
            case NoteScale.Dflat: num_flats = 5;  break;
            case NoteScale.D:     num_sharps = 2; break;
            case NoteScale.Eflat: num_flats = 3;  break;
            case NoteScale.E:     num_sharps = 4; break;
            case NoteScale.F:     num_flats = 1;  break;
            case NoteScale.Gflat: num_flats = 6;  break;
            case NoteScale.G:     num_sharps = 1; break;
            case NoteScale.Aflat: num_flats = 4;  break;
            default:              throw new IllegalArgumentException(); 
        }
        
        CreateAccidentalMaps();
        keymap = new Accid[160];
        ResetKeyMap();
        CreateSymbols();
    }

    /** Iniitalize the sharpkeys and flatkeys maps */
    private static void CreateAccidentalMaps() {
        if (sharpkeys != null)
            return; 

        Accid[] map;
        sharpkeys = new Accid[8][];
        flatkeys = new Accid[8][];

        for (int i = 0; i < 8; i++) {
            sharpkeys[i] = new Accid[12];
            flatkeys[i] = new Accid[12];
        }

        map = sharpkeys[C];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Asharp ] = Accid.Flat;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Csharp ] = Accid.Sharp;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Dsharp ] = Accid.Sharp;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Fsharp ] = Accid.Sharp;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Gsharp ] = Accid.Sharp;

        map = sharpkeys[G];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Asharp ] = Accid.Flat;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Csharp ] = Accid.Sharp;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Dsharp ] = Accid.Sharp;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.Natural;
        map[ NoteScale.Fsharp ] = Accid.None;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Gsharp ] = Accid.Sharp;

        map = sharpkeys[D];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Asharp ] = Accid.Flat;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.Natural;
        map[ NoteScale.Csharp ] = Accid.None;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Dsharp ] = Accid.Sharp;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.Natural;
        map[ NoteScale.Fsharp ] = Accid.None;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Gsharp ] = Accid.Sharp;

        map = sharpkeys[A];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Asharp ] = Accid.Flat;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.Natural;
        map[ NoteScale.Csharp ] = Accid.None;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Dsharp ] = Accid.Sharp;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.Natural;
        map[ NoteScale.Fsharp ] = Accid.None;
        map[ NoteScale.G ]      = Accid.Natural;
        map[ NoteScale.Gsharp ] = Accid.None;

        map = sharpkeys[E];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Asharp ] = Accid.Flat;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.Natural;
        map[ NoteScale.Csharp ] = Accid.None;
        map[ NoteScale.D ]      = Accid.Natural;
        map[ NoteScale.Dsharp ] = Accid.None;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.Natural;
        map[ NoteScale.Fsharp ] = Accid.None;
        map[ NoteScale.G ]      = Accid.Natural;
        map[ NoteScale.Gsharp ] = Accid.None;

        map = sharpkeys[B];
        map[ NoteScale.A ]      = Accid.Natural;
        map[ NoteScale.Asharp ] = Accid.None;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.Natural;
        map[ NoteScale.Csharp ] = Accid.None;
        map[ NoteScale.D ]      = Accid.Natural;
        map[ NoteScale.Dsharp ] = Accid.None;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.Natural;
        map[ NoteScale.Fsharp ] = Accid.None;
        map[ NoteScale.G ]      = Accid.Natural;
        map[ NoteScale.Gsharp ] = Accid.None;

        /* Flat keys */
        map = flatkeys[C];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Asharp ] = Accid.Flat;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Csharp ] = Accid.Sharp;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Dsharp ] = Accid.Sharp;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Fsharp ] = Accid.Sharp;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Gsharp ] = Accid.Sharp;

        map = flatkeys[F];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Bflat ]  = Accid.None;
        map[ NoteScale.B ]      = Accid.Natural;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Csharp ] = Accid.Sharp;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Eflat ]  = Accid.Flat;
        map[ NoteScale.E ]      = Accid.None;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Fsharp ] = Accid.Sharp;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Aflat ]  = Accid.Flat;

        map = flatkeys[Bflat];
        map[ NoteScale.A ]      = Accid.None;
        map[ NoteScale.Bflat ]  = Accid.None;
        map[ NoteScale.B ]      = Accid.Natural;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Csharp ] = Accid.Sharp;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Eflat ]  = Accid.None;
        map[ NoteScale.E ]      = Accid.Natural;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Fsharp ] = Accid.Sharp;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Aflat ]  = Accid.Flat;

        map = flatkeys[Eflat];
        map[ NoteScale.A ]      = Accid.Natural;
        map[ NoteScale.Bflat ]  = Accid.None;
        map[ NoteScale.B ]      = Accid.Natural;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Dflat ]  = Accid.Flat;
        map[ NoteScale.D ]      = Accid.None;
        map[ NoteScale.Eflat ]  = Accid.None;
        map[ NoteScale.E ]      = Accid.Natural;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Fsharp ] = Accid.Sharp;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Aflat ]  = Accid.None;

        map = flatkeys[Aflat];
        map[ NoteScale.A ]      = Accid.Natural;
        map[ NoteScale.Bflat ]  = Accid.None;
        map[ NoteScale.B ]      = Accid.Natural;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Dflat ]  = Accid.None;
        map[ NoteScale.D ]      = Accid.Natural;
        map[ NoteScale.Eflat ]  = Accid.None;
        map[ NoteScale.E ]      = Accid.Natural;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Fsharp ] = Accid.Sharp;
        map[ NoteScale.G ]      = Accid.None;
        map[ NoteScale.Aflat ]  = Accid.None;

        map = flatkeys[Dflat];
        map[ NoteScale.A ]      = Accid.Natural;
        map[ NoteScale.Bflat ]  = Accid.None;
        map[ NoteScale.B ]      = Accid.Natural;
        map[ NoteScale.C ]      = Accid.None;
        map[ NoteScale.Dflat ]  = Accid.None;
        map[ NoteScale.D ]      = Accid.Natural;
        map[ NoteScale.Eflat ]  = Accid.None;
        map[ NoteScale.E ]      = Accid.Natural;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Gflat ]  = Accid.None;
        map[ NoteScale.G ]      = Accid.Natural;
        map[ NoteScale.Aflat ]  = Accid.None;

        map = flatkeys[Gflat];
        map[ NoteScale.A ]      = Accid.Natural;
        map[ NoteScale.Bflat ]  = Accid.None;
        map[ NoteScale.B ]      = Accid.None;
        map[ NoteScale.C ]      = Accid.Natural;
        map[ NoteScale.Dflat ]  = Accid.None;
        map[ NoteScale.D ]      = Accid.Natural;
        map[ NoteScale.Eflat ]  = Accid.None;
        map[ NoteScale.E ]      = Accid.Natural;
        map[ NoteScale.F ]      = Accid.None;
        map[ NoteScale.Gflat ]  = Accid.None;
        map[ NoteScale.G ]      = Accid.Natural;
        map[ NoteScale.Aflat ]  = Accid.None;


    }

    /** The keymap tells what accidental symbol is needed for each
     *  note in the scale.  Reset the keymap to the values of the
     *  key signature.
     */
    private void ResetKeyMap()
    {
        Accid[] key;
        if (num_flats > 0)
            key = flatkeys[num_flats];
        else
            key = sharpkeys[num_sharps];

        for (int notenumber = 0; notenumber < keymap.length; notenumber++) {
            keymap[notenumber] = key[NoteScale.FromNumber(notenumber)];
        }
    }


    /** Create the Accidental symbols for this key, for
     * the treble and bass clefs.
     */
    private void CreateSymbols() {
        int count = Math.max(num_sharps, num_flats);
        treble = new AccidSymbol[count];
        bass = new AccidSymbol[count];

        if (count == 0) {
            return;
        }

        WhiteNote[] treblenotes = null;
        WhiteNote[] bassnotes = null;

        if (num_sharps > 0)  {
            treblenotes = new WhiteNote[] {
                new WhiteNote(WhiteNote.F, 5),
                new WhiteNote(WhiteNote.C, 5),
                new WhiteNote(WhiteNote.G, 5),
                new WhiteNote(WhiteNote.D, 5),
                new WhiteNote(WhiteNote.A, 6),
                new WhiteNote(WhiteNote.E, 5)
            };
            bassnotes = new WhiteNote[] {
                new WhiteNote(WhiteNote.F, 3),
                new WhiteNote(WhiteNote.C, 3),
                new WhiteNote(WhiteNote.G, 3),
                new WhiteNote(WhiteNote.D, 3),
                new WhiteNote(WhiteNote.A, 4),
                new WhiteNote(WhiteNote.E, 3)
            };
        }
        else if (num_flats > 0) {
            treblenotes = new WhiteNote[] {
                new WhiteNote(WhiteNote.B, 5),
                new WhiteNote(WhiteNote.E, 5),
                new WhiteNote(WhiteNote.A, 5),
                new WhiteNote(WhiteNote.D, 5),
                new WhiteNote(WhiteNote.G, 4),
                new WhiteNote(WhiteNote.C, 5)
            };
            bassnotes = new WhiteNote[] {
                new WhiteNote(WhiteNote.B, 3),
                new WhiteNote(WhiteNote.E, 3),
                new WhiteNote(WhiteNote.A, 3),
                new WhiteNote(WhiteNote.D, 3),
                new WhiteNote(WhiteNote.G, 2),
                new WhiteNote(WhiteNote.C, 3)
            };
        }

        Accid a = Accid.None;
        if (num_sharps > 0)
            a = Accid.Sharp;
        else
            a = Accid.Flat;

        for (int i = 0; i < count; i++) {
            treble[i] = new AccidSymbol(a, treblenotes[i], Clef.Treble);
            bass[i] = new AccidSymbol(a, bassnotes[i], Clef.Bass);
        }
    }

    /** Return the Accidental symbols for displaying this key signature
     * for the given clef.
     */
    public AccidSymbol[] GetSymbols(Clef clef) {
        if (clef == Clef.Treble)
            return treble;
        else
            return bass;
    }

    /** Given a midi note number, return the accidental (if any) 
     * that should be used when displaying the note in this key signature.
     *
     * The current measure is also required.  Once we return an
     * accidental for a measure, the accidental remains for the
     * rest of the measure. So we must update the current keymap
     * with any new accidentals that we return.  When we move to another
     * measure, we reset the keymap back to the key signature.
     */
    public Accid GetAccidental(int notenumber, int measure) {
        if (measure != prevmeasure) {
            ResetKeyMap();
            prevmeasure = measure;
        }
        if (notenumber <= 1 || notenumber >= 127) {
            return Accid.None;
        } 

        Accid result = keymap[notenumber];
        if (result == Accid.Sharp) {
            keymap[notenumber] = Accid.None;
            keymap[notenumber-1] = Accid.Natural;
        }
        else if (result == Accid.Flat) {
            keymap[notenumber] = Accid.None;
            keymap[notenumber+1] = Accid.Natural;
        }
        else if (result == Accid.Natural) {
            keymap[notenumber] = Accid.None;
            int nextkey = NoteScale.FromNumber(notenumber+1);
            int prevkey = NoteScale.FromNumber(notenumber-1);

            /* If we insert a natural, then either:
             * - the next key must go back to sharp,
             * - the previous key must go back to flat.
             */
            if (keymap[notenumber-1] == Accid.None && keymap[notenumber+1] == Accid.None &&
                NoteScale.IsBlackKey(nextkey) && NoteScale.IsBlackKey(prevkey) ) {

                if (num_flats == 0) {
                    keymap[notenumber+1] = Accid.Sharp;
                }
                else {
                    keymap[notenumber-1] = Accid.Flat;
                }
            }
            else if (keymap[notenumber-1] == Accid.None && NoteScale.IsBlackKey(prevkey)) {
                keymap[notenumber-1] = Accid.Flat;
            }
            else if (keymap[notenumber+1] == Accid.None && NoteScale.IsBlackKey(nextkey)) {
                keymap[notenumber+1] = Accid.Sharp;
            }
            else {
                /* Shouldn't get here */
            }
        }
        return result;
    }


    /** Given a midi note number, return the white note (the
     * non-sharp/non-flat note) that should be used when displaying
     * this note in this key signature.  This should be called
     * before calling GetAccidental().
     */
    public WhiteNote GetWhiteNote(int notenumber) {
        int notescale = NoteScale.FromNumber(notenumber);
        int octave = (notenumber + 3) / 12 - 1;
        int letter = 0;

        int[] whole_sharps = { 
            WhiteNote.A, WhiteNote.A, 
            WhiteNote.B, 
            WhiteNote.C, WhiteNote.C,
            WhiteNote.D, WhiteNote.D,
            WhiteNote.E,
            WhiteNote.F, WhiteNote.F,
            WhiteNote.G, WhiteNote.G
        };

        int[] whole_flats = {
            WhiteNote.A, 
            WhiteNote.B, WhiteNote.B,
            WhiteNote.C,
            WhiteNote.D, WhiteNote.D,
            WhiteNote.E, WhiteNote.E,
            WhiteNote.F,
            WhiteNote.G, WhiteNote.G,
            WhiteNote.A
        };

        Accid accid = keymap[notenumber];
        if (accid == Accid.Flat) {
            letter = whole_flats[notescale];
        }
        else if (accid == Accid.Sharp) {
            letter = whole_sharps[notescale];
        }
        else if (accid == Accid.Natural) {
            letter = whole_sharps[notescale];
        }
        else if (accid == Accid.None) {
            letter = whole_sharps[notescale];

            /* If the note number is a sharp/flat, and there's no accidental,
             * determine the white note by seeing whether the previous or next note
             * is a natural.
             */
            if (NoteScale.IsBlackKey(notescale)) {
                if (keymap[notenumber-1] == Accid.Natural && 
                    keymap[notenumber+1] == Accid.Natural) {

                    if (num_flats > 0) {
                        letter = whole_flats[notescale];
                    }
                    else {
                        letter = whole_sharps[notescale];
                    }
                }
                else if (keymap[notenumber-1] == Accid.Natural) {
                    letter = whole_sharps[notescale];
                }
                else if (keymap[notenumber+1] == Accid.Natural) {
                    letter = whole_flats[notescale];
                }
            }
        }

        /* The above algorithm doesn't quite work for G-flat major.
         * Handle it here.
         */
        if (num_flats == Gflat && notescale == NoteScale.B) {
            letter = WhiteNote.C;
        }
        if (num_flats == Gflat && notescale == NoteScale.Bflat) {
            letter = WhiteNote.B;
        }

        if (num_flats > 0 && notescale == NoteScale.Aflat) {
            octave++;
        }

        return new WhiteNote(letter, octave);
    }


    /** Guess the key signature, given the midi note numbers used in
     * the song.
     */
    public static KeySignature Guess(ListInt notes) {
        CreateAccidentalMaps();

        /* Get the frequency count of each note in the 12-note scale */
        int[] notecount = new int[12];
        for (int i = 0; i < notes.size(); i++) {
            int notenumber = notes.get(i);
            int notescale = (notenumber + 3) % 12;
            notecount[notescale] += 1;
        }

        /* For each key signature, count the total number of accidentals
         * needed to display all the notes.  Choose the key signature
         * with the fewest accidentals.
         */
        int bestkey = 0;
        boolean is_best_sharp = true;
        int smallest_accid_count = notes.size();
        int key;

        for (key = 0; key < 6; key++) {
            int accid_count = 0;
            for (int n = 0; n < 12; n++) {
                if (sharpkeys[key][n] != Accid.None) {
                    accid_count += notecount[n];
                }
            }
            if (accid_count < smallest_accid_count) {
                smallest_accid_count = accid_count;
                bestkey = key;
                is_best_sharp = true;
            }
        }

        for (key = 0; key < 7; key++) {
            int accid_count = 0;
            for (int n = 0; n < 12; n++) {
                if (flatkeys[key][n] != Accid.None) {
                    accid_count += notecount[n];
                }
            }
            if (accid_count < smallest_accid_count) {
                smallest_accid_count = accid_count;
                bestkey = key;
                is_best_sharp = false;
            }
        }
        if (is_best_sharp) {
            return new KeySignature(bestkey, 0);
        }
        else {
            return new KeySignature(0, bestkey);
        }
    }

    /** Return true if this key signature is equal to key signature k */
    public boolean equals(KeySignature k) {
        if (k.num_sharps == num_sharps && k.num_flats == num_flats)
            return true;
        else
            return false;
    }

    /* Return the Major Key of this Key Signature */
    public int Notescale() {
        int[] flatmajor = {
            NoteScale.C, NoteScale.F, NoteScale.Bflat, NoteScale.Eflat,
            NoteScale.Aflat, NoteScale.Dflat, NoteScale.Gflat, NoteScale.B 
        };

        int[] sharpmajor = {
            NoteScale.C, NoteScale.G, NoteScale.D, NoteScale.A, NoteScale.E,
            NoteScale.B, NoteScale.Fsharp, NoteScale.Csharp, NoteScale.Gsharp,
            NoteScale.Dsharp
        };
        if (num_flats > 0)
            return flatmajor[num_flats];
        else 
            return sharpmajor[num_sharps];
    }

    /* Convert a Major Key into a String */
    public static String KeyToString(int notescale) {
        switch (notescale) {
            case NoteScale.A:     return "A major" ;
            case NoteScale.Bflat: return "B-flat major";
            case NoteScale.B:     return "B major";
            case NoteScale.C:     return "C major";
            case NoteScale.Dflat: return "D-flat major";
            case NoteScale.D:     return "D major";
            case NoteScale.Eflat: return "E-flat major";
            case NoteScale.E:     return "E major";
            case NoteScale.F:     return "F major";
            case NoteScale.Gflat: return "G-flat major";
            case NoteScale.G:     return "G major";
            case NoteScale.Aflat: return "A-flat major";
            default:              return "";
        }
    }

    /* Return a string representation of this key signature.
     * We only return the major key signature, not the minor one.
     */
    @Override
    public String toString() {
        return KeyToString( Notescale() );
    }


}

