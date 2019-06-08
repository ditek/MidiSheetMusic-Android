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
import android.graphics.*;

import com.midisheetmusic.KeySignature;
import com.midisheetmusic.MidiNote;
import com.midisheetmusic.MidiOptions;
import com.midisheetmusic.NoteData;
import com.midisheetmusic.NoteDuration;
import com.midisheetmusic.NoteScale;
import com.midisheetmusic.SheetMusic;
import com.midisheetmusic.TimeSignature;


/** @class ChordSymbol
 * A chord symbol represents a group of notes that are played at the same
 * time.  A chord includes the notes, the accidental symbols for each
 * note, and the stem (or stems) to use.  A single chord may have two 
 * stems if the notes have different durations (e.g. if one note is a
 * quarter note, and another is an eighth note).
 */
public class ChordSymbol implements MusicSymbol {
    private Clef clef;             /** Which clef the chord is being drawn in */
    private int starttime;         /** The time (in pulses) the notes occurs at */
    private int endtime;           /** The starttime plus the longest note duration */
    private NoteData[] notedata;   /** The notes to draw */
    private AccidSymbol[] accidsymbols;   /** The accidental symbols to draw */
    private int width;             /** The width of the chord */
    private Stem stem1;            /** The stem of the chord. Can be null. */
    private Stem stem2;            /** The second stem of the chord. Can be null */
    private boolean hastwostems;   /** True if this chord has two stems */
    private SheetMusic sheetmusic; /** Used to get colors and other options */


    /** Create a new Chord Symbol from the given list of midi notes.
     * All the midi notes will have the same start time.  Use the
     * key signature to get the white key and accidental symbol for
     * each note.  Use the time signature to calculate the duration
     * of the notes. Use the clef when drawing the chord.
     */
    public ChordSymbol(ArrayList<MidiNote> midinotes, KeySignature key,
                       TimeSignature time, Clef c, SheetMusic sheet) {

        int len = midinotes.size();
        int i;

        hastwostems = false;
        clef = c;
        sheetmusic = sheet;

        starttime = midinotes.get(0).getStartTime();
        endtime = midinotes.get(0).getEndTime();

        for (i = 0; i < len; i++) {
            if (i > 1) {
                if (!(midinotes.get(i).getNumber() >= midinotes.get(i-1).getNumber()) ) {
                    throw new IllegalArgumentException();
                }
            }
            endtime = Math.max(endtime, midinotes.get(i).getEndTime());
        }

        notedata = CreateNoteData(midinotes, key, time);
        accidsymbols = CreateAccidSymbols(notedata, clef);


        /* Find out how many stems we need (1 or 2) */
        NoteDuration dur1 = notedata[0].duration;
        NoteDuration dur2 = dur1;
        int change = -1;
        for (i = 0; i < notedata.length; i++) {
            dur2 = notedata[i].duration;
            if (dur1 != dur2) {
                change = i;
                break;
            }
        }

        if (dur1 != dur2) {
            /* We have notes with different durations.  So we will need
             * two stems.  The first stem points down, and contains the
             * bottom note up to the note with the different duration.
             *
             * The second stem points up, and contains the note with the
             * different duration up to the top note.
             */
            hastwostems = true;
            stem1 = new Stem(notedata[0].whitenote, 
                             notedata[change-1].whitenote,
                             dur1, 
                             Stem.Down,
                             NotesOverlap(notedata, 0, change)
                            );

            stem2 = new Stem(notedata[change].whitenote, 
                             notedata[notedata.length-1].whitenote,
                             dur2, 
                             Stem.Up,
                             NotesOverlap(notedata, change, notedata.length)
                            );
        }
        else {
            /* All notes have the same duration, so we only need one stem. */
            int direction = StemDirection(notedata[0].whitenote, 
                                          notedata[notedata.length-1].whitenote,
                                          clef);

            stem1 = new Stem(notedata[0].whitenote,
                             notedata[notedata.length-1].whitenote,
                             dur1, 
                             direction,
                             NotesOverlap(notedata, 0, notedata.length)
                            );
            stem2 = null;
        }

        /* For whole notes, no stem is drawn. */
        if (dur1 == NoteDuration.Whole)
            stem1 = null;
        if (dur2 == NoteDuration.Whole)
            stem2 = null;

        width = getMinWidth();
    }


