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

package com.midisheetmusic;

import java.util.*;
import java.io.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.util.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.os.*;
import android.media.*;


/** @class MidiPlayer
 *
 * The MidiPlayer is the panel at the top used to play the sound
 * of the midi file.  It consists of:
 *
 * - The Rewind button
 * - The Play/Pause button
 * - The Stop button
 * - The Fast Forward button
 * - The Playback speed bar
 *
 * The sound of the midi file depends on
 * - The MidiOptions (taken from the menus)
 *   Which tracks are selected
 *   How much to transpose the keys by
 *   What instruments to use per track
 * - The tempo (from the Speed bar)
 * - The volume
 *
 * The MidiFile.ChangeSound() method is used to create a new midi file
 * with these options.  The mciSendString() function is used for 
 * playing, pausing, and stopping the sound.
 *
 * For shading the notes during playback, the method
 * SheetMusic.ShadeNotes() is used.  It takes the current 'pulse time',
 * and determines which notes to shade.
 */
public class MidiPlayer extends LinearLayout {
    static Bitmap rewindImage;           /** The rewind image */
    static Bitmap playImage;             /** The play image */
    static Bitmap pauseImage;            /** The pause image */
    static Bitmap stopImage;             /** The stop image */
    static Bitmap fastFwdImage;          /** The fast forward image */
    static Bitmap volumeImage;           /** The volume image */
    static Bitmap settingsImage;         /** The settings image */

    private ImageButton rewindButton;    /** The rewind button */
    private ImageButton playButton;      /** The play/pause button */
    private ImageButton stopButton;      /** The stop button */
    private ImageButton fastFwdButton;   /** The fast forward button */
    private ImageButton settingsButton;  /** The fast forward button */
    private TextView speedText;          /** The "Speed %" label */
    private SeekBar speedBar;    /** The seekbar for controlling the playback speed */

    int playstate;               /** The playing state of the Midi Player */
    final int stopped   = 1;     /** Currently stopped */
    final int playing   = 2;     /** Currently playing music */
    final int paused    = 3;     /** Currently paused */
    final int initStop  = 4;     /** Transitioning from playing to stop */
    final int initPause = 5;     /** Transitioning from playing to pause */

    final String tempSoundFile = "playing.mid"; /** The filename to play sound from */

    MediaPlayer player;         /** For playing the audio */
    MidiFile midifile;          /** The midi file to play */
    MidiOptions options;        /** The sound options for playing the midi file */
    double pulsesPerMsec;       /** The number of pulses per millisec */
    SheetMusic sheet;           /** The sheet music to shade while playing */
    Piano piano;                /** The piano to shade while playing */
    Handler timer;              /** Timer used to update the sheet music while playing */
    long startTime;             /** Absolute time when music started playing (msec) */
    double startPulseTime;      /** Time (in pulses) when music started playing */
    double currentPulseTime;    /** Time (in pulses) music is currently at */
    double prevPulseTime;       /** Time (in pulses) music was last at */
    Activity activity;          /** The parent activity. */

    
    /** Load the play/pause/stop button images */
    public static void LoadImages(Context context) {
        if (rewindImage != null) {
            return;
        }
        Resources res = context.getResources();
        rewindImage = BitmapFactory.decodeResource(res, R.drawable.rewind);
        playImage = BitmapFactory.decodeResource(res, R.drawable.play);
        pauseImage = BitmapFactory.decodeResource(res, R.drawable.pause);
        stopImage = BitmapFactory.decodeResource(res, R.drawable.stop);
        fastFwdImage = BitmapFactory.decodeResource(res, R.drawable.fastforward);
        settingsImage = BitmapFactory.decodeResource(res, R.drawable.settings);
    }


