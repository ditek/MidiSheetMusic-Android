package com.midisheetmusic;

/**
 * A listener that allows {@link MidiPlayer} to send a request
 * to {@link SheetMusicActivity} to update the sheet when it
 * changes the settings
 */
public interface SheetUpdateRequestListener {
    void onSheetUpdateRequest();
}
