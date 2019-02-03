/*
 * Copyright (c) 2011-2013 Madhav Vaidyanathan
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

import java.io.*;
import java.util.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.util.Log;
import android.view.*;
import android.content.*;
import org.json.*;
import android.graphics.*;


/** @class RecentSongsActivity
 * The RecentSongsActivity class displays a list of songs
 * that were recently accessed.  The list comes from the
 * SharedPreferences ????
 */
public class RecentSongsActivity extends ListActivity {
    private ArrayList<FileUri> filelist; /* List of recent files opened */


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("MidiSheetMusic: Recent Songs");
        getListView().setBackgroundColor(Color.rgb(0, 0, 0));
        // Load the list of songs
        loadFileList();
        IconArrayAdapter<FileUri> adapter = new IconArrayAdapter<FileUri>(this, android.R.layout.simple_list_item_1, filelist);
        this.setListAdapter(adapter);
    }

    private void loadFileList() {
        filelist = new ArrayList<FileUri>();
        SharedPreferences settings = getSharedPreferences("midisheetmusic.recentFiles", 0);
        String recentFilesString = settings.getString("recentFiles", null);
        if (recentFilesString == null) {
            return;
        }
        try {
            JSONArray jsonArray = new JSONArray(recentFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                FileUri file = FileUri.fromJson(obj, this);
                if (file != null) {
                    filelist.add(file);
                }
            }
        }
        catch (Exception e) {
        }
    }
            
    @Override
    public void onResume() {
        super.onResume();
        loadFileList();
    }

    /** When a user selects a song, open the SheetMusicActivity. */
    @Override
    protected void onListItemClick(ListView parent, View view, int position, long id) {
        super.onListItemClick(parent, view, position, id);
        FileUri file = (FileUri) this.getListAdapter().getItem(position);
        ChooseSongActivity.openFile(file);
    }  
}