    /** Create a new MidiPlayer, displaying the play/stop buttons, and the
     *  speed bar.  The midifile and sheetmusic are initially null.
     */
    public MidiPlayer(Activity activity) {
        super(activity);
        LoadImages(activity);
        this.activity = activity;
        this.midifile = null;
        this.options = null;
        this.sheet = null;
        playstate = stopped;
        startTime = SystemClock.uptimeMillis();
        startPulseTime = 0;
        currentPulseTime = 0;
        prevPulseTime = -10;
        this.setPadding(0, 0, 0, 0);
        CreateButtons();

        int screenwidth = activity.getWindowManager().getDefaultDisplay().getWidth();
        int screenheight = activity.getWindowManager().getDefaultDisplay().getHeight();
        Point newsize = MidiPlayer.getPreferredSize(screenwidth, screenheight);
        resizeButtons(newsize.x, newsize.y);
        player = new MediaPlayer();
        setBackgroundColor(Color.BLACK);
    }

    /** Get the preferred width/height given the screen width/height */
    public static Point getPreferredSize(int screenwidth, int screenheight) {
        int height = (int) (5.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        height = height * 2/3 ;
        Point result = new Point(screenwidth, height);
        return result;
    }

    /** Determine the measured width and height.
     *  Resize the individual buttons according to the new width/height.
     */
    @Override
    protected void onMeasure(int widthspec, int heightspec) {
        super.onMeasure(widthspec, heightspec);
        int screenwidth = MeasureSpec.getSize(widthspec);
        int screenheight = MeasureSpec.getSize(heightspec);

        /* Make the button height 2/3 the piano WhiteKeyHeight */
        int width = screenwidth;
        int height = (int) (5.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        height = height * 2/3;
        setMeasuredDimension(width, height);
    }

    /** When this view is resized, adjust the button sizes */
    @Override
    protected void 
    onSizeChanged(int newwidth, int newheight, int oldwidth, int oldheight) {
        resizeButtons(newwidth, newheight);
        super.onSizeChanged(newwidth, newheight, oldwidth, oldheight);
    }
     

    /** Create the rewind, play, stop, and fast forward buttons */
    void CreateButtons() {
        this.setOrientation(LinearLayout.HORIZONTAL);

        /* Create the rewind button */
        rewindButton = new ImageButton(activity);
        rewindButton.setBackgroundColor(Color.BLACK);
        rewindButton.setImageBitmap(rewindImage);
        rewindButton.setScaleType(ImageView.ScaleType.FIT_XY);
        rewindButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Rewind();
            }
        });
        this.addView(rewindButton);