    /** Given the raw midi notes (the note number and duration in pulses),
     * calculate the following note data:
     * - The white key
     * - The accidental (if any)
     * - The note duration (half, quarter, eighth, etc)
     * - The side it should be drawn (left or side)
     * By default, notes are drawn on the left side.  However, if two notes
     * overlap (like A and B) you cannot draw the next note directly above it.
     * Instead you must shift one of the notes to the right.
     *
     * The KeySignature is used to determine the white key and accidental.
     * The TimeSignature is used to determine the duration.
     */
 
    private static NoteData[] 
    CreateNoteData(ArrayList<MidiNote> midinotes, KeySignature key,
                   TimeSignature time) {

        int len = midinotes.size();
        NoteData[] notedata = new NoteData[len];

        for (int i = 0; i < len; i++) {
            MidiNote midi = midinotes.get(i);
            notedata[i] = new NoteData();
            notedata[i].number = midi.getNumber();
            notedata[i].leftside = true;
            notedata[i].whitenote = key.GetWhiteNote(midi.getNumber());
            notedata[i].duration = time.GetNoteDuration(midi.getEndTime() - midi.getStartTime());
            notedata[i].accid = key.GetAccidental(midi.getNumber(), midi.getStartTime() / time.getMeasure());

            if (i > 0 && (notedata[i].whitenote.Dist(notedata[i-1].whitenote) == 1)) {
                /* This note (notedata[i]) overlaps with the previous note.
                 * Change the side of this note.
                 */
                notedata[i].leftside = !notedata[i - 1].leftside;
            } else {
                notedata[i].leftside = true;
            }
        }
        return notedata;
    }


    /** Given the note data (the white keys and accidentals), create 
     * the Accidental Symbols and return them.
     */
    private static AccidSymbol[] 
    CreateAccidSymbols(NoteData[] notedata, Clef clef) {
        int count = 0;
        for (NoteData n : notedata) {
            if (n.accid != Accid.None) {
                count++;
            }
        }
        AccidSymbol[] accidsymbols = new AccidSymbol[count];
        int i = 0;
        for (NoteData n : notedata) {
            if (n.accid != Accid.None) {
                accidsymbols[i] = new AccidSymbol(n.accid, n.whitenote, clef);
                i++;
            }
        }
        return accidsymbols;
    }

    /** Calculate the stem direction (Up or down) based on the top and
     * bottom note in the chord.  If the average of the notes is above
     * the middle of the staff, the direction is down.  Else, the
     * direction is up.
     */
    private static int 
    StemDirection(WhiteNote bottom, WhiteNote top, Clef clef) {
        WhiteNote middle;
        if (clef == Clef.Treble)
            middle = new WhiteNote(WhiteNote.B, 5);
        else
            middle = new WhiteNote(WhiteNote.D, 3);

        int dist = middle.Dist(bottom) + middle.Dist(top);
        if (dist >= 0)
            return Stem.Up;
        else 
            return Stem.Down;
    }

    /** Return whether any of the notes in notedata (between start and
     * end indexes) overlap.  This is needed by the Stem class to
     * determine the position of the stem (left or right of notes).
     */
    private static boolean NotesOverlap(NoteData[] notedata, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!notedata[i].leftside) {
                return true;
            }
        }
        return false;
    }

    /** Get the time (in pulses) this symbol occurs at.
     * This is used to determine the measure this symbol belongs to.
     */
    public int getStartTime() { return starttime; }

    /** Get the end time (in pulses) of the longest note in the chord.
     * Used to determine whether two adjacent chords can be joined
     * by a stem.
     */
    public int getEndTime() { return endtime; }

    /** Return the clef this chord is drawn in. */
    public Clef getClef() { return clef; }

    /** Return true if this chord has two stems */
    public boolean getHasTwoStems() { return hastwostems; }

    /* Return the stem will the smallest duration.  This property
     * is used when making chord pairs (chords joined by a horizontal
     * beam stem). The stem durations must match in order to make
     * a chord pair.  If a chord has two stems, we always return
     * the one with a smaller duration, because it has a better 
     * chance of making a pair.
     */
    public Stem getStem() { 
            if (stem1 == null) { return stem2; }
            else if (stem2 == null) { return stem1; }
            else if (stem1.getDuration().ordinal() < stem2.getDuration().ordinal()) { return stem1; }
            else { return stem2; }
        }

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    public int getWidth() { return width; }
    public void setWidth(int value) { width = value; }

    /* Return the minimum width needed to display this chord.
     *
     * The accidental symbols can be drawn above one another as long
     * as they don't overlap (they must be at least 6 notes apart).
     * If two accidental symbols do overlap, the accidental symbol
     * on top must be shifted to the right.  So the width needed for
     * accidental symbols depends on whether they overlap or not.
     *
     * If we are also displaying the letters, include extra width.
     */
    public int getMinWidth() {
        /* The width needed for the note circles */
        int result = 2*SheetMusic.NoteHeight + SheetMusic.NoteHeight*3/4;

        if (accidsymbols.length > 0) {
            result += accidsymbols[0].getMinWidth();
            for (int i = 1; i < accidsymbols.length; i++) {
                AccidSymbol accid = accidsymbols[i];
                AccidSymbol prev = accidsymbols[i-1];
                if (accid.getNote().Dist(prev.getNote()) < 6) {
                    result += accid.getMinWidth();
                }
            }
        }
        if (sheetmusic != null && sheetmusic.getShowNoteLetters() != MidiOptions.NoteNameNone) {
            result += 8;
        }
        return result;
    }


    /** Get the number of pixels this symbol extends above the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getAboveStaff() {
        /* Find the topmost note in the chord */
        WhiteNote topnote = notedata[ notedata.length-1 ].whitenote;

        /* The stem.End is the note position where the stem ends.
         * Check if the stem end is higher than the top note.
         */
        if (stem1 != null)
            topnote = WhiteNote.Max(topnote, stem1.getEnd());
        if (stem2 != null)
            topnote = WhiteNote.Max(topnote, stem2.getEnd());

        int dist = topnote.Dist(WhiteNote.Top(clef)) * SheetMusic.NoteHeight/2;
        int result = 0;
        if (dist > 0)
            result = dist;

        /* Check if any accidental symbols extend above the staff */
        for (AccidSymbol symbol : accidsymbols) {
            if (symbol.getAboveStaff() > result) {
                result = symbol.getAboveStaff();
            }
        }
        return result;
    }

    /** Get the number of pixels this symbol extends below the staff. Used
     *  to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    public int getBelowStaff() {
        /* Find the bottom note in the chord */
        WhiteNote bottomnote = notedata[0].whitenote;

        /* The stem.End is the note position where the stem ends.
         * Check if the stem end is lower than the bottom note.
         */
        if (stem1 != null)
            bottomnote = WhiteNote.Min(bottomnote, stem1.getEnd());
        if (stem2 != null)
            bottomnote = WhiteNote.Min(bottomnote, stem2.getEnd());

        int dist = WhiteNote.Bottom(clef).Dist(bottomnote) *
                   SheetMusic.NoteHeight/2;

        int result = 0;
        if (dist > 0)
            result = dist;

        /* Check if any accidental symbols extend below the staff */ 
        for (AccidSymbol symbol : accidsymbols) {
            if (symbol.getBelowStaff() > result) {
                result = symbol.getBelowStaff();
            }
        }
        return result;
    }


    /** Get the name for this note */
    private String NoteName(int notenumber, WhiteNote whitenote) {
        if (sheetmusic.getShowNoteLetters() == MidiOptions.NoteNameLetter) {
            return Letter(notenumber, whitenote);
        }
        else if (sheetmusic.getShowNoteLetters() == MidiOptions.NoteNameFixedDoReMi) {
            String[] fixedDoReMi = {
                "La", "Li", "Ti", "Do", "Di", "Re", "Ri", "Mi", "Fa", "Fi", "So", "Si"
            };
            int notescale = NoteScale.FromNumber(notenumber);
            return fixedDoReMi[notescale];
        }
        else if (sheetmusic.getShowNoteLetters() == MidiOptions.NoteNameMovableDoReMi) {
            String[] fixedDoReMi = {
                "La", "Li", "Ti", "Do", "Di", "Re", "Ri", "Mi", "Fa", "Fi", "So", "Si"
            };
            int mainscale = sheetmusic.getMainKey().Notescale();
            int diff = NoteScale.C - mainscale;
            notenumber += diff;
            if (notenumber < 0) {
                notenumber += 12;
            }
            int notescale = NoteScale.FromNumber(notenumber);
            return fixedDoReMi[notescale];
        }
        else if (sheetmusic.getShowNoteLetters() == MidiOptions.NoteNameFixedNumber) {
            String[] num = {
                "10", "11", "12", "1", "2", "3", "4", "5", "6", "7", "8", "9"
            };
            int notescale = NoteScale.FromNumber(notenumber);
            return num[notescale];
        }
        else if (sheetmusic.getShowNoteLetters() == MidiOptions.NoteNameMovableNumber) {
            String[] num = {
                "10", "11", "12", "1", "2", "3", "4", "5", "6", "7", "8", "9"
            };
            int mainscale = sheetmusic.getMainKey().Notescale();
            int diff = NoteScale.C - mainscale;
            notenumber += diff;
            if (notenumber < 0) {
                notenumber += 12;
            }
            int notescale = NoteScale.FromNumber(notenumber);
            return num[notescale];
        }
        else {
            return "";
        }
    }


    /** Get the letter (A, A#, Bb) representing this note */
    private String Letter(int notenumber, WhiteNote whitenote) {
        int notescale = NoteScale.FromNumber(notenumber);
        switch(notescale) {
            case NoteScale.A: return "A";
            case NoteScale.B: return "B";
            case NoteScale.C: return "C";
            case NoteScale.D: return "D";
            case NoteScale.E: return "E";
            case NoteScale.F: return "F";
            case NoteScale.G: return "G";
            case NoteScale.Asharp:
                if (whitenote.getLetter() == WhiteNote.A)
                    return "A#";
                else
                    return "Bb";
            case NoteScale.Csharp:
                if (whitenote.getLetter() == WhiteNote.C)
                    return "C#";
                else
                    return "Db";
            case NoteScale.Dsharp:
                if (whitenote.getLetter() == WhiteNote.D)
                    return "D#";
                else
                    return "Eb";
            case NoteScale.Fsharp:
                if (whitenote.getLetter() == WhiteNote.F)
                    return "F#";
                else
                    return "Gb";
            case NoteScale.Gsharp:
                if (whitenote.getLetter() == WhiteNote.G)
                    return "G#";
                else
                    return "Ab";
            default:
                return "";
        }
    }

    /** Draw the Chord Symbol:
     * - Draw the accidental symbols.
     * - Draw the black circle notes.
     * - Draw the stems.
     *   @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    public void Draw(Canvas canvas, Paint paint, int ytop) {
        paint.setStyle(Paint.Style.STROKE);

        /* Align the chord to the right */
        canvas.translate(getWidth() - getMinWidth(), 0);

        /* Draw the accidentals. */
        WhiteNote topstaff = WhiteNote.Top(clef);
        int xpos = DrawAccid(canvas, paint, ytop);

        /* Draw the notes */
        canvas.translate(xpos, 0);
        DrawNotes(canvas, paint, ytop, topstaff);

        if (sheetmusic != null && sheetmusic.getShowNoteLetters() != 0) {
            DrawNoteLetters(canvas, paint, ytop, topstaff);
        }

        /* Draw the stems */
        if (stem1 != null)
            stem1.Draw(canvas, paint, ytop, topstaff);
        if (stem2 != null)
            stem2.Draw(canvas, paint, ytop, topstaff);

        canvas.translate(-xpos, 0);
        canvas.translate(-(getWidth() - getMinWidth()), 0);
    }

    /* Draw the accidental symbols.  If two symbols overlap (if they
     * are less than 6 notes apart), we cannot draw the symbol directly
     * above the previous one.  Instead, we must shift it to the right.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     * @return The x pixel width used by all the accidentals.
     */
    public int DrawAccid(Canvas canvas, Paint paint, int ytop) {
        int xpos = 0;

        AccidSymbol prev = null;
        for (AccidSymbol symbol : accidsymbols) {
            if (prev != null && symbol.getNote().Dist(prev.getNote()) < 6) {
                xpos += symbol.getWidth();
            }
            canvas.translate(xpos, 0);
            symbol.Draw(canvas, paint, ytop);
            canvas.translate(-xpos, 0);
            prev = symbol;
        }
        if (prev != null) {
            xpos += prev.getWidth();
        }
        return xpos;
    }

    /** Draw the black circle notes.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     * @param topstaff The white note of the top of the staff.
     */
    public void DrawNotes(Canvas canvas, Paint paint, int ytop, WhiteNote topstaff) {
        paint.setStrokeWidth(1);
        for (NoteData note : notedata) {
            /* Get the x,y position to draw the note */
            int ynote = ytop + topstaff.Dist(note.whitenote) * 
                        SheetMusic.NoteHeight/2;

            int xnote = SheetMusic.LineSpace/4;
            if (!note.leftside)
                xnote += SheetMusic.NoteWidth;

            /* Draw rotated ellipse.  You must first translate (0,0)
             * to the center of the ellipse.
             */
            canvas.translate(xnote + SheetMusic.NoteWidth/2 + 1, 
                             ynote - SheetMusic.LineWidth + SheetMusic.NoteHeight/2);
            canvas.rotate(-45);

            if (sheetmusic != null) {
                paint.setColor( sheetmusic.NoteColor(note.number) );
            }
            else {
                paint.setColor(Color.BLACK);
            }

            if (note.duration == NoteDuration.Whole || 
                note.duration == NoteDuration.Half ||
                note.duration == NoteDuration.DottedHalf) {

                RectF rect = new RectF(-SheetMusic.NoteWidth/2, -SheetMusic.NoteHeight/2,
                                       -SheetMusic.NoteWidth/2 + SheetMusic.NoteWidth, 
                                       -SheetMusic.NoteHeight/2 + SheetMusic.NoteHeight-1);
                canvas.drawOval(rect, paint);
                rect = new RectF(-SheetMusic.NoteWidth/2, -SheetMusic.NoteHeight/2 + 1,
                                 -SheetMusic.NoteWidth/2 +  SheetMusic.NoteWidth, 
                                 -SheetMusic.NoteHeight/2 + 1 + SheetMusic.NoteHeight-2);
                canvas.drawOval(rect, paint);
                rect = new RectF(-SheetMusic.NoteWidth/2, -SheetMusic.NoteHeight/2 + 1,
                                 -SheetMusic.NoteWidth/2 + SheetMusic.NoteWidth, 
                                 -SheetMusic.NoteHeight/2 + 1 + SheetMusic.NoteHeight-3);
                canvas.drawOval(rect, paint);

            }
            else {
                paint.setStyle(Paint.Style.FILL);
                RectF rect = new RectF(-SheetMusic.NoteWidth/2, -SheetMusic.NoteHeight/2,
                                       -SheetMusic.NoteWidth/2 + SheetMusic.NoteWidth, 
                                       -SheetMusic.NoteHeight/2 + SheetMusic.NoteHeight-1);
                canvas.drawOval(rect, paint);
                paint.setStyle(Paint.Style.STROKE);
            }

            paint.setColor(Color.BLACK);

            canvas.rotate(45);
            canvas.translate(- (xnote + SheetMusic.NoteWidth/2 + 1), 
                             - (ynote - SheetMusic.LineWidth + SheetMusic.NoteHeight/2));

            /* Draw a dot if this is a dotted duration. */
            if (note.duration == NoteDuration.DottedHalf ||
                note.duration == NoteDuration.DottedQuarter ||
                note.duration == NoteDuration.DottedEighth) {

                RectF rect = new RectF(xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace/3, 
                                       ynote + SheetMusic.LineSpace/3, 
                                       xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace/3 + 4, 
                                       ynote + SheetMusic.LineSpace/3 + 4);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawOval(rect, paint);
                paint.setStyle(Paint.Style.STROKE);
            }

            /* Draw horizontal lines if note is above/below the staff */
            WhiteNote top = topstaff.Add(1);
            int dist = note.whitenote.Dist(top);
            int y = ytop - SheetMusic.LineWidth;

            if (dist >= 2) {
                for (int i = 2; i <= dist; i += 2) {
                    y -= SheetMusic.NoteHeight;
                    canvas.drawLine(xnote - SheetMusic.LineSpace/4, y, 
                                    xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace/4, 
                                    y, paint);
                }
            }

            WhiteNote bottom = top.Add(-8);
            y = ytop + (SheetMusic.LineSpace + SheetMusic.LineWidth) * 4 - 1;
            dist = bottom.Dist(note.whitenote);
            if (dist >= 2) {
                for (int i = 2; i <= dist; i+= 2) {
                    y += SheetMusic.NoteHeight;
                    canvas.drawLine(xnote - SheetMusic.LineSpace/4, y, 
                                    xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace/4,
                                    y, paint);
                }
            }
            /* End drawing horizontal lines */

        }
    }

    /** Draw the note letters (A, A#, Bb, etc) next to the note circles.
     * @param ytop The y location (in pixels) where the top of the staff starts.
     * @param topstaff The white note of the top of the staff.
     */
    public void DrawNoteLetters(Canvas canvas, Paint paint, int ytop, WhiteNote topstaff) {
        boolean overlap = NotesOverlap(notedata, 0, notedata.length);
        paint.setStrokeWidth(1);
        paint.setColor(SheetMusic.getTextColor());

        for (NoteData note : notedata) {
            if (!note.leftside) {
                // There's not enough pixel room to show the letter
                continue;
            }

            // Get the x,y position to draw the note 
            int ynote = ytop + topstaff.Dist(note.whitenote) * 
                        SheetMusic.NoteHeight/2;

            // Draw the letter to the right side of the note 
            int xnote = SheetMusic.NoteWidth + SheetMusic.NoteWidth/2;

            if (note.duration == NoteDuration.DottedHalf ||
                note.duration == NoteDuration.DottedQuarter ||
                note.duration == NoteDuration.DottedEighth || overlap) {

                xnote += SheetMusic.NoteWidth/2;
            } 
            canvas.drawText(NoteName(note.number, note.whitenote),
                            xnote,
                            ynote + SheetMusic.NoteHeight/2, paint);
        }
    }


    /** Return true if the chords can be connected, where their stems are
     * joined by a horizontal beam. In order to create the beam:
     *
     * - The chords must be in the same measure.
     * - The chord stems should not be a dotted duration.
     * - The chord stems must be the same duration, with one exception
     *   (Dotted Eighth to Sixteenth).
     * - The stems must all point in the same direction (up or down).
     * - The chord cannot already be part of a beam.
     *
     * - 6-chord beams must be 8th notes in 3/4, 6/8, or 6/4 time
     * - 3-chord beams must be either triplets, or 8th notes (12/8 time signature)
     * - 4-chord beams are ok for 2/2, 2/4 or 4/4 time, any duration
     * - 4-chord beams are ok for other times if the duration is 16th
     * - 2-chord beams are ok for any duration
     *
     * If startQuarter is true, the first note should start on a quarter note
     * (only applies to 2-chord beams).
     */
    public static
    boolean CanCreateBeam(ChordSymbol[] chords, TimeSignature time, boolean startQuarter) {
        int numChords = chords.length;
        Stem firstStem = chords[0].getStem();
        Stem lastStem = chords[chords.length-1].getStem();
        if (firstStem == null || lastStem == null) {
            return false;
        }
        int measure = chords[0].getStartTime() / time.getMeasure();
        NoteDuration dur = firstStem.getDuration();
        NoteDuration dur2 = lastStem.getDuration();

        boolean dotted8_to_16 = false;
        if (chords.length == 2 && dur == NoteDuration.DottedEighth &&
            dur2 == NoteDuration.Sixteenth) {
            dotted8_to_16 = true;
        }

        if (dur == NoteDuration.Whole || dur == NoteDuration.Half ||
            dur == NoteDuration.DottedHalf || dur == NoteDuration.Quarter ||
            dur == NoteDuration.DottedQuarter ||
            (dur == NoteDuration.DottedEighth && !dotted8_to_16)) {

            return false;
        }

        if (numChords == 6) {
            if (dur != NoteDuration.Eighth) {
                return false;
            }
            boolean correctTime =
               ((time.getNumerator() == 3 && time.getDenominator() == 4) ||
                (time.getNumerator() == 6 && time.getDenominator() == 8) ||
                (time.getNumerator() == 6 && time.getDenominator() == 4) );
            if (!correctTime) {
                return false;
            }

            if (time.getNumerator() == 6 && time.getDenominator() == 4) {
                /* first chord must start at 1st or 4th quarter note */
                int beat = time.getQuarter() * 3;
                if ((chords[0].getStartTime() % beat) > time.getQuarter()/6) {
                    return false;
                }
            }
        }
        else if (numChords == 4) {
            if (time.getNumerator() == 3 && time.getDenominator() == 8) {
                return false;
            }
            boolean correctTime =
              (time.getNumerator() == 2 || time.getNumerator() == 4 || time.getNumerator() == 8);
            if (!correctTime && dur != NoteDuration.Sixteenth) {
                return false;
            }

            /* chord must start on quarter note */
            int beat = time.getQuarter();
            if (dur == NoteDuration.Eighth) {
                /* 8th note chord must start on 1st or 3rd quarter beat */
                beat = time.getQuarter() * 2;
            }
            else if (dur == NoteDuration.ThirtySecond) {
                /* 32nd note must start on an 8th beat */
                beat = time.getQuarter() / 2;
            }

            if ((chords[0].getStartTime() % beat) > time.getQuarter()/6) {
                return false;
            }
        }
        else if (numChords == 3) {
            boolean valid = (dur == NoteDuration.Triplet) ||
                          (dur == NoteDuration.Eighth &&
                           time.getNumerator() == 12 && time.getDenominator() == 8);
            if (!valid) {
                return false;
            }

            /* chord must start on quarter note */
            int beat = time.getQuarter();
            if (time.getNumerator() == 12 && time.getDenominator() == 8) {
                /* In 12/8 time, chord must start on 3*8th beat */
                beat = time.getQuarter()/2 * 3;
            }
            if ((chords[0].getStartTime() % beat) > time.getQuarter()/6) {
                return false;
            }
        }
        else if (numChords == 2) {
            if (startQuarter) {
                int beat = time.getQuarter();
                if ((chords[0].getStartTime() % beat) > time.getQuarter()/6) {
                    return false;
                }
            }
        }

        for (ChordSymbol chord : chords) {
            if ((chord.getStartTime() / time.getMeasure()) != measure)
                return false;
            if (chord.getStem() == null)
                return false;
            if (chord.getStem().getDuration() != dur && !dotted8_to_16)
                return false;
            if (chord.getStem().IsBeam())
                return false;
        }

        /* Check that all stems can point in same direction */
        boolean hasTwoStems = false;
        int direction = Stem.Up;
        for (ChordSymbol chord : chords) {
            if (chord.getHasTwoStems()) {
                if (hasTwoStems && chord.getStem().getDirection() != direction) {
                    return false;
                }
                hasTwoStems = true;
                direction = chord.getStem().getDirection();
            }
        }

        /* Get the final stem direction */
        if (!hasTwoStems) {
            WhiteNote note1;
            WhiteNote note2;
            note1 = (firstStem.getDirection() == Stem.Up ? firstStem.getTop() : firstStem.getBottom());
            note2 = (lastStem.getDirection() == Stem.Up ? lastStem.getTop() : lastStem.getBottom());
            direction = StemDirection(note1, note2, chords[0].getClef());
        }

        /* If the notes are too far apart, don't use a beam */
        if (direction == Stem.Up) {
            return Math.abs(firstStem.getTop().Dist(lastStem.getTop())) < 11;
        }
        else {
            return Math.abs(firstStem.getBottom().Dist(lastStem.getBottom())) < 11;
        }
    }


    /** Connect the chords using a horizontal beam.
     *
     * spacing is the horizontal distance (in pixels) between the right side
     * of the first chord, and the right side of the last chord.
     *
     * To make the beam:
     * - Change the stem directions for each chord, so they match.
     * - In the first chord, pass the stem location of the last chord, and
     *   the horizontal spacing to that last stem.
     * - Mark all chords (except the first) as "receiver" pairs, so that
     *   they don't draw a curvy stem.
     */
    public static
    void CreateBeam(ChordSymbol[] chords, int spacing) {
        Stem firstStem = chords[0].getStem();
        Stem lastStem = chords[chords.length-1].getStem();

        /* Calculate the new stem direction */
        int newdirection = -1;
        for (ChordSymbol chord : chords) {
            if (chord.getHasTwoStems()) {
                newdirection = chord.getStem().getDirection();
                break;
            }
        }

        if (newdirection == -1) {
            WhiteNote note1;
            WhiteNote note2;
            note1 = (firstStem.getDirection() == Stem.Up ? firstStem.getTop() : firstStem.getBottom());
            note2 = (lastStem.getDirection() == Stem.Up ? lastStem.getTop() : lastStem.getBottom());
            newdirection = StemDirection(note1, note2, chords[0].getClef());
        }
        for (ChordSymbol chord : chords) {
            chord.getStem().setDirection(newdirection);
        }

        if (chords.length == 2) {
            BringStemsCloser(chords);
        }
        else {
            LineUpStemEnds(chords);
        }

        firstStem.SetPair(lastStem, spacing);
        for (int i = 1; i < chords.length; i++) {
            chords[i].getStem().setReceiver(true);
        }
    }

    /** We're connecting the stems of two chords using a horizontal beam.
     *  Adjust the vertical endpoint of the stems, so that they're closer
     *  together.  For a dotted 8th to 16th beam, increase the stem of the
     *  dotted eighth, so that it's as long as a 16th stem.
     */
    static void
    BringStemsCloser(ChordSymbol[] chords) {
        Stem firstStem = chords[0].getStem();
        Stem lastStem = chords[1].getStem();

        /* If we're connecting a dotted 8th to a 16th, increase
         * the stem end of the dotted eighth.
         */
        if (firstStem.getDuration() == NoteDuration.DottedEighth &&
            lastStem.getDuration() == NoteDuration.Sixteenth) {
            if (firstStem.getDirection() == Stem.Up) {
                firstStem.setEnd(firstStem.getEnd().Add(2));
            }
            else {
                firstStem.setEnd(firstStem.getEnd().Add(-2));
            }
        }

        /* Bring the stem ends closer together */
        int distance = Math.abs(firstStem.getEnd().Dist(lastStem.getEnd()));
        if (firstStem.getDirection() == Stem.Up) {
            if (WhiteNote.Max(firstStem.getEnd(), lastStem.getEnd()) == firstStem.getEnd())
                lastStem.setEnd(lastStem.getEnd().Add(distance/2));
            else
                firstStem.setEnd(firstStem.getEnd().Add(distance/2));
        }
        else {
            if (WhiteNote.Min(firstStem.getEnd(), lastStem.getEnd()) == firstStem.getEnd())
                lastStem.setEnd(lastStem.getEnd().Add(-distance/2));
            else
                firstStem.setEnd(firstStem.getEnd().Add(-distance/2));
        }
    }

    /** We're connecting the stems of three or more chords using a horizontal beam.
     *  Adjust the vertical endpoint of the stems, so that the middle chord stems
     *  are vertically in between the first and last stem.
     */
    static void
    LineUpStemEnds(ChordSymbol[] chords) {
        Stem firstStem = chords[0].getStem();
        Stem lastStem = chords[chords.length-1].getStem();
        Stem middleStem = chords[1].getStem();

        if (firstStem.getDirection() == Stem.Up) {
            /* Find the highest stem. The beam will either:
             * - Slant downwards (first stem is highest)
             * - Slant upwards (last stem is highest)
             * - Be straight (middle stem is highest)
             */
            WhiteNote top = firstStem.getEnd();
            for (ChordSymbol chord : chords) {
                top = WhiteNote.Max(top, chord.getStem().getEnd());
            }
            if (top == firstStem.getEnd() && top.Dist(lastStem.getEnd()) >= 2) {
                firstStem.setEnd(top);
                middleStem.setEnd(top.Add(-1));
                lastStem.setEnd(top.Add(-2));
            }
            else if (top == lastStem.getEnd() && top.Dist(firstStem.getEnd()) >= 2) {
                firstStem.setEnd(top.Add(-2));
                middleStem.setEnd(top.Add(-1));
                lastStem.setEnd(top);
            }
            else {
                firstStem.setEnd(top);
                middleStem.setEnd(top);
                lastStem.setEnd(top);
            }
        }
        else {
            /* Find the bottommost stem. The beam will either:
             * - Slant upwards (first stem is lowest)
             * - Slant downwards (last stem is lowest)
             * - Be straight (middle stem is highest)
             */
            WhiteNote bottom = firstStem.getEnd();
            for (ChordSymbol chord : chords) {
                bottom = WhiteNote.Min(bottom, chord.getStem().getEnd());
            }

            if (bottom == firstStem.getEnd() && lastStem.getEnd().Dist(bottom) >= 2) {
                middleStem.setEnd(bottom.Add(1));
                lastStem.setEnd(bottom.Add(2));
            }
            else if (bottom == lastStem.getEnd() && firstStem.getEnd().Dist(bottom) >= 2) {
                middleStem.setEnd(bottom.Add(1));
                firstStem.setEnd(bottom.Add(2));
            }
            else {
                firstStem.setEnd(bottom);
                middleStem.setEnd(bottom);
                lastStem.setEnd(bottom);
            }
        }

        /* All middle stems have the same end */
        for (int i = 1; i < chords.length-1; i++) {
            Stem stem = chords[i].getStem();
            stem.setEnd(middleStem.getEnd());
        }
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(String.format(
                "ChordSymbol clef=%1$s start=%2$s end=%3$s width=%4$s hastwostems=%5$s ",
                clef, getStartTime(), getEndTime(), getWidth(), hastwostems));
        for (AccidSymbol symbol : accidsymbols) {
            result.append(symbol.toString()).append(" ");
        }
        for (NoteData note : notedata) {
            result.append(String.format("Note whitenote=%1$s duration=%2$s leftside=%3$s ",
                    note.whitenote, note.duration, note.leftside));
        }
        if (stem1 != null) {
            result.append(stem1.toString()).append(" ");
        }
        if (stem2 != null) {
            result.append(stem2.toString()).append(" ");
        }
        return result.toString();
    }

    public NoteData[] getNotedata() {
        return notedata;
    }
}