        /* Create the stop button */
        stopButton = new ImageButton(activity);
        stopButton.setBackgroundColor(Color.BLACK);
        stopButton.setImageBitmap(stopImage);
        stopButton.setScaleType(ImageView.ScaleType.FIT_XY);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Stop();
            }
        });
        this.addView(stopButton);

        
        /* Create the play button */
        playButton = new ImageButton(activity);
        playButton.setBackgroundColor(Color.BLACK);
        playButton.setImageBitmap(playImage);
        playButton.setScaleType(ImageView.ScaleType.FIT_XY);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Play();
            }
        });
        this.addView(playButton);        
        
        /* Create the fastFwd button */        
        fastFwdButton = new ImageButton(activity);
        fastFwdButton.setBackgroundColor(Color.BLACK);
        fastFwdButton.setImageBitmap(fastFwdImage);
        fastFwdButton.setScaleType(ImageView.ScaleType.FIT_XY);
        fastFwdButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FastForward();
            }
        });
        this.addView(fastFwdButton);


        /* Create the Speed bar */
        speedText = new TextView(activity);
        speedText.setText("   Speed: 100%   ");
        speedText.setGravity(Gravity.CENTER);
        this.addView(speedText);

        speedBar = new SeekBar(activity);
        speedBar.setMax(150);
        speedBar.setProgress(100);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                speedText.setText("   Speed: " + String.format(Locale.US, "%03d", progress) + "%   ");
            }
            public void onStartTrackingTouch(SeekBar bar) {
            }
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        this.addView(speedBar);

        /* Create the settings button */        
        settingsButton = new ImageButton(activity);
        settingsButton.setBackgroundColor(Color.BLACK);
        settingsButton.setImageBitmap(settingsImage);
        settingsButton.setScaleType(ImageView.ScaleType.FIT_XY);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.openOptionsMenu();
            }
        });
        this.addView(settingsButton);

        /* Initialize the timer used for playback, but don't start
         * the timer yet (enabled = false).
         */
        timer = new Handler();
    }

    void resizeButtons(int newwidth, int newheight) {
        int buttonheight = newheight;
        int pad = buttonheight/6;
        rewindButton.setPadding(pad, pad, pad, pad);
        stopButton.setPadding(pad, pad, pad, pad);
        playButton.setPadding(pad, pad, pad, pad);
        fastFwdButton.setPadding(pad, pad, pad, pad);
        settingsButton.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams params;
        
        params = new LinearLayout.LayoutParams(buttonheight, buttonheight);
        params.width = buttonheight;
        params.height = buttonheight;
        params.bottomMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        params.leftMargin = buttonheight/6;

        rewindButton.setLayoutParams(params);
        
        params = new LinearLayout.LayoutParams(buttonheight, buttonheight);
        params.bottomMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        params.leftMargin = 0;

        playButton.setLayoutParams(params);
        stopButton.setLayoutParams(params);
        fastFwdButton.setLayoutParams(params);

        params = (LinearLayout.LayoutParams) speedText.getLayoutParams();
        params.height = buttonheight;
        speedText.setLayoutParams(params);
        
        params = new LinearLayout.LayoutParams(buttonheight * 5, buttonheight);
        params.width = buttonheight * 5;
        params.bottomMargin = 0;
        params.leftMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        speedBar.setLayoutParams(params);
        speedBar.setPadding(pad, pad, pad, pad);

        params = new LinearLayout.LayoutParams(buttonheight, buttonheight);
        params.bottomMargin = 0;
        params.topMargin = 0;
        params.rightMargin = 0;
        params.leftMargin = buttonheight/8;
        settingsButton.setLayoutParams(params);
    }
    
    public void SetPiano(Piano p) {
        piano = p;
    }

    /** The MidiFile and/or SheetMusic has changed. Stop any playback sound,
     *  and store the current midifile and sheet music.
     */
    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {

        /* If we're paused, and using the same midi file, redraw the
         * highlighted notes.
         */
        if ((file == midifile && midifile != null && playstate == paused)) {
            options = opt;
            sheet = s;
            sheet.ShadeNotes((int)currentPulseTime, (int)-1, SheetMusic.DontScroll);

            /* We have to wait some time (200 msec) for the sheet music
             * to scroll and redraw, before we can re-shade.
             */
            timer.removeCallbacks(TimerCallback);
            timer.postDelayed(ReShade, 500);
        }
        else {
            Stop();
            midifile = file;
            options = opt;
            sheet = s;
        }
    }

    /** If we're paused, reshade the sheet music and piano. */
    Runnable ReShade = new Runnable() {
      public void run() {
        if (playstate == paused || playstate == stopped) {
            sheet.ShadeNotes((int)currentPulseTime, (int)-10, SheetMusic.ImmediateScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
        }
      }
    };


    /** Return the number of tracks selected in the MidiOptions.
     *  If the number of tracks is 0, there is no sound to play.
     */
    private int numberTracks() {
        int count = 0;
        for (int i = 0; i < options.tracks.length; i++) {
            if (options.tracks[i] && !options.mute[i]) {
                count += 1;
            }
        }
        return count;
    }

    /** Create a new midi file with all the MidiOptions incorporated.
     *  Save the new file to playing.mid, and store
     *  this temporary filename in tempSoundFile.
     */ 
    private void CreateMidiFile() {
        double inverse_tempo = 1.0 / midifile.getTime().getTempo();
        double inverse_tempo_scaled = inverse_tempo * speedBar.getProgress() / 100.0;
        // double inverse_tempo_scaled = inverse_tempo * 100.0 / 100.0;
        options.tempo = (int)(1.0 / inverse_tempo_scaled);
        pulsesPerMsec = midifile.getTime().getQuarter() * (1000.0 / options.tempo);

        try {
            FileOutputStream dest = activity.openFileOutput(tempSoundFile, Context.MODE_PRIVATE);
            midifile.ChangeSound(dest, options);
            dest.close();
            // checkFile(tempSoundFile);
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "Error: Unable to create MIDI file for playing.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void checkFile(String name) {
        try {
            FileInputStream in = activity.openFileInput(name);
            byte[] data = new byte[4096];
            int total = 0, len = 0;
            while (true) {
                len = in.read(data, 0, 4096);
                if (len > 0)
                    total += len;
                else
                    break;
            } 
            in.close();
            data = new byte[total];
            in = activity.openFileInput(name);
            int offset = 0;
            while (offset < total) {
                len = in.read(data, offset, total - offset);
                if (len > 0)
                    offset += len;
            }
            in.close();
            MidiFile testmidi = new MidiFile(data, name); 
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "CheckFile: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        catch (MidiFileException e) {
            Toast toast = Toast.makeText(activity, "CheckFile midi: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        } 
    }


    /** Play the sound for the given MIDI file */
    private void PlaySound(String filename) {
        if (player == null)
            return;
        try {
            FileInputStream input = activity.openFileInput(filename);
            player.reset();
            player.setDataSource(input.getFD());
            input.close();
            player.prepare();
            player.start();
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "Error: Unable to play MIDI sound", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /** Stop playing the MIDI music */
    private void StopSound() {
        if (player == null)
            return;
        player.stop();
        player.reset();
    }


    /** The callback for the play button.
     *  If we're stopped or pause, then play the midi file.
     */
    private void Play() {
        if (midifile == null || sheet == null || numberTracks() == 0) {
            return;
        }
        else if (playstate == initStop || playstate == initPause || playstate == playing) {
            return;
        }
        // playstate is stopped or paused

        // Hide the midi player, wait a little for the view
        // to refresh, and then start playing
        this.setVisibility(View.GONE);
        timer.removeCallbacks(TimerCallback);
        timer.postDelayed(DoPlay, 1000);
    }

    Runnable DoPlay = new Runnable() {
      public void run() {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /* The startPulseTime is the pulse time of the midi file when
         * we first start playing the music.  It's used during shading.
         */
        if (options.playMeasuresInLoop) {
            /* If we're playing measures in a loop, make sure the
             * currentPulseTime is somewhere inside the loop measures.
             */
            int measure = (int)(currentPulseTime / midifile.getTime().getMeasure());
            if ((measure < options.playMeasuresInLoopStart) ||
                (measure > options.playMeasuresInLoopEnd)) {
                currentPulseTime = options.playMeasuresInLoopStart * midifile.getTime().getMeasure();
            }
            startPulseTime = currentPulseTime;
            options.pauseTime = (int)(currentPulseTime - options.shifttime);
        }
        else if (playstate == paused) {
            startPulseTime = currentPulseTime;
            options.pauseTime = (int)(currentPulseTime - options.shifttime);
        }
        else {
            options.pauseTime = 0;
            startPulseTime = options.shifttime;
            currentPulseTime = options.shifttime;
            prevPulseTime = options.shifttime - midifile.getTime().getQuarter();
        }

        CreateMidiFile();
        playstate = playing;
        PlaySound(tempSoundFile);
        startTime = SystemClock.uptimeMillis();

        timer.removeCallbacks(TimerCallback);
        timer.removeCallbacks(ReShade);
        timer.postDelayed(TimerCallback, 100);

        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
        return;
      }
    };


    /** The callback for pausing playback.
     *  If we're currently playing, pause the music.
     *  The actual pause is done when the timer is invoked.
     */
    public void Pause() {
        this.setVisibility(View.VISIBLE);
        LinearLayout layout = (LinearLayout)this.getParent();
        layout.requestLayout();
        this.requestLayout();
        this.invalidate();

        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if (midifile == null || sheet == null || numberTracks() == 0) {
            return;
        }
        else if (playstate == playing) {
            playstate = initPause;
            return;
        }
    }


    /** The callback for the Stop button.
     *  If playing, initiate a stop and wait for the timer to finish.
     *  Then do the actual stop.
     */
    void Stop() {
        this.setVisibility(View.VISIBLE);
        if (midifile == null || sheet == null || playstate == stopped) {
            return;
        }

        if (playstate == initPause || playstate == initStop || playstate == playing) {
            /* Wait for timer to finish */
            playstate = initStop;
            DoStop();
        }
        else if (playstate == paused) {
            DoStop();
        }
    }

    /** Perform the actual stop, by stopping the sound,
     *  removing any shading, and clearing the state.
     */
    void DoStop() { 
        playstate = stopped;
        timer.removeCallbacks(TimerCallback);
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)prevPulseTime);
        piano.ShadeNotes(-10, (int)currentPulseTime);
        startPulseTime = 0;
        currentPulseTime = 0;
        prevPulseTime = 0;
        setVisibility(View.VISIBLE);
        StopSound();
    }

    /** Rewind the midi music back one measure.
     *  The music must be in the paused state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So to rewind, just decrease the currentPulseTime,
     *  and re-shade the sheet music.
     */
    void Rewind() {
        if (midifile == null || sheet == null || playstate != paused) {
            return;
        }

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);
   
        prevPulseTime = currentPulseTime; 
        currentPulseTime -= midifile.getTime().getMeasure();
        if (currentPulseTime < options.shifttime) {
            currentPulseTime = options.shifttime;
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }
    
    /** Fast forward the midi music by one measure.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So to fast forward, just increase the currentPulseTime,
     *  and re-shade the sheet music.
     */
    void FastForward() {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped) {
            return;
        }
        playstate = paused;

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);
   
        prevPulseTime = currentPulseTime; 
        currentPulseTime += midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) {
            currentPulseTime -= midifile.getTime().getMeasure();
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }


    /** Move the current position to the location clicked.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So, set the currentPulseTime to the position clicked.
     */
    public void MoveToClicked(int x, int y) {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped) {
            return;
        }
        playstate = paused;

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);

        currentPulseTime = sheet.PulseTimeForPoint(new Point(x, y));
        prevPulseTime = currentPulseTime - midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) {
            currentPulseTime -= midifile.getTime().getMeasure();
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }


    /** The callback for the timer. If the midi is still playing, 
     *  update the currentPulseTime and shade the sheet music.  
     *  If a stop or pause has been initiated (by someone clicking
     *  the stop or pause button), then stop the timer.
     */
    Runnable TimerCallback = new Runnable() {
      public void run() {
        if (midifile == null || sheet == null) {
            playstate = stopped;
            return;
        }
        else if (playstate == stopped || playstate == paused) {
            /* This case should never happen */
            return;
        }
        else if (playstate == initStop) {
            return;
        }
        else if (playstate == playing) {
            long msec = SystemClock.uptimeMillis() - startTime;
            prevPulseTime = currentPulseTime;
            currentPulseTime = startPulseTime + msec * pulsesPerMsec;

            /* If we're playing in a loop, stop and restart */
            if (options.playMeasuresInLoop) {
                double nearEndTime = currentPulseTime + pulsesPerMsec*10;
                int measure = (int)(nearEndTime / midifile.getTime().getMeasure());
                if (measure > options.playMeasuresInLoopEnd) {
                    RestartPlayMeasuresInLoop();
                    return;
                }
            }

            /* Stop if we've reached the end of the song */
            if (currentPulseTime > midifile.getTotalPulses()) {
                DoStop();
                return;
            }
            sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
            timer.postDelayed(TimerCallback, 100);
            return;
        }
        else if (playstate == initPause) {
            long msec = SystemClock.uptimeMillis() - startTime;
            StopSound();

            prevPulseTime = currentPulseTime;
            currentPulseTime = startPulseTime + msec * pulsesPerMsec;
            sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
            playstate = paused;
            timer.postDelayed(ReShade, 1000);
            return;
        }
      }
    };


    /** The "Play Measures in a Loop" feature is enabled, and we've reached
     *  the last measure. Stop the sound, unshade the music, and then
     *  start playing again.
     */
    private void RestartPlayMeasuresInLoop() {
        playstate = stopped;
        piano.ShadeNotes(-10, (int)prevPulseTime);
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        currentPulseTime = 0;
        prevPulseTime = -1;
        StopSound();
        timer.postDelayed(DoPlay, 300);
    }

}


